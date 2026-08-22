package com.focustimershop.client.gui;

import com.focustimershop.timer.SessionCategory;
import com.focustimershop.todo.FloatingWindowType;
import net.minecraft.client.gui.DrawContext;

/**
 * v1.0.7-beta Timer UI Overhaul - Category selection screen
 * Shows categories and clock config together
 */
public class CategorySelectionScreen {
	private final TimerTabScreenV2 parent;
	private SessionCategory selectedCategory = SessionCategory.TAP_TRUNG;
	private ClockConfigScreen clockConfig;
	
	// v1.0.7-beta - Floating windows
	private TodoListWindow todoWindow = null;
	private FloatingWindowMenu floatingMenu = null;

	public CategorySelectionScreen(TimerTabScreenV2 parent) {
		this.parent = parent;
		this.clockConfig = new ClockConfigScreen(parent, selectedCategory);
		
		// Initialize floating windows
		if (parent.getClient().player != null) {
			todoWindow = new TodoListWindow(parent.getClient());
			floatingMenu = new FloatingWindowMenu(parent.getClient());
			// Wire up callback
			floatingMenu.setWindowTypeSelectedCallback(this::onWindowTypeSelected);
		}
	}

	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// Check if clock config is showing a popup - if so, ONLY render that
		if (clockConfig.isShowingPopup()) {
			clockConfig.renderContent(context, x, y + 70, width, height - 70, mouseX, mouseY, delta);
			return;
		}
		
		// Normal render: categories + clock config content
		// Categories in horizontal row at top
		int buttonWidth = 110;
		int buttonHeight = 35;
		int spacing = 10;
		int totalWidth = (buttonWidth * 3 + spacing * 2);
		int startX = x + (width - totalWidth) / 2;
		int categoryY = y + 20;
		
		SessionCategory[] categories = {SessionCategory.TAP_TRUNG, SessionCategory.NGHI_NGAN, SessionCategory.NGHI_DAI};
		
		for (int i = 0; i < categories.length; i++) {
			int btnX = startX + i * (buttonWidth + spacing);
			SessionCategory category = categories[i];
			boolean selected = (category == selectedCategory);
			
			int bgColor = selected ? 0xFF4A9EFF : 0xFF2A2A2A;
			int borderColor = selected ? 0xFF6ABFFF : 0xFF404040;
			
			context.fill(btnX - 2, categoryY - 2, btnX + buttonWidth + 2, categoryY + buttonHeight + 2, borderColor);
			context.fill(btnX, categoryY, btnX + buttonWidth, categoryY + buttonHeight, bgColor);
			
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), category.getDisplayName(), 
				btnX + buttonWidth / 2, categoryY + 12, 0xFFFFFFFF);
		}
		
		// Render clock config below categories
		clockConfig.renderContent(context, x, y + 70, width, height - 70, mouseX, mouseY, delta);
		
		// v1.0.7-beta - Render floating windows (on top of everything)
		if (todoWindow != null) {
			todoWindow.render(context, mouseX, mouseY, delta);
		}
		if (floatingMenu != null) {
			floatingMenu.render(context, mouseX, mouseY, delta);
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
		// v1.0.7-beta - Check floating windows first (highest priority)
		if (floatingMenu != null && floatingMenu.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		
		if (todoWindow != null && todoWindow.isVisible() && todoWindow.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		
		int x = contentX;
		int y = contentY;
		int width = contentWidth;
		
		// Category buttons
		int buttonWidth = 110;
		int buttonHeight = 35;
		int spacing = 10;
		int totalWidth = (buttonWidth * 3 + spacing * 2);
		int startX = x + (width - totalWidth) / 2;
		int categoryY = y + 20;
		
		SessionCategory[] categories = {SessionCategory.TAP_TRUNG, SessionCategory.NGHI_NGAN, SessionCategory.NGHI_DAI};
		for (int i = 0; i < categories.length; i++) {
			int btnX = startX + i * (buttonWidth + spacing);
			
			if (mouseX >= btnX && mouseX <= btnX + buttonWidth && 
			    mouseY >= categoryY && mouseY <= categoryY + buttonHeight) {
				selectedCategory = categories[i];
				// Update clock config with new category
				clockConfig = new ClockConfigScreen(parent, selectedCategory);
				return true;
			}
		}
		
		// Delegate to clock config for other clicks
		return clockConfig.mouseClicked(mouseX, mouseY, button, contentX, contentY + 70, contentWidth, contentHeight - 70);
	}
	
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		// v1.0.7-beta - Check floating windows first
		if (todoWindow != null && todoWindow.isVisible() && todoWindow.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
			return true;
		}
		
		return clockConfig.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		// v1.0.7-beta - Check floating windows first
		if (todoWindow != null && todoWindow.isVisible() && todoWindow.mouseReleased(mouseX, mouseY, button)) {
			return true;
		}
		
		return clockConfig.mouseReleased(mouseX, mouseY, button);
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// v1.0.7-beta - Check floating windows for keyboard input
		if (floatingMenu != null && floatingMenu.isOpen() && floatingMenu.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		
		if (todoWindow != null && todoWindow.isVisible() && todoWindow.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		
		return clockConfig.keyPressed(keyCode, scanCode, modifiers);
	}
	
	public boolean charTyped(char chr, int modifiers) {
		// v1.0.7-beta - Check floating windows for char input
		if (floatingMenu != null && floatingMenu.isOpen() && floatingMenu.charTyped(chr, modifiers)) {
			return true;
		}
		
		if (todoWindow != null && todoWindow.isVisible() && todoWindow.charTyped(chr, modifiers)) {
			return true;
		}
		
		return clockConfig.charTyped(chr, modifiers);
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// v1.0.7-beta - Check TodoListWindow for scroll events
		if (todoWindow != null && todoWindow.isVisible() && todoWindow.mouseScrolled(mouseX, mouseY, 0, verticalAmount)) {
			return true;
		}
		
		return false;
	}
	
	public SessionCategory getSelectedCategory() {
		return selectedCategory;
	}
	
	/**
	 * Check if clock config is showing a modal
	 */
	public boolean isShowingModal() {
		return clockConfig.isShowingPopup();
	}
	
	/**
	 * v1.0.7-beta - Callback when a window type is selected from menu
	 */
	private void onWindowTypeSelected(FloatingWindowType type) {
		if (type.getId().equals("todo_list")) {
			// Open/restore TodoListWindow
			if (todoWindow != null) {
				todoWindow.open();
			}
		}
		// Future window types will be handled here
	}
}
