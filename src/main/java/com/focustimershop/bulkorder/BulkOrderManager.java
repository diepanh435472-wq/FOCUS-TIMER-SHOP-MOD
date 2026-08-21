package com.focustimershop.bulkorder;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.focustimershop.database.PlayerStatsData;
import com.focustimershop.database.ActivityEntry;
import com.focustimershop.economy.EconomyManager;
import com.focustimershop.economy.PlayerEconomyData;
import com.focustimershop.shop.ShopItem;
import com.focustimershop.shop.ShopManager;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * Bulk Order Manager - handles chest-based bulk purchasing
 * v1.0.6-beta - Simplified design (no shipping/delivery mechanics)
 */
public class BulkOrderManager {
	
	private static BulkOrderConfig config;
	private static boolean initialized = false;
	
	/**
	 * Initialize bulk order system - load config
	 */
	public static void initialize() {
		if (initialized) return;
		
		config = DatabaseManager.loadBulkOrderConfig();
		FocusTimerShop.LOGGER.info("BulkOrder: Initialized with discount table: {}", 
			config.getDiscountTable().size() + " ranges");
		
		initialized = true;
	}
	
	/**
	 * Ensure initialized before any operations
	 */
	private static void ensureInitialized() {
		if (!initialized) {
			initialize();
		}
	}
	
	/**
	 * Get current config (loads if not initialized)
	 */
	public static BulkOrderConfig getConfig() {
		ensureInitialized();
		return config;
	}
	
