package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.luckychest.ChestRarity;
import com.focustimershop.luckychest.ChestTier;
import com.focustimershop.luckychest.LootReward;
import com.focustimershop.luckychest.LuckyChestManager;
import com.focustimershop.luckychest.PaymentOption;
import com.focustimershop.network.ModNetworking;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Map;

/**
 * Lucky Chest tab - gacha system
 */
public class LuckyChestTabScreen {
	private final MainMenuScreen parent;
	private int selectedChestIndex = 0;
	private boolean showDetailModal = false; // Modal state for "Xem thêm"
	private int modalScrollOffset = 0; // Scroll position for modal content

	public LuckyChestTabScreen(MainMenuScreen parent) {
		this.parent = parent;
	}

	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// Title
		context.drawText(parent.getTextRenderer(), "Lucky Chest", x + 10, y + 10, 0xFFFFFFFF, true);
		context.drawText(parent.getTextRenderer(), "§7Open chests for random rewards!", x + 10, y + 30, 0xFFAAAAAA, false);

		// Split layout: left = grid selector, right = detail panel
		int leftWidth = (int)(width * 0.6); // 60% for grid
		int rightWidth = width - leftWidth - 30; // 40% for details, with padding
		
		ChestTier selectedTier = ChestTier.values()[selectedChestIndex];
		
