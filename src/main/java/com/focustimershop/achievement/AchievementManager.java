package com.focustimershop.achievement;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Achievement system manager (v1.0.6 Phase 5)
 * Config-driven, cosmetic-only achievements
 */
public class AchievementManager {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<Achievement> achievements = new ArrayList<>();
	private static boolean initialized = false;
	
	/**
	 * Initialize achievement system
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}
		
		File configFile = DatabaseManager.getConfigDir().resolve("achievements.json").toFile();
		
		if (configFile.exists()) {
			// Load existing config
			try (FileReader reader = new FileReader(configFile)) {
				achievements = GSON.fromJson(reader, new TypeToken<List<Achievement>>(){}.getType());
				FocusTimerShop.LOGGER.info("Loaded {} achievements", achievements.size());
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("Failed to load achievements, generating default", e);
				generateDefaultAchievements(configFile);
			}
		} else {
			// Generate default config
			generateDefaultAchievements(configFile);
		}
		
		initialized = true;
	}
	
	/**
	 * Generate default achievement config
	 */
	private static void generateDefaultAchievements(File configFile) {
		achievements.clear();
		
		// Trivial first achievement - unlocks on first session
		achievements.add(new Achievement(
			"first_session",
			"Phiên Đầu Tiên",
			"first_session_icon",
			Achievement.ConditionType.TOTAL_SESSIONS,
			1
		));
		
		// Session milestones
		achievements.add(new Achievement(
			"session_10",
			"Kiên Trì I",
			"sessions_10_icon",
			Achievement.ConditionType.TOTAL_SESSIONS,
			10
		));
		
		achievements.add(new Achievement(
			"session_50",
			"Kiên Trì II",
			"sessions_50_icon",
			Achievement.ConditionType.TOTAL_SESSIONS,
			50
		));
		
		achievements.add(new Achievement(
			"session_100",
			"Kiên Trì III",
			"sessions_100_icon",
			Achievement.ConditionType.TOTAL_SESSIONS,
			100
		));
		
		// Focus hour milestones
		achievements.add(new Achievement(
			"hours_10",
			"Tập Trung I",
			"hours_10_icon",
			Achievement.ConditionType.TOTAL_FOCUS_HOURS,
			10
		));
		
		achievements.add(new Achievement(
			"hours_50",
			"Tập Trung II",
			"hours_50_icon",
			Achievement.ConditionType.TOTAL_FOCUS_HOURS,
			50
		));
		
		achievements.add(new Achievement(
			"hours_100",
			"Tập Trung III",
			"hours_100_icon",
			Achievement.ConditionType.TOTAL_FOCUS_HOURS,
			100
		));
		
		// Streak milestones
		achievements.add(new Achievement(
			"streak_7",
			"Tuần Đầu Tiên",
			"streak_7_icon",
			Achievement.ConditionType.STREAK_DAYS,
			7
		));
		
		achievements.add(new Achievement(
			"streak_30",
			"Tháng Đầu Tiên",
			"streak_30_icon",
			Achievement.ConditionType.STREAK_DAYS,
			30
		));
		
		achievements.add(new Achievement(
			"streak_100",
			"Bất Bại",
			"streak_100_icon",
			Achievement.ConditionType.STREAK_DAYS,
			100
		));
		
		// Save to file
		try (FileWriter writer = new FileWriter(configFile)) {
			GSON.toJson(achievements, writer);
			FocusTimerShop.LOGGER.info("Generated default achievements config with {} achievements", 
				achievements.size());
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to save achievements config", e);
		}
	}
	
	/**
	 * Check for newly-unlocked achievements
	 * Call this after session completion, rank-up, etc.
	 */
	public static void checkAchievements(ServerPlayerEntity player) {
		var profile = com.focustimershop.profile.ProfileManager.getProfile(player.getUuid());
		var stats = DatabaseManager.getPlayerStats(player.getUuid());
		var rank = com.focustimershop.profile.RankManager.resolveRank(stats.getTotalXpEarned());
		
		// Get unlocked IDs
		List<String> unlockedIds = profile.getUnlockedTitles(); // Reusing this list for achievement IDs
		if (unlockedIds == null) {
			unlockedIds = new ArrayList<>();
			profile.setUnlockedTitles(unlockedIds);
		}
		
		// Check each achievement
		for (Achievement achievement : achievements) {
			// Skip if already unlocked
			if (unlockedIds.contains(achievement.getId())) {
				continue;
			}
			
			// Check condition
			if (achievement.checkCondition(stats, profile, rank)) {
				// Unlock!
				unlockedIds.add(achievement.getId());
				
				// Feedback
				player.sendMessage(
					net.minecraft.text.Text.literal("§e🏆 Thành tựu mới: §f" + achievement.getDisplayName()),
					false
				);
				
				// Play sound
				player.playSound(
					net.minecraft.sound.SoundEvents.UI_TOAST_CHALLENGE_COMPLETE,
					net.minecraft.sound.SoundCategory.PLAYERS,
					1.0f, 1.0f
				);
				
				// Log
				FocusTimerShop.LOGGER.info("Player {} unlocked achievement: {}",
					player.getName().getString(), achievement.getDisplayName());
			}
		}
		
		// Save profile if any new achievements
		com.focustimershop.profile.ProfileManager.saveProfile(profile);
	}
	
	/**
	 * Get all achievements (for UI display)
	 */
	public static List<Achievement> getAllAchievements() {
		if (!initialized) {
			initialize();
		}
		return new ArrayList<>(achievements);
	}
}
