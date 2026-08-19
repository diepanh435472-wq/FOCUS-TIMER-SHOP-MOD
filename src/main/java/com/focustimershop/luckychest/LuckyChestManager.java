package com.focustimershop.luckychest;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.economy.EconomyManager;
import com.focustimershop.economy.PlayerEconomyData;
import com.focustimershop.network.ModNetworking;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.*;

/**
 * Server-side lucky chest manager
 * Handles gacha mechanics with weighted rarity system
 */
public class LuckyChestManager {
	
	// PHASE 3: Thread-safe Random (BUG #12)
	// Use ThreadLocalRandom.current() instead of shared instance
	private static final Map<ChestRarity, List<LootReward>> lootPools = new HashMap<>();
	
	// PHASE 3: Separate idempotency from cooldown (BUG #7, #8, #13)
	// Idempotency: Track requestId to prevent duplicate processing
	private static final Map<UUID, Long> lastBulkRequestId = new java.util.concurrent.ConcurrentHashMap<>();
	// Cooldown: Track timestamp to rate-limit requests
	private static final Map<UUID, Long> lastBulkTimestamp = new java.util.concurrent.ConcurrentHashMap<>();
	private static final long REQUEST_COOLDOWN_MS = 2000; // 2 second cooldown between bulk requests
	private static final long IDEMPOTENCY_CLEANUP_AGE_MS = 5 * 60 * 1000; // 5 minutes
	
	private static volatile boolean initialized = false;

	/**
	 * Ensure initialization happens on first use, not class load
	 */
	private static void ensureInitialized() {
		if (!initialized) {
			synchronized (LuckyChestManager.class) {
				if (!initialized) {
					initializeLootPools();
					initialized = true;
				}
			}
		}
	}

	/**
	 * Initialize loot pools for each rarity tier
	 */
	private static void initializeLootPools() {
		// COMMON rewards
		List<LootReward> commonRewards = new ArrayList<>();
		commonRewards.add(LootReward.range(Items.IRON_INGOT, 1, 4));
		commonRewards.add(LootReward.range(Items.COPPER_INGOT, 1, 4));
		commonRewards.add(LootReward.range(Items.COAL, 1, 8));
		commonRewards.add(LootReward.range(Items.BREAD, 1, 4));
		commonRewards.add(LootReward.range(Items.COOKED_BEEF, 1, 4));
		commonRewards.add(LootReward.range(Items.EXPERIENCE_BOTTLE, 1, 2));
		lootPools.put(ChestRarity.COMMON, commonRewards);

		// UNCOMMON rewards
		List<LootReward> uncommonRewards = new ArrayList<>();
		uncommonRewards.add(LootReward.enchantedBook(Enchantments.SHARPNESS, 1));
		uncommonRewards.add(LootReward.enchantedBook(Enchantments.SHARPNESS, 2));
		uncommonRewards.add(LootReward.enchantedBook(Enchantments.EFFICIENCY, 1));
		uncommonRewards.add(LootReward.enchantedBook(Enchantments.EFFICIENCY, 2));
		uncommonRewards.add(LootReward.enchantedBook(Enchantments.UNBREAKING, 1));
		uncommonRewards.add(LootReward.simple(Items.SADDLE, 1));
		uncommonRewards.add(LootReward.simple(Items.NAME_TAG, 1));
		uncommonRewards.add(LootReward.range(Items.DIAMOND, 1, 4));
		lootPools.put(ChestRarity.UNCOMMON, uncommonRewards);

		// RARE rewards
		List<LootReward> rareRewards = new ArrayList<>();
		rareRewards.add(LootReward.enchantedBook(Enchantments.MENDING, 1));
		rareRewards.add(LootReward.enchantedBook(Enchantments.SILK_TOUCH, 1));
		rareRewards.add(LootReward.enchantedBook(Enchantments.FORTUNE, 2));
		rareRewards.add(LootReward.enchantedBook(Enchantments.FORTUNE, 3));
		rareRewards.add(LootReward.enchantedBook(Enchantments.SHARPNESS, 3));
		rareRewards.add(LootReward.enchantedBook(Enchantments.SHARPNESS, 4));
		rareRewards.add(LootReward.simple(Items.SHULKER_BOX, 1));
		rareRewards.add(LootReward.simple(Items.TOTEM_OF_UNDYING, 1));
		rareRewards.add(LootReward.simple(Items.NETHERITE_SCRAP, 1));
		lootPools.put(ChestRarity.RARE, rareRewards);

		// EPIC rewards
		List<LootReward> epicRewards = new ArrayList<>();
		epicRewards.add(LootReward.simple(Items.ELYTRA, 1));
		epicRewards.add(LootReward.range(Items.NETHERITE_INGOT, 1, 3));
		epicRewards.add(LootReward.enchantedBook(Enchantments.MENDING, 1));
		epicRewards.add(LootReward.enchantedBook(Enchantments.UNBREAKING, 3));
		epicRewards.add(LootReward.simple(Items.BEACON, 1));
		epicRewards.add(LootReward.enchantedItem(Items.NETHERITE_SWORD, Enchantments.SHARPNESS, 5));
		epicRewards.add(LootReward.enchantedItem(Items.NETHERITE_PICKAXE, Enchantments.EFFICIENCY, 5));
		lootPools.put(ChestRarity.EPIC, epicRewards);

		// LEGENDARY rewards
		List<LootReward> legendaryRewards = new ArrayList<>();
		legendaryRewards.add(LootReward.enchantedItem(Items.NETHERITE_HELMET, Enchantments.PROTECTION, 4));
		legendaryRewards.add(LootReward.enchantedItem(Items.NETHERITE_CHESTPLATE, Enchantments.PROTECTION, 4));
		legendaryRewards.add(LootReward.enchantedItem(Items.NETHERITE_LEGGINGS, Enchantments.PROTECTION, 4));
		legendaryRewards.add(LootReward.enchantedItem(Items.NETHERITE_BOOTS, Enchantments.PROTECTION, 4));
		legendaryRewards.add(LootReward.enchantedItem(Items.TRIDENT, Enchantments.LOYALTY, 3));
		legendaryRewards.add(LootReward.range(Items.DIAMOND, 16, 32));
		lootPools.put(ChestRarity.LEGENDARY, legendaryRewards);
	}

