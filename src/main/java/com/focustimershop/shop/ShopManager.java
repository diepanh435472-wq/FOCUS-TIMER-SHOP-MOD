package com.focustimershop.shop;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.economy.EconomyManager;
import com.focustimershop.economy.PlayerEconomyData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.*;

/**
 * Server-side shop manager
 * Validates purchases and handles item dispensing
 * 
 * SECURITY: All limits enforced server-side to prevent DoS attacks
 */
public class ShopManager {
	
	// SECURITY: DoS prevention constants (VULN-002 fix)
	public static final int MAX_PURCHASE_QUANTITY = 6400; // 100 stacks of 64
	public static final int MAX_CART_ITEMS = 100; // Maximum unique items in cart
	public static final int MAX_ITEM_ID_LENGTH = 128; // Reasonable max for "minecraft:item_name" or "enchanted_book:enchant:level"
	public static final int MAX_TOTAL_ITEMS_PER_TRANSACTION = 10000; // Total items across all cart entries
	
	private static final Map<String, ShopItem> shopItems = new HashMap<>();
	private static final Set<Item> blacklistedItems = new HashSet<>();
	private static volatile boolean initialized = false;

	/**
	 * Ensure initialization happens on first use, not class load
	 */
	private static void ensureInitialized() {
		if (!initialized) {
			synchronized (ShopManager.class) {
				if (!initialized) {
					initializeBlacklist();
					loadShopItems();
					initialized = true;
				}
			}
		}
	}

	/**
	 * Initialize blacklist of forbidden items (creative-only, etc.)
	 */
	/**
	 * Format enchantment name for display
	 */
	private static String formatEnchantmentName(String enchantmentName) {
		// Convert snake_case to Title Case
		String[] words = enchantmentName.split("_");
		StringBuilder result = new StringBuilder();
		for (String word : words) {
			if (result.length() > 0) result.append(" ");
			result.append(Character.toUpperCase(word.charAt(0)))
				.append(word.substring(1));
		}
		return result.toString();
	}

	/**
	 * Create an enchanted book ItemStack from enchanted_book:enchantment_name:level format
	 */
	private static ItemStack createEnchantedBook(String itemId) {
		String[] parts = itemId.split(":");
		if (parts.length != 3) {
			FocusTimerShop.LOGGER.error("Invalid enchanted book format: {}", itemId);
			return null;
		}
		
		String enchantmentName = parts[1];
		int level;
		try {
			level = Integer.parseInt(parts[2]);
		} catch (NumberFormatException e) {
			FocusTimerShop.LOGGER.error("Invalid enchantment level in: {}", itemId);
			return null;
		}
		
		// Create the enchanted book item
		ItemStack stack = new ItemStack(Items.ENCHANTED_BOOK, 1);
		
		// Get the enchantment from registry
		Identifier enchantmentId = new Identifier("minecraft", enchantmentName);
		net.minecraft.enchantment.Enchantment enchantment = 
			net.minecraft.registry.Registries.ENCHANTMENT.get(enchantmentId);
		
		if (enchantment == null) {
			FocusTimerShop.LOGGER.error("Enchantment not found: {}", enchantmentName);
			return null;
		}
		
		// Add the enchantment to the book using the proper API for enchanted books
		net.minecraft.item.EnchantedBookItem.addEnchantment(stack, new net.minecraft.enchantment.EnchantmentLevelEntry(enchantment, level));
		
		return stack;
	}

