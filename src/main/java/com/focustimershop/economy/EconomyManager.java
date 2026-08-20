package com.focustimershop.economy;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.util.IEntityDataSaver;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.registry.RegistryKey;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side manager for player economy data
 * Handles persistent storage via NBT
 * NOW SUPPORTS PER-WORLD ECONOMY: Each world has separate currency/XP
 */
public class EconomyManager {
	private static final String NBT_KEY = "FocusTimerShopEconomy";
	private static final String NBT_WORLDS_KEY = "Worlds";
	
	// Cache for quick access during runtime
	// Key format: "playerUUID_worldKey"
	// PHASE 3 FIX (BUG #16): ConcurrentHashMap for thread-safety
	private static final Map<String, PlayerEconomyData> playerDataCache = new java.util.concurrent.ConcurrentHashMap<>();
	
	/**
	 * Get cache key for player + world combination
	 */
	private static String getCacheKey(UUID playerId, String worldKey) {
		return playerId.toString() + "_" + worldKey;
	}
	
	/**
	 * Get world key from player's current dimension
	 */
	private static String getWorldKey(ServerPlayerEntity player) {
		RegistryKey<World> worldKey = player.getWorld().getRegistryKey();
		return worldKey.getValue().toString(); // e.g. "minecraft:overworld", "minecraft:the_nether"
	}

	/**
	 * Get or create economy data for a player IN THEIR CURRENT WORLD
	 */
	public static PlayerEconomyData getPlayerData(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		String worldKey = getWorldKey(player);
		String cacheKey = getCacheKey(uuid, worldKey);
		
		// Check cache first
		if (playerDataCache.containsKey(cacheKey)) {
			return playerDataCache.get(cacheKey);
		}

		// Load from NBT using persistent state
		NbtCompound playerNbt = ((IEntityDataSaver) player).focustimershop$getPersistentData();
		PlayerEconomyData data;
		
		if (playerNbt.contains(NBT_KEY)) {
			NbtCompound economyNbt = playerNbt.getCompound(NBT_KEY);
			
			// Check if per-world data exists
			if (economyNbt.contains(NBT_WORLDS_KEY)) {
				NbtCompound worldsNbt = economyNbt.getCompound(NBT_WORLDS_KEY);
				
				// Load data for current world
				if (worldsNbt.contains(worldKey)) {
					data = PlayerEconomyData.fromNbt(worldsNbt.getCompound(worldKey));
					FocusTimerShop.LOGGER.info("Loaded economy for player {} in world {}: {}s, {}xp, {}g", 
						player.getName().getString(), worldKey, 
						data.getSilverCoins(), data.getFocusXp(), data.getGoldCoins());
				} else {
					// First time in this world - create fresh economy
					data = new PlayerEconomyData();
					FocusTimerShop.LOGGER.info("Created fresh economy for player {} in NEW world {}", 
						player.getName().getString(), worldKey);
				}
			} else {
				// Legacy data (old version without per-world) - migrate to current world
				data = PlayerEconomyData.fromNbt(economyNbt);
				FocusTimerShop.LOGGER.info("Migrated legacy economy for player {} to world {}", 
					player.getName().getString(), worldKey);
			}
		} else {
			// Brand new player
			data = new PlayerEconomyData();
			FocusTimerShop.LOGGER.info("Created fresh economy for NEW player {} in world {}", 
				player.getName().getString(), worldKey);
		}

		playerDataCache.put(cacheKey, data);
		return data;
	}

	/**
	 * Save economy data to player NBT (for current world)
	 */
	public static void savePlayerData(ServerPlayerEntity player) {
		UUID uuid = player.getUuid();
		String worldKey = getWorldKey(player);
		String cacheKey = getCacheKey(uuid, worldKey);
		PlayerEconomyData data = playerDataCache.get(cacheKey);
		
		if (data != null) {
			NbtCompound playerNbt = ((IEntityDataSaver) player).focustimershop$getPersistentData();
			
			// Get or create economy root
			NbtCompound economyRoot;
			if (playerNbt.contains(NBT_KEY)) {
				economyRoot = playerNbt.getCompound(NBT_KEY);
			} else {
				economyRoot = new NbtCompound();
			}
			
			// Get or create worlds compound
			NbtCompound worldsNbt;
			if (economyRoot.contains(NBT_WORLDS_KEY)) {
				worldsNbt = economyRoot.getCompound(NBT_WORLDS_KEY);
			} else {
				worldsNbt = new NbtCompound();
			}
			
			// Save current world's data
			NbtCompound worldDataNbt = new NbtCompound();
			data.writeNbt(worldDataNbt);
			worldsNbt.put(worldKey, worldDataNbt);
			
			// Save back to root
			economyRoot.put(NBT_WORLDS_KEY, worldsNbt);
			playerNbt.put(NBT_KEY, economyRoot);
			
			FocusTimerShop.LOGGER.debug("Saved economy for player {} in world {}: {}s, {}xp, {}g",
				player.getName().getString(), worldKey,
				data.getSilverCoins(), data.getFocusXp(), data.getGoldCoins());
		}
	}

