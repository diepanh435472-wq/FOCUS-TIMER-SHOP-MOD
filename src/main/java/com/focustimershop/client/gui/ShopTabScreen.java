package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.shop.ShopCategory;
import com.focustimershop.shop.ShopItem;
import com.focustimershop.shop.ShoppingCart;
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
 * Shop tab - 2-column layout with shopping cart
 * Left: item grid selector, Right: cart + checkout
 */
public class ShopTabScreen {
	private final MainMenuScreen parent;
	private ShopCategory selectedCategory = ShopCategory.BUILDING_BLOCKS;
	private int scrollOffset = 0;
	private String searchQuery = "";
	private ShoppingCart cart = new ShoppingCart();
	private TextFieldWidget searchField = null;
	private int lastContentX = 0;
	private int lastContentY = 0;
	private int lastContentWidth = 0;
	private int lastContentHeight = 0;
	
	// ===== TAB HORIZONTAL SCROLL =====
	private int tabScrollOffset = 0;  // Horizontal scroll for category tabs
	private boolean isDraggingTabs = false;
	private double dragStartX = 0;
	private int dragStartOffset = 0;
	// =================================
	
	// ===== GRID GEOMETRY CONSTANTS =====
	private static final int GRID_ICON_SIZE = 32;
	private static final int GRID_SPACING = 4;
	private static final int GRID_CELL_SIZE = GRID_ICON_SIZE + GRID_SPACING; // 36
	// ====================================
	
	// ===== FIX: CACHED FILTERED LIST =====
	// Single source of truth for visible items - shared between render and click
	private List<ShopItem> cachedFilteredItems = new ArrayList<>();
	private ShopCategory cachedCategory = null;
	private String cachedSearchQuery = null;
	// =====================================

	public ShopTabScreen(MainMenuScreen parent) {
		this.parent = parent;
	}
	
	// ===== FIX: SHARED GRID GEOMETRY HELPERS =====
	/**
	 * Get left panel width (65% of content width)
	 */
	private int getLeftPanelWidth(int contentWidth) {
		return (int)(contentWidth * 0.65);
	}
	
	/**
	 * Get actual grid width used for item rendering
	 * This accounts for padding/margins in renderLeftPanel
	 */
	private int getGridWidth(int contentWidth) {
		// renderLeftPanel calls renderItemGrid with (width - 10)
		return getLeftPanelWidth(contentWidth) - 10;
	}
	
	/**
	 * Get number of columns in grid
	 * MUST be used by render, click, and scroll to ensure consistency
	 */
	private int getGridColumns(int contentWidth) {
		return Math.max(1, getGridWidth(contentWidth) / GRID_CELL_SIZE);
	}
	// =============================================

	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// Save coords for widget positioning and scroll calculation
		lastContentX = x;
		lastContentY = y;
		lastContentWidth = width;
		lastContentHeight = height;
		
		// Initialize search field if needed
		if (searchField == null) {
			searchField = new TextFieldWidget(parent.getTextRenderer(), x + 10, y + 27, width - 20, 16, Text.literal(""));
			searchField.setMaxLength(50);
			searchField.setPlaceholder(Text.literal("🔍 Tìm kiếm..."));
			searchField.setFocusUnlocked(true);  // Allow focus
			searchField.setEditable(true);  // Enable editing
			searchField.setChangedListener(text -> {
				searchQuery = text;
				scrollOffset = 0; // Reset scroll when search changes
			});
		}
		
		// Update search field position (in case window resized)
		int leftWidth = (int)(width * 0.65);
		searchField.setX(x + 10);
		searchField.setY(y + 27);
		searchField.setWidth(leftWidth - 15);
		
		// Tick search field to update cursor
		searchField.tick();
		
		// Split layout: 65% left for grid, 35% right for cart
		int rightWidth = width - leftWidth - 20;
		
		// Left panel: search + categories + item grid
		renderLeftPanel(context, x, y, leftWidth, height, mouseX, mouseY);
		