	private static void initializeBlacklist() {
		blacklistedItems.add(Items.BEDROCK);
		blacklistedItems.add(Items.COMMAND_BLOCK);
		blacklistedItems.add(Items.CHAIN_COMMAND_BLOCK);
		blacklistedItems.add(Items.REPEATING_COMMAND_BLOCK);
		blacklistedItems.add(Items.STRUCTURE_BLOCK);
		blacklistedItems.add(Items.STRUCTURE_VOID);
		blacklistedItems.add(Items.JIGSAW);
		blacklistedItems.add(Items.BARRIER);
		blacklistedItems.add(Items.LIGHT);
		blacklistedItems.add(Items.DEBUG_STICK);
		blacklistedItems.add(Items.DRAGON_EGG);
		blacklistedItems.add(Items.END_PORTAL_FRAME);
		blacklistedItems.add(Items.COMMAND_BLOCK_MINECART);
		blacklistedItems.add(Items.SPAWNER);
		// Add all spawn eggs
		for (Item item : Registries.ITEM) {
			String id = Registries.ITEM.getId(item).getPath();
			if (id.endsWith("_spawn_egg")) {
				blacklistedItems.add(item);
			}
		}
	}

	/**
	 * Load shop items from configuration
	 * Load from FCTMS database price files
	 */
	private static void loadShopItems() {
		System.out.println("[DEBUG-LỖI#3] ========== BẮT ĐẦU LOAD SHOP ITEMS ==========");
		int buildingCount = loadItemsFromPriceList("building_blocks.json", ShopCategory.BUILDING_BLOCKS);
		int coloredCount = loadItemsFromPriceList("colored_blocks.json", ShopCategory.COLORED_BLOCKS);
		int naturalCount = loadItemsFromPriceList("natural_blocks.json", ShopCategory.NATURAL_BLOCKS);
		int functionalCount = loadItemsFromPriceList("functional_blocks.json", ShopCategory.FUNCTIONAL_BLOCKS);
		int redstoneCount = loadItemsFromPriceList("redstone_blocks.json", ShopCategory.REDSTONE);
		int toolsCount = loadItemsFromPriceList("tools_utilities.json", ShopCategory.TOOLS_UTILITIES);
		int foodCount = loadItemsFromPriceList("food_drinks.json", ShopCategory.FOOD_DRINKS);
		int ingredientsCount = loadItemsFromPriceList("ingredients.json", ShopCategory.INGREDIENTS);
		
		System.out.println("[DEBUG-LỖI#3] ========== KẾT QUẢ LOAD ==========");
		System.out.println("[DEBUG-LỖI#3] Building blocks: " + buildingCount + " items");
		System.out.println("[DEBUG-LỖI#3] Colored blocks: " + coloredCount + " items");
		System.out.println("[DEBUG-LỖI#3] Natural blocks: " + naturalCount + " items");
		System.out.println("[DEBUG-LỖI#3] Functional blocks: " + functionalCount + " items");
		System.out.println("[DEBUG-LỖI#3] Redstone blocks: " + redstoneCount + " items");
		System.out.println("[DEBUG-LỖI#3] Tools & Utilities: " + toolsCount + " items");
		System.out.println("[DEBUG-LỖI#3] Food & Drinks: " + foodCount + " items");
		System.out.println("[DEBUG-LỖI#3] Ingredients: " + ingredientsCount + " items");
		System.out.println("[DEBUG-LỖI#3] TỔNG CỘNG: " + shopItems.size() + " items trong map");
		
		FocusTimerShop.LOGGER.info("Loaded {} shop items ({} building, {} colored, {} natural, {} functional, {} redstone, {} tools, {} food, {} ingredients)", 
			shopItems.size(), buildingCount, coloredCount, naturalCount, functionalCount, redstoneCount, toolsCount, foodCount, ingredientsCount);
	}
	
