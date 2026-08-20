package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.timer.SessionCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

/**
 * v1.0.7-beta Timer UI Overhaul - New timer tab screen
 * Orchestrates: Category Selection → Clock Config → Active Session
 */
public class TimerTabScreenV2 {
	private final MainMenuScreen parent;
	
	// Sub-screens
	private CategorySelectionScreen categoryScreen;
	private ActiveSessionScreen activeSessionScreen;

	public TimerTabScreenV2(MainMenuScreen parent) {
		this.parent = parent;
		this.categoryScreen = new CategorySelectionScreen(this);
		this.activeSessionScreen = new ActiveSessionScreen(this);
	}

	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// Check if timer is active
		if (ClientDataCache.hasActiveTimer()) {
			activeSessionScreen.render(context, x, y, width, height, mouseX, mouseY, delta);
			return;
		}
		
		// Otherwise show category selection (which includes clock config)
		categoryScreen.render(context, x, y, width, height, mouseX, mouseY, delta);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
		// Active timer - delegate to active session screen
		if (ClientDataCache.hasActiveTimer()) {
			return activeSessionScreen.mouseClicked(mouseX, mouseY, button, 
				contentX, contentY, contentWidth, contentHeight);
		}
		
		// Setup flow - delegate to category screen
		return categoryScreen.mouseClicked(mouseX, mouseY, button, 
			contentX, contentY, contentWidth, contentHeight);
	}
	
	/**
	 * Deprecated - no longer needed as CategorySelectionScreen contains ClockConfigScreen
	 */
	@Deprecated
	public void showClockConfigScreen(SessionCategory category) {
		// No-op - kept for compatibility
	}
	
	/**
	 * Deprecated - no longer needed
	 */
	@Deprecated
	public void showCategorySelection() {
		// No-op - kept for compatibility
	}
	
	public void onTimerStarted() {
		// Automatically switches to active session view when timer starts
	}
	
	/**
	 * Check if a modal popup is currently showing
	 * Used by MainMenuScreen to determine if full-screen dim overlay is needed
	 */
	public boolean isShowingModal() {
		return categoryScreen.isShowingModal();
	}
	
	public TextRenderer getTextRenderer() {
		return parent.getTextRenderer();
	}
	
	public MainMenuScreen getParent() {
		return parent;
	}
}