	/**
	 * Handle bulk order purchase request
	 * Server-side validation and fulfillment
	 * 
	 * @param player the player making the purchase
	 * @param itemId the item to purchase in bulk (e.g., "stone", "diamond")
	 * @param chestCount number of chests to purchase (must be >= 1)
	 * @param useSilverOnly if true, pay 100% silver; if false, pay mixed silver+gold
	 */
	public static void handleBulkPurchase(ServerPlayerEntity player, String itemId, 
	                                      int chestCount, boolean useSilverOnly) {
		ensureInitialized();
		
		// === VALIDATION ===
		
		// 1. Validate chest count
		if (chestCount < 1) {
			player.sendMessage(Text.literal("§cInvalid chest count! Must be at least 1."), false);
			FocusTimerShop.LOGGER.warn("BULKORDER: Player {} tried to buy {} chests (invalid)", 
				player.getName().getString(), chestCount);
			return;
		}
		
		// 2. Validate item exists in shop
		ShopItem shopItem = ShopManager.getItem(itemId);
		if (shopItem == null) {
			player.sendMessage(Text.literal("§cInvalid item: " + itemId), false);
			FocusTimerShop.LOGGER.warn("BULKORDER: Player {} tried to buy invalid item: {}", 
				player.getName().getString(), itemId);
			return;
		}
		
		// 3. Calculate price SERVER-SIDE (never trust client)
		long unitPrice = shopItem.getSilverPrice();
		long totalSilverPrice = config.calculateTotalPrice(unitPrice, chestCount);
		
		// 4. Convert to mixed currency if requested (100 silver = 1 gold)
		int goldCost = 0;
		long silverCost = totalSilverPrice;
		
		if (!useSilverOnly) {
			// Convert to mixed: use gold for every 100 silver
			goldCost = (int)(totalSilverPrice / 100);
			silverCost = totalSilverPrice % 100;
		}
		
		// 5. Check affordability
		PlayerEconomyData economy = EconomyManager.getPlayerData(player);
		boolean canAfford = (economy.getSilverCoins() >= silverCost) && 
		                    (economy.getGoldCoins() >= goldCost);
		
		if (!canAfford) {
			if (useSilverOnly) {
				player.sendMessage(Text.literal("§cKhông đủ tiền! Cần: " + silverCost + " silver"), false);
			} else {
				player.sendMessage(Text.literal("§cKhông đủ tiền! Cần: " + goldCost + "g + " + silverCost + "s"), false);
			}
			return;
		}
		
		// === PAYMENT (ATOMIC) ===
		
		boolean paymentSuccess = false;
		
		// Deduct silver
		if (silverCost > 0 && !economy.removeSilverCoins(silverCost)) {
			// Should not happen after affordability check, but safety
			player.sendMessage(Text.literal("§cGiao dịch thất bại! (silver)"), false);
			return;
		}
		
		// Deduct gold
		if (goldCost > 0 && !economy.removeGoldCoins(goldCost)) {
			// Rollback silver
			economy.addSilverCoins(silverCost);
			player.sendMessage(Text.literal("§cGiao dịch thất bại! (gold)"), false);
			return;
		}
		
		paymentSuccess = true;
		
		// === FULFILLMENT - DIRECT ITEM SPAWN (NO CHEST) ===
		
		if (paymentSuccess) {
			// Calculate total items to give
			long totalItems = config.getTotalItemCount(chestCount);
			int itemsGiven = 0;
			int itemsDropped = 0;
			
			// Get Minecraft item
			try {
				net.minecraft.item.Item minecraftItem = Registries.ITEM.get(new Identifier("minecraft", itemId));
				if (minecraftItem == null || minecraftItem == Items.AIR) {
					// Rollback payment
					economy.addSilverCoins(silverCost);
					economy.addGoldCoins(goldCost);
					player.sendMessage(Text.literal("§cLỗi: Item không tồn tại!"), false);
					return;
				}
				
				int maxStackSize = minecraftItem.getMaxCount();
				long remaining = totalItems;
				
				// Give items in stacks
				while (remaining > 0) {
					int stackSize = (int) Math.min(remaining, maxStackSize);
					ItemStack stack = new ItemStack(minecraftItem, stackSize);
					
					boolean addedToInv = tryGiveItemToPlayer(player, stack);
					if (addedToInv) {
						itemsGiven += stackSize;
					} else {
						itemsDropped += stackSize;
					}
					
					remaining -= stackSize;
				}
				
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("BULKORDER: Failed to spawn items: {}", e.getMessage());
				// Rollback payment
				economy.addSilverCoins(silverCost);
				economy.addGoldCoins(goldCost);
				player.sendMessage(Text.literal("§cLỗi: Không thể tạo items!"), false);
				return;
			}
			
			// Save and sync economy
			EconomyManager.savePlayerData(player);
			EconomyManager.syncToClient(player);
			
			// === STATS TRACKING ===
			
			PlayerStatsData stats = DatabaseManager.getPlayerStats(player.getUuid());
			stats.setTotalItemsPurchased(stats.getTotalItemsPurchased() + totalItems);
			
			// Activity log
			String costDisplay = useSilverOnly ? 
				String.format("%ds", silverCost) : 
				String.format("%dg+%ds", goldCost, silverCost);
			
			double discount = config.getDiscountForChestCount(chestCount) * 100;
			String discountStr = discount > 0 ? String.format(" (%.0f%% off)", discount) : "";
			
			stats.addActivity(
				ActivityEntry.Type.SHOP_PURCHASE,
				String.format("📦 Bulk: %dx %s [%d chests]%s   -%s", 
					totalItems, shopItem.getDisplayName(), chestCount, discountStr, costDisplay)
			);
			
			DatabaseManager.savePlayerStats(stats);
			
			// === CONFIRMATION MESSAGE ===
			
			player.sendMessage(Text.literal(String.format("§a✓ Đã mua %d x %s! (tương đương %d rương)", 
				totalItems, shopItem.getDisplayName(), chestCount)), false);
			
			if (itemsDropped > 0) {
				player.sendMessage(Text.literal("§e⚠ " + itemsDropped + " items rơi xuống đất (inv đầy)"), false);
			}
			
			// === LOGGING ===
			
			FocusTimerShop.LOGGER.info("BULKORDER: {} purchased {} x {} ({} chests equivalent) for {} " +
				"({}% discount, given:{}, dropped:{})",
				player.getName().getString(), 
				totalItems,
				itemId, 
				chestCount, 
				costDisplay,
				String.format("%.1f", discount),
				itemsGiven,
				itemsDropped
			);
		}
	}
	
