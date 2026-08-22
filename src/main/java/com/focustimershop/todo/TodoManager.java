package com.focustimershop.todo;

import com.focustimershop.FocusTimerShop;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * v1.0.7-beta - To-Do List Manager
 * Manages player's to-do list tasks with persistence
 */
public class TodoManager {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final File TODO_DIR = new File("config/focustimershop/todo");
	
	// In-memory cache: playerUuid -> list of tasks
	private static final Map<UUID, List<TodoTask>> playerTasks = new HashMap<>();
	
	// Window states: playerUuid -> windowId -> state
	private static final Map<UUID, Map<String, FloatingWindowState>> playerWindowStates = new HashMap<>();
	
	static {
		TODO_DIR.mkdirs();
	}
	
	/**
	 * Get all tasks for a player (incomplete first, then completed)
	 * Returns an UNMODIFIABLE view - do not modify directly!
	 */
	public static List<TodoTask> getTasks(UUID playerUuid) {
		if (!playerTasks.containsKey(playerUuid)) {
			loadTasks(playerUuid);
		}
		
		List<TodoTask> tasks = playerTasks.get(playerUuid);
		if (tasks == null) {
			tasks = new ArrayList<>();
			playerTasks.put(playerUuid, tasks);
		}
		
		// IMPORTANT: Sort in-place instead of creating a copy
		// This ensures all TodoListWindow instances see the same data
		tasks.sort((a, b) -> {
			if (a.isCompleted() == b.isCompleted()) {
				return Integer.compare(a.getOrder(), b.getOrder());
			}
			return a.isCompleted() ? 1 : -1; // Incomplete first
		});
		
		return tasks; // Return the SAME list, not a copy
	}
	
	/**
	 * Force reload tasks from disk (useful for debugging or external changes)
	 */
	public static void reloadTasks(UUID playerUuid) {
		playerTasks.remove(playerUuid);
		loadTasks(playerUuid);
	}
	
	/**
	 * Add a new task
	 */
	public static TodoTask addTask(UUID playerUuid, String text) {
		// Get the actual cached list, not the sorted copy
		if (!playerTasks.containsKey(playerUuid)) {
			loadTasks(playerUuid);
		}
		
		List<TodoTask> tasks = playerTasks.get(playerUuid);
		if (tasks == null) {
			tasks = new ArrayList<>();
			playerTasks.put(playerUuid, tasks);
		}
		
		// Calculate next order number for incomplete tasks
		int maxOrder = tasks.stream()
			.filter(t -> !t.isCompleted())
			.mapToInt(TodoTask::getOrder)
			.max()
			.orElse(-1);
		
		TodoTask task = new TodoTask(text);
		task.setOrder(maxOrder + 1);
		tasks.add(task);
		
		FocusTimerShop.LOGGER.info("[TodoManager] Added task '{}' (ID: {}, order: {}) for player {}", 
			text, task.getId(), task.getOrder(), playerUuid);
		
		saveTasks(playerUuid);
		return task;
	}
	
	/**
	 * Update task text
	 */
	public static void updateTaskText(UUID playerUuid, String taskId, String newText) {
		// Get actual cached list
		if (!playerTasks.containsKey(playerUuid)) {
			loadTasks(playerUuid);
		}
		
		List<TodoTask> tasks = playerTasks.get(playerUuid);
		if (tasks != null) {
			tasks.stream()
				.filter(t -> t.getId().equals(taskId))
				.findFirst()
				.ifPresent(task -> {
					task.setText(newText);
					saveTasks(playerUuid);
				});
		}
	}
	
	/**
	 * Toggle task completion
	 */
	public static void toggleTaskCompletion(UUID playerUuid, String taskId) {
		// Get actual cached list
		if (!playerTasks.containsKey(playerUuid)) {
			loadTasks(playerUuid);
		}
		
		List<TodoTask> tasks = playerTasks.get(playerUuid);
		if (tasks != null) {
			tasks.stream()
				.filter(t -> t.getId().equals(taskId))
				.findFirst()
				.ifPresent(task -> {
					task.setCompleted(!task.isCompleted());
					
					// If marking as complete, move to end
					if (task.isCompleted()) {
						task.setOrder(Integer.MAX_VALUE);
					} else {
						// If marking as incomplete, give it the next available order
						int maxOrder = tasks.stream()
							.filter(t -> !t.isCompleted())
							.mapToInt(TodoTask::getOrder)
							.max()
							.orElse(-1);
						task.setOrder(maxOrder + 1);
					}
					
					saveTasks(playerUuid);
				});
		}
	}
	
