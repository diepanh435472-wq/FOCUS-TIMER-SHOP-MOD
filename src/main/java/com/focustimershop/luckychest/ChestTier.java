package com.focustimershop.luckychest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public enum ChestTier {
	// Format: displayName, openOne options, openTenPlusOne options, rarity weights
	WOODEN("Wooden Chest", 
		List.of(PaymentOption.silver(40)),
		List.of(PaymentOption.silver(400), PaymentOption.gold(4)),
		buildWeights(80, 18, 2, 0, 0)),
	
	STONE("Stone Chest",
		List.of(PaymentOption.silver(60)),
		List.of(PaymentOption.silver(600), PaymentOption.gold(6)),
		buildWeights(70, 25, 5, 0, 0)),
	
	COAL("Coal Chest",
		List.of(PaymentOption.silver(80)),
		List.of(PaymentOption.silver(800), PaymentOption.gold(8)),
		buildWeights(60, 30, 9, 1, 0)),
	
	COPPER("Copper Chest",
		List.of(PaymentOption.silver(110), PaymentOption.mixed(1, 10)),
		List.of(PaymentOption.silver(1100), PaymentOption.gold(11)),
		buildWeights(50, 35, 12, 3, 0)),
	
	IRON("Iron Chest",
		List.of(PaymentOption.silver(150), PaymentOption.mixed(1, 50)),
		List.of(PaymentOption.silver(1500), PaymentOption.gold(15)),
		buildWeights(40, 35, 18, 7, 0)),
	
	GOLD("Gold Chest",
		List.of(PaymentOption.silver(300), PaymentOption.gold(3)),
		List.of(PaymentOption.silver(3000), PaymentOption.gold(30)),
		buildWeights(30, 35, 22, 12, 1)),
	
	LAPIS("Lapis Chest",
		List.of(PaymentOption.silver(400), PaymentOption.gold(4)),
		List.of(PaymentOption.silver(4000), PaymentOption.gold(40)),
		buildWeights(25, 30, 28, 15, 2)),
	
	DIAMOND("Diamond Chest",
		List.of(PaymentOption.silver(800), PaymentOption.gold(8)),
		List.of(PaymentOption.silver(8000), PaymentOption.gold(80)),
		buildWeights(20, 25, 30, 20, 5)),
	
	QUARTZ("Quartz Chest",
		List.of(PaymentOption.silver(1000), PaymentOption.gold(10)),
		List.of(PaymentOption.silver(10000), PaymentOption.gold(100)),
		buildWeights(15, 23, 30, 25, 7)),
	
	NETHERITE("Netherite Chest",
		List.of(PaymentOption.silver(1500), PaymentOption.gold(15)),
		List.of(PaymentOption.silver(15000), PaymentOption.gold(150)),
		buildWeights(12, 20, 30, 28, 10)),
	
	OBSIDIAN("Obsidian Chest",
		List.of(PaymentOption.silver(2000), PaymentOption.gold(20)),
		List.of(PaymentOption.silver(20000), PaymentOption.gold(200)),
		buildWeights(10, 18, 28, 30, 14)),
	
	BEDROCK("Bedrock Chest",
		List.of(PaymentOption.silver(3000), PaymentOption.gold(30)),
		List.of(PaymentOption.silver(30000), PaymentOption.gold(300)),
		buildWeights(10, 25, 30, 25, 10));

	private final String displayName;
	private final List<PaymentOption> openOneOptions;
	private final List<PaymentOption> openTenPlusOneOptions;
	private final Map<ChestRarity, Integer> rarityWeights;

	ChestTier(String displayName, List<PaymentOption> openOneOptions, 
	          List<PaymentOption> openTenPlusOneOptions, Map<ChestRarity, Integer> rarityWeights) {
		this.displayName = displayName;
		this.openOneOptions = openOneOptions;
		this.openTenPlusOneOptions = openTenPlusOneOptions;
		this.rarityWeights = rarityWeights;
	}

	private static Map<ChestRarity, Integer> buildWeights(int common, int uncommon, int rare, int epic, int legendary) {
		Map<ChestRarity, Integer> weights = new HashMap<>();
		weights.put(ChestRarity.COMMON, common);
		weights.put(ChestRarity.UNCOMMON, uncommon);
		weights.put(ChestRarity.RARE, rare);
		weights.put(ChestRarity.EPIC, epic);
		weights.put(ChestRarity.LEGENDARY, legendary);
		return weights;
	}

	public String getDisplayName() {
		return displayName;
	}

	public List<PaymentOption> getOpenOneOptions() {
		return openOneOptions;
	}

	public List<PaymentOption> getOpenTenPlusOneOptions() {
		return openTenPlusOneOptions;
	}

	/**
	 * @deprecated Use getOpenOneOptions() instead
	 */
	@Deprecated
	public int getCost() {
		// Fallback for backward compatibility - return first option's cost
		if (openOneOptions.isEmpty()) return 0;
		PaymentOption first = openOneOptions.get(0);
		return first.getSilverCoins() > 0 ? first.getSilverCoins() : first.getGoldCoins();
	}

	/**
	 * @deprecated Use getOpenOneOptions() to check payment types
	 */
	@Deprecated
	public boolean usesSilver() {
		if (openOneOptions.isEmpty()) return true;
		return openOneOptions.get(0).getSilverCoins() > 0;
	}

	/**
	 * Check if player can afford with ANY of the payment options
	 */
	public boolean canAffordOpenOne(long playerSilver, long playerGold) {
		for (PaymentOption option : openOneOptions) {
			if (option.canAfford(playerSilver, playerGold)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if player can afford x10+1 with ANY of the payment options
	 */
	public boolean canAffordOpenTenPlusOne(long playerSilver, long playerGold) {
		for (PaymentOption option : openTenPlusOneOptions) {
			if (option.canAfford(playerSilver, playerGold)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Get cheapest affordable option for opening one
	 */
	public PaymentOption getCheapestOpenOne(long playerSilver, long playerGold) {
		PaymentOption cheapest = null;
		int cheapestValue = Integer.MAX_VALUE;
		
		for (PaymentOption option : openOneOptions) {
			if (option.canAfford(playerSilver, playerGold)) {
				int value = option.getSilverCoins() + (option.getGoldCoins() * 100); // 1g = 100s
				if (value < cheapestValue) {
					cheapestValue = value;
					cheapest = option;
				}
			}
		}
		
		return cheapest;
	}

	public Map<ChestRarity, Integer> getRarityWeights() {
		return rarityWeights;
	}

	public int getTotalWeight() {
		return rarityWeights.values().stream().mapToInt(Integer::intValue).sum();
	}

	/**
	 * Roll for a rarity based on weighted probabilities
	 * PHASE 3: Now accepts Random parameter for thread-safety
	 */
	public ChestRarity rollRarity(java.util.Random random) {
		int totalWeight = getTotalWeight();
		int roll = random.nextInt(totalWeight);
		int cumulative = 0;

		for (Map.Entry<ChestRarity, Integer> entry : rarityWeights.entrySet()) {
			cumulative += entry.getValue();
			if (roll < cumulative) {
				return entry.getKey();
			}
		}

		return ChestRarity.COMMON; // Fallback
	}
}
