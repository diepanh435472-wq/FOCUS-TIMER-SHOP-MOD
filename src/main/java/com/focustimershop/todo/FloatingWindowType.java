package com.focustimershop.todo;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;

import java.util.function.Function;

/**
 * v1.0.7-beta - Floating Window Type
 * Represents a type of floating window that can be opened from the Menu
 */
public class FloatingWindowType {
	private final String id;
	private final String displayName;
	private final String category; // "Tất cả", "Công cụ", etc.
	private final String icon; // Text icon for now (☰, ✓, etc.)
	private final Function<Screen, Screen> windowFactory; // Creates the window instance
	
	public FloatingWindowType(String id, String displayName, String category, String icon, 
	                          Function<Screen, Screen> windowFactory) {
		this.id = id;
		this.displayName = displayName;
		this.category = category;
		this.icon = icon;
		this.windowFactory = windowFactory;
	}
	
	public String getId() {
		return id;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public String getCategory() {
		return category;
	}
	
	public String getIcon() {
		return icon;
	}
	
	/**
	 * Create a new instance of this window type
	 * @param parent The parent screen to return to when closing
	 */
	public Screen createWindow(Screen parent) {
		return windowFactory.apply(parent);
	}
	
	/**
	 * Check if this window matches a search query
	 */
	public boolean matchesSearch(String query) {
		if (query == null || query.trim().isEmpty()) {
			return true;
		}
		String lowerQuery = query.toLowerCase().trim();
		return displayName.toLowerCase().contains(lowerQuery) ||
		       id.toLowerCase().contains(lowerQuery);
	}
}