	/**
	 * Delete a task
	 */
	public static void deleteTask(UUID playerUuid, String taskId) {
		// Get actual cached list
		if (!playerTasks.containsKey(playerUuid)) {
			loadTasks(playerUuid);
		}
		
		List<TodoTask> tasks = playerTasks.get(playerUuid);
		if (tasks != null) {
			tasks.removeIf(t -> t.getId().equals(taskId));
			saveTasks(playerUuid);
		}
	}
	
	/**
	 * Reorder incomplete tasks
	 */
	public static void reorderTasks(UUID playerUuid, String taskId, int newOrder) {
		// Get actual cached list
		if (!playerTasks.containsKey(playerUuid)) {
			loadTasks(playerUuid);
		}
		
		List<TodoTask> tasks = playerTasks.get(playerUuid);
		if (tasks == null) return;
		
		TodoTask movedTask = tasks.stream()
			.filter(t -> t.getId().equals(taskId) && !t.isCompleted())
			.findFirst()
			.orElse(null);
		
		if (movedTask == null) return;
		
		// Get all incomplete tasks
		List<TodoTask> incompleteTasks = tasks.stream()
			.filter(t -> !t.isCompleted())
			.sorted(Comparator.comparingInt(TodoTask::getOrder))
			.collect(Collectors.toList());
		
		// Remove moved task
		incompleteTasks.remove(movedTask);
		
		// Insert at new position
		int insertIndex = Math.max(0, Math.min(newOrder, incompleteTasks.size()));
		incompleteTasks.add(insertIndex, movedTask);
		
		// Reassign order numbers
		for (int i = 0; i < incompleteTasks.size(); i++) {
			incompleteTasks.get(i).setOrder(i);
		}
		
		saveTasks(playerUuid);
	}
	
	/**
	 * Get or create window state
	 */
	public static FloatingWindowState getWindowState(UUID playerUuid, String windowId) {
		if (!playerWindowStates.containsKey(playerUuid)) {
			loadWindowStates(playerUuid);
		}
		
		Map<String, FloatingWindowState> states = playerWindowStates.get(playerUuid);
		if (states == null) {
			states = new HashMap<>();
			playerWindowStates.put(playerUuid, states);
		}
		
		return states.computeIfAbsent(windowId, id -> new FloatingWindowState(id));
	}
	
	/**
	 * Save window state
	 */
	public static void saveWindowState(UUID playerUuid, FloatingWindowState state) {
		if (!playerWindowStates.containsKey(playerUuid)) {
			playerWindowStates.put(playerUuid, new HashMap<>());
		}
		playerWindowStates.get(playerUuid).put(state.getWindowId(), state);
		saveWindowStates(playerUuid);
	}
	
	/**
	 * Load tasks from disk
	 */
	private static void loadTasks(UUID playerUuid) {
		File file = new File(TODO_DIR, playerUuid + "_tasks.json");
		FocusTimerShop.LOGGER.info("[TodoManager] Loading tasks from file: {}", file.getAbsolutePath());
		
		if (!file.exists()) {
			FocusTimerShop.LOGGER.info("[TodoManager] No tasks file found, creating empty list for player {}", playerUuid);
			playerTasks.put(playerUuid, new ArrayList<>());
			return;
		}
		
		try (FileReader reader = new FileReader(file)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			JsonArray tasksArray = root.getAsJsonArray("tasks");
			
			List<TodoTask> tasks = new ArrayList<>();
			for (int i = 0; i < tasksArray.size(); i++) {
				tasks.add(TodoTask.fromJson(tasksArray.get(i).getAsJsonObject()));
			}
			
			playerTasks.put(playerUuid, tasks);
			FocusTimerShop.LOGGER.info("[TodoManager] Successfully loaded {} tasks for player {}", tasks.size(), playerUuid);
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("[TodoManager] Exception while loading tasks for player " + playerUuid, e);
			playerTasks.put(playerUuid, new ArrayList<>());
		}
	}
	
