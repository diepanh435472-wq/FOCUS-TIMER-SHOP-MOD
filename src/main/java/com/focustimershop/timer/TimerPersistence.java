package com.focustimershop.timer;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;

/**
 * PHASE 2: Timer state persistence
 * Saves/loads timer sessions to/from disk to survive disconnects and server restarts
 * 
 * File format: FCTMS/timers/<uuid>.json
 * Contains: type, state, elapsed, target, lastTick, rounds
 */
public class TimerPersistence {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	
	/**
	 * Data class for serialization
	 */
	public static class TimerSaveData {
		public String type;           // POMODORO_FOCUS, STOPWATCH, COUNTDOWN, etc.
		public String state;          // RUNNING, PAUSED, IDLE
		public int elapsedSeconds;    // How long timer has been running
		public int targetSeconds;     // Target time (0 for stopwatch)
		public long lastTickTime;     // System.currentTimeMillis() of last update
		public int pomodoroRounds;    // Completed pomodoro rounds
		public long savedAt;          // When this save was created
		
		public TimerSaveData() {
			// For GSON deserialization
		}
		
		public TimerSaveData(TimerSession session) {
			this.type = session.getType().name();
			this.state = session.getState().name();
			this.elapsedSeconds = session.getElapsedTime();
			this.targetSeconds = session.getTargetTime();
			this.lastTickTime = session.getLastTickTime();
			this.pomodoroRounds = session.getPomodoroRounds();
			this.savedAt = System.currentTimeMillis();
		}
	}
	
	/**
	 * Get timer save directory
	 */
	private static File getTimerDir() {
		File dir = DatabaseManager.getRoot().resolve("timers").toFile();
		if (!dir.exists()) {
			dir.mkdirs();
		}
		return dir;
	}
	
	/**
	 * Get save file for player
	 */
	private static File getSaveFile(UUID playerId) {
		return new File(getTimerDir(), playerId.toString() + ".json");
	}
	
	/**
	 * Save timer session to disk
	 * Called on: pause, disconnect, server shutdown, periodic auto-save
	 */
	public static void saveTimer(UUID playerId, TimerSession session) {
		if (session == null) {
			// Delete save file if session is null (timer stopped)
			File file = getSaveFile(playerId);
			if (file.exists()) {
				file.delete();
				FocusTimerShop.LOGGER.info("Deleted timer save for player {}", playerId);
			}
			return;
		}
		
		try {
			// Force final tick to capture accurate elapsed time before saving
			session.finalTick();
			
			TimerSaveData data = new TimerSaveData(session);
			File file = getSaveFile(playerId);
			
			try (FileWriter writer = new FileWriter(file)) {
				GSON.toJson(data, writer);
			}
			
			FocusTimerShop.LOGGER.info("Saved timer for player {} (type: {}, elapsed: {}s, state: {})",
				playerId, data.type, data.elapsedSeconds, data.state);
				
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to save timer for player {}: {}", playerId, e.getMessage());
		}
	}
	
	/**
	 * Load timer session from disk
	 * Called on: player join, server startup
	 * 
	 * @return TimerSaveData if found and valid, null otherwise
	 */
	public static TimerSaveData loadTimer(UUID playerId) {
		File file = getSaveFile(playerId);
		
		if (!file.exists()) {
			return null; // No saved timer
		}
		
		try (FileReader reader = new FileReader(file)) {
			TimerSaveData data = GSON.fromJson(reader, TimerSaveData.class);
			
			// Validate loaded data
			if (data == null) {
				FocusTimerShop.LOGGER.warn("Invalid timer save data for player {}", playerId);
				file.delete();
				return null;
			}
			
			// Check if save is too old (more than 7 days = 604800000ms)
			long age = System.currentTimeMillis() - data.savedAt;
			if (age > 604_800_000L) {
				FocusTimerShop.LOGGER.warn("Timer save for player {} is too old ({}ms), discarding", 
					playerId, age);
				file.delete();
				return null;
			}
			
			// Validate enum values
			try {
				TimerType.valueOf(data.type);
				TimerState.valueOf(data.state);
			} catch (IllegalArgumentException e) {
				FocusTimerShop.LOGGER.warn("Invalid timer type/state for player {}: {} / {}", 
					playerId, data.type, data.state);
				file.delete();
				return null;
			}
			
			FocusTimerShop.LOGGER.info("Loaded timer for player {} (type: {}, elapsed: {}s, state: {})",
				playerId, data.type, data.elapsedSeconds, data.state);
			
			return data;
			
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to load timer for player {}: {}", playerId, e.getMessage());
			return null;
		}
	}
	
	/**
	 * Restore timer session from save data
	 * 
	 * @param playerId Player UUID
	 * @param data Save data loaded from disk
	 * @return Restored TimerSession
	 */
	public static TimerSession restoreTimer(UUID playerId, TimerSaveData data) {
		TimerType type = TimerType.valueOf(data.type);
		TimerState state = TimerState.valueOf(data.state);
		
		// Create session with saved data
		TimerSession session = new TimerSession(playerId, type, data.targetSeconds);
		
		// Restore elapsed time
		session.setElapsedTime(data.elapsedSeconds);
		
		// Restore pomodoro rounds
		if (type == TimerType.POMODORO_FOCUS) {
			for (int i = 0; i < data.pomodoroRounds; i++) {
				session.incrementPomodoroRound();
			}
		}
		
		// Restore state
		if (state == TimerState.RUNNING) {
			// Resume running timer
			// Adjust lastTickTime to account for time offline
			long offlineTime = System.currentTimeMillis() - data.savedAt;
			
			// If offline < 5 minutes, try to resume
			if (offlineTime < 300_000L) {
				session.start();
				FocusTimerShop.LOGGER.info("Resumed RUNNING timer for player {} (was offline {}ms)", 
					playerId, offlineTime);
			} else {
				// Too long offline, convert to PAUSED
				session.setState(TimerState.PAUSED);
				FocusTimerShop.LOGGER.info("Timer for player {} was RUNNING but offline too long ({}ms), converted to PAUSED", 
					playerId, offlineTime);
			}
		} else if (state == TimerState.PAUSED) {
			session.setState(TimerState.PAUSED);
		}
		
		return session;
	}
	
	/**
	 * Delete timer save file
	 */
	public static void deleteTimer(UUID playerId) {
		File file = getSaveFile(playerId);
		if (file.exists()) {
			file.delete();
		}
	}
	
	/**
	 * Get all saved timer files (for debugging/admin)
	 */
	public static File[] getAllSavedTimers() {
		File dir = getTimerDir();
		return dir.listFiles((d, name) -> name.endsWith(".json"));
	}
	
	/**
	 * Clean up old timer saves (older than 7 days)
	 * Called on server startup
	 */
	public static void cleanupOldSaves() {
		File[] saves = getAllSavedTimers();
		if (saves == null) return;
		
		int deleted = 0;
		long cutoff = System.currentTimeMillis() - 604_800_000L; // 7 days
		
		for (File file : saves) {
			if (file.lastModified() < cutoff) {
				file.delete();
				deleted++;
			}
		}
		
		if (deleted > 0) {
			FocusTimerShop.LOGGER.info("Cleaned up {} old timer saves", deleted);
		}
	}
}
