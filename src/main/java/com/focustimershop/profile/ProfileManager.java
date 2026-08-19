package com.focustimershop.profile;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages player profiles
 * Persistence: FCTMS/profiles/<uuid>.json
 */
public class ProfileManager {
	
	private static final Map<UUID, PlayerProfile> profileCache = new HashMap<>();
	
	/**
	 * Initialize profile system
	 */
	public static void initialize() {
		// Ensure profiles directory exists
		File profilesDir = getProfilesDir().toFile();
		if (!profilesDir.exists()) {
			profilesDir.mkdirs();
		}
		
		FocusTimerShop.LOGGER.info("ProfileManager initialized");
	}
	
	/**
	 * Get profiles directory path
	 */
	private static java.nio.file.Path getProfilesDir() {
		return DatabaseManager.getRoot().resolve("profiles");
	}
	
	/**
	 * Load or create player profile
	 */
	public static PlayerProfile getProfile(UUID playerId) {
		// Check cache
		PlayerProfile profile = profileCache.get(playerId);
		if (profile != null) {
			return profile;
		}
		
		// Load from disk
		File file = getProfilesDir().resolve(playerId.toString() + ".json").toFile();
		profile = DatabaseManager.readJson(file, PlayerProfile.class);
		
		if (profile == null) {
			// Create new profile
			profile = new PlayerProfile(playerId, "Unknown");
			FocusTimerShop.LOGGER.info("Created new profile for {}", playerId);
		}
		
		// Cache
		profileCache.put(playerId, profile);
		return profile;
	}
	
	/**
	 * Save player profile
	 */
	public static void saveProfile(PlayerProfile profile) {
		if (profile == null || profile.getPlayerUuid() == null) {
			return;
		}
		
		UUID playerId = UUID.fromString(profile.getPlayerUuid());
		File file = getProfilesDir().resolve(playerId.toString() + ".json").toFile();
		DatabaseManager.writeJson(file, profile);
		
		// Update cache
		profileCache.put(playerId, profile);
	}
	