	/**
	 * Save tasks to disk (atomic write)
	 */
	private static void saveTasks(UUID playerUuid) {
		List<TodoTask> tasks = playerTasks.get(playerUuid);
		if (tasks == null) {
			FocusTimerShop.LOGGER.warn("[TodoManager] saveTasks() called but tasks list is null for player {}", playerUuid);
			return;
		}
		
		File file = new File(TODO_DIR, playerUuid + "_tasks.json");
		File tempFile = new File(TODO_DIR, playerUuid + "_tasks.json.tmp");
		
		FocusTimerShop.LOGGER.info("[TodoManager] Saving {} tasks to file: {}", tasks.size(), file.getAbsolutePath());
		
		try {
			JsonObject root = new JsonObject();
			JsonArray tasksArray = new JsonArray();
			for (TodoTask task : tasks) {
				tasksArray.add(task.toJson());
			}
			root.add("tasks", tasksArray);
			
			try (FileWriter writer = new FileWriter(tempFile)) {
				GSON.toJson(root, writer);
				writer.flush(); // Force write
			}
			
			if (file.exists()) {
				file.delete();
			}
			boolean renamed = tempFile.renameTo(file);
			
			if (renamed) {
				FocusTimerShop.LOGGER.info("[TodoManager] Successfully saved {} tasks for player {}", tasks.size(), playerUuid);
			} else {
				FocusTimerShop.LOGGER.error("[TodoManager] Failed to rename temp file to {}", file.getAbsolutePath());
			}
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("[TodoManager] IOException while saving tasks for player " + playerUuid, e);
		}
	}
	
	/**
	 * Load window states from disk
	 */
	private static void loadWindowStates(UUID playerUuid) {
		File file = new File(TODO_DIR, playerUuid + "_windows.json");
		if (!file.exists()) {
			playerWindowStates.put(playerUuid, new HashMap<>());
			return;
		}
		
		try (FileReader reader = new FileReader(file)) {
			JsonObject root = GSON.fromJson(reader, JsonObject.class);
			JsonArray statesArray = root.getAsJsonArray("windows");
			
			Map<String, FloatingWindowState> states = new HashMap<>();
			for (int i = 0; i < statesArray.size(); i++) {
				FloatingWindowState state = FloatingWindowState.fromJson(statesArray.get(i).getAsJsonObject());
				states.put(state.getWindowId(), state);
			}
			
			playerWindowStates.put(playerUuid, states);
			FocusTimerShop.LOGGER.info("Loaded {} window states for player {}", states.size(), playerUuid);
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to load window states for player " + playerUuid, e);
			playerWindowStates.put(playerUuid, new HashMap<>());
		}
	}
	
	/**
	 * Save window states to disk (atomic write)
	 */
	private static void saveWindowStates(UUID playerUuid) {
		Map<String, FloatingWindowState> states = playerWindowStates.get(playerUuid);
		if (states == null) return;
		
		File file = new File(TODO_DIR, playerUuid + "_windows.json");
		File tempFile = new File(TODO_DIR, playerUuid + "_windows.json.tmp");
		
		try {
			JsonObject root = new JsonObject();
			JsonArray statesArray = new JsonArray();
			for (FloatingWindowState state : states.values()) {
				statesArray.add(state.toJson());
			}
			root.add("windows", statesArray);
			
			try (FileWriter writer = new FileWriter(tempFile)) {
				GSON.toJson(root, writer);
			}
			
			if (file.exists()) {
				file.delete();
			}
			tempFile.renameTo(file);
			
			FocusTimerShop.LOGGER.debug("Saved {} window states for player {}", states.size(), playerUuid);
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to save window states for player " + playerUuid, e);
		}
	}
}
