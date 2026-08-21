package com.focustimershop.client.gui;

import com.focustimershop.timer.SessionCategory;
import net.minecraft.client.gui.DrawContext;

/**
 * v1.0.7-beta Timer UI Overhaul - Category selection screen
 * Shows categories and clock config together
 */
public class CategorySelectionScreen {
	private final TimerTabScreenV2 parent;
	private SessionCategory selectedCategory = SessionCategory.TAP_TRUNG;
	private ClockConfigScreen clockConfig;

	public CategorySelectionScreen(TimerTabScreenV2 parent) {
		this.parent = parent;
		this.clockConfig = new ClockConfigScreen(parent, selectedCategory);
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
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
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
		return clockConfig.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		return clockConfig.mouseReleased(mouseX, mouseY, button);
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		return clockConfig.keyPressed(keyCode, scanCode, modifiers);
	}
	
	public boolean charTyped(char chr, int modifiers) {
		return clockConfig.charTyped(chr, modifiers);
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
}