	/**
	 * Handle chest opening request
	 * PHASE 3: Fixed payment options (BUG #10)
	 */
	public static void openChest(ServerPlayerEntity player, String chestTypeName) {
		ensureInitialized();
		ChestTier tier;
		try {
			tier = ChestTier.valueOf(chestTypeName.toUpperCase());
		} catch (IllegalArgumentException e) {
			player.sendMessage(Text.literal("§cInvalid chest type!"), false);
			return;
		}

		PlayerEconomyData economy = EconomyManager.getPlayerData(player);

		// PHASE 3 FIX (BUG #10): Use payment options instead of deprecated methods
		// Find cheapest affordable option
		PaymentOption chosenOption = tier.getCheapestOpenOne(
			economy.getSilverCoins(), 
			economy.getGoldCoins()
		);
		
		if (chosenOption == null) {
			player.sendMessage(Text.literal("§cKhông đủ tiền để mở rương!"), false);
			return;
		}
		
		// PHASE 3 FIX (BUG #9 pattern): Atomic payment with rollback
		boolean silverDeducted = false;
		boolean goldDeducted = false;
		
		// Step 1: Deduct silver (if needed)
		if (chosenOption.getSilverCoins() > 0) {
			silverDeducted = economy.removeSilverCoins(chosenOption.getSilverCoins());
			if (!silverDeducted) {
				player.sendMessage(Text.literal("§cKhông đủ Silver Coins!"), false);
				return;
			}
		}
		
		// Step 2: Deduct gold (if needed, with rollback)
		if (chosenOption.getGoldCoins() > 0) {
			goldDeducted = economy.removeGoldCoins(chosenOption.getGoldCoins());
			if (!goldDeducted) {
				// ROLLBACK: Refund silver
				if (silverDeducted) {
					economy.addSilverCoins(chosenOption.getSilverCoins());
					FocusTimerShop.LOGGER.warn("PHASE3_CHEST: Rolled back silver for {} - insufficient gold", 
						player.getName().getString());
				}
				player.sendMessage(Text.literal("§cKhông đủ Gold Coins!"), false);
				return;
			}
		}

		// Roll for rarity (PHASE 3: ThreadLocalRandom for thread-safety)
		ChestRarity rarity = tier.rollRarity(java.util.concurrent.ThreadLocalRandom.current());

		// Select reward from pool
		List<LootReward> pool = lootPools.get(rarity);
		if (pool == null || pool.isEmpty()) {
			player.sendMessage(Text.literal("§cNo rewards configured for this rarity!"), false);
			return;
		}

		LootReward reward = pool.get(java.util.concurrent.ThreadLocalRandom.current().nextInt(pool.size()));
		ItemStack stack = reward.generateStack(java.util.concurrent.ThreadLocalRandom.current());

		// PHASE 3 FIX (BUG #11): Try to give to inventory first, spawn only if full
		boolean addedToInventory = tryGiveItemToPlayer(player, stack);

		// Save and sync economy
		EconomyManager.savePlayerData(player);
		EconomyManager.syncToClient(player);
		
		// Track stats (v1.0.6 - Phase A)
		com.focustimershop.database.PlayerStatsData stats = 
			com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		stats.setTotalChestsOpened(stats.getTotalChestsOpened() + 1);
		
		// Phase B - Activity log
		stats.addActivity(
			com.focustimershop.database.ActivityEntry.Type.CHEST_OPEN,
			String.format("🎁 Mở %s (%s)", tier.getDisplayName(), rarity.getDisplayName())
		);
		
		com.focustimershop.database.DatabaseManager.savePlayerStats(stats);

		// Play sound and send message
		player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
		player.sendMessage(Text.literal("§6Opened " + tier.getDisplayName() + "!"), false);
		player.sendMessage(Text.literal(rarity.getDisplayName() + " §r- " + stack.getName().getString() + " x" + stack.getCount()), false);

		FocusTimerShop.LOGGER.info("PHASE3_CHEST: Player {} opened {} and received {} (rarity: {}, inventory: {})",
			player.getName().getString(), tier.name(), stack.getName().getString(), rarity.name(), 
			addedToInventory ? "direct" : "spawned");
	}

