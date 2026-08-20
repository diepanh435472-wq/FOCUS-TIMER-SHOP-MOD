package com.focustimershop.achievement;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Achievement system manager - loads achievements from JSON (v1.0.6 Phase 5)
 */
public class AchievementSystemManager {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<AchievementDefinition> allAchievements = new ArrayList<>();
	private static Map<String, AchievementDefinition> achievementsById = new HashMap<>();
	private static boolean initialized = false;
	
	/**
	 * Initialize achievement system - load from achievements.json
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}
		
		File configFile = DatabaseManager.getConfigDir().resolve("achievements.json").toFile();
		
		if (!configFile.exists()) {
			FocusTimerShop.LOGGER.warn("achievements.json not found at {}", configFile.getAbsolutePath());
			initialized = true;
			return;
		}
		
		try (FileReader reader = new FileReader(configFile)) {
			allAchievements = GSON.fromJson(reader, new TypeToken<List<AchievementDefinition>>(){}.getType());
			
			// Build ID map
			achievementsById.clear();
			for (AchievementDefinition def : allAchievements) {
				achievementsById.put(def.getId(), def);
			}
			
			FocusTimerShop.LOGGER.info("Loaded {} achievements from JSON", allAchievements.size());
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to load achievements.json", e);
		}
		
		initialized = true;
	}
	
	/**
	 * Get all achievements
	 */
	public static List<AchievementDefinition> getAllAchievements() {
		if (!initialized) {
			initialize();
		}
		return new ArrayList<>(allAchievements);
	}
	
	/**
	 * Get achievement by ID
	 */
	public static AchievementDefinition getAchievementById(String id) {
		if (!initialized) {
			initialize();
		}
		return achievementsById.get(id);
	}
	
	/**
	 * Get achievements grouped by rarity
	 */
	public static Map<String, List<AchievementDefinition>> getAchievementsByRarity() {
		if (!initialized) {
			initialize();
		}
		
		Map<String, List<AchievementDefinition>> grouped = new HashMap<>();
		String[] rarities = {"Phổ biến", "Không phổ biến", "Hiếm", "Cực hiếm", "Huyền thoại"};
		
		for (String rarity : rarities) {
			grouped.put(rarity, new ArrayList<>());
		}
		
		for (AchievementDefinition def : allAchievements) {
			String rarity = def.getRarity();
			if (grouped.containsKey(rarity)) {
				grouped.get(rarity).add(def);
			}
		}
		
		return grouped;
	}
	
	/**
	 * v1.0.7-beta - Check achievements from JSON and unlock any newly completed
	 * Call this after timer session completion
	 */
	public static void checkAndUnlockAchievements(net.minecraft.server.network.ServerPlayerEntity player) {
		if (!initialized) {
			initialize();
		}
		
		if (allAchievements.isEmpty()) {
			return;
		}
		
		var profile = com.focustimershop.profile.ProfileManager.getProfile(player.getUuid());
		var stats = DatabaseManager.getPlayerStats(player.getUuid());
		
		// Get unlocked achievement IDs
		List<String> unlockedIds = profile.getUnlockedTitles(); // Reusing this list for achievement IDs
		if (unlockedIds == null) {
			unlockedIds = new ArrayList<>();
			profile.setUnlockedTitles(unlockedIds);
		}
		
		// Check each achievement
		for (AchievementDefinition achievement : allAchievements) {
			// Skip if already unlocked
			if (unlockedIds.contains(achievement.getId())) {
				continue;
			}
			
			// Check condition
			if (achievement.checkCondition(stats)) {
				// Unlock!
				unlockedIds.add(achievement.getId());
				
				// Feedback
				player.sendMessage(
					net.minecraft.text.Text.literal("§e🏆 Thành tựu mới: §f" + achievement.getName()),
					false
				);
				
				// Play sound
				player.playSound(
					net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
					net.minecraft.sound.SoundCategory.PLAYERS,
					1.0f, 1.0f
				);
				
				// Log
				FocusTimerShop.LOGGER.info("Player {} unlocked achievement: {} ({})",
					player.getName().getString(), achievement.getName(), achievement.getId());
			}
		}
		
		// Save profile if any new achievements
		com.focustimershop.profile.ProfileManager.saveProfile(profile);
	}
}
