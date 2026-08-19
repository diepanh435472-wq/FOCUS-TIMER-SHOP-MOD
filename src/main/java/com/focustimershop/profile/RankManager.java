package com.focustimershop.profile;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Manages rank configuration and rank resolution
 * Pure functions - no mutable state except config cache
 */
public class RankManager {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<RankConfig> rankConfigs = new ArrayList<>();
	private static boolean initialized = false;
	
	/**
	 * Initialize rank system - load or generate config
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}
		
		File configFile = DatabaseManager.getConfigDir().resolve("rank_config.json").toFile();
		
		if (configFile.exists()) {
			// Load existing config
			try (FileReader reader = new FileReader(configFile)) {
				rankConfigs = GSON.fromJson(reader, new TypeToken<List<RankConfig>>(){}.getType());
				FocusTimerShop.LOGGER.info("Loaded {} rank configurations", rankConfigs.size());
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("Failed to load rank config, generating default", e);
				generateDefaultConfig(configFile);
			}
		} else {
			// Generate default config
			generateDefaultConfig(configFile);
		}
		
		initialized = true;
	}
	
	/**
	 * Generate default 58-rank configuration based on spec
	 */
	private static void generateDefaultConfig(File configFile) {
		rankConfigs.clear();
		
		// All 58 ranks from the spec table
		// Format: tier, level, symbol, requiredXP, cumulativeXP, frameColor, animated
		
		// Chưa Hạng (Unranked) - 5 levels
		addRank("Chưa Hạng", 1, "unranked", 15, 15, "#808080", false);
		addRank("Chưa Hạng", 2, "unranked", 15, 30, "#808080", false);
		addRank("Chưa Hạng", 3, "unranked", 15, 45, "#808080", false);
		addRank("Chưa Hạng", 4, "unranked", 20, 65, "#808080", false);
		addRank("Chưa Hạng", 5, "unranked", 20, 85, "#808080", false);
		
		// Đồng (Bronze) - 5 levels
		addRank("Đồng", 1, "bronze", 20, 105, "#CD7F32", false);
		addRank("Đồng", 2, "bronze", 25, 130, "#CD7F32", false);
		addRank("Đồng", 3, "bronze", 25, 155, "#CD7F32", false);
		addRank("Đồng", 4, "bronze", 30, 185, "#CD7F32", false);
		addRank("Đồng", 5, "bronze", 30, 215, "#CD7F32", false);
		
		// Sắt (Iron) - 5 levels
		addRank("Sắt", 1, "iron", 30, 245, "#B0B0B0", false);
		addRank("Sắt", 2, "iron", 35, 280, "#B0B0B0", false);
		addRank("Sắt", 3, "iron", 40, 320, "#B0B0B0", false);
		addRank("Sắt", 4, "iron", 45, 365, "#B0B0B0", false);
		addRank("Sắt", 5, "iron", 50, 415, "#B0B0B0", false);
		
		// Thép (Steel) - 5 levels
		addRank("Thép", 1, "steel", 50, 465, "#707070", false);
		addRank("Thép", 2, "steel", 55, 520, "#707070", false);
		addRank("Thép", 3, "steel", 60, 580, "#707070", false);
		addRank("Thép", 4, "steel", 70, 650, "#707070", false);
		addRank("Thép", 5, "steel", 75, 725, "#707070", false);
		
		// Silver - 5 levels
		addRank("Silver", 1, "silver", 75, 800, "#C0C0C0", false);
		addRank("Silver", 2, "silver", 85, 885, "#C0C0C0", false);
		addRank("Silver", 3, "silver", 95, 980, "#C0C0C0", false);
		addRank("Silver", 4, "silver", 105, 1085, "#C0C0C0", false);
		addRank("Silver", 5, "silver", 120, 1205, "#C0C0C0", false);
		
		// Gold - 5 levels
		addRank("Gold", 1, "gold", 115, 1320, "#FFD700", false);
		addRank("Gold", 2, "gold", 130, 1450, "#FFD700", false);
		addRank("Gold", 3, "gold", 145, 1595, "#FFD700", false);
		addRank("Gold", 4, "gold", 165, 1760, "#FFD700", false);
		addRank("Gold", 5, "gold", 185, 1945, "#FFD700", false);
		
		// Platinum - 5 levels
		addRank("Platinum", 1, "platinum", 180, 2125, "#7FFFD4", false);
		addRank("Platinum", 2, "platinum", 205, 2330, "#7FFFD4", false);
		addRank("Platinum", 3, "platinum", 225, 2555, "#7FFFD4", false);
		addRank("Platinum", 4, "platinum", 255, 2810, "#7FFFD4", false);
		addRank("Platinum", 5, "platinum", 285, 3095, "#7FFFD4", false);
		
		// Titanium - 5 levels
		addRank("Titanium", 1, "titanium", 280, 3375, "#4682B4", false);
		addRank("Titanium", 2, "titanium", 315, 3690, "#4682B4", false);
		addRank("Titanium", 3, "titanium", 350, 4040, "#4682B4", false);
		addRank("Titanium", 4, "titanium", 395, 4435, "#4682B4", false);
		addRank("Titanium", 5, "titanium", 440, 4875, "#4682B4", false);
		
		// Diamond - 5 levels
		addRank("Diamond", 1, "diamond", 435, 5310, "#00FFFF", false);
		addRank("Diamond", 2, "diamond", 485, 5795, "#00FFFF", false);
		addRank("Diamond", 3, "diamond", 545, 6340, "#00FFFF", false);
		addRank("Diamond", 4, "diamond", 610, 6950, "#00FFFF", false);
		addRank("Diamond", 5, "diamond", 685, 7635, "#00FFFF", false);
		
		// Elite - 5 levels
		addRank("Elite", 1, "elite", 675, 8310, "#9400D3", false);
		addRank("Elite", 2, "elite", 755, 9065, "#9400D3", false);
		addRank("Elite", 3, "elite", 845, 9910, "#9400D3", false);
		addRank("Elite", 4, "elite", 945, 10855, "#9400D3", false);
		addRank("Elite", 5, "elite", 1060, 11915, "#9400D3", false);
		
		// Master - 5 levels
		addRank("Master", 1, "master", 1045, 12960, "#FF4500", false);
		addRank("Master", 2, "master", 1170, 14130, "#FF4500", false);
		addRank("Master", 3, "master", 1310, 15440, "#FF4500", false);
		addRank("Master", 4, "master", 1465, 16905, "#FF4500", false);
		addRank("Master", 5, "master", 1645, 18550, "#FF4500", false);
		
		// Legend - 3 levels (final tier)
		addRank("Legend", 1, "legend_flame", 1620, 20170, "#FF0000", true);
		addRank("Legend", 2, "legend_flame", 1810, 21980, "#FF0000", true);
		addRank("Legend", 3, "legend_flame", 2030, 24010, "#FF0000", true);
		
		// Save to file
		try (FileWriter writer = new FileWriter(configFile)) {
			GSON.toJson(rankConfigs, writer);
			FocusTimerShop.LOGGER.info("Generated default rank config with {} ranks", rankConfigs.size());
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to save rank config", e);
		}
	}
	