	/**
	 * Handle player join
	 */
	public static void onPlayerJoin(ServerPlayerEntity player) {
		PlayerProfile profile = getProfile(player.getUuid());
		
		// Update in-game name
		profile.setInGameName(player.getName().getString());
		
		// Save
		saveProfile(profile);
		
		// Sync to client (v1.0.6)
		com.focustimershop.network.ModNetworking.sendProfileSync(player, profile);
		
		// Rank is now derived from PlayerStatsData.totalXpEarned
		long totalXp = com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid()).getTotalXpEarned();
		FocusTimerShop.LOGGER.info("Player {} joined - Profile loaded (Rank: {})", 
			player.getName().getString(), 
			RankManager.resolveRank(totalXp).getDisplayName());
	}
	
	/**
	 * Handle player disconnect
	 */
	public static void onPlayerDisconnect(ServerPlayerEntity player) {
		PlayerProfile profile = profileCache.get(player.getUuid());
		if (profile != null) {
			saveProfile(profile);
		}
	}
	
	/**
	 * Award Focus XP and check for rank-up (Phase A - uses PlayerStatsData)
	 */
	public static void awardFocusXp(ServerPlayerEntity player, long xp) {
		// XP is already tracked in PlayerStatsData by EconomyManager
		// Just do rank-up check here
		com.focustimershop.database.PlayerStatsData stats = 
			com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		
		long oldXP = stats.getTotalXpEarned() - xp; // Before this award
		long newXP = stats.getTotalXpEarned();
		
		// Check for rank-up
		RankTier newRank = RankManager.checkRankUp(oldXP, newXP);
		if (newRank != null) {
			handleRankUp(player, newRank);
		}
		
		// No need to sync profile here - data is in PlayerStatsData
	}
	
	/**
	 * Record completed session
	 */
	public static void recordSession(ServerPlayerEntity player, int sessionSeconds, String timerType) {
		PlayerProfile profile = getProfile(player.getUuid());
		profile.recordSession(sessionSeconds, timerType);
		saveProfile(profile);
		
		// Sync to client (v1.0.6)
		com.focustimershop.network.ModNetworking.sendProfileSync(player, profile);
	}
	
	/**
	 * Handle rank-up feedback
	 */
	private static void handleRankUp(ServerPlayerEntity player, RankTier newRank) {
		// Play sound
		player.playSound(
			net.minecraft.sound.SoundEvents.ENTITY_PLAYER_LEVELUP,
			net.minecraft.sound.SoundCategory.PLAYERS,
			1.0f, 1.0f
		);
		
		// Action bar message
		player.sendMessage(
			net.minecraft.text.Text.literal("🎉 Thăng hạng: " + newRank.getDisplayName() + "!"),
			true // Action bar
		);
		
		// Phase B - Activity log
		com.focustimershop.database.PlayerStatsData stats = 
			com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		stats.addActivity(
			com.focustimershop.database.ActivityEntry.Type.RANK_UP,
			String.format("⬆ Đã đạt %s", newRank.getDisplayName())
		);
		com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
		
		// TODO: If tier boundary crossed, broadcast to server
		// For now, just log
		FocusTimerShop.LOGGER.info("Player {} ranked up to {}", 
			player.getName().getString(), newRank.getDisplayName());
	}
	
	/**
	 * Set custom name (with validation)
	 */
	public static boolean setCustomName(UUID playerId, String customName) {
		// Validation
		if (customName == null || customName.trim().isEmpty()) {
			return false;
		}
		
		// Max length 24
		if (customName.length() > 24) {
			return false;
		}
		
		// No formatting codes
		if (customName.contains("§") || customName.contains("&")) {
			return false;
		}
		
		// Apply
		PlayerProfile profile = getProfile(playerId);
		profile.setCustomName(customName.trim());
		saveProfile(profile);
		
		return true;
	}
	
	/**
	 * Update custom name from client (v1.0.6)
	 */
	public static void updateCustomName(ServerPlayerEntity player, String newName) {
		boolean success = setCustomName(player.getUuid(), newName);
		
		if (success) {
			// Sync back to client
			PlayerProfile profile = getProfile(player.getUuid());
			com.focustimershop.network.ModNetworking.sendProfileSync(player, profile);
			
			// Feedback
			player.sendMessage(
				net.minecraft.text.Text.literal("§a✔ Đã cập nhật tên custom: §f" + newName),
				false
			);
			
			FocusTimerShop.LOGGER.info("Player {} updated custom name to: {}", 
				player.getName().getString(), newName);
		} else {
			// Error
			player.sendMessage(
				net.minecraft.text.Text.literal("§c✖ Không thể cập nhật tên custom (kiểm tra độ dài hoặc ký tự đặc biệt)"),
				false
			);
		}
	}
	
	/**
	 * Equip title (v1.0.6 Phase 5)
	 */
	public static void equipTitle(ServerPlayerEntity player, String titleId) {
		PlayerProfile profile = getProfile(player.getUuid());
		
		// Check if title is unlocked
		java.util.List<String> unlockedTitles = profile.getUnlockedTitles();
		if (unlockedTitles == null || !unlockedTitles.contains(titleId)) {
			player.sendMessage(
				net.minecraft.text.Text.literal("§c✖ Danh hiệu này chưa được mở khóa!"),
				false
			);
			return;
		}
		
		// Equip
		profile.setEquippedTitleId(titleId);
		saveProfile(profile);
		
		// Get title name for feedback
		com.focustimershop.title.TitleDefinition title = 
			com.focustimershop.title.TitleSystemManager.getTitleById(titleId);
		String titleName = title != null ? title.getName() : titleId;
		
		// Feedback
		player.sendMessage(
			net.minecraft.text.Text.literal("§d✔ Đã trang bị danh hiệu: §f" + titleName),
			false
		);
		
		// Sync to client
		com.focustimershop.network.ModNetworking.sendProfileSync(player, profile);
		
		FocusTimerShop.LOGGER.info("Player {} equipped title: {}", 
			player.getName().getString(), titleName);
	}
	
	/**
	 * Clear all cached profiles (server shutdown)
	 */
	public static void clearAll() {
		for (PlayerProfile profile : profileCache.values()) {
			saveProfile(profile);
		}
		profileCache.clear();
	}
}