	/**
	 * Create a chest filled with the specified item
	 * Reuses pattern from Lucky Chest for consistency
	 * 
	 * @param itemId the item to fill chest with
	 * @param shopItem the shop item definition (for display name)
	 * @return filled chest ItemStack with NBT data, or null on failure
	 */
	private static ItemStack createFilledChest(String itemId, ShopItem shopItem) {
		// Create chest item
		ItemStack chest = new ItemStack(Items.CHEST, 1);
		
		// Add custom name showing what's inside
		String chestName = String.format("§6Rương: §f%s §7(x%d)", 
			shopItem.getDisplayName(), config.getItemsPerChest());
		chest.setCustomName(Text.literal(chestName));
		
		// Add lore showing contents
		net.minecraft.nbt.NbtCompound nbt = chest.getOrCreateNbt();
		net.minecraft.nbt.NbtList lore = new net.minecraft.nbt.NbtList();
		
		// Line 1: Item name
		lore.add(net.minecraft.nbt.NbtString.of(
			Text.Serializer.toJson(Text.literal("§7Chứa: §f" + shopItem.getDisplayName()))
		));
		
		// Line 2: Quantity
		lore.add(net.minecraft.nbt.NbtString.of(
			Text.Serializer.toJson(Text.literal("§7Số lượng: §e" + config.getItemsPerChest() + " items"))
		));
		
		// Line 3: Instructions
		lore.add(net.minecraft.nbt.NbtString.of(
			Text.Serializer.toJson(Text.literal("§7Đặt xuống để nhận items"))
		));
		
		net.minecraft.nbt.NbtCompound display = nbt.getCompound("display");
		display.put("Lore", lore);
		nbt.put("display", display);
		
		// Store item data in custom NBT for later extraction
		// (When chest is placed, this data will be used to spawn items)
		net.minecraft.nbt.NbtCompound bulkData = new net.minecraft.nbt.NbtCompound();
		bulkData.putString("BulkItemId", itemId);
		bulkData.putInt("BulkItemCount", config.getItemsPerChest());
		nbt.put("FocusTimerBulkOrder", bulkData);
		
		chest.setNbt(nbt);
		
		return chest;
	}
	
	/**
	 * Try to give item to player
	 * Reuses pattern from LuckyChestManager for consistency
	 * 
	 * @param player the player
	 * @param stack the item stack to give
	 * @return true if added to inventory, false if dropped
	 */
	private static boolean tryGiveItemToPlayer(ServerPlayerEntity player, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		
		// Try to add to player's inventory
		boolean addedToInventory = player.getInventory().insertStack(stack);
		
		if (addedToInventory) {
			return true;
		}
		
		// Inventory full - spawn as entity at player location
		net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
			player.getWorld(),
			player.getX(),
			player.getY() + 0.5,
			player.getZ(),
			stack.copy()
		);
		itemEntity.setVelocity(0, 0, 0); // Drop straight down
		player.getWorld().spawnEntity(itemEntity);
		
		return false; // Had to spawn
	}
	
	/**
	 * Calculate display price for UI (with discount applied)
	 * Used by client to show prices before purchase
	 * 
	 * @param unitPrice base price per item
	 * @param chestCount number of chests
	 * @return array [goldCost, silverCost] for mixed payment display
	 */
	public static long[] calculateDisplayPrice(long unitPrice, int chestCount, boolean useSilverOnly) {
		ensureInitialized();
		
		long totalSilver = config.calculateTotalPrice(unitPrice, chestCount);
		
		if (useSilverOnly) {
			return new long[]{0, totalSilver};
		} else {
			// Mixed: convert to gold + remainder silver
			long gold = totalSilver / 100;
			long silver = totalSilver % 100;
			return new long[]{gold, silver};
		}
	}
}
