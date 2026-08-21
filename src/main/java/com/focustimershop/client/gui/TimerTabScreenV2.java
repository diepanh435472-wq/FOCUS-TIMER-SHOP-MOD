package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.timer.SessionCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;

/**
 * v1.0.7-beta Timer UI Overhaul - New timer tab screen
 * Orchestrates: Category Selection → Clock Config
 * Active session now handled by fullscreen ActiveSessionScreen
 */
public class TimerTabScreenV2 {
	private final MainMenuScreen parent;
	
	// Sub-screens
	private CategorySelectionScreen categoryScreen;

	public TimerTabScreenV2(MainMenuScreen parent) {
		this.parent = parent;
		this.categoryScreen = new CategorySelectionScreen(this);
	}

	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// Category selection (which includes clock config)
		categoryScreen.render(context, x, y, width, height, mouseX, mouseY, delta);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
		// Setup flow - delegate to category screen
		return categoryScreen.mouseClicked(mouseX, mouseY, button, 
			contentX, contentY, contentWidth, contentHeight);
	}
	
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		return categoryScreen.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return categoryScreen.mouseReleased(mouseX, mouseY, button);
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return categoryScreen.keyPressed(keyCode, scanCode, modifiers);
	}
	
	public boolean charTyped(char chr, int modifiers) {
		return categoryScreen.charTyped(chr, modifiers);
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
