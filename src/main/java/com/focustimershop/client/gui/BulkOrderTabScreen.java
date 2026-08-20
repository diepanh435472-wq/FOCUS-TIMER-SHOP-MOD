package com.focustimershop.client.gui;

import com.focustimershop.bulkorder.BulkOrderManager;
import com.focustimershop.client.ClientDataCache;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.shop.ShopCategory;
import com.focustimershop.shop.ShopItem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Bulk Order tab - simplified chest-based bulk purchasing
 * v1.0.6-beta - reuses Shop UI patterns (categories, grid, search)
 * 
 * Layout: Left panel (item grid), Right panel (quantity input, payment, buy button)
 */
public class BulkOrderTabScreen {
	private final MainMenuScreen parent;
	
	// === CATEGORY & SEARCH ===
	private ShopCategory selectedCategory = ShopCategory.ALL;
	private int scrollOffset = 0;
	private String searchQuery = "";
	private TextFieldWidget searchField = null;
	
	// === SELECTION STATE ===
	private ShopItem selectedItem = null;  // Currently selected item for bulk order
	private int chestCount = 1;            // Number of chests to buy (min 1)
	private boolean useSilverOnly = true;  // Payment mode: true = 100% silver, false = mixed
	
	// === QUANTITY INPUT ===
	private TextFieldWidget quantityField = null;
	
	// === CLICK PREVENTION ===
	private long lastClickTime = 0;
	private static final long CLICK_COOLDOWN_MS = 300;
	
	// === TAB SCROLL ===
	private int tabScrollOffset = 0;
	private boolean isDraggingTabs = false;
	private double dragStartX = 0;
	private int dragStartOffset = 0;
	private static final int TAB_DRAG_THRESHOLD = 5;
	
	// === GRID SCROLL ===
	private boolean isDraggingGrid = false;
	private double gridDragStartY = 0;
	private int gridDragStartOffset = 0;
	private static final int GRID_DRAG_THRESHOLD = 3;
	
	// === GRID GEOMETRY ===
	private static final int GRID_ICON_SIZE = 32;
	private static final int GRID_SPACING = 4;
	private static final int GRID_CELL_SIZE = GRID_ICON_SIZE + GRID_SPACING; // 36
	
	// === CACHED FILTERED LIST ===
	private List<ShopItem> cachedFilteredItems = new ArrayList<>();
	private ShopCategory cachedCategory = null;
	private String cachedSearchQuery = null;
	
	// === LAYOUT CACHE ===
	private int lastContentX = 0;
	private int lastContentY = 0;
	private int lastContentWidth = 0;
	private int lastContentHeight = 0;
	
	public BulkOrderTabScreen(MainMenuScreen parent) {
		this.parent = parent;
	}
	
	// ===== GRID GEOMETRY HELPERS =====
	
	private int getLeftPanelWidth(int contentWidth) {
		return (int)(contentWidth * 0.65);
	}
	
	private int getGridWidth(int contentWidth) {
		return getLeftPanelWidth(contentWidth) - 10;
	}
	
	private int getGridColumns(int contentWidth) {
		return Math.max(1, getGridWidth(contentWidth) / GRID_CELL_SIZE);
	}
	
	// ===== RENDER =====
	
	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		lastContentX = x;
		lastContentY = y;
		lastContentWidth = width;
		lastContentHeight = height;
		
		// Background
		context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
		
		// Split into left (item selection) and right (order panel)
		int leftWidth = getLeftPanelWidth(width);
		int rightWidth = width - leftWidth - 10;
		int rightX = x + leftWidth + 10;
		