	/**
	 * Get all available chest tiers
	 */
	public static ChestTier[] getAllChestTiers() {
		ensureInitialized();
		return ChestTier.values();
	}

	/**
	 * Handle bulk chest opening (x10+1 package)
	 * Server-side only - rolls 11 times independently, charges once
	 * 
	 * PHASE 3 FIXES:
	 * - BUG #7: Separate idempotency from cooldown
	 * - BUG #8: Record idempotency BEFORE payment
	 * - BUG #9: Atomic payment with rollback
	 * - BUG #12: ThreadLocalRandom for thread-safe RNG
	 * - BUG #11: Inventory space validation
	 */
	public static void openChestBulk(ServerPlayerEntity player, String chestTypeName, long requestId) {
		ensureInitialized();
		UUID playerId = player.getUuid();
		
		// PHASE 3 FIX (BUG #7): Separate idempotency check from cooldown check
		// Idempotency: Check if THIS requestId was already processed
		Long lastRequestId = lastBulkRequestId.get(playerId);
		if (lastRequestId != null && lastRequestId.equals(requestId)) {
			FocusTimerShop.LOGGER.warn("PHASE3_CHEST: Player {} attempted duplicate bulk request {}", 
				player.getName().getString(), requestId);
			return; // Duplicate request - ignore
		}
		
		// Cooldown: Check if player is spamming (separate from idempotency)
		Long lastTimestamp = lastBulkTimestamp.get(playerId);
		if (lastTimestamp != null) {
			long timeSinceLastRequest = System.currentTimeMillis() - lastTimestamp;
			if (timeSinceLastRequest < REQUEST_COOLDOWN_MS) {
				long waitMs = REQUEST_COOLDOWN_MS - timeSinceLastRequest;
				player.sendMessage(Text.literal(String.format(
					"§cVui lòng chờ %.1f giây trước khi mở bulk tiếp!", 
					waitMs / 1000.0)), false);
				return;
			}
		}
		
		// Parse chest tier
		ChestTier tier;
		try {
			tier = ChestTier.valueOf(chestTypeName.toUpperCase());
		} catch (IllegalArgumentException e) {
			player.sendMessage(Text.literal("§cInvalid chest type!"), false);
			return;
		}

		PlayerEconomyData economy = EconomyManager.getPlayerData(player);
		long playerSilver = economy.getSilverCoins();
		long playerGold = economy.getGoldCoins();
		
		// Check if player can afford x10+1 package
		PaymentOption chosenOption = null;
		for (PaymentOption option : tier.getOpenTenPlusOneOptions()) {
			if (option.canAfford(playerSilver, playerGold)) {
				chosenOption = option;
				break; // Use first affordable option
			}
		}
		
		if (chosenOption == null) {
			player.sendMessage(Text.literal("§cKhông đủ tiền để mở bulk!"), false);
			return;
		}
		
		// PHASE 3 FIX (BUG #8): Record idempotency BEFORE payment (prevent race condition)
		lastBulkRequestId.put(playerId, requestId);
		lastBulkTimestamp.put(playerId, System.currentTimeMillis());
		
		// PHASE 3 FIX (BUG #9): Atomic payment with rollback
		boolean silverDeducted = false;
		boolean goldDeducted = false;
		
		// Step 1: Deduct silver (if needed)
		if (chosenOption.getSilverCoins() > 0) {
			silverDeducted = economy.removeSilverCoins(chosenOption.getSilverCoins());
			if (!silverDeducted) {
				player.sendMessage(Text.literal("§cKhông đủ Silver Coins!"), false);
				// Remove idempotency record (payment failed, allow retry)
				lastBulkRequestId.remove(playerId);
				return;
			}
		}
		
		// Step 2: Deduct gold (if needed, with rollback)
		if (chosenOption.getGoldCoins() > 0) {
			goldDeducted = economy.removeGoldCoins(chosenOption.getGoldCoins());
			if (!goldDeducted) {
				// ROLLBACK: Refund silver
				if (silverDeducted) {
					economy.addSilverCoins(chosenOption.getSilverCoins());
					FocusTimerShop.LOGGER.warn("PHASE3_CHEST: Rolled back {} silver for {} - insufficient gold", 
						chosenOption.getSilverCoins(), player.getName().getString());
				}
				player.sendMessage(Text.literal("§cKhông đủ Gold Coins!"), false);
				// Remove idempotency record (payment failed, allow retry)
				lastBulkRequestId.remove(playerId);
				return;
			}
		}
		
		// Payment successful - proceed with rewards
		
		// Roll 11 times (10 paid + 1 free)
		// PHASE 3 FIX (BUG #12): Use ThreadLocalRandom for thread-safe RNG
		List<ItemStack> rewards = new ArrayList<>();
		Map<ChestRarity, Integer> rarityCounts = new HashMap<>();
		
		java.util.concurrent.ThreadLocalRandom rng = java.util.concurrent.ThreadLocalRandom.current();
		
		for (int i = 0; i < 11; i++) {
			// Roll for rarity
			ChestRarity rarity = tier.rollRarity(rng);
			rarityCounts.put(rarity, rarityCounts.getOrDefault(rarity, 0) + 1);
			
			// Select reward from pool
			List<LootReward> pool = lootPools.get(rarity);
			if (pool != null && !pool.isEmpty()) {
				LootReward reward = pool.get(rng.nextInt(pool.size()));
				ItemStack stack = reward.generateStack(rng);
				rewards.add(stack);
			}
		}
		
		// Send bulk result to client for grid display
		ModNetworking.sendChestBulkResult(player, tier.getDisplayName(), rewards);
		
		// PHASE 3 FIX (BUG #11): Try to give items to inventory first, spawn only if full
		int addedToInventory = 0;
		int spawned = 0;
		for (ItemStack stack : rewards) {
			if (stack != null && !stack.isEmpty()) {
				if (tryGiveItemToPlayer(player, stack)) {
					addedToInventory++;
				} else {
					spawned++;
				}
			}
		}
		
		// Save and sync economy
		EconomyManager.savePlayerData(player);
		EconomyManager.syncToClient(player);
		
		// Track stats (v1.0.6 - Phase A) - count 11 chests
		com.focustimershop.database.PlayerStatsData stats = 
			com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		stats.setTotalChestsOpened(stats.getTotalChestsOpened() + 11);
		
		// Phase B - Activity log
		stats.addActivity(
			com.focustimershop.database.ActivityEntry.Type.CHEST_OPEN,
			String.format("🎁 Mở %s x11", tier.getDisplayName())
		);
		
		com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
		
		// Play sound (client will also play on screen open)
		player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		
		FocusTimerShop.LOGGER.info("PHASE3_CHEST: Player {} opened {} x11 (requestId: {}), received {} items (inventory: {}, spawned: {})",
			player.getName().getString(), tier.name(), requestId, rewards.size(), addedToInventory, spawned);
	}