	/**
	 * Sync economy data to client
	 */
	public static void syncToClient(ServerPlayerEntity player) {
		PlayerEconomyData data = getPlayerData(player);
		ModNetworking.sendEconomySync(player, data);
	}

	/**
	 * Award rewards after timer completion
	 * @param elapsedSeconds Total seconds completed in focus session
	 */
	public static void awardTimerReward(ServerPlayerEntity player, int elapsedSeconds) {
		if (elapsedSeconds <= 0) {
			return;
		}

		PlayerEconomyData data = getPlayerData(player);
		
		// Calculate rewards: 45s = 1 Silver, 90s = 1 XP
		int silverEarned = elapsedSeconds / 45;
		int xpEarned = elapsedSeconds / 90;

		data.addSilverCoins(silverEarned);
		data.addFocusXp(xpEarned);

		savePlayerData(player);
		syncToClient(player);
		
		// Track stats (v1.0.6 - Phase A/Phase 1)
		com.focustimershop.database.PlayerStatsData stats = 
			com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		stats.setTotalSilverEarned(stats.getTotalSilverEarned() + silverEarned);
		
		// v1.0.6-beta SEASON SYSTEM - Update BOTH lifetime and seasonal XP
		stats.setTotalXpEarned(stats.getTotalXpEarned() + xpEarned);  // Lifetime (never decays)
		stats.setSeasonRankXp(stats.getSeasonRankXp() + xpEarned);    // Seasonal (decays monthly)
		
		// v1.0.6 Phase 1 - Add to daily stats (focus time, session count, XP)
		stats.addDailyStat(elapsedSeconds, xpEarned);
		
		com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
		
		// Award XP to profile system (v1.0.6) - handles rank-ups
		com.focustimershop.profile.ProfileManager.awardFocusXp(player, xpEarned);
		
		// v1.0.6 Phase 5 - Check achievements
		com.focustimershop.achievement.AchievementManager.checkAchievements(player);
		
		// v1.0.7-beta - Check achievements from JSON (includes TIMER_TYPE_USES)
		com.focustimershop.achievement.AchievementSystemManager.checkAndUnlockAchievements(player);
		
		// v1.0.6 Phase 6 - Check missions
		com.focustimershop.mission.MissionManager.checkMissions(player);

		FocusTimerShop.LOGGER.info("Player {} earned {} Silver Coins and {} Focus XP from {}s timer",
			player.getName().getString(), silverEarned, xpEarned, elapsedSeconds);
	}

	/**
	 * Clean up cache when player disconnects
	 */
	public static void onPlayerDisconnect(ServerPlayerEntity player) {
		savePlayerData(player);
		
		// Remove from cache (per-world cache key)
		UUID uuid = player.getUuid();
		String worldKey = getWorldKey(player);
		String cacheKey = getCacheKey(uuid, worldKey);
		playerDataCache.remove(cacheKey);
	}

	/**
	 * Called when player joins - ensure data is loaded
	 */
	public static void onPlayerJoin(ServerPlayerEntity player) {
		getPlayerData(player); // Load into cache
		syncToClient(player);
	}
	
	/**
	 * Called when player changes dimension - sync new world's economy
	 */
	public static void onPlayerChangeDimension(ServerPlayerEntity player) {
		// Save old world's data (already done by dimension change event)
		// Load and sync new world's economy
		FocusTimerShop.LOGGER.info("Player {} changed dimension to {}, syncing economy",
			player.getName().getString(), getWorldKey(player));
		
		getPlayerData(player); // Load new world's data into cache
		syncToClient(player);   // Sync to client
	}
}
