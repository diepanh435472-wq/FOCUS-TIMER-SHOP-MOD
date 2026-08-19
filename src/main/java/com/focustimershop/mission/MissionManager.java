package com.focustimershop.mission;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.minecraft.server.network.ServerPlayerEntity;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Mission system manager (v1.0.6 Phase 6)
 * Server-wide daily/weekly missions
 */
public class MissionManager {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<Mission> missionPool = new ArrayList<>();
	private static boolean initialized = false;
	
	// Current active missions (server-wide)
	private static Mission currentDailyMission;
	private static Mission currentWeeklyMission;
	private static String currentDailyDate = ""; // yyyy-MM-dd
	private static int currentWeekNumber = 0; // year*100 + week
	
	/**
	 * Initialize mission system
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}
		
		File configFile = DatabaseManager.getConfigDir().resolve("missions.json").toFile();
		
		if (configFile.exists()) {
			// Load existing config
			try (FileReader reader = new FileReader(configFile)) {
				missionPool = GSON.fromJson(reader, new TypeToken<List<Mission>>(){}.getType());
				FocusTimerShop.LOGGER.info("Loaded {} missions", missionPool.size());
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("Failed to load missions, generating default", e);
				generateDefaultMissions(configFile);
			}
		} else {
			// Generate default config
			generateDefaultMissions(configFile);
		}
		
		// Initialize current missions
		refreshMissions();
		
		initialized = true;
	}
	
	/**
	 * Generate default mission pool
	 */
	private static void generateDefaultMissions(File configFile) {
		missionPool.clear();
		
		// Daily missions
		missionPool.add(new Mission(
			"daily_15min",
			Mission.MissionType.DAILY,
			Mission.MetricType.FOCUS_MINUTES,
			15,
			"Tập trung 15 phút hôm nay"
		));
		
		missionPool.add(new Mission(
			"daily_30min",
			Mission.MissionType.DAILY,
			Mission.MetricType.FOCUS_MINUTES,
			30,
			"Tập trung 30 phút hôm nay"
		));
		
		missionPool.add(new Mission(
			"daily_2sessions",
			Mission.MissionType.DAILY,
			Mission.MetricType.SESSION_COUNT,
			2,
			"Hoàn thành 2 phiên hôm nay"
		));
		
		missionPool.add(new Mission(
			"daily_10xp",
			Mission.MissionType.DAILY,
			Mission.MetricType.XP_EARNED,
			10,
			"Kiếm 10 XP hôm nay"
		));
		
		// Weekly missions
		missionPool.add(new Mission(
			"weekly_120min",
			Mission.MissionType.WEEKLY,
			Mission.MetricType.FOCUS_MINUTES,
			120,
			"Tập trung 2 giờ tuần này"
		));
		
		missionPool.add(new Mission(
			"weekly_180min",
			Mission.MissionType.WEEKLY,
			Mission.MetricType.FOCUS_MINUTES,
			180,
			"Tập trung 3 giờ tuần này"
		));
		
		missionPool.add(new Mission(
			"weekly_10sessions",
			Mission.MissionType.WEEKLY,
			Mission.MetricType.SESSION_COUNT,
			10,
			"Hoàn thành 10 phiên tuần này"
		));
		
		missionPool.add(new Mission(
			"weekly_80xp",
			Mission.MissionType.WEEKLY,
			Mission.MetricType.XP_EARNED,
			80,
			"Kiếm 80 XP tuần này"
		));
		
		// Save to file
		try (FileWriter writer = new FileWriter(configFile)) {
			GSON.toJson(missionPool, writer);
			FocusTimerShop.LOGGER.info("Generated default missions config with {} missions", 
				missionPool.size());
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to save missions config", e);
		}
	}
	
	/**
	 * Refresh missions - called on server start and periodically
	 */
	public static void refreshMissions() {
		String today = new SimpleDateFormat("yyyy-MM-dd").format(new Date());
		int thisWeek = getWeekNumber();
		
		boolean needRefresh = false;
		
		// Check daily mission
		if (!today.equals(currentDailyDate) || currentDailyMission == null) {
			currentDailyDate = today;
			currentDailyMission = pickRandomMission(Mission.MissionType.DAILY);
			needRefresh = true;
			FocusTimerShop.LOGGER.info("New daily mission: {}", 
				currentDailyMission != null ? currentDailyMission.getDescription() : "none");
		}
		
		// Check weekly mission
		if (thisWeek != currentWeekNumber || currentWeeklyMission == null) {
			currentWeekNumber = thisWeek;
			currentWeeklyMission = pickRandomMission(Mission.MissionType.WEEKLY);
			needRefresh = true;
			FocusTimerShop.LOGGER.info("New weekly mission: {}", 
				currentWeeklyMission != null ? currentWeeklyMission.getDescription() : "none");
		}
	}
	
