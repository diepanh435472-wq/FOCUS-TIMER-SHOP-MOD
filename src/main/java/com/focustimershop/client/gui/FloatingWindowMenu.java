package com.focustimershop.client.gui;

import com.focustimershop.todo.FloatingWindowRegistry;
import com.focustimershop.todo.FloatingWindowType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * v1.0.7-beta - Floating Window Menu
 * Bottom-left hamburger button that opens a menu to launch floating windows
 * 
 * Features:
 * - Fixed position button (bottom-left)
 * - Popup menu with categories (Tất cả, etc.)
 * - Grid of window type icons
 * - Search field to filter windows
 * - Extensible - new window types automatically appear
 */
public class FloatingWindowMenu {
	private static final int BUTTON_SIZE = 40;
	private static final int BUTTON_MARGIN = 10;
	private static final int POPUP_WIDTH = 350;
	private static final int POPUP_HEIGHT = 250;
	private static final int CATEGORY_TAB_WIDTH = 100;
	
	private final MinecraftClient client;
	
	// Menu state
	private boolean isOpen = false;
	private String selectedCategory = "Tất cả";
	private TextFieldWidget searchField = null;
	private String searchQuery = "";
	
	// Button position (bottom-left)
	private int buttonX, buttonY;
	
	// Popup position
	private int popupX, popupY;
	
	public FloatingWindowMenu(MinecraftClient client) {
		this.client = client;
		updatePositions();
	}
	
	private void updatePositions() {
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		
		// Button at bottom-left
		buttonX = BUTTON_MARGIN;
		buttonY = screenHeight - BUTTON_SIZE - BUTTON_MARGIN;
		
		// Popup above and to the right of button
		popupX = BUTTON_MARGIN + BUTTON_SIZE + 10;
		popupY = Math.max(BUTTON_MARGIN, buttonY - POPUP_HEIGHT + BUTTON_SIZE);
		
		// Clamp popup to screen
		if (popupX + POPUP_WIDTH > screenWidth - BUTTON_MARGIN) {
			popupX = screenWidth - POPUP_WIDTH - BUTTON_MARGIN;
		}
		if (popupY < BUTTON_MARGIN) {
			popupY = BUTTON_MARGIN;
		}
		
		// Initialize search field if needed
		if (searchField == null) {
			searchField = new TextFieldWidget(
				client.textRenderer,
				popupX + 10,
				popupY + POPUP_HEIGHT - 35,
				POPUP_WIDTH - 20,
				20,
				Text.literal("")
			);
			searchField.setMaxLength(50);
			searchField.setPlaceholder(Text.literal("Tìm kiếm"));
			searchField.setChangedListener(text -> {
				searchQuery = text;
			});
		} else {
			searchField.setX(popupX + 10);
			searchField.setY(popupY + POPUP_HEIGHT - 35);
		}
	}
	
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		updatePositions();
		
		// Render hamburger button
		renderButton(context, mouseX, mouseY);
		