		renderLeftPanel(context, x, y, leftWidth, height, mouseX, mouseY);
		renderRightPanel(context, rightX, y, rightWidth, height, mouseX, mouseY);
	}
	
	private void renderLeftPanel(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Title
		context.drawText(parent.getTextRenderer(), "§lBulk Order Shop", x + 5, y + 5, 0xFFFFFFFF, true);
		
		// Search bar
		int searchY = y + 20;
		if (searchField == null) {
			searchField = new TextFieldWidget(parent.getTextRenderer(), x + 5, searchY, width - 10, 18, Text.literal("Search"));
			searchField.setMaxLength(50);
			searchField.setPlaceholder(Text.literal("Search items..."));
		}
		searchField.setPosition(x + 5, searchY);
		searchField.setWidth(width - 10);
		searchField.render(context, mouseX, mouseY, 0);
		
		// Category tabs
		int tabY = y + 45;
		renderCategoryTabs(context, x + 5, tabY, width - 10, 30, mouseX, mouseY);
		
		// Item grid
		int gridY = y + 85;
		int gridHeight = height - 90;
		renderItemGrid(context, x + 5, gridY, width - 10, gridHeight, mouseX, mouseY);
	}
	
	private void renderCategoryTabs(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Reuse Shop's category tab rendering logic
		ShopCategory[] categories = ShopCategory.values();
		int tabWidth = 80;
		int totalWidth = categories.length * (tabWidth + 2);
		int maxScroll = Math.max(0, totalWidth - width);
		tabScrollOffset = Math.max(0, Math.min(tabScrollOffset, maxScroll));
		
		int currentX = x - tabScrollOffset;
		for (ShopCategory cat : categories) {
			if (currentX + tabWidth >= x && currentX <= x + width) {
				boolean selected = (cat == selectedCategory);
				boolean hovered = mouseX >= currentX && mouseX <= currentX + tabWidth &&
				                  mouseY >= y && mouseY <= y + height;
				
				int bgColor = selected ? 0xFF4A9EFF : (hovered ? 0xFF3A4A5A : 0xFF2A2A2A);
				context.fill(currentX, y, currentX + tabWidth, y + height, bgColor);
				
				String displayName = cat.getDisplayName();
				int textWidth = parent.getTextRenderer().getWidth(displayName);
				int textX = currentX + (tabWidth - textWidth) / 2;
				int textY = y + (height - 8) / 2;
				context.drawText(parent.getTextRenderer(), displayName, textX, textY, 0xFFFFFFFF, false);
			}
			currentX += tabWidth + 2;
		}
	}
	
	private void renderItemGrid(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		List<ShopItem> items = getFilteredItems();
		
		int columns = Math.max(1, width / GRID_CELL_SIZE);
		int visibleRows = height / GRID_CELL_SIZE;
		int totalRows = (int) Math.ceil((double) items.size() / columns);
		int maxScroll = Math.max(0, totalRows - visibleRows);
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
		
		int startIndex = scrollOffset * columns;
		int endIndex = Math.min(startIndex + visibleRows * columns, items.size());
		
		for (int i = startIndex; i < endIndex; i++) {
			ShopItem item = items.get(i);
			int index = i - startIndex;
			int col = index % columns;
			int row = index / columns;
			
			int cellX = x + col * GRID_CELL_SIZE;
			int cellY = y + row * GRID_CELL_SIZE;
			
			boolean isSelected = (selectedItem != null && selectedItem.getItemId().equals(item.getItemId()));
			renderItemCell(context, item, cellX, cellY, GRID_ICON_SIZE, isSelected, mouseX, mouseY);
		}
	}
	
	private void renderItemCell(DrawContext context, ShopItem item, int x, int y, int size, 
	                            boolean isSelected, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
		
		// Background - yellow if selected, blue if hovered, gray otherwise
		int bgColor = isSelected ? 0xFFFFAA00 : (hovered ? 0xFF3A4A5A : 0xFF2A2A2A);
		context.fill(x, y, x + size, y + size, bgColor);
		
		// Border
		if (isSelected || hovered) {
			int borderColor = isSelected ? 0xFFFFDD00 : 0xFF4A9EFF;
			context.fill(x, y, x + size, y + 1, borderColor);
			context.fill(x, y + size - 1, x + size, y + size, borderColor);
			context.fill(x, y, x + 1, y + size, borderColor);
			context.fill(x + size - 1, y, x + size, y + size, borderColor);
		}
		
		// Render item icon
		try {
			Item minecraftItem = Registries.ITEM.get(new Identifier("minecraft", item.getItemId()));
			if (minecraftItem != null && minecraftItem != Items.AIR) {
				ItemStack stack = new ItemStack(minecraftItem);
				context.drawItem(stack, x + (size - 16) / 2, y + (size - 16) / 2);
			}
		} catch (Exception e) {
			context.drawText(parent.getTextRenderer(), "?", x + size / 2 - 3, y + size / 2 - 4, 0xFFFFFFFF, false);
		}
	}
	
	private void renderRightPanel(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Background
		context.fill(x - 5, y, x + width + 5, y + height, 0xFF1A1A1A);
		context.fill(x, y + 5, x + width, y + height - 5, 0xFF2A2A2A);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§lĐƠN HÀNG SỐ LƯỢNG LỚN", x + 10, y + 10, 0xFFFFFFFF, true);
		
		int currentY = y + 30;
		
		// === SELECTED ITEM ===
		if (selectedItem != null) {
			context.drawText(parent.getTextRenderer(), "§7Item:", x + 10, currentY, 0xFFFFFFFF, false);
			currentY += 12;
			
			// Item icon + name
			try {
				Item minecraftItem = Registries.ITEM.get(new Identifier("minecraft", selectedItem.getItemId()));
				if (minecraftItem != null && minecraftItem != Items.AIR) {
					ItemStack stack = new ItemStack(minecraftItem);
					context.drawItem(stack, x + 10, currentY);
				}
			} catch (Exception e) {
				// Fallback
			}
			context.drawText(parent.getTextRenderer(), selectedItem.getDisplayName(), 
				x + 30, currentY + 4, 0xFFFFFFFF, false);
			currentY += 25;
			
			// Unit price
			context.drawText(parent.getTextRenderer(), 
				String.format("§7Giá đơn vị: §f%d silver", selectedItem.getSilverPrice()), 
				x + 10, currentY, 0xFFFFFFFF, false);
			currentY += 15;
			
		} else {
			context.drawText(parent.getTextRenderer(), "§7Chọn item bên trái", 
				x + 10, currentY, 0xFF888888, false);
			currentY += 30;
		}
		
		currentY += 10;
		
		// === QUANTITY INPUT ===
		context.drawText(parent.getTextRenderer(), "§7Số rương:", x + 10, currentY, 0xFFFFFFFF, false);
		currentY += 12;
		
		if (quantityField == null) {
			quantityField = new TextFieldWidget(parent.getTextRenderer(), x + 10, currentY, width - 20, 18, 
				Text.literal("Chest Count"));
			quantityField.setMaxLength(6);
			quantityField.setText("1");
		}
		quantityField.setPosition(x + 10, currentY);
		quantityField.setWidth(width - 20);
		quantityField.render(context, mouseX, mouseY, 0);
		currentY += 25;
		
		// Parse chest count
		try {
			int parsed = Integer.parseInt(quantityField.getText());
			if (parsed >= 1) {
				chestCount = parsed;
			} else {
				chestCount = 1;
				quantityField.setText("1");
			}
		} catch (NumberFormatException e) {
			chestCount = 1;
		}
		
		// === PRICE CALCULATION ===
		if (selectedItem != null) {
			long unitPrice = selectedItem.getSilverPrice();
			long[] displayPrice = BulkOrderManager.calculateDisplayPrice(unitPrice, chestCount, useSilverOnly);
			long gold = displayPrice[0];
			long silver = displayPrice[1];
			
			double discount = BulkOrderManager.getConfig().getDiscountForChestCount(chestCount) * 100;
			long totalItems = BulkOrderManager.getConfig().getTotalItemCount(chestCount);
			
			context.drawText(parent.getTextRenderer(), 
				String.format("§7Tổng items: §e%d", totalItems), 
				x + 10, currentY, 0xFFFFFFFF, false);
			currentY += 12;
			
			if (discount > 0) {
				context.drawText(parent.getTextRenderer(), 
					String.format("§7Giảm giá: §a%.0f%%", discount), 
					x + 10, currentY, 0xFFFFFFFF, false);
				currentY += 12;
			}
			
			currentY += 5;
			
			// === PAYMENT SELECTOR ===
			context.drawText(parent.getTextRenderer(), "§7Thanh toán:", x + 10, currentY, 0xFFFFFFFF, false);
			currentY += 12;
			
			// Silver-only button
			int silverButtonY = currentY;
			boolean silverHovered = mouseX >= x + 10 && mouseX <= x + width / 2 - 5 &&
			                        mouseY >= silverButtonY && mouseY <= silverButtonY + 20;
			int silverBg = useSilverOnly ? 0xFF4A9EFF : (silverHovered ? 0xFF3A4A5A : 0xFF2A2A2A);
			context.fill(x + 10, silverButtonY, x + width / 2 - 5, silverButtonY + 20, silverBg);
			
			long totalSilver = unitPrice * BulkOrderManager.getConfig().getItemsPerChest() * chestCount;
			totalSilver = (long) Math.ceil(totalSilver * (1.0 - discount / 100.0));
			
			String silverText = String.format("%ds", totalSilver);
			int silverTextWidth = parent.getTextRenderer().getWidth(silverText);
			context.drawText(parent.getTextRenderer(), silverText, 
				x + 10 + (width / 2 - 15 - silverTextWidth) / 2, silverButtonY + 6, 0xFFFFFFFF, false);
			
			// Mixed button
			int mixedButtonX = x + width / 2;
			boolean mixedHovered = mouseX >= mixedButtonX && mouseX <= x + width - 10 &&
			                       mouseY >= silverButtonY && mouseY <= silverButtonY + 20;
			int mixedBg = !useSilverOnly ? 0xFF4A9EFF : (mixedHovered ? 0xFF3A4A5A : 0xFF2A2A2A);
			context.fill(mixedButtonX, silverButtonY, x + width - 10, silverButtonY + 20, mixedBg);
			
			String mixedText = String.format("%dg+%ds", gold, silver);
			int mixedTextWidth = parent.getTextRenderer().getWidth(mixedText);
			context.drawText(parent.getTextRenderer(), mixedText, 
				mixedButtonX + (width / 2 - 10 - mixedTextWidth) / 2, silverButtonY + 6, 0xFFFFFFFF, false);
			
			currentY += 30;
			
			// === AFFORDABILITY CHECK ===
			long playerSilver = ClientDataCache.getSilverCoins();
			long playerGold = ClientDataCache.getGoldCoins();
			
			boolean canAfford = useSilverOnly ? 
				(playerSilver >= totalSilver) : 
				(playerGold >= gold && playerSilver >= silver);
			
			if (!canAfford) {
				context.drawText(parent.getTextRenderer(), "§cKhông đủ tiền!", 
					x + 10, currentY, 0xFFFF5555, false);
				currentY += 15;
			}
			
			currentY += 10;
			
			// === BUY BUTTON ===
			int buttonY = currentY;
			int buttonHeight = 30;
			boolean buttonHovered = mouseX >= x + 10 && mouseX <= x + width - 10 &&
			                        mouseY >= buttonY && mouseY <= buttonY + buttonHeight;
			
			int buttonBg = canAfford ? 
				(buttonHovered ? 0xFF55DD55 : 0xFF44CC44) : 
				0xFF555555;
			
			context.fill(x + 10, buttonY, x + width - 10, buttonY + buttonHeight, buttonBg);
			
			String buttonText = canAfford ? "§lMUA NGAY" : "§7KHÔNG ĐỦ TIỀN";
			int buttonTextWidth = parent.getTextRenderer().getWidth(buttonText);
			context.drawText(parent.getTextRenderer(), buttonText, 
				x + 10 + (width - 20 - buttonTextWidth) / 2, 
				buttonY + (buttonHeight - 8) / 2, 
				0xFFFFFFFF, false);
		}
	}
	
	// ===== FILTERING =====
	
	private List<ShopItem> getFilteredItems() {
		// Cache check
		if (cachedCategory == selectedCategory && 
		    (cachedSearchQuery != null && cachedSearchQuery.equals(searchQuery))) {
			return cachedFilteredItems;
		}
		
		// Update cache
		cachedCategory = selectedCategory;
		cachedSearchQuery = searchQuery;
		cachedFilteredItems = new ArrayList<>();
		
		// Filter by category and search
		for (ShopItem item : ClientDataCache.getAllShopItems()) {
			
			// Category filter
			if (selectedCategory != ShopCategory.ALL && item.getCategory() != selectedCategory) {
				continue;
			}
			
			// Search filter
			if (!searchQuery.isEmpty()) {
				String query = searchQuery.toLowerCase();
				if (!item.getDisplayName().toLowerCase().contains(query) &&
				    !item.getItemId().toLowerCase().contains(query)) {
					continue;
				}
			}
			
			cachedFilteredItems.add(item);
		}
		
		return cachedFilteredItems;
	}
	
	// ===== INPUT HANDLING =====
	
	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
		// Check search field
		if (searchField != null && searchField.mouseClicked(mouseX, mouseY, button)) {
			searchField.setFocused(true);
			if (quantityField != null) quantityField.setFocused(false);
			return true;
		}
		
		// Check quantity field
		if (quantityField != null && quantityField.mouseClicked(mouseX, mouseY, button)) {
			quantityField.setFocused(true);
			if (searchField != null) searchField.setFocused(false);
			return true;
		}
		
		// Cooldown check
		long currentTime = System.currentTimeMillis();
		if (currentTime - lastClickTime < CLICK_COOLDOWN_MS) {
			return false;
		}
		
		// Category tabs
		int leftWidth = getLeftPanelWidth(contentWidth);
		int tabY = contentY + 45;
		if (mouseY >= tabY && mouseY <= tabY + 30 && mouseX >= contentX + 5 && mouseX <= contentX + leftWidth - 5) {
			ShopCategory[] categories = ShopCategory.values();
			int tabWidth = 80;
			int currentX = contentX + 5 - tabScrollOffset;
			
			for (ShopCategory cat : categories) {
				if (mouseX >= currentX && mouseX <= currentX + tabWidth) {
					selectedCategory = cat;
					scrollOffset = 0;
					lastClickTime = currentTime;
					return true;
				}
				currentX += tabWidth + 2;
			}
		}
		
		// Item grid clicks
		int gridY = contentY + 85;
		int gridHeight = contentHeight - 90;
		if (mouseX >= contentX + 5 && mouseX <= contentX + leftWidth - 5 &&
		    mouseY >= gridY && mouseY <= gridY + gridHeight) {
			
			List<ShopItem> items = getFilteredItems();
			int columns = getGridColumns(contentWidth);
			int startIndex = scrollOffset * columns;
			int endIndex = Math.min(startIndex + (gridHeight / GRID_CELL_SIZE) * columns, items.size());
			
			for (int i = startIndex; i < endIndex; i++) {
				ShopItem item = items.get(i);
				int index = i - startIndex;
				int col = index % columns;
				int row = index / columns;
				
				int cellX = contentX + 5 + col * GRID_CELL_SIZE;
				int cellY = gridY + row * GRID_CELL_SIZE;
				
				if (mouseX >= cellX && mouseX <= cellX + GRID_ICON_SIZE &&
				    mouseY >= cellY && mouseY <= cellY + GRID_ICON_SIZE) {
					selectedItem = item;
					lastClickTime = currentTime;
					return true;
				}
			}
		}
		
		// Right panel clicks
		if (selectedItem != null) {
			int rightWidth = contentWidth - leftWidth - 10;
			int rightX = contentX + leftWidth + 10;
			
			// Payment selector buttons
			int silverButtonY = contentY + 185; // Approximate position
			if (mouseY >= silverButtonY && mouseY <= silverButtonY + 20) {
				if (mouseX >= rightX + 10 && mouseX <= rightX + rightWidth / 2 - 5) {
					useSilverOnly = true;
					lastClickTime = currentTime;
					return true;
				} else if (mouseX >= rightX + rightWidth / 2 && mouseX <= rightX + rightWidth - 10) {
					useSilverOnly = false;
					lastClickTime = currentTime;
					return true;
				}
			}
			
			// Buy button
			int buttonY = silverButtonY + 60; // Approximate position
			int buttonHeight = 30;
			if (mouseX >= rightX + 10 && mouseX <= rightX + rightWidth - 10 &&
			    mouseY >= buttonY && mouseY <= buttonY + buttonHeight) {
				
				// Check affordability
				long unitPrice = selectedItem.getSilverPrice();
				long[] displayPrice = BulkOrderManager.calculateDisplayPrice(unitPrice, chestCount, useSilverOnly);
				long gold = displayPrice[0];
				long silver = displayPrice[1];
				
				long playerSilver = ClientDataCache.getSilverCoins();
				long playerGold = ClientDataCache.getGoldCoins();
				
				boolean canAfford = useSilverOnly ? 
					(playerSilver >= (unitPrice * BulkOrderManager.getConfig().getItemsPerChest() * chestCount)) : 
					(playerGold >= gold && playerSilver >= silver);
				
				if (canAfford) {
					// Send bulk order packet
					ModNetworking.sendBulkOrderPurchase(selectedItem.getItemId(), chestCount, useSilverOnly);
					
					// Reset state
					selectedItem = null;
					chestCount = 1;
					if (quantityField != null) {
						quantityField.setText("1");
					}
					
					lastClickTime = currentTime;
					return true;
				}
			}
		}
		
		return false;
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// Scroll item grid
		int leftWidth = getLeftPanelWidth(lastContentWidth);
		int gridY = lastContentY + 85;
		int gridHeight = lastContentHeight - 90;
		
		if (mouseX >= lastContentX + 5 && mouseX <= lastContentX + leftWidth - 5 &&
		    mouseY >= gridY && mouseY <= gridY + gridHeight) {
			scrollOffset -= (int) verticalAmount;
			
			List<ShopItem> items = getFilteredItems();
			int columns = getGridColumns(lastContentWidth);
			int visibleRows = gridHeight / GRID_CELL_SIZE;
			int totalRows = (int) Math.ceil((double) items.size() / columns);
			int maxScroll = Math.max(0, totalRows - visibleRows);
			scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
			
			return true;
		}
		
		return false;
	}
	
	public boolean charTyped(char chr, int modifiers) {
		if (searchField != null && searchField.isFocused() && searchField.charTyped(chr, modifiers)) {
			searchQuery = searchField.getText();
			scrollOffset = 0;
			return true;
		}
		
		if (quantityField != null && quantityField.isFocused() && quantityField.charTyped(chr, modifiers)) {
			return true;
		}
		
		return false;
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (searchField != null && searchField.isFocused() && searchField.keyPressed(keyCode, scanCode, modifiers)) {
			searchQuery = searchField.getText();
			scrollOffset = 0;
			return true;
		}
		
		if (quantityField != null && quantityField.isFocused() && quantityField.keyPressed(keyCode, scanCode, modifiers)) {
			return true;
		}
		
		return false;
	}
	
	public void tick() {
		if (searchField != null) {
			searchField.tick();
		}
		if (quantityField != null) {
			quantityField.tick();
		}
	}
}
