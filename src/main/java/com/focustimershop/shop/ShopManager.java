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
 */
public class ShopManager {
	
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
			Identifier id = new Identifier("minecraft", itemId);
			Item item = Registries.ITEM.get(id);
			ItemStack stack = new ItemStack(item, 1);

			// Drop item near player (not add to inventory)
			player.dropItem(stack, false);

			// Save and sync
			EconomyManager.savePlayerData(player);
			EconomyManager.syncToClient(player);

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
	 */
	public static void handleCheckout(ServerPlayerEntity player, Map<String, Integer> cartItems, boolean useSilver) {
		ensureInitialized();
		if (cartItems.isEmpty()) {
			player.sendMessage(Text.literal("§cCart is empty!"), false);
			return;
		}

		PlayerEconomyData economy = EconomyManager.getPlayerData(player);
		
		// Calculate total cost
		int totalCostSilver = 0;
		List<ItemStack> itemsToGive = new ArrayList<>();
		
		for (Map.Entry<String, Integer> entry : cartItems.entrySet()) {
			String itemId = entry.getKey();
			int quantity = entry.getValue();
			
			if (quantity <= 0 || quantity > 64) {
				player.sendMessage(Text.literal("§cInvalid quantity for " + itemId), false);
				return;
			}
			
			ShopItem shopItem = shopItems.get(itemId);
			if (shopItem == null) {
				player.sendMessage(Text.literal("§cInvalid item: " + itemId), false);
				return;
			}
			
			// Calculate cost (always in silver base)
			int itemCost = shopItem.getSilverPrice();
			totalCostSilver += itemCost * quantity;
			
			// Prepare item stacks
			Identifier id = new Identifier("minecraft", itemId);
			Item item = Registries.ITEM.get(id);
			ItemStack stack = new ItemStack(item, quantity);
			itemsToGive.add(stack);
		}
		
		// Mixed payment: convert silver cost to gold + silver
		int goldCost = 0;
		int silverCost = totalCostSilver;
		
		if (!useSilver) {
			// Gold mode: auto-convert (100 silver = 1 gold)
			goldCost = totalCostSilver / 100;
			silverCost = totalCostSilver % 100;
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
		
		// Deduct currency (mixed)
		boolean success;
		if (useSilver) {
			success = economy.removeSilverCoins(silverCost);
		} else {
			// Gold mode: deduct both currencies
			boolean goldSuccess = goldCost == 0 || economy.removeGoldCoins(goldCost);
			boolean silverSuccess = silverCost == 0 || economy.removeSilverCoins(silverCost);
			success = goldSuccess && silverSuccess;
		}
		
		if (!success) {
			player.sendMessage(Text.literal("§cTransaction failed!"), false);
			return;
		}
		
		// Give all items (spawn near player)
		for (ItemStack stack : itemsToGive) {
			player.dropItem(stack, false);
		}
		
		// Save and sync
		EconomyManager.savePlayerData(player);
		EconomyManager.syncToClient(player);
		
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
