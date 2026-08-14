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
	
	private static final Random random = new Random();
	private static final Map<ChestRarity, List<LootReward>> lootPools = new HashMap<>();
	
	// Idempotency tracking for bulk opens (prevent double-processing)
	private static final Map<UUID, Long> lastBulkRequestId = new HashMap<>();
	private static final long REQUEST_COOLDOWN_MS = 2000; // 2 second cooldown between bulk requests
	
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

		// Validate payment
		boolean success = false;
		if (tier.usesSilver()) {
			if (economy.removeSilverCoins(tier.getCost())) {
				success = true;
			} else {
				player.sendMessage(Text.literal("§cNot enough Silver Coins!"), false);
				return;
			}
		} else {
			if (economy.removeGoldCoins(tier.getCost())) {
				success = true;
			} else {
				player.sendMessage(Text.literal("§cNot enough Gold Coins!"), false);
				return;
			}
		}

		if (!success) {
			return;
		}

		// Roll for rarity
		ChestRarity rarity = tier.rollRarity(random);

		// Select reward from pool
		List<LootReward> pool = lootPools.get(rarity);
		if (pool == null || pool.isEmpty()) {
			player.sendMessage(Text.literal("§cNo rewards configured for this rarity!"), false);
			return;
		}

		LootReward reward = pool.get(random.nextInt(pool.size()));
		ItemStack stack = reward.generateStack(random);

		// Give reward (drop near player)
		player.dropItem(stack, false);

		// Save and sync economy
		EconomyManager.savePlayerData(player);
		EconomyManager.syncToClient(player);

		// Play sound and send message
		player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0f, 1.0f);
		player.sendMessage(Text.literal("§6Opened " + tier.getDisplayName() + "!"), false);
		player.sendMessage(Text.literal(rarity.getDisplayName() + " §r- " + stack.getName().getString() + " x" + stack.getCount()), false);

		FocusTimerShop.LOGGER.info("Player {} opened {} and received {} (rarity: {})",
			player.getName().getString(), tier.name(), stack.getName().getString(), rarity.name());
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
	 */
	public static void openChestBulk(ServerPlayerEntity player, String chestTypeName, long requestId) {
		ensureInitialized();
		UUID playerId = player.getUuid();
		
		// Idempotency check - prevent duplicate processing
		Long lastRequestId = lastBulkRequestId.get(playerId);
		if (lastRequestId != null && lastRequestId == requestId) {
			FocusTimerShop.LOGGER.warn("Player {} attempted duplicate bulk request {}", player.getName().getString(), requestId);
			return;
		}
		
		// Cooldown check - prevent spam
		if (lastRequestId != null) {
			long timeSinceLastRequest = System.currentTimeMillis() - lastRequestId;
			if (timeSinceLastRequest < REQUEST_COOLDOWN_MS) {
				player.sendMessage(Text.literal("§cPlease wait before opening another bulk chest!"), false);
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
		int playerSilver = economy.getSilverCoins();
		int playerGold = economy.getGoldCoins();
		
		// Check if player can afford x10+1 package
		PaymentOption chosenOption = null;
		for (PaymentOption option : tier.getOpenTenPlusOneOptions()) {
			if (option.canAfford(playerSilver, playerGold)) {
				chosenOption = option;
				break; // Use first affordable option
			}
		}
		
		if (chosenOption == null) {
			player.sendMessage(Text.literal("§cNot enough currency for bulk opening!"), false);
			return;
		}
		
		// Deduct payment ONCE
		boolean paymentSuccess = true;
		if (chosenOption.getSilverCoins() > 0) {
			paymentSuccess = paymentSuccess && economy.removeSilverCoins(chosenOption.getSilverCoins());
		}
		if (chosenOption.getGoldCoins() > 0) {
			paymentSuccess = paymentSuccess && economy.removeGoldCoins(chosenOption.getGoldCoins());
		}
		
		if (!paymentSuccess) {
			player.sendMessage(Text.literal("§cPayment failed!"), false);
			return;
		}
		
		// Record this request to prevent duplicates
		lastBulkRequestId.put(playerId, requestId);
		
		// Roll 11 times (10 paid + 1 free)
		List<ItemStack> rewards = new ArrayList<>();
		Map<ChestRarity, Integer> rarityCounts = new HashMap<>();
		
		for (int i = 0; i < 11; i++) {
			// Roll for rarity
			ChestRarity rarity = tier.rollRarity(random);
			rarityCounts.put(rarity, rarityCounts.getOrDefault(rarity, 0) + 1);
			
			// Select reward from pool
			List<LootReward> pool = lootPools.get(rarity);
			if (pool != null && !pool.isEmpty()) {
				LootReward reward = pool.get(random.nextInt(pool.size()));
				ItemStack stack = reward.generateStack(random);
				rewards.add(stack);
			}
		}
		
		// Send bulk result to client for grid display
		ModNetworking.sendChestBulkResult(player, tier.getDisplayName(), rewards);
		
		// FIX: Actually give items to player!
		for (ItemStack stack : rewards) {
			if (stack != null && !stack.isEmpty()) {
				player.dropItem(stack, false); // Drop near player
			}
		}
		
		// Save and sync economy
		EconomyManager.savePlayerData(player);
		EconomyManager.syncToClient(player);
		
		// Play sound (client will also play on screen open)
		player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundCategory.PLAYERS, 1.0f, 1.0f);
		
		FocusTimerShop.LOGGER.info("Player {} opened {} x11 (requestId: {}), received {} items",
			player.getName().getString(), tier.name(), requestId, rewards.size());
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
}