	/**
	 * Calculate and return chest probability display for UI
	 */
	public static Map<ChestRarity, Double> getChestProbabilities(ChestTier tier) {
		ensureInitialized();
		Map<ChestRarity, Double> probabilities = new HashMap<>();
		int totalWeight = tier.getTotalWeight();
		
		for (Map.Entry<ChestRarity, Integer> entry : tier.getRarityWeights().entrySet()) {
			double percentage = (entry.getValue() * 100.0) / totalWeight;
			probabilities.put(entry.getKey(), percentage);
		}
		
		return probabilities;
	}
	
	/**
	 * Get loot pool for specific rarity (for UI display)
	 * Returns list of possible rewards
	 */
	public static List<LootReward> getLootPoolForRarity(ChestRarity rarity) {
		ensureInitialized();
		List<LootReward> pool = lootPools.get(rarity);
		return pool != null ? new ArrayList<>(pool) : new ArrayList<>();
	}
	
	/**
	 * PHASE 3 FIX (BUG #11): Helper to give item to player with inventory space check
	 * Tries to add to inventory first, spawns entity only if inventory is full
	 * 
	 * @return true if added to inventory, false if spawned as entity
	 */
	private static boolean tryGiveItemToPlayer(ServerPlayerEntity player, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return false;
		}
		