		// Render popup if open
		if (isOpen) {
			renderPopup(context, mouseX, mouseY, delta);
		}
	}
	
	private void renderButton(DrawContext context, int mouseX, int mouseY) {
		boolean hovered = mouseX >= buttonX && mouseX <= buttonX + BUTTON_SIZE &&
		                  mouseY >= buttonY && mouseY <= buttonY + BUTTON_SIZE;
		
		// Button background
		int bgColor = isOpen ? 0xFF4A6A9E : (hovered ? 0xFF3A5A8E : 0xFF2A4A7E);
		context.fill(buttonX, buttonY, buttonX + BUTTON_SIZE, buttonY + BUTTON_SIZE, bgColor);
		
		// Border
		context.fill(buttonX, buttonY, buttonX + BUTTON_SIZE, buttonY + 2, 0xFF5A7AAE);
		context.fill(buttonX, buttonY + BUTTON_SIZE - 2, buttonX + BUTTON_SIZE, buttonY + BUTTON_SIZE, 0xFF1A2A4E);
		
		// Hamburger icon (three horizontal bars)
		int iconX = buttonX + 10;
		int iconY = buttonY + 12;
		int barWidth = 20;
		int barHeight = 3;
		int barSpacing = 5;
		
		int barColor = 0xFFFFFFFF;
		context.fill(iconX, iconY, iconX + barWidth, iconY + barHeight, barColor);
		context.fill(iconX, iconY + barSpacing, iconX + barWidth, iconY + barSpacing + barHeight, barColor);
		context.fill(iconX, iconY + barSpacing * 2, iconX + barWidth, iconY + barSpacing * 2 + barHeight, barColor);
	}
	
	private void renderPopup(DrawContext context, int mouseX, int mouseY, float delta) {
		// Popup background
		context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT, 0xEE2D2D2D);
		
		// Title bar
		context.fill(popupX, popupY, popupX + POPUP_WIDTH, popupY + 30, 0xFF2A4A7E);
		context.drawText(client.textRenderer, "§lMenu", popupX + 10, popupY + 10, 0xFFFFFFFF, true);
		
		// Divider
		context.fill(popupX, popupY + 30, popupX + POPUP_WIDTH, popupY + 31, 0xFF4A4A4A);
		
		// Category tabs (left side)
		renderCategoryTabs(context, mouseX, mouseY);
		
		// Vertical divider
		int dividerX = popupX + CATEGORY_TAB_WIDTH;
		context.fill(dividerX, popupY + 31, dividerX + 1, popupY + POPUP_HEIGHT - 45, 0xFF4A4A4A);
		
		// Window type grid (right side)
		renderWindowTypeGrid(context, mouseX, mouseY);
		
		// Search field
		context.fill(popupX, popupY + POPUP_HEIGHT - 45, popupX + POPUP_WIDTH, popupY + POPUP_HEIGHT - 44, 0xFF4A4A4A);
		searchField.render(context, mouseX, mouseY, delta);
	}
	
	private void renderCategoryTabs(DrawContext context, int mouseX, int mouseY) {
		List<String> categories = FloatingWindowRegistry.getCategories();
		
		int tabY = popupY + 35;
		int tabHeight = 30;
		
		for (String category : categories) {
			boolean selected = category.equals(selectedCategory);
			boolean hovered = mouseX >= popupX && mouseX <= popupX + CATEGORY_TAB_WIDTH &&
			                  mouseY >= tabY && mouseY <= tabY + tabHeight;
			
			// Tab background
			int bgColor = selected ? 0xFF3A5A8E : (hovered ? 0xFF3A3A3A : 0x00000000);
			if (bgColor != 0) {
				context.fill(popupX, tabY, popupX + CATEGORY_TAB_WIDTH, tabY + tabHeight, bgColor);
			}
			
			// Selection indicator
			if (selected) {
				context.fill(popupX, tabY, popupX + 3, tabY + tabHeight, 0xFF5A9AEE);
			}
			
			// Tab text
			String displayText = category;
			int textColor = selected ? 0xFFFFFFFF : (hovered ? 0xFFCCCCCC : 0xFF888888);
			context.drawText(client.textRenderer, displayText, popupX + 10, tabY + 10, textColor, false);
			
			tabY += tabHeight;
		}
	}
	
	private void renderWindowTypeGrid(DrawContext context, int mouseX, int mouseY) {
		List<FloatingWindowType> types = FloatingWindowRegistry.search(searchQuery, selectedCategory);
		
		int gridX = popupX + CATEGORY_TAB_WIDTH + 10;
		int gridY = popupY + 35;
		int gridWidth = POPUP_WIDTH - CATEGORY_TAB_WIDTH - 20;
		int gridHeight = POPUP_HEIGHT - 80; // Leave space for search field
		
		// Icon grid layout
		int iconSize = 60;
		int iconSpacing = 10;
		int iconsPerRow = (gridWidth + iconSpacing) / (iconSize + iconSpacing);
		
		int currentX = gridX;
		int currentY = gridY;
		int count = 0;
		
		for (FloatingWindowType type : types) {
			boolean hovered = mouseX >= currentX && mouseX <= currentX + iconSize &&
			                  mouseY >= currentY && mouseY <= currentY + iconSize;
			
			// Icon background
			int bgColor = hovered ? 0xFF3A5A8E : 0xFF2A2A2A;
			context.fill(currentX, currentY, currentX + iconSize, currentY + iconSize, bgColor);
			
			// Border
			if (hovered) {
				context.fill(currentX, currentY, currentX + iconSize, currentY + 2, 0xFF5A9AEE);
				context.fill(currentX, currentY + iconSize - 2, currentX + iconSize, currentY + iconSize, 0xFF5A9AEE);
			}
			
			// Icon (text icon for now)
			String icon = type.getIcon();
			int iconTextWidth = client.textRenderer.getWidth(icon);
			context.drawText(client.textRenderer, "§l" + icon, 
				currentX + iconSize / 2 - iconTextWidth / 2, 
				currentY + 15, 
				0xFFFFFFFF, false);
			
			// Label (wrapped if needed)
			String label = type.getDisplayName();
			int labelWidth = client.textRenderer.getWidth(label);
			if (labelWidth > iconSize - 4) {
				label = client.textRenderer.trimToWidth(label, iconSize - 4);
			}
			int labelX = currentX + iconSize / 2 - client.textRenderer.getWidth(label) / 2;
			context.drawText(client.textRenderer, "§7" + label, labelX, currentY + 40, 0xFFFFFFFF, false);
			
			// Move to next position
			count++;
			if (count % iconsPerRow == 0) {
				currentX = gridX;
				currentY += iconSize + iconSpacing;
			} else {
				currentX += iconSize + iconSpacing;
			}
			
			// Stop if out of space
			if (currentY + iconSize > gridY + gridHeight) {
				break;
			}
		}
		
		// If no results
		if (types.isEmpty()) {
			String noResults = "Không tìm thấy";
			int textWidth = client.textRenderer.getWidth(noResults);
			context.drawText(client.textRenderer, "§7" + noResults, 
				gridX + gridWidth / 2 - textWidth / 2, 
				gridY + gridHeight / 2 - 4, 
				0xFFFFFFFF, false);
		}
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return false;
		
		int mx = (int) mouseX;
		int my = (int) mouseY;
		
		// Check hamburger button
		if (mx >= buttonX && mx <= buttonX + BUTTON_SIZE &&
		    my >= buttonY && my <= buttonY + BUTTON_SIZE) {
			toggle();
			return true;
		}
		
		// If menu not open, nothing else to check
		if (!isOpen) return false;
		
		// Check if click is outside popup (close menu)
		if (mx < popupX || mx > popupX + POPUP_WIDTH ||
		    my < popupY || my > popupY + POPUP_HEIGHT) {
			close();
			return true;
		}
		
		// Check search field
		if (searchField != null && searchField.mouseClicked(mouseX, mouseY, button)) {
			return true;
		}
		
		// Check category tabs
		if (handleCategoryClick(mx, my)) {
			return true;
		}
		
		// Check window type icons
		if (handleWindowTypeClick(mx, my)) {
			return true;
		}
		
		return true; // Consume click if inside popup
	}
	
	private boolean handleCategoryClick(int mouseX, int mouseY) {
		List<String> categories = FloatingWindowRegistry.getCategories();
		
		int tabY = popupY + 35;
		int tabHeight = 30;
		
		for (String category : categories) {
			if (mouseX >= popupX && mouseX <= popupX + CATEGORY_TAB_WIDTH &&
			    mouseY >= tabY && mouseY <= tabY + tabHeight) {
				selectedCategory = category;
				return true;
			}
			tabY += tabHeight;
		}
		
		return false;
	}
	
	private boolean handleWindowTypeClick(int mouseX, int mouseY) {
		List<FloatingWindowType> types = FloatingWindowRegistry.search(searchQuery, selectedCategory);
		
		int gridX = popupX + CATEGORY_TAB_WIDTH + 10;
		int gridY = popupY + 35;
		int gridWidth = POPUP_WIDTH - CATEGORY_TAB_WIDTH - 20;
		int gridHeight = POPUP_HEIGHT - 80;
		
		int iconSize = 60;
		int iconSpacing = 10;
		int iconsPerRow = (gridWidth + iconSpacing) / (iconSize + iconSpacing);
		
		int currentX = gridX;
		int currentY = gridY;
		int count = 0;
		
		for (FloatingWindowType type : types) {
			if (mouseX >= currentX && mouseX <= currentX + iconSize &&
			    mouseY >= currentY && mouseY <= currentY + iconSize) {
				// Window type clicked - will be handled by parent screen
				onWindowTypeSelected(type);
				return true;
			}
			
			count++;
			if (count % iconsPerRow == 0) {
				currentX = gridX;
				currentY += iconSize + iconSpacing;
			} else {
				currentX += iconSize + iconSpacing;
			}
			
			if (currentY + iconSize > gridY + gridHeight) {
				break;
			}
		}
		
		return false;
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (!isOpen) return false;
		
		// Escape closes menu
		if (keyCode == 256) { // Escape
			close();
			return true;
		}
		
		// Pass to search field
		if (searchField != null && searchField.isFocused()) {
			return searchField.keyPressed(keyCode, scanCode, modifiers);
		}
		
		return false;
	}
	
	public boolean charTyped(char chr, int modifiers) {
		if (!isOpen) return false;
		
		if (searchField != null && searchField.isFocused()) {
			return searchField.charTyped(chr, modifiers);
		}
		
		return false;
	}
	
	public void toggle() {
		isOpen = !isOpen;
		if (isOpen) {
			searchQuery = "";
			if (searchField != null) {
				searchField.setText("");
			}
		}
	}
	
	public void close() {
		isOpen = false;
	}
	
	public boolean isOpen() {
		return isOpen;
	}
	
	// Callback for when a window type is selected
	private FloatingWindowTypeSelectedCallback callback = null;
	
	public void setWindowTypeSelectedCallback(FloatingWindowTypeSelectedCallback callback) {
		this.callback = callback;
	}
	
	private void onWindowTypeSelected(FloatingWindowType type) {
		if (callback != null) {
			callback.onWindowTypeSelected(type);
		}
		close();
	}
	
	@FunctionalInterface
	public interface FloatingWindowTypeSelectedCallback {
		void onWindowTypeSelected(FloatingWindowType type);
	}
}