	/**
	 * Load items from a price list JSON file
	 */
	private static int loadItemsFromPriceList(String filename, ShopCategory category) {
		java.io.File file = com.focustimershop.database.DatabaseManager.getPricesDir()
			.resolve(filename).toFile();
		
		if (!file.exists()) {
			FocusTimerShop.LOGGER.warn("Price list file not found: {}", filename);
			return 0;
		}
		
		com.focustimershop.database.PriceList priceList = 
			com.focustimershop.database.DatabaseManager.readJson(file, com.focustimershop.database.PriceList.class);
		
		if (priceList == null) {
			FocusTimerShop.LOGGER.error("Failed to parse price list: {}", filename);
			return 0;
		}
		
		FocusTimerShop.LOGGER.info("Loading from {}: {} entries in JSON", filename, priceList.getPrices().size());
		
		int count = 0;
		int skipped = 0;
		for (java.util.Map.Entry<String, Integer> entry : priceList.getPrices().entrySet()) {
			String itemId = entry.getKey();
			int silverPrice = entry.getValue();
			
			// Check if this is an enchanted book (format: enchanted_book:enchantment_name:level)
			if (itemId.startsWith("enchanted_book:")) {
				String[] parts = itemId.split(":");
				if (parts.length == 3) {
					String enchantmentName = parts[1];
					int level = Integer.parseInt(parts[2]);
					
					// Create display name for the enchanted book
					String displayName = formatEnchantmentName(enchantmentName) + " " + level;
					
					// Store with full ID as key for lookup
					shopItems.put(itemId, new ShopItem(itemId, category, silverPrice, 0, displayName));
					count++;
					continue;
				} else {
					FocusTimerShop.LOGGER.warn("Invalid enchanted book format: {}", itemId);
					skipped++;
					continue;
				}
			}
			
			// Extract minecraft:item_name -> item_name
			String itemName = itemId;
			if (itemId.startsWith("minecraft:")) {
				itemName = itemId.substring("minecraft:".length());
			}
			
			// Get item from registry
			Identifier id = new Identifier("minecraft", itemName);
			Item item = Registries.ITEM.get(id);
			
			if (item != Items.AIR && !blacklistedItems.contains(item)) {
				String displayName = item.getName().getString();
				shopItems.put(itemName, new ShopItem(itemName, category, silverPrice, 0, displayName));
				count++;
			} else {
				skipped++;
				if (item == Items.AIR) {
					FocusTimerShop.LOGGER.warn("Skipped invalid item: {} (not found in registry)", itemId);
				}
			}
		}
		
		FocusTimerShop.LOGGER.info("Loaded {} items from {} (category: {}), skipped {}", 
			count, filename, category.getDisplayName(), skipped);
		
		return count;
	}

	/**
	 * Get all available shop items
	 */
	public static Collection<ShopItem> getAllItems() {
		ensureInitialized();
		return shopItems.values();
	}

	/**
	 * Get items by category
	 */
	public static List<ShopItem> getItemsByCategory(ShopCategory category) {
		ensureInitialized();
		List<ShopItem> result = new ArrayList<>();
		for (ShopItem item : shopItems.values()) {
			if (item.getCategory() == category) {
				result.add(item);
			}
		}
		return result;
	}

	/**
	 * Get single item by ID
	 */
	public static ShopItem getItem(String itemId) {
		ensureInitialized();
		return shopItems.get(itemId);
	}