		// Right panel: cart + checkout
		int cartX = x + leftWidth + 20;
		renderRightPanel(context, cartX, y, rightWidth, height, mouseX, mouseY);
	}

	private void renderLeftPanel(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Title
		context.drawText(parent.getTextRenderer(), "Shop", x + 5, y + 5, 0xFFFFFFFF, true);
		
		// Search box - render the actual widget
		int searchY = y + 25;
		if (searchField != null) {
			searchField.render(context, mouseX, mouseY, 0);
		}
		
		// Category tabs
		int tabY = y + 50;
		renderCategoryTabs(context, x + 5, tabY, width - 10, mouseX, mouseY);
		
		// Item grid
		int gridY = y + 85;
		int gridHeight = height - 90;
		renderItemGrid(context, x + 5, gridY, width - 10, gridHeight, mouseX, mouseY);
	}

	private void renderCategoryTabs(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		String[] tabNames = {"Tất cả", "Xây dựng", "Màu sắc", "Tự nhiên", "Chức năng", "Redstone", "Công cụ", "Đồ ăn", "Nguyên liệu"};
		ShopCategory[] categories = {ShopCategory.ALL, ShopCategory.BUILDING_BLOCKS, ShopCategory.COLORED_BLOCKS, ShopCategory.NATURAL_BLOCKS, ShopCategory.FUNCTIONAL_BLOCKS, ShopCategory.REDSTONE, ShopCategory.TOOLS_UTILITIES, ShopCategory.FOOD_DRINKS, ShopCategory.INGREDIENTS};
		
		int tabWidth = 80;
		int spacing = 5;
		int totalTabsWidth = tabNames.length * (tabWidth + spacing) - spacing;
		int maxScroll = Math.max(0, totalTabsWidth - width);
		
		// Clamp scroll offset
		tabScrollOffset = Math.max(0, Math.min(tabScrollOffset, maxScroll));
		
		// Enable scissor (clip rendering outside bounds)
		context.enableScissor(x, y, x + width, y + 25);
		
		for (int i = 0; i < tabNames.length; i++) {
			int tabX = x + i * (tabWidth + spacing) - tabScrollOffset;
			
			// Skip rendering if tab is outside visible area
			if (tabX + tabWidth < x || tabX > x + width) {
				continue;
			}
			
			boolean selected = (selectedCategory == categories[i]);
			boolean hovered = mouseX >= tabX && mouseX <= tabX + tabWidth &&
			                  mouseY >= y && mouseY <= y + 25;
			
			int bgColor = selected ? 0xFF4A9EFF : (hovered ? 0xFF3A3A3A : 0xFF2A2A2A);
			context.fill(tabX, y, tabX + tabWidth, y + 25, bgColor);
			
			if (selected) {
				context.fill(tabX, y + 23, tabX + tabWidth, y + 25, 0xFFFFFFFF);
			}
			
			int textWidth = parent.getTextRenderer().getWidth(tabNames[i]);
			int textX = tabX + (tabWidth - textWidth) / 2;
			context.drawText(parent.getTextRenderer(), tabNames[i], textX, y + 8, 0xFFFFFFFF, false);
		}
		
		context.disableScissor();
		
		// Scroll hint (if tabs overflow)
		if (maxScroll > 0) {
			String hint = "↔ Scroll";
			int hintWidth = parent.getTextRenderer().getWidth(hint);
			context.drawText(parent.getTextRenderer(), hint, x + width - hintWidth - 5, y - 12, 0xFF888888, false);
		}
	}

	private void renderItemGrid(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		List<ShopItem> items = getFilteredItems();
		
		// ===== FIX: USE CONSTANTS INSTEAD OF HARDCODED VALUES =====
		int columns = Math.max(1, width / GRID_CELL_SIZE);
		// ===========================================================
		
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
			
			renderItemCell(context, item, cellX, cellY, GRID_ICON_SIZE, mouseX, mouseY);
		}
		
		// Scroll indicator
		if (totalRows > visibleRows) {
			context.drawText(parent.getTextRenderer(), "§7↕ Scroll", x + width - 50, y - 15, 0xFF888888, false);
		}
	}

	private void renderItemCell(DrawContext context, ShopItem item, int x, int y, int size, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX <= x + size && mouseY >= y && mouseY <= y + size;
		
		// Background
		int bgColor = hovered ? 0xFF3A4A5A : 0xFF2A2A2A;
		context.fill(x, y, x + size, y + size, bgColor);
		
		// Border
		if (hovered) {
			context.fill(x, y, x + size, y + 1, 0xFF4A9EFF);
			context.fill(x, y + size - 1, x + size, y + size, 0xFF4A9EFF);
			context.fill(x, y, x + 1, y + size, 0xFF4A9EFF);
			context.fill(x + size - 1, y, x + size, y + size, 0xFF4A9EFF);
		}
		
		// Render item icon (scaled)
		try {
			// Create proper namespaced identifier
			Item minecraftItem = Registries.ITEM.get(new Identifier("minecraft", item.getItemId()));
			if (minecraftItem != null && minecraftItem != Items.AIR) {
				ItemStack stack = new ItemStack(minecraftItem);
				// Center the 16x16 item in 32x32 cell
				context.drawItem(stack, x + (size - 16) / 2, y + (size - 16) / 2);
			}
		} catch (Exception e) {
			// Fallback: draw placeholder
			context.drawText(parent.getTextRenderer(), "?", x + size / 2 - 3, y + size / 2 - 4, 0xFFFFFFFF, false);
		}
		
		// Tooltip on hover
		if (hovered) {
			// Will render after main content
		}
	}

	private void renderRightPanel(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Background panel
		context.fill(x - 5, y, x + width + 5, y + height, 0xFF1A1A1A);
		context.fill(x, y + 5, x + width, y + height - 5, 0xFF2A2A2A);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§lGIỎ HÀNG", x + 10, y + 10, 0xFFFFFFFF, true);
		
		// Cart items list
		int listY = y + 30;
		int listHeight = height - 180;
		renderCartItems(context, x + 10, listY, width - 20, listHeight, mouseX, mouseY);
		
		// Payment method selector
		int paymentY = y + height - 145;
		renderPaymentSelector(context, x + 10, paymentY, width - 20, mouseX, mouseY);
		
		// Total cost
		int totalY = y + height - 95;
		renderTotalCost(context, x + 10, totalY, width - 20);
		
		// Checkout button
		int btnY = y + height - 55;
		renderCheckoutButton(context, x + 10, btnY, width - 20, mouseX, mouseY);
	}

	private void renderCartItems(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		Map<String, Integer> items = cart.getItems();
		
		if (items.isEmpty()) {
			context.drawText(parent.getTextRenderer(), "§7Giỏ hàng trống", x + 5, y + 10, 0xFF888888, false);
			return;
		}
		
		int lineHeight = 30;
		int currentY = y;
		
		for (Map.Entry<String, Integer> entry : items.entrySet()) {
			String itemId = entry.getKey();
			int quantity = entry.getValue();
			ShopItem item = ClientDataCache.getShopItem(itemId);  // Use client cache
			
			if (item == null) continue;
			
			// Item row background
			context.fill(x, currentY, x + width, currentY + lineHeight - 2, 0xFF1A1A1A);
			
			// Category + name
			String displayText = "§7[" + item.getCategory().getShortName() + "] §f" + 
			                     item.getDisplayName() + " §ex" + quantity;
			context.drawText(parent.getTextRenderer(), displayText, x + 5, currentY + 5, 0xFFFFFFFF, false);
			
			// Buttons: [-] and [x]
			int btnSize = 18;
			int btnY = currentY + 5;
			
			// Decrease button [-]
			int decreaseX = x + width - btnSize * 2 - 5;
			boolean hoverDecrease = mouseX >= decreaseX && mouseX <= decreaseX + btnSize &&
			                        mouseY >= btnY && mouseY <= btnY + btnSize;
			context.fill(decreaseX, btnY, decreaseX + btnSize, btnY + btnSize, hoverDecrease ? 0xFF4A5A6A : 0xFF3A3A3A);
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), "-", decreaseX + btnSize / 2, btnY + 5, 0xFFFFFFFF);
			
			// Remove button [x]
			int removeX = x + width - btnSize;
			boolean hoverRemove = mouseX >= removeX && mouseX <= removeX + btnSize &&
			                      mouseY >= btnY && mouseY <= btnY + btnSize;
			context.fill(removeX, btnY, removeX + btnSize, btnY + btnSize, hoverRemove ? 0xFFFF4444 : 0xFF3A3A3A);
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), "x", removeX + btnSize / 2, btnY + 5, 0xFFFFFFFF);
			
			currentY += lineHeight;
			
			// Stop if exceeds height
			if (currentY - y > height - lineHeight) {
				context.drawText(parent.getTextRenderer(), "§7...scroll", x + 5, currentY, 0xFF888888, false);
				break;
			}
		}
	}

	private void renderPaymentSelector(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		context.drawText(parent.getTextRenderer(), "§7Thanh toán bằng:", x, y, 0xFFAAAAAA, false);
		
		int btnWidth = (width - 10) / 2;
		int btnHeight = 30;
		int btnY = y + 15;
		
		// Silver button
		int silverX = x;
		boolean selectedSilver = cart.isUsingSilver();
		boolean hoverSilver = mouseX >= silverX && mouseX <= silverX + btnWidth &&
		                      mouseY >= btnY && mouseY <= btnY + btnHeight;
		int silverColor = selectedSilver ? 0xFF4A9EFF : (hoverSilver ? 0xFF3A3A3A : 0xFF2A2A2A);
		context.fill(silverX, btnY, silverX + btnWidth, btnY + btnHeight, silverColor);
		if (selectedSilver) {
			context.fill(silverX, btnY, silverX + btnWidth, btnY + 2, 0xFFFFFFFF);
		}
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Silver", silverX + btnWidth / 2, btnY + 10, 0xFFC0C0C0);
		
		// Gold button
		int goldX = x + btnWidth + 10;
		boolean selectedGold = !cart.isUsingSilver();
		boolean hoverGold = mouseX >= goldX && mouseX <= goldX + btnWidth &&
		                    mouseY >= btnY && mouseY <= btnY + btnHeight;
		int goldColor = selectedGold ? 0xFF4A9EFF : (hoverGold ? 0xFF3A3A3A : 0xFF2A2A2A);
		context.fill(goldX, btnY, goldX + btnWidth, btnY + btnHeight, goldColor);
		if (selectedGold) {
			context.fill(goldX, btnY, goldX + btnWidth, btnY + 2, 0xFFFFFFFF);
		}
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Gold", goldX + btnWidth / 2, btnY + 10, 0xFFFFD700);
	}

	private void renderTotalCost(DrawContext context, int x, int y, int width) {
		int[] cost = cart.getTotalCostMixed();
		int goldCost = cost[0];
		int silverCost = cost[1];
		
		context.fill(x, y, x + width, y + 1, 0xFF4A4A4A);
		context.drawText(parent.getTextRenderer(), "§7Tổng tiền:", x, y + 10, 0xFFAAAAAA, false);
		
		// Display mixed cost
		String costText = cart.getTotalCostDisplay();
		int textWidth = parent.getTextRenderer().getWidth(costText);
		
		// Color: gold if has gold, silver otherwise
		int color = goldCost > 0 ? 0xFFFFD700 : 0xFFC0C0C0;
		context.drawText(parent.getTextRenderer(), costText, x + width - textWidth, y + 10, color, true);
	}

	private void renderCheckoutButton(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		int btnHeight = 40;
		
		// Check if can afford (mixed payment)
		int[] cost = cart.getTotalCostMixed();
		int goldCost = cost[0];
		int silverCost = cost[1];
		
		boolean canAfford;
		if (cart.isUsingSilver()) {
			// Silver mode: check total silver
			canAfford = ClientDataCache.getSilverCoins() >= silverCost;
		} else {
			// Gold mode: check gold + silver separately
			int playerGold = ClientDataCache.getGoldCoins();
			int playerSilver = ClientDataCache.getSilverCoins();
			canAfford = (playerGold >= goldCost) && (playerSilver >= silverCost);
		}
		
		boolean empty = cart.isEmpty();
		boolean enabled = !empty && canAfford;
		
		boolean hovered = mouseX >= x && mouseX <= x + width &&
		                  mouseY >= y && mouseY <= y + btnHeight;
		
		int bgColor = enabled ? (hovered ? 0xFF5ABAFF : 0xFF4A9EFF) : 0xFF444444;
		context.fill(x, y, x + width, y + btnHeight, bgColor);
		
		if (enabled) {
			context.fill(x, y, x + width, y + 2, 0xFF6ABAFF);
			context.fill(x, y + btnHeight - 2, x + width, y + btnHeight, 0xFF6ABAFF);
		}
		
		String buttonText = empty ? "§7Giỏ hàng trống" : 
		                    (!canAfford ? "§7Không đủ tiền" : "§lTHANH TOÁN");
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), buttonText, 
			x + width / 2, y + 14, 0xFFFFFFFF);
	}

	private List<ShopItem> getFilteredItems() {
		// ===== FIX: CHECK CACHE FIRST =====
		// Only rebuild if category or search changed
		if (cachedCategory == selectedCategory && 
		    searchQuery.equals(cachedSearchQuery) &&
		    !cachedFilteredItems.isEmpty()) {
			// Return cached list - SAME reference for render and click
			return cachedFilteredItems;
		}
		// ====================================
		
		// Use client cache instead of server-side ShopManager
		List<ShopItem> all = new ArrayList<>(ClientDataCache.getAllShopItems());
		List<ShopItem> filtered = new ArrayList<>();
		
		if (all.isEmpty()) {
			System.err.println("[SHOP] WARNING: No shop items loaded from ClientDataCache!");
		}
		
		for (ShopItem item : all) {
			// Filter by category
			if (selectedCategory != ShopCategory.ALL && item.getCategory() != selectedCategory) {
				continue;
			}
			
			// Filter by search
			if (!searchQuery.isEmpty() && !item.getDisplayName().toLowerCase().contains(searchQuery.toLowerCase())) {
				continue;
			}
			
			filtered.add(item);
		}
		
		// Apply sorting for colored blocks
		if (selectedCategory == ShopCategory.COLORED_BLOCKS) {
			filtered.sort(new ColoredBlockComparator());
		}
		
		// ===== FIX: UPDATE CACHE =====
		cachedFilteredItems = filtered;
		cachedCategory = selectedCategory;
		cachedSearchQuery = searchQuery;
		// ==============================
		
		return filtered;
	}
	
	/**
	 * Comparator for colored blocks - sorts by group type first, then by color within group
	 * FIX: Added tiebreaker (itemId comparison) for stable sort
	 */
	private static class ColoredBlockComparator implements java.util.Comparator<ShopItem> {
		// Group order: Len → Thảm → Đất nung → Bê tông → Bột bê tông → Đất nung tráng men → Kính màu → Tấm kính màu → Shulker box → Giường → Nến → Cờ hiệu
		private static final String[] GROUP_ORDER = {
			"_wool", "_carpet", "_terracotta", "_concrete", "_concrete_powder", 
			"_glazed_terracotta", "_stained_glass", "_stained_glass_pane", 
			"_shulker_box", "_bed", "_candle", "_banner"
		};
		
		// Color order: White, Orange, Magenta, Light Blue, Yellow, Lime, Pink, Gray, Light Gray, Cyan, Purple, Blue, Brown, Green, Red, Black
		private static final String[] COLOR_ORDER = {
			"white", "orange", "magenta", "light_blue", "yellow", "lime",
			"pink", "gray", "light_gray", "cyan", "purple", "blue",
			"brown", "green", "red", "black"
		};
		
		@Override
		public int compare(ShopItem a, ShopItem b) {
			String idA = a.getItemId();
			String idB = b.getItemId();
			
			// Find group indices
			int groupA = getGroupIndex(idA);
			int groupB = getGroupIndex(idB);
			
			// Compare by group first
			if (groupA != groupB) {
				return Integer.compare(groupA, groupB);
			}
			
			// Same group - compare by color
			int colorA = getColorIndex(idA);
			int colorB = getColorIndex(idB);
			
			if (colorA != colorB) {
				return Integer.compare(colorA, colorB);
			}
			
			// ===== FIX: TIEBREAKER FOR STABLE SORT =====
			// If same group AND same color, sort by itemId alphabetically
			// This ensures consistent order for items with equal group+color indices
			return idA.compareTo(idB);
			// ============================================
		}
		
		private int getGroupIndex(String itemId) {
			for (int i = 0; i < GROUP_ORDER.length; i++) {
				if (itemId.contains(GROUP_ORDER[i])) {
					return i;
				}
			}
			return 999; // Unknown group - put at end
		}
		
		private int getColorIndex(String itemId) {
			for (int i = 0; i < COLOR_ORDER.length; i++) {
				if (itemId.startsWith("minecraft:" + COLOR_ORDER[i])) {
					return i;
				}
			}
			return 999; // No color prefix - put at end (e.g. plain terracotta, candle)
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
		// Check if search field was clicked
		if (searchField != null && searchField.mouseClicked(mouseX, mouseY, button)) {
			// EXPLICIT set focus sau khi click thành công
			searchField.setFocused(true);
			return true;
		}
		
		int leftWidth = getLeftPanelWidth(contentWidth);
		int rightWidth = contentWidth - leftWidth - 20;
		int cartX = contentX + leftWidth + 20;
		
		// Check category tabs - must match render coordinates exactly
		int tabY = contentY + 50;
		String[] tabNames = {"Tất cả", "Xây dựng", "Màu sắc", "Tự nhiên", "Chức năng", "Redstone", "Công cụ", "Đồ ăn", "Nguyên liệu"};
		ShopCategory[] categories = {ShopCategory.ALL, ShopCategory.BUILDING_BLOCKS, ShopCategory.COLORED_BLOCKS, ShopCategory.NATURAL_BLOCKS, ShopCategory.FUNCTIONAL_BLOCKS, ShopCategory.REDSTONE, ShopCategory.TOOLS_UTILITIES, ShopCategory.FOOD_DRINKS, ShopCategory.INGREDIENTS};
		int tabWidth = 80;
		int spacing = 5;
		
		// Check if clicking in tab area (for drag detection)
		int leftPanelWidth = getLeftPanelWidth(contentWidth);
		if (mouseY >= tabY && mouseY <= tabY + 25 && mouseX >= contentX && mouseX <= contentX + leftPanelWidth) {
			isDraggingTabs = true;
			dragStartX = mouseX;
			dragStartOffset = tabScrollOffset;
		}
		
		for (int i = 0; i < tabNames.length; i++) {
			int tabX = contentX + 5 + i * (tabWidth + spacing) - tabScrollOffset;  // Apply scroll offset
			if (mouseX >= tabX && mouseX <= tabX + tabWidth &&
			    mouseY >= tabY && mouseY <= tabY + 25) {
				selectedCategory = categories[i];
				scrollOffset = 0;  // Reset scroll when changing category
				return true;
			}
		}
		
		// Check item grid clicks (add to cart) - match render coordinates
		int gridY = contentY + 85;  // Match render: y + 85
		int gridHeight = contentHeight - 90;
		List<ShopItem> items = getFilteredItems();
		
		// ===== FIX: USE SHARED GRID GEOMETRY =====
		// MUST use getGridColumns() to match render geometry exactly
		int columns = getGridColumns(contentWidth);
		// =========================================
		
		int visibleRows = gridHeight / GRID_CELL_SIZE;
		int startIndex = scrollOffset * columns;
		int endIndex = Math.min(startIndex + visibleRows * columns, items.size());
		
		for (int i = startIndex; i < endIndex; i++) {
			ShopItem item = items.get(i);
			int index = i - startIndex;
			int col = index % columns;
			int row = index / columns;
			
			int cellX = contentX + 5 + col * GRID_CELL_SIZE;  // Match render: x + 5
			int cellY = gridY + row * GRID_CELL_SIZE;
			
			if (mouseX >= cellX && mouseX <= cellX + GRID_ICON_SIZE &&
			    mouseY >= cellY && mouseY <= cellY + GRID_ICON_SIZE) {
				cart.addItem(item.getItemId(), 1);
				return true;
			}
		}
		
		// Check cart item buttons (decrease/remove) - match render coordinates
		int listY = contentY + 30;  // Match renderCartItems offset
		Map<String, Integer> cartItems = cart.getItems();
		int lineHeight = 30;
		int currentY = listY;
		int cartListX = cartX + 10;  // Match render: x + 10
		int cartListWidth = rightWidth - 20;
		
		for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
			String itemId = entry.getKey();
			int btnSize = 18;
			int btnY = currentY + 5;
			
			// Decrease button [-]
			int decreaseX = cartListX + cartListWidth - btnSize * 2 - 5;
			if (mouseX >= decreaseX && mouseX <= decreaseX + btnSize &&
			    mouseY >= btnY && mouseY <= btnY + btnSize) {
				cart.decreaseItem(itemId);
				return true;
			}
			
			// Remove button [x]
			int removeX = cartListX + cartListWidth - btnSize;
			if (mouseX >= removeX && mouseX <= removeX + btnSize &&
			    mouseY >= btnY && mouseY <= btnY + btnSize) {
				cart.removeItem(itemId);
				return true;
			}
			
			currentY += lineHeight;
		}
		
		// Check payment method buttons - match renderPaymentSelector
		int paymentY = contentY + contentHeight - 145;
		int btnWidth = (rightWidth - 30) / 2;  // Match render calculation
		int btnHeight = 30;
		int btnY = paymentY + 15;
		int paymentX = cartX + 10;  // Match render: x + 10
		
		// Silver button
		int silverX = paymentX;
		if (mouseX >= silverX && mouseX <= silverX + btnWidth &&
		    mouseY >= btnY && mouseY <= btnY + btnHeight) {
			cart.setPaymentMethod(true);
			return true;
		}
		
		// Gold button
		int goldX = silverX + btnWidth + 10;
		if (mouseX >= goldX && mouseX <= goldX + btnWidth &&
		    mouseY >= btnY && mouseY <= btnY + btnHeight) {
			cart.setPaymentMethod(false);
			return true;
		}
		
		// Check checkout button - match renderCheckoutButton
		int checkoutY = contentY + contentHeight - 55;
		int checkoutHeight = 40;
		int checkoutX = cartX + 10;  // Match render: x + 10
		int checkoutWidth = rightWidth - 20;
		
		if (mouseX >= checkoutX && mouseX <= checkoutX + checkoutWidth &&
		    mouseY >= checkoutY && mouseY <= checkoutY + checkoutHeight) {
			// Check if can checkout (mixed payment)
			int[] cost = cart.getTotalCostMixed();
			int goldCost = cost[0];
			int silverCost = cost[1];
			
			boolean canAfford;
			if (cart.isUsingSilver()) {
				// Silver mode
				canAfford = ClientDataCache.getSilverCoins() >= silverCost;
			} else {
				// Gold mode: check both currencies
				canAfford = (ClientDataCache.getGoldCoins() >= goldCost) && 
				            (ClientDataCache.getSilverCoins() >= silverCost);
			}
			
			if (!cart.isEmpty() && canAfford) {
				// Send checkout packet
				ModNetworking.sendShopCheckout(cart.getItems(), cart.isUsingSilver());
				// Clear cart after successful send
				cart.clear();
				return true;
			}
		}
		
		return false;
	}

	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// Check if scrolling over tabs area (horizontal scroll)
		int tabY = lastContentY + 50;
		int leftPanelWidth = getLeftPanelWidth(lastContentWidth);
		
		if (mouseY >= tabY && mouseY <= tabY + 25 && mouseX >= lastContentX && mouseX <= lastContentX + leftPanelWidth) {
			// Horizontal scroll for tabs
			tabScrollOffset -= (int)(verticalAmount * 20);  // Scroll tabs
			return true;
		}
		
		// Vertical scroll for item grid (existing logic)
		int gridHeight = lastContentHeight - 90;
		
		List<ShopItem> items = getFilteredItems();
		
		// ===== FIX: USE SHARED GRID GEOMETRY =====
		// MUST use getGridColumns() to match render geometry exactly
		int columns = getGridColumns(lastContentWidth);
		// =========================================
		
		int visibleRows = Math.max(1, gridHeight / GRID_CELL_SIZE);
		int totalRows = (int) Math.ceil((double) items.size() / columns);
		int maxScroll = Math.max(0, totalRows - visibleRows);
		
		scrollOffset -= (int) verticalAmount;
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
		
		return true;
	}
	
	/**
	 * Handle mouse release (stop dragging)
	 */
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		isDraggingTabs = false;
		return false;
	}
	
	/**
	 * Handle mouse drag (move tabs)
	 */
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (isDraggingTabs) {
			int dragDelta = (int)(mouseX - dragStartX);
			tabScrollOffset = dragStartOffset - dragDelta;  // Drag opposite direction
			return true;
		}
		return false;
	}
	
	/**
	 * Handle keyboard input for search field
	 */
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (searchField != null && searchField.isFocused()) {
			return searchField.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}
	
	/**
	 * Handle character typing for search field
	 */
	public boolean charTyped(char chr, int modifiers) {
		if (searchField != null && searchField.isFocused()) {
			return searchField.charTyped(chr, modifiers);
		}
		return false;
	}
}
