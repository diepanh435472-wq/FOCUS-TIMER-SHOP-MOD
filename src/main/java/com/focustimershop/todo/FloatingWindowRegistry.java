package com.focustimershop.todo;

import com.focustimershop.FocusTimerShop;

import java.util.*;
import java.util.stream.Collectors;

/**
 * v1.0.7-beta - Floating Window Registry
 * Central registry for all available floating window types
 * Built as extensible system so new window types can be added easily
 */
public class FloatingWindowRegistry {
	private static final List<FloatingWindowType> registeredTypes = new ArrayList<>();
	private static final Set<String> categories = new LinkedHashSet<>();
	
	/**
	 * Register a new floating window type
	 */
	public static void register(FloatingWindowType type) {
		registeredTypes.add(type);
		categories.add(type.getCategory());
		FocusTimerShop.LOGGER.info("Registered floating window type: {} ({})", 
			type.getDisplayName(), type.getId());
	}
	
	/**
	 * Get all registered window types
	 */
	public static List<FloatingWindowType> getAll() {
		return new ArrayList<>(registeredTypes);
	}
	
	/**
	 * Get window types by category
	 */
	public static List<FloatingWindowType> getByCategory(String category) {
		if ("Tất cả".equals(category) || category == null) {
			return getAll();
		}
		return registeredTypes.stream()
			.filter(type -> type.getCategory().equals(category))
			.collect(Collectors.toList());
	}
	
	/**
	 * Get window type by ID
	 */
	public static FloatingWindowType getById(String id) {
		return registeredTypes.stream()
			.filter(type -> type.getId().equals(id))
			.findFirst()
			.orElse(null);
	}
	
	/**
	 * Get all categories
	 */
	public static List<String> getCategories() {
		List<String> cats = new ArrayList<>();
		cats.add("Tất cả"); // Always first
		cats.addAll(categories);
		return cats;
	}
	
	/**
	 * Search window types by query
	 */
	public static List<FloatingWindowType> search(String query, String category) {
		return getByCategory(category).stream()
			.filter(type -> type.matchesSearch(query))
			.collect(Collectors.toList());
	}
	
	/**
	 * Initialize and register built-in window types
	 */
	public static void init() {
		// Register To-Do List window
		register(new FloatingWindowType(
			"todo_list",
			"To-do list",
			"Công cụ",
			"✓", // Checkmark icon
			parent -> {
				// This callback is used by the menu to open/restore the window
				// The actual TodoListWindow instance is managed by CategorySelectionScreen
				// This is just for menu display purposes
				return null; // Not used in this architecture
			}
		));
		
		FocusTimerShop.LOGGER.info("FloatingWindowRegistry initialized with {} types", 
			registeredTypes.size());
	}
}