		// Render modal if active (overlay on top of everything)
		if (showDetailModal) {
			// WHEN MODAL IS OPEN: Show ONLY modal with dimmed overlay
			renderDetailModal(context, selectedTier, x, y, width, height, mouseX, mouseY);
		} else {
			// WHEN MODAL IS CLOSED: Show normal chest list + detail panel
			
			// Left: Chest selector grid
			renderChestSelector(context, x + 10, y + 50, leftWidth, mouseX, mouseY);

			// Right: Selected chest details
			int detailX = x + leftWidth + 20;
			int detailY = y + 50;
			int detailHeight = height - 60;
			
			// Draw detail panel background
			context.fill(detailX - 5, detailY - 5, detailX + rightWidth + 5, detailY + detailHeight + 5, 0xFF1A1A1A);
			context.fill(detailX, detailY, detailX + rightWidth, detailY + detailHeight, 0xFF2A2A2A);
			
			renderChestDetails(context, selectedTier, detailX + 10, detailY + 10, rightWidth - 20, detailHeight - 20, mouseX, mouseY);
		}
	}

	private void renderChestSelector(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		ChestTier[] tiers = ChestTier.values();
		int cardWidth = 80;
		int cardHeight = 100;
		int spacing = 8;
		int perRow = Math.max(1, width / (cardWidth + spacing));

		for (int i = 0; i < tiers.length; i++) {
			ChestTier tier = tiers[i];
			int col = i % perRow;
			int row = i / perRow;
			int cardX = x + col * (cardWidth + spacing);
			int cardY = y + row * (cardHeight + spacing);

			boolean selected = (i == selectedChestIndex);
			boolean hovered = mouseX >= cardX && mouseX <= cardX + cardWidth && 
			                  mouseY >= cardY && mouseY <= cardY + cardHeight;
			
			// Check if player can afford (any payment option for openOne)
			int playerSilver = ClientDataCache.getSilverCoins();
			int playerGold = ClientDataCache.getGoldCoins();
			boolean canAfford = tier.canAffordOpenOne(playerSilver, playerGold);
			
			// Main card background
			int bgColor = selected ? 0xFF3A4A5A : (hovered ? 0xFF3A3A3A : 0xFF2A2A2A);
			context.fill(cardX, cardY, cardX + cardWidth, cardY + cardHeight, bgColor);
			
			// Border for selected
			if (selected) {
				context.fill(cardX, cardY, cardX + cardWidth, cardY + 2, 0xFFFFFFFF);
				context.fill(cardX, cardY + cardHeight - 2, cardX + cardWidth, cardY + cardHeight, 0xFFFFFFFF);
				context.fill(cardX, cardY, cardX + 2, cardY + cardHeight, 0xFFFFFFFF);
				context.fill(cardX + cardWidth - 2, cardY, cardX + cardWidth, cardY + cardHeight, 0xFFFFFFFF);
			}
			
			// LAYOUT: Tên → Icon 3D → Giá → X/V status
			
			// 1. Tên chest (trên, centered)
			String shortName = getShortChestName(tier);
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), shortName, 
				cardX + cardWidth / 2, cardY + 5, 0xFFFFFFFF);
			
			// 2. Icon 3D đại diện (giữa)
			int iconY = cardY + 20;
			renderChestIcon(context, tier, cardX + cardWidth / 2 - 8, iconY);
			
			// 3. Giá (dưới icon) - show first/cheapest option
			int priceY = iconY + 24;
			List<PaymentOption> options = tier.getOpenOneOptions();
			String priceText = options.isEmpty() ? "?" : options.get(0).getShortDisplay();
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), priceText, 
				cardX + cardWidth / 2, priceY, 0xFFC0C0C0);
			
			// 4. Status bar ở bottom (separator + X/V indicator)
			int statusY = cardY + cardHeight - 20;
			context.fill(cardX, statusY, cardX + cardWidth, statusY + 1, 0xFF4A4A4A);
			
			// X or V indicator
			String statusIcon = canAfford ? "§a✓" : "§c✗";
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), statusIcon, 
				cardX + cardWidth / 2, statusY + 5, 0xFFFFFFFF);
		}
	}
	
	/**
	 * Get short chest name for display in card
	 */
	private String getShortChestName(ChestTier tier) {
		switch (tier) {
			case WOODEN: return "Gỗ";
			case STONE: return "Đá";
			case COAL: return "Than";
			case COPPER: return "Đồng";
			case IRON: return "Sắt";
			case GOLD: return "Vàng";
			case LAPIS: return "Lapis";
			case DIAMOND: return "Kim";
			case QUARTZ: return "Quartz";
			case NETHERITE: return "Neth";
			case OBSIDIAN: return "Obsi";
			case BEDROCK: return "Bed";
			default: return tier.name();
		}
	}
	
	/**
	 * Render chest icon (3D item representation)
	 */
	private void renderChestIcon(DrawContext context, ChestTier tier, int x, int y) {
		// Map chest tier to Minecraft chest item
		net.minecraft.item.Item chestItem = getChestItemForTier(tier);
		net.minecraft.item.ItemStack stack = new net.minecraft.item.ItemStack(chestItem);
		
		// Render item icon
		context.drawItem(stack, x, y);
	}
	
	/**
	 * Get Minecraft chest item for each tier
	 */
	private net.minecraft.item.Item getChestItemForTier(ChestTier tier) {
		switch (tier) {
			case WOODEN: return net.minecraft.item.Items.CHEST;
			case STONE: return net.minecraft.item.Items.COBBLESTONE;
			case COAL: return net.minecraft.item.Items.COAL_BLOCK;
			case COPPER: return net.minecraft.item.Items.COPPER_BLOCK;
			case IRON: return net.minecraft.item.Items.IRON_BLOCK;
			case GOLD: return net.minecraft.item.Items.GOLD_BLOCK;
			case LAPIS: return net.minecraft.item.Items.LAPIS_BLOCK;
			case DIAMOND: return net.minecraft.item.Items.DIAMOND_BLOCK;
			case QUARTZ: return net.minecraft.item.Items.QUARTZ_BLOCK;
			case NETHERITE: return net.minecraft.item.Items.NETHERITE_BLOCK;
			case OBSIDIAN: return net.minecraft.item.Items.OBSIDIAN;
			case BEDROCK: return net.minecraft.item.Items.BEDROCK;
			default: return net.minecraft.item.Items.CHEST;
		}
	}

	private void renderChestDetails(DrawContext context, ChestTier tier, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Chest name with larger font
		context.drawText(parent.getTextRenderer(), "§l" + tier.getDisplayName(), x, y, 0xFFFFFFFF, true);

		// Separator line
		context.fill(x, y + 25, x + width, y + 27, 0xFF4A4A4A);

		// Drop Rates header
		context.drawText(parent.getTextRenderer(), "§7Drop Rates:", x, y + 35, 0xFFFFFFFF, false);
		
		// Only show % by rarity (not individual items)
		Map<ChestRarity, Double> probabilities = LuckyChestManager.getChestProbabilities(tier);
		int rarityY = y + 50;
		
		for (ChestRarity rarity : ChestRarity.values()) {
			Double chance = probabilities.get(rarity);
			if (chance != null && chance > 0) {
				// Rarity name with color
				String rarityText = rarity.getDisplayName();
				context.drawText(parent.getTextRenderer(), rarityText, x + 5, rarityY, rarity.getColor(), false);
				
				// Percentage aligned to the right
				String percentText = String.format("%.1f%%", chance);
				int percentWidth = parent.getTextRenderer().getWidth(percentText);
				context.drawText(parent.getTextRenderer(), percentText, x + width - percentWidth - 5, rarityY, 0xFFAAAAAA, false);
				
				rarityY += 16;
			}
		}
		
		// "Xem thêm" link (underlined, clickable)
		String viewMoreText = "§n§9Xem thêm";
		int viewMoreX = x + 5;
		int viewMoreWidth = parent.getTextRenderer().getWidth("Xem thêm"); // Without formatting for width calc
		boolean viewMoreHovered = mouseX >= viewMoreX && mouseX <= viewMoreX + viewMoreWidth &&
		                          mouseY >= rarityY + 5 && mouseY <= rarityY + 5 + parent.getTextRenderer().fontHeight;
		
		// Highlight on hover with underline
		if (viewMoreHovered) {
			context.fill(viewMoreX, rarityY + 5 + parent.getTextRenderer().fontHeight + 1, 
			             viewMoreX + viewMoreWidth, rarityY + 5 + parent.getTextRenderer().fontHeight + 2, 
			             0xFF5599FF); // Blue underline
		}
		
		context.drawText(parent.getTextRenderer(), viewMoreText, viewMoreX, rarityY + 5, 
		                 viewMoreHovered ? 0xFF77BBFF : 0xFF5599FF, false);

		// Two buttons: "Mở x1" and "Mở x10+1" with real pricing
		int btnY = y + height - 50;
		int btnHeight = 22;
		int btnSpacing = 5;
		
		int playerSilver = ClientDataCache.getSilverCoins();
		int playerGold = ClientDataCache.getGoldCoins();
		
		// Check if can afford x1 (with any payment option)
		boolean canAffordX1 = tier.canAffordOpenOne(playerSilver, playerGold);
		
		// Check if can afford x10+1 (with any payment option)
		boolean canAffordX10 = tier.canAffordOpenTenPlusOne(playerSilver, playerGold);
		
		// Button "Mở x1" - show first payment option or indicate multiple
		List<PaymentOption> x1Options = tier.getOpenOneOptions();
		String btn1PriceText = x1Options.isEmpty() ? "?" : 
			(x1Options.size() > 1 ? x1Options.get(0).getShortDisplay() + "/..." : x1Options.get(0).getShortDisplay());
		
		int btn1Color = canAffordX1 ? 0xFF4A9EFF : 0xFF444444;
		context.fill(x, btnY, x + width, btnY + btnHeight, btn1Color);
		if (canAffordX1) {
			context.fill(x, btnY, x + width, btnY + 2, 0xFF6ABAFF);
		}
		String btn1Text = "Mở x1 (" + btn1PriceText + ")";
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), btn1Text, 
			x + width / 2, btnY + 7, canAffordX1 ? 0xFFFFFFFF : 0xFF888888);
		
		// Button "Mở x10+1" - show first payment option or indicate multiple
		List<PaymentOption> x10Options = tier.getOpenTenPlusOneOptions();
		String btn2PriceText = x10Options.isEmpty() ? "?" : 
			(x10Options.size() > 1 ? x10Options.get(0).getShortDisplay() + "/..." : x10Options.get(0).getShortDisplay());
		
		int btn2Y = btnY + btnHeight + btnSpacing;
		int btn2Color = canAffordX10 ? 0xFF4A9EFF : 0xFF444444;
		context.fill(x, btn2Y, x + width, btn2Y + btnHeight, btn2Color);
		if (canAffordX10) {
			context.fill(x, btn2Y, x + width, btn2Y + 2, 0xFF6ABAFF);
		}
		String btn2Text = "Mở x10+1 (" + btn2PriceText + ")";
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), btn2Text, 
			x + width / 2, btn2Y + 7, canAffordX10 ? 0xFFFFFFFF : 0xFF888888);
	}

	/**
	 * Format item/enchantment name: remove underscores and capitalize words
	 */
	private String formatName(String name) {
		// Remove minecraft: prefix if present
		if (name.contains(":")) {
			name = name.substring(name.indexOf(":") + 1);
		}
		
		// Replace underscores with spaces and capitalize each word
		String[] words = name.split("_");
		StringBuilder formatted = new StringBuilder();
		for (String word : words) {
			if (formatted.length() > 0) {
				formatted.append(" ");
			}
			// Capitalize first letter
			if (!word.isEmpty()) {
				formatted.append(Character.toUpperCase(word.charAt(0)));
				if (word.length() > 1) {
					formatted.append(word.substring(1));
				}
			}
		}
		return formatted.toString();
	}

	/**
	 * Render detail modal showing full rarity breakdown with item lists
	 */
	private void renderDetailModal(DrawContext context, ChestTier tier, int screenX, int screenY, int screenWidth, int screenHeight, int mouseX, int mouseY) {
		// Dimmed overlay background (FULL SCREEN)
		context.fill(screenX, screenY, screenX + screenWidth, screenY + screenHeight, 0xDD000000);
		
		// Modal window (centered, 75% width, 85% height for more space)
		int modalWidth = (int)(screenWidth * 0.75);
		int modalHeight = (int)(screenHeight * 0.85);
		int modalX = screenX + (screenWidth - modalWidth) / 2;
		int modalY = screenY + (screenHeight - modalHeight) / 2;
		
		// Modal background (SOLID)
		context.fill(modalX, modalY, modalX + modalWidth, modalY + modalHeight, 0xFF1A1A1A);
		context.fill(modalX + 2, modalY + 2, modalX + modalWidth - 2, modalY + modalHeight - 2, 0xFF2A2A2A);
		
		// Header with chest name and close button
		String headerText = "§l" + tier.getDisplayName() + " - Chi tiết độ hiếm";
		context.drawText(parent.getTextRenderer(), headerText, 
			modalX + 10, modalY + 10, 0xFFFFFFFF, true);
		
		// Close button [X] at top-right
		String closeText = "§l§c[X]";
		int closeWidth = parent.getTextRenderer().getWidth(closeText);
		int closeX = modalX + modalWidth - closeWidth - 10;
		int closeY = modalY + 10;
		
		// Highlight close button on hover
		boolean closeHovered = mouseX >= closeX && mouseX <= closeX + closeWidth &&
		                       mouseY >= closeY && mouseY <= closeY + parent.getTextRenderer().fontHeight;
		if (closeHovered) {
			context.fill(closeX - 2, closeY - 2, closeX + closeWidth + 2, closeY + parent.getTextRenderer().fontHeight + 2, 0x44FFFFFF);
		}
		context.drawText(parent.getTextRenderer(), closeText, closeX, closeY, closeHovered ? 0xFFFFFFFF : 0xFFFF5555, false);
		
		// Separator
		context.fill(modalX + 10, modalY + 30, modalX + modalWidth - 10, modalY + 32, 0xFF4A4A4A);
		
		// Scrollable content area
		int contentStartY = modalY + 40;
		int contentX = modalX + 15;
		int contentWidth = modalWidth - 30;
		int visibleHeight = modalHeight - 50; // Visible area height
		
		// Enable scissor for scrolling (clip content outside visible area)
		context.enableScissor(modalX, contentStartY, modalX + modalWidth, modalY + modalHeight - 10);
		
		// Apply scroll offset
		int contentY = contentStartY - modalScrollOffset;
		
		// Section: Drop Rates by Rarity
		context.drawText(parent.getTextRenderer(), "§e§lXác suất theo độ hiếm:", contentX, contentY, 0xFFFFAA00, true);
		contentY += 20;
		
		Map<ChestRarity, Double> probabilities = LuckyChestManager.getChestProbabilities(tier);
		
		for (ChestRarity rarity : ChestRarity.values()) {
			Double chance = probabilities.get(rarity);
			if (chance != null && chance > 0) {
				// Rarity name with color
				String rarityText = rarity.getDisplayName();
				context.drawText(parent.getTextRenderer(), rarityText, contentX + 10, contentY, rarity.getColor(), true);
				
				// Percentage aligned to right
				String percentText = String.format("%.1f%%", chance);
				int percentWidth = parent.getTextRenderer().getWidth(percentText);
				context.drawText(parent.getTextRenderer(), percentText, contentX + contentWidth - percentWidth - 10, contentY, 0xFFFFFFFF, false);
				
				contentY += 16;
				
				// ===== PROGRESS BAR WITH PROPER COLOR =====
				int barWidth = (int)(contentWidth * 0.8);
				int barHeight = 8;
				int barX = contentX + 10;
				
				// Background (dark gray)
				context.fill(barX, contentY, barX + barWidth, contentY + barHeight, 0xFF2A2A2A);
				
				// Filled portion based on % (using rarity color with FULL OPACITY)
				int fillWidth = (int)(barWidth * (chance / 100.0));
				int rarityColorOpaque = rarity.getColor() | 0xFF000000; // Force alpha to FF
				context.fill(barX, contentY, barX + fillWidth, contentY + barHeight, rarityColorOpaque);
				
				// Border for clarity
				context.fill(barX, contentY, barX + barWidth, contentY + 1, 0xFF5A5A5A); // Top
				context.fill(barX, contentY + barHeight - 1, barX + barWidth, contentY + barHeight, 0xFF5A5A5A); // Bottom
				
				contentY += 14;
				
				// ===== ITEM LIST WITH GROUPING FOR ENCHANTED BOOKS =====
				java.util.List<LootReward> items = LuckyChestManager.getLootPoolForRarity(rarity);
				if (!items.isEmpty()) {
					// Group enchanted books by enchantment type
					java.util.Map<String, java.util.List<Integer>> enchantmentGroups = new java.util.LinkedHashMap<>();
					java.util.List<LootReward> nonBookItems = new java.util.ArrayList<>();
					
					for (LootReward reward : items) {
						if (reward.getItem() == net.minecraft.item.Items.ENCHANTED_BOOK && reward.getEnchantment() != null) {
							String enchName = net.minecraft.registry.Registries.ENCHANTMENT.getId(reward.getEnchantment()).getPath();
							enchantmentGroups.computeIfAbsent(enchName, k -> new java.util.ArrayList<>()).add(reward.getEnchantLevel());
						} else {
							nonBookItems.add(reward);
						}
					}
					
					// Render grouped enchanted books first
					for (java.util.Map.Entry<String, java.util.List<Integer>> entry : enchantmentGroups.entrySet()) {
						String enchName = entry.getKey();
						java.util.List<Integer> levels = entry.getValue();
						
						// Sort levels
						java.util.Collections.sort(levels);
						
						// Render book icon
						net.minecraft.item.ItemStack bookStack = new net.minecraft.item.ItemStack(net.minecraft.item.Items.ENCHANTED_BOOK);
						context.drawItem(bookStack, contentX + 15, contentY);
						
						// Render grouped name with formatted enchantment name
						String levelRange = levels.size() == 1 ? String.valueOf(levels.get(0)) : 
						                    (levels.get(0) + "-" + levels.get(levels.size() - 1));
						String displayName = "§7" + formatName("enchanted_book") + " (" + formatName(enchName) + " " + levelRange + ")";
						context.drawText(parent.getTextRenderer(), displayName, contentX + 35, contentY + 4, 0xFFCCCCCC, false);
						
						contentY += 18;
					}
					
					// Render non-book items
					int maxNonBookItems = Math.min(nonBookItems.size(), 8); // Show max 8 non-book items
					for (int i = 0; i < maxNonBookItems; i++) {
						LootReward reward = nonBookItems.get(i);
						
						// Render item icon
						net.minecraft.item.ItemStack displayStack = reward.generateStack(new java.util.Random(0));
						context.drawItem(displayStack, contentX + 15, contentY);
						
						// Render item name (formatted without underscores)
						String rawName = net.minecraft.registry.Registries.ITEM.getId(reward.getItem()).getPath();
						String formattedName = formatName(rawName);
						
						// Add count/range if applicable
						if (reward.getMinCount() == reward.getMaxCount() && reward.getMinCount() > 1) {
							formattedName += " x" + reward.getMinCount();
						} else if (reward.getMinCount() != reward.getMaxCount()) {
							formattedName += " x" + reward.getMinCount() + "-" + reward.getMaxCount();
						}
						
						// Add enchantment if applicable
						if (reward.getEnchantment() != null && reward.getEnchantLevel() > 0) {
							String enchName = net.minecraft.registry.Registries.ENCHANTMENT.getId(reward.getEnchantment()).getPath();
							formattedName += " (" + formatName(enchName) + " " + reward.getEnchantLevel() + ")";
						}
						
						String itemName = "§7" + formattedName;
						context.drawText(parent.getTextRenderer(), itemName, contentX + 35, contentY + 4, 0xFFCCCCCC, false);
						
						contentY += 18;
					}
					
					// If more items exist, show "..." indicator
					if (nonBookItems.size() > maxNonBookItems) {
						context.drawText(parent.getTextRenderer(), "§7... và " + (nonBookItems.size() - maxNonBookItems) + " vật phẩm khác", 
							contentX + 35, contentY, 0xFF888888, false);
						contentY += 18;
					}
				}
				
				contentY += 10; // Space between rarities
			}
		}
		
		// Note section at bottom
		contentY += 10;
		context.fill(modalX + 15, contentY, modalX + modalWidth - 15, contentY + 1, 0xFF4A4A4A);
		contentY += 10;
		context.drawText(parent.getTextRenderer(), "§7Lưu ý:", contentX, contentY, 0xFFAAAAAA, false);
		contentY += 16;
		context.drawText(parent.getTextRenderer(), "§7• Mỗi rương cho 1 vật phẩm ngẫu nhiên từ pool theo độ hiếm", contentX + 5, contentY, 0xFF888888, false);
		contentY += 14;
		context.drawText(parent.getTextRenderer(), "§7• Gói x10+1: mua 10 tặng 1 (11 lần mở độc lập)", contentX + 5, contentY, 0xFF888888, false);
		contentY += 14;
		context.drawText(parent.getTextRenderer(), "§7• Cuộn chuột để xem thêm", contentX + 5, contentY, 0xFF888888, false);
		
		// Disable scissor
		context.disableScissor();
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
		// If modal is open, check modal interactions first
		if (showDetailModal) {
			// Modal calculations must match render() exactly
			int modalWidth = (int)(contentWidth * 0.7);
			int modalHeight = (int)(contentHeight * 0.8);
			int modalX = contentX + (contentWidth - modalWidth) / 2;
			int modalY = contentY + (contentHeight - modalHeight) / 2;
			
			// Check close button [X]
			String closeText = "§l§c[X]";
			int closeWidth = parent.getTextRenderer().getWidth(closeText);
			int closeX = modalX + modalWidth - closeWidth - 10;
			int closeY = modalY + 10;
			int closeHeight = parent.getTextRenderer().fontHeight;
			
			if (mouseX >= closeX && mouseX <= closeX + closeWidth &&
			    mouseY >= closeY && mouseY <= closeY + closeHeight) {
				showDetailModal = false;
				modalScrollOffset = 0; // Reset scroll when closing
				return true;
			}
			
			// Click outside modal to close
			if (mouseX < modalX || mouseX > modalX + modalWidth ||
			    mouseY < modalY || mouseY > modalY + modalHeight) {
				showDetailModal = false;
				modalScrollOffset = 0; // Reset scroll when closing
				return true;
			}
			
			// Consumed by modal (don't process clicks behind it)
			return true;
		}
		
		// When not in modal, handle main content clicks
		// Use same coordinate system as render()
		int x = contentX;
		int y = contentY;
		
		// Calculate layout dimensions (match render())
		int leftWidth = (int)(contentWidth * 0.6);
		int rightWidth = contentWidth - leftWidth - 30;

		// Check chest selector clicks (left panel)
		int selectorX = x + 10;  // Match render: x + 10
		int selectorY = y + 50;  // Match render: y + 50
		int cardWidth = 80;
		int cardHeight = 100;
		int spacing = 8;
		int perRow = Math.max(1, leftWidth / (cardWidth + spacing));

		ChestTier[] tiers = ChestTier.values();
		for (int i = 0; i < tiers.length; i++) {
			int col = i % perRow;
			int row = i / perRow;
			int cardX = selectorX + col * (cardWidth + spacing);
			int cardY = selectorY + row * (cardHeight + spacing);

			if (mouseX >= cardX && mouseX <= cardX + cardWidth && 
			    mouseY >= cardY && mouseY <= cardY + cardHeight) {
				selectedChestIndex = i;
				return true;
			}
		}

		// Check detail panel interactions (right panel)
		// Match render() coordinates exactly: detailX = x + leftWidth + 20
		ChestTier selectedTier = tiers[selectedChestIndex];
		int detailX = x + 10 + leftWidth + 20;  // x is contentX, add the +10 offset from render
		int detailY = y + 50;
		int detailHeight = contentHeight - 60;
		int detailContentX = detailX + 10;
		int detailContentY = detailY + 10;
		int detailContentWidth = rightWidth - 20;
		int detailContentHeight = detailHeight - 20;
		
		// Check "Xem thêm" link click
		Map<ChestRarity, Double> probabilities = LuckyChestManager.getChestProbabilities(selectedTier);
		int rarityCount = 0;
		for (ChestRarity rarity : ChestRarity.values()) {
			Double chance = probabilities.get(rarity);
			if (chance != null && chance > 0) {
				rarityCount++;
			}
		}
		int viewMoreY = detailContentY + 50 + (rarityCount * 16) + 5;
		String viewMoreText = "Xem thêm"; // Without formatting for width calc
		int viewMoreWidth = parent.getTextRenderer().getWidth(viewMoreText);
		int viewMoreX = detailContentX + 5;
		
		if (mouseX >= viewMoreX && mouseX <= viewMoreX + viewMoreWidth &&
		    mouseY >= viewMoreY && mouseY <= viewMoreY + parent.getTextRenderer().fontHeight) {
			showDetailModal = true;
			modalScrollOffset = 0; // Reset scroll when opening
			return true;
		}
		
		// Check button clicks
		int btnY = detailContentY + detailContentHeight - 50;
		int btnHeight = 22;
		int btnSpacing = 5;
		
		int playerSilver = ClientDataCache.getSilverCoins();
		int playerGold = ClientDataCache.getGoldCoins();
		
		// Check "Mở x1" button
		if (mouseX >= detailContentX && mouseX <= detailContentX + detailContentWidth &&
		    mouseY >= btnY && mouseY <= btnY + btnHeight) {
			// Check if can afford with ANY payment option
			if (selectedTier.canAffordOpenOne(playerSilver, playerGold)) {
				// TODO: If multiple payment options, show selection dialog
				// For now, use cheapest option
				PaymentOption chosenOption = selectedTier.getCheapestOpenOne(playerSilver, playerGold);
				if (chosenOption != null) {
					ModNetworking.sendChestOpen(selectedTier.name());
				}
			}
			return true;
		}
		
		// Check "Mở x10+1" button
		int btn2Y = btnY + btnHeight + btnSpacing;
		if (mouseX >= detailContentX && mouseX <= detailContentX + detailContentWidth &&
		    mouseY >= btn2Y && mouseY <= btn2Y + btnHeight) {
			// Check if can afford with ANY payment option
			if (selectedTier.canAffordOpenTenPlusOne(playerSilver, playerGold)) {
				// Send bulk open packet (server handles 11 rolls)
				ModNetworking.sendChestOpenBulk(selectedTier.name());
			}
			return true;
		}

		return false;
	}
	
	/**
	 * Handle mouse scroll for modal scrolling
	 */
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (showDetailModal) {
			// Scroll modal content
			int scrollAmount = (int) (amount * 20); // 20 pixels per scroll tick
			modalScrollOffset -= scrollAmount;
			
			// Clamp scroll offset (min 0, max determined by content height)
			modalScrollOffset = Math.max(0, modalScrollOffset);
			// Note: Max scroll is hard to calculate dynamically, let user scroll freely
			// Content will just stop showing when reaching bottom
			
			return true; // Consume scroll event
		}
		return false;
	}
}