	private static void addRank(String tier, int level, String symbol, int requiredXP, 
	                             long cumulativeXP, String frameColor, boolean animated) {
		rankConfigs.add(new RankConfig(tier, level, symbol, requiredXP, cumulativeXP, frameColor, animated));
	}
	
	/**
	 * Resolve rank from total Focus XP earned
	 * Pure function - always derives from XP, never stored separately
	 * 
	 * FIXED v1.0.6 Bug Fix 3: Player at EXACT cumulative boundary advances to next rank
	 */
	public static RankTier resolveRank(long totalFocusXpEarned) {
		if (rankConfigs.isEmpty()) {
			initialize();
		}
		
		// Find the CURRENT rank by checking where player's XP falls
		// Logic: Find the LAST rank whose ENTRY threshold has been met
		// (not completion threshold - that would advance to next rank)
		RankConfig currentRank = rankConfigs.get(0);
		int rankIndex = 0;
		
		for (int i = 0; i < rankConfigs.size(); i++) {
			RankConfig config = rankConfigs.get(i);
			long rankEntryXP = config.getCumulativeXP() - config.getRequiredXP();
			
			// If player has enough XP to ENTER this rank, they're at least this rank
			if (totalFocusXpEarned >= rankEntryXP) {
				currentRank = config;
				rankIndex = i;
			}
			
			// Check if player has COMPLETED this rank (reached its cumulative XP)
			// If yes and there's a next rank, they should be at the next rank instead
			if (totalFocusXpEarned >= config.getCumulativeXP()) {
				// Player completed this rank - check if there's a next rank
				if (i + 1 < rankConfigs.size()) {
					currentRank = rankConfigs.get(i + 1);
					rankIndex = i + 1;
				}
				// If this is last rank, keep them here at 100%+
			}
		}
		
		// Calculate XP into current rank
		long startOfCurrentRankXP = currentRank.getCumulativeXP() - currentRank.getRequiredXP();
		long xpIntoLevel = totalFocusXpEarned - startOfCurrentRankXP;
		long xpNeededForLevel = currentRank.getRequiredXP();
		
		// For max rank, allow going over 100%
		boolean maxRank = (rankIndex == rankConfigs.size() - 1);
		if (maxRank && xpIntoLevel > xpNeededForLevel) {
			// At max rank, just cap at 100% for display
			xpIntoLevel = xpNeededForLevel;
		}
		
		// Sanity check for non-max ranks
		if (!maxRank && xpIntoLevel > xpNeededForLevel) {
			FocusTimerShop.LOGGER.error(
				"RANK BUG: Player has {} XP, currently at rank {} (index {}), but xpIntoLevel={} > xpNeededForLevel={}",
				totalFocusXpEarned, currentRank.getDisplayName(), rankIndex, xpIntoLevel, xpNeededForLevel
			);
			xpIntoLevel = xpNeededForLevel; // Clamp to prevent visual glitch
		}
		
		// Calculate next rank info
		String nextRankName = null;
		long xpToNextRank = 0;
		
		if (!maxRank) {
			RankConfig nextRankConfig = rankConfigs.get(rankIndex + 1);
			nextRankName = nextRankConfig.getDisplayName();
			xpToNextRank = nextRankConfig.getCumulativeXP() - totalFocusXpEarned;
		}
		
		return new RankTier(
			currentRank.getTier(),
			currentRank.getLevel(),
			currentRank.getDisplayName(),
			xpIntoLevel,
			xpNeededForLevel,
			currentRank.getFrameColor(),
			currentRank.isAnimated(),
			maxRank,
			nextRankName,
			xpToNextRank
		);
	}
	
	/**
	 * Check if XP gain causes a rank-up
	 * Returns the new rank if rank-up occurred, null otherwise
	 */
	public static RankTier checkRankUp(long oldXP, long newXP) {
		RankTier oldRank = resolveRank(oldXP);
		RankTier newRank = resolveRank(newXP);
		
		// Check if rank changed
		if (!oldRank.getTier().equals(newRank.getTier()) || 
		    oldRank.getLevel() != newRank.getLevel()) {
			return newRank;
		}
		
		return null;
	}
	
	/**
	 * Get all rank configs (for admin/debug)
	 */
	public static List<RankConfig> getAllRanks() {
		if (rankConfigs.isEmpty()) {
			initialize();
		}
		return new ArrayList<>(rankConfigs);
	}
}