	/**
	 * Handle purchase request from client
	 * Server-side validation prevents cheating
	 */
	public static void handlePurchase(ServerPlayerEntity player, String itemId, boolean useGold) {
		ensureInitialized();
		ShopItem shopItem = shopItems.get(itemId);
		if (shopItem == null) {
			player.sendMessage(Text.literal("§cInvalid item!"), false);
			return;
		}

		PlayerEconomyData economy = EconomyManager.getPlayerData(player);
		boolean success = false;

		if (useGold) {
			// Pay with gold
			int goldCost = shopItem.getGoldCost();
			if (economy.removeGoldCoins(goldCost)) {
				success = true;
			} else {
				player.sendMessage(Text.literal("§cNot enough Gold Coins!"), false);
				return;
			}
		} else {
			// Pay with silver
			if (economy.removeSilverCoins(shopItem.getSilverPrice())) {
				success = true;
			} else {
				player.sendMessage(Text.literal("§cNot enough Silver Coins!"), false);
				return;
			}
		}

		if (success) {
			// Create item stack
			ItemStack stack;
			
			// Check if this is an enchanted book
			if (itemId.startsWith("enchanted_book:")) {
				stack = createEnchantedBook(itemId);
				if (stack == null) {
					player.sendMessage(Text.literal("§cFailed to create enchanted book!"), false);
					// Refund the payment
					if (useGold) {
						economy.addGoldCoins(shopItem.getGoldCost());
					} else {
						economy.addSilverCoins(shopItem.getSilverPrice());
					}
					return;
				}
			} else {
				// Regular item
				Identifier id = new Identifier("minecraft", itemId);
				Item item = Registries.ITEM.get(id);
				stack = new ItemStack(item, 1);
			}

			// Try to add to inventory first, drop remaining
			if (!player.getInventory().insertStack(stack)) {
				// Inventory full, drop item at player's body center (not throw randomly)
				// Spawn at player position (center of body), items will fall down naturally
				net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
					player.getWorld(),
					player.getX(), // X position (center of player)
					player.getY() + 0.5, // Y position (waist level, will fall down)
					player.getZ(), // Z position (center of player)
					stack
				);
				// No velocity - items just drop straight down
				itemEntity.setVelocity(0, 0, 0);
				player.getWorld().spawnEntity(itemEntity);
			}

			// Save and sync
			EconomyManager.savePlayerData(player);
			EconomyManager.syncToClient(player);
			
			// Track stats (v1.0.6 - Phase A)
			com.focustimershop.database.PlayerStatsData stats = 
				com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
			stats.setTotalItemsPurchased(stats.getTotalItemsPurchased() + 1);
			
			// Phase B - Activity log
			int cost = useGold ? shopItem.getGoldCost() : shopItem.getSilverPrice();
			String currency = useGold ? "Gold" : "Silver";
			stats.addActivity(
				com.focustimershop.database.ActivityEntry.Type.SHOP_PURCHASE,
				String.format("🛒 Mua %s        -%d %s", shopItem.getDisplayName(), cost, currency)
			);
			
			com.focustimershop.database.DatabaseManager.savePlayerStats(stats);

			player.sendMessage(Text.literal("§aPurchased " + shopItem.getDisplayName() + "!"), false);
			FocusTimerShop.LOGGER.info("Player {} purchased {} for {} {}",
				player.getName().getString(), itemId, 
				useGold ? shopItem.getGoldCost() : shopItem.getSilverPrice(),
				useGold ? "gold" : "silver");
		}
	}

	/**
	 * Handle checkout request from cart (v1.0.2+)
	 * Validates all items and processes batch purchase atomically
	 * SECURITY: VULN-002 fix - comprehensive validation to prevent DoS
	 */
	public static void handleCheckout(ServerPlayerEntity player, Map<String, Integer> cartItems, boolean useSilver) {
		ensureInitialized();
		
		// SECURITY VALIDATION: Check cart size
		if (cartItems == null || cartItems.isEmpty()) {
			player.sendMessage(Text.literal("§cCart is empty!"), false);
			return;
		}
		
		if (cartItems.size() > MAX_CART_ITEMS) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} attempted cart with {} items (max: {})", 
				player.getName().getString(), cartItems.size(), MAX_CART_ITEMS);
			player.sendMessage(Text.literal("§cCart too large! Maximum " + MAX_CART_ITEMS + " unique items."), false);
			return;
		}

		PlayerEconomyData economy = EconomyManager.getPlayerData(player);
		
		// Calculate total cost and validate all items
		long totalCostSilver = 0; // Use long to detect overflow
		int totalQuantity = 0;
		List<ItemStack> itemsToGive = new ArrayList<>();
		
		for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
			String itemId = entry.getKey();
			int quantity = entry.getValue();
			
			// SECURITY: Validate item ID length
			if (itemId == null || itemId.length() > MAX_ITEM_ID_LENGTH) {
				FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid item ID length: {}", 
					player.getName().getString(), itemId != null ? itemId.length() : "null");
				player.sendMessage(Text.literal("§cInvalid item ID!"), false);
				return;
			}
			
			// SECURITY: Validate quantity bounds
			if (quantity <= 0 || quantity > MAX_PURCHASE_QUANTITY) {
				FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid quantity {} for {} (max: {})", 
					player.getName().getString(), quantity, itemId, MAX_PURCHASE_QUANTITY);
				player.sendMessage(Text.literal("§cInvalid quantity for " + itemId + "! Maximum " + MAX_PURCHASE_QUANTITY + " per item."), false);
				return;
			}
			
			// SECURITY: Track total quantity to prevent overflow attacks
			totalQuantity += quantity;
			if (totalQuantity > MAX_TOTAL_ITEMS_PER_TRANSACTION) {
				FocusTimerShop.LOGGER.warn("SECURITY: Player {} exceeded total item limit: {} (max: {})", 
					player.getName().getString(), totalQuantity, MAX_TOTAL_ITEMS_PER_TRANSACTION);
				player.sendMessage(Text.literal("§cTotal quantity too large! Maximum " + MAX_TOTAL_ITEMS_PER_TRANSACTION + " items per transaction."), false);
				return;
			}
			
			ShopItem shopItem = shopItems.get(itemId);
			if (shopItem == null) {
				player.sendMessage(Text.literal("§cInvalid item: " + itemId), false);
				return;
			}
			
			// Calculate cost (use long to detect overflow)
			long itemCost = (long) shopItem.getSilverPrice() * quantity;
			totalCostSilver += itemCost;
			
			// SECURITY: Check for cost overflow
			if (totalCostSilver < 0 || totalCostSilver > Integer.MAX_VALUE) {
				FocusTimerShop.LOGGER.warn("SECURITY: Player {} caused cost overflow: {} (item: {}, qty: {})", 
					player.getName().getString(), totalCostSilver, itemId, quantity);
				player.sendMessage(Text.literal("§cTransaction cost too large!"), false);
				return;
			}
			
			// Prepare item stacks
			if (itemId.startsWith("enchanted_book:")) {
				// Create enchanted books one by one (they can't stack)
				for (int i = 0; i < quantity; i++) {
					ItemStack enchantedBook = createEnchantedBook(itemId);
					if (enchantedBook == null) {
						player.sendMessage(Text.literal("§cFailed to create enchanted book: " + itemId), false);
						return;
					}
					itemsToGive.add(enchantedBook);
				}
			} else {
				// FIX: Split into multiple stacks of 64 if quantity > 64
				Identifier id = new Identifier("minecraft", itemId);
				Item item = Registries.ITEM.get(id);
				
				int remaining = quantity;
				while (remaining > 0) {
					int stackSize = Math.min(remaining, 64);
					ItemStack stack = new ItemStack(item, stackSize);
					itemsToGive.add(stack);
					remaining -= stackSize;
				}
			}
		}
		
		// Mixed payment: convert silver cost to gold + silver
		// SECURITY: totalCostSilver is long, validate it fits in int before casting
		if (totalCostSilver > Integer.MAX_VALUE) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} total cost exceeds int max: {}", 
				player.getName().getString(), totalCostSilver);
			player.sendMessage(Text.literal("§cTransaction cost too large!"), false);
			return;
		}
		
		int goldCost = 0;
		int silverCost = (int) totalCostSilver;
		
		if (!useSilver) {
			// Gold mode: auto-convert (100 silver = 1 gold)
			goldCost = (int) (totalCostSilver / 100);
			silverCost = (int) (totalCostSilver % 100);
		}
		
		// Check if can afford (mixed payment)
		boolean canAfford;
		if (useSilver) {
			canAfford = economy.getSilverCoins() >= silverCost;
		} else {
			canAfford = (economy.getGoldCoins() >= goldCost) && (economy.getSilverCoins() >= silverCost);
		}
		
		if (!canAfford) {
			String costMsg = useSilver ? 
				silverCost + " Silver" :
				(goldCost > 0 && silverCost > 0 ? goldCost + " Gold + " + silverCost + " Silver" :
				 goldCost > 0 ? goldCost + " Gold" : silverCost + " Silver");
			player.sendMessage(Text.literal("§cNot enough money! Need: " + costMsg), false);
			return;
		}
		
		// ATOMIC TRANSACTION: Deduct currency with rollback on failure
		// SECURITY FIX VULN-001: Proper mixed payment with rollback
		boolean success = false;
		boolean goldDeducted = false;
		boolean silverDeducted = false;
		
		if (useSilver) {
			// Silver-only mode
			success = economy.removeSilverCoins(silverCost);
			silverDeducted = success;
		} else {
			// Gold mode: MUST deduct both gold AND silver remainder atomically
			// Step 1: Try to deduct gold
			if (goldCost > 0) {
				goldDeducted = economy.removeGoldCoins(goldCost);
				if (!goldDeducted) {
					player.sendMessage(Text.literal("§cNot enough Gold!"), false);
					return;
				}
			}
			
			// Step 2: Try to deduct silver remainder
			if (silverCost > 0) {
				silverDeducted = economy.removeSilverCoins(silverCost);
				if (!silverDeducted) {
					// ROLLBACK: Refund the gold we just deducted
					if (goldDeducted) {
						economy.addGoldCoins(goldCost);
						FocusTimerShop.LOGGER.warn("Shop transaction rolled back for {} - insufficient silver after gold deduction", 
							player.getName().getString());
					}
					player.sendMessage(Text.literal("§cNot enough Silver!"), false);
					return;
				}
			}
			
			success = true; // Both succeeded (or were 0)
		}
		
		if (!success) {
			player.sendMessage(Text.literal("§cTransaction failed!"), false);
			return;
		}
		
		// Give all items (try inventory first, drop if full)
		for (ItemStack stack : itemsToGive) {
			if (!player.getInventory().insertStack(stack)) {
				// Inventory full, drop item at player's body center (not throw randomly)
				// Spawn at player position (center of body), items will fall down naturally
				net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
					player.getWorld(),
					player.getX(), // X position (center of player)
					player.getY() + 0.5, // Y position (waist level, will fall down)
					player.getZ(), // Z position (center of player)
					stack
				);
				// No velocity - items just drop straight down
				itemEntity.setVelocity(0, 0, 0);
				player.getWorld().spawnEntity(itemEntity);
			}
		}
		
		// Save and sync
		EconomyManager.savePlayerData(player);
		EconomyManager.syncToClient(player);
		
		// Track stats (v1.0.6 - Phase A) - use existing totalQuantity from validation
		com.focustimershop.database.PlayerStatsData stats = 
			com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		stats.setTotalItemsPurchased(stats.getTotalItemsPurchased() + totalQuantity);
		
		// Phase B - Activity log
		String activityCostMsg = useSilver ? 
			silverCost + " Silver" :
			(goldCost > 0 && silverCost > 0 ? goldCost + " Gold + " + silverCost + " Silver" :
			 goldCost > 0 ? goldCost + " Gold" : silverCost + " Silver");
		stats.addActivity(
			com.focustimershop.database.ActivityEntry.Type.SHOP_PURCHASE,
			String.format("🛒 Mua %d items        -%s", cartItems.size(), activityCostMsg)
		);
		
		com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
		
		// Success message (mixed payment display)
		String costMsg = useSilver ? 
			silverCost + " Silver" :
			(goldCost > 0 && silverCost > 0 ? goldCost + " Gold + " + silverCost + " Silver" :
			 goldCost > 0 ? goldCost + " Gold" : silverCost + " Silver");
		
		player.sendMessage(Text.literal("§aPurchased " + cartItems.size() + " items for " + costMsg + "!"), false);
		FocusTimerShop.LOGGER.info("Player {} checked out {} items for {} (mode: {})", 
			player.getName().getString(), cartItems.size(), costMsg, useSilver ? "silver" : "gold");
	}
}