	/**
	 * Pick random mission from pool
	 */
	private static Mission pickRandomMission(Mission.MissionType type) {
		List<Mission> candidates = new ArrayList<>();
		for (Mission mission : missionPool) {
			if (mission.getType() == type) {
				candidates.add(mission);
			}
		}
		
		if (candidates.isEmpty()) {
			return null;
		}
		
		// Deterministic "random" based on date (so it's the same for all players)
		int seed = currentDailyDate.hashCode() + currentWeekNumber;
		Random random = new Random(seed);
		return candidates.get(random.nextInt(candidates.size()));
	}
	
	/**
	 * Get current week number (year*100 + week)
	 */
	private static int getWeekNumber() {
		Calendar cal = Calendar.getInstance();
		int year = cal.get(Calendar.YEAR);
		int week = cal.get(Calendar.WEEK_OF_YEAR);
		return year * 100 + week;
	}
	
	/**
	 * Check mission progress for player (call after session completion)
	 */
	public static void checkMissions(ServerPlayerEntity player) {
		refreshMissions(); // Ensure missions are current
		
		var profile = com.focustimershop.profile.ProfileManager.getProfile(player.getUuid());
		var stats = DatabaseManager.getPlayerStats(player.getUuid());
		var progress = profile.getMissionProgress();
		
		if (progress == null) {
			progress = new MissionProgress();
			profile.setMissionProgress(progress);
		}
		
		boolean updated = false;
		
		// Check daily mission
		if (currentDailyMission != null) {
			// Reset if new day
			if (!currentDailyDate.equals(progress.getDailyCompletedDate())) {
				progress.setCurrentDailyMissionId(currentDailyMission.getId());
				progress.setDailyCompleted(false);
				updated = true;
			}
			
			// Check completion
			if (!progress.isDailyCompleted() && currentDailyMission.isComplete(stats)) {
				progress.setDailyCompleted(true);
				progress.setDailyCompletedDate(currentDailyDate);
				progress.incrementTotalCompleted();
				updated = true;
				
				// Feedback
				player.sendMessage(
					net.minecraft.text.Text.literal("§a✔ Hoàn thành nhiệm vụ ngày: §f" + 
						currentDailyMission.getDescription()),
					false
				);
				player.playSound(
					net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
					net.minecraft.sound.SoundCategory.PLAYERS,
					1.0f, 1.2f
				);
			}
		}
		
		// Check weekly mission
		if (currentWeeklyMission != null) {
			// Reset if new week
			if (currentWeekNumber != progress.getWeeklyCompletedWeek()) {
				progress.setCurrentWeeklyMissionId(currentWeeklyMission.getId());
				progress.setWeeklyCompleted(false);
				updated = true;
			}
			
			// Check completion
			if (!progress.isWeeklyCompleted() && currentWeeklyMission.isComplete(stats)) {
				progress.setWeeklyCompleted(true);
				progress.setWeeklyCompletedWeek(currentWeekNumber);
				progress.incrementTotalCompleted();
				updated = true;
				
				// Feedback
				player.sendMessage(
					net.minecraft.text.Text.literal("§a✔ Hoàn thành nhiệm vụ tuần: §f" + 
						currentWeeklyMission.getDescription()),
					false
				);
				player.playSound(
					net.minecraft.sound.SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP,
					net.minecraft.sound.SoundCategory.PLAYERS,
					1.0f, 1.2f
				);
			}
		}
		
		if (updated) {
			com.focustimershop.profile.ProfileManager.saveProfile(profile);
		}
	}
	
	/**
	 * Get current daily mission
	 */
	public static Mission getCurrentDailyMission() {
		refreshMissions();
		return currentDailyMission;
	}
	
	/**
	 * Get current weekly mission
	 */
	public static Mission getCurrentWeeklyMission() {
		refreshMissions();
		return currentWeeklyMission;
	}
}