		// Try to add to player's inventory
		boolean addedToInventory = player.getInventory().insertStack(stack);
		
		if (addedToInventory) {
			// Successfully added to inventory
			return true;
		}
		
		// Inventory full - spawn as entity at player location
		net.minecraft.entity.ItemEntity itemEntity = new net.minecraft.entity.ItemEntity(
			player.getWorld(),
			player.getX(),
			player.getY() + 0.5,
			player.getZ(),
			stack.copy() // Copy to avoid issues if insertStack modified it
		);
		itemEntity.setVelocity(0, 0, 0); // Drop straight down
		player.getWorld().spawnEntity(itemEntity);
		
		// Warn player about inventory space
		player.sendMessage(Text.literal(
			"§eInv đầy! Item rơi xuống đất, nhặt ngay kẻo mất!"), false);
		
		FocusTimerShop.LOGGER.debug("PHASE3_CHEST: Spawned {} for {} (inventory full)", 
			stack.getName().getString(), player.getName().getString());
		
		return false; // Had to spawn
	}
	
	/**
	 * PHASE 3 FIX (BUG #13): Periodic cleanup of old idempotency records
	 * Called periodically (e.g., every minute) to prevent memory leak
	 * Removes entries older than 5 minutes
	 */
	public static void cleanupOldIdempotencyRecords() {
		long now = System.currentTimeMillis();
		final int[] removedCount = {0}; // Use array for lambda mutability
		
		// Cleanup idempotency map
		lastBulkRequestId.entrySet().removeIf(entry -> {
			// Check if timestamp map has entry
			Long timestamp = lastBulkTimestamp.get(entry.getKey());
			if (timestamp != null && (now - timestamp) > IDEMPOTENCY_CLEANUP_AGE_MS) {
				removedCount[0]++;
				return true;
			}
			return false;
		});
		
		// Cleanup timestamp map
		lastBulkTimestamp.entrySet().removeIf(entry -> 
			(now - entry.getValue()) > IDEMPOTENCY_CLEANUP_AGE_MS
		);
		
		if (removedCount[0] > 0) {
			FocusTimerShop.LOGGER.debug("PHASE3_CHEST: Cleaned up {} old bulk request records", removedCount[0]);
		}
	}
}
