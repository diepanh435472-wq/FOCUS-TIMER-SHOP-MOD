package com.focustimershop.season;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.focustimershop.database.PlayerStatsData;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.server.MinecraftServer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

/**
 * Season Manager - Handles monthly rank decay system
 * v1.0.6-beta Season System
 * 
 * Each calendar month is a Season (SS1, SS2, SS3...)
 * At month start: seasonRankXp reduced by 95% (keep 5%)
 * totalXpEarned is NEVER touched (lifetime, drives achievements/titles)
 */
public class SeasonManager {
	
	private static final String SEASON_STATE_FILE = "season_state.txt";
	private static Path seasonStateFile;
	private static int globalSeasonNumber = 1; // Current global season
	private static String lastProcessedMonth = ""; // Format: "yyyy-MM"
	private static boolean initialized = false;
	
	/**
	 * Initialize season system
	 * Called once at server startup
	 */
	public static void initialize() {
		if (initialized) return;
		
		// Season state file location: FCTMS/season_state.txt
		seasonStateFile = DatabaseManager.getFCTMSRoot().resolve(SEASON_STATE_FILE);
		
		// Load season state
		loadSeasonState();
		
		// Register server start event to check for month rollover
		ServerLifecycleEvents.SERVER_STARTED.register(SeasonManager::onServerStart);
		
		FocusTimerShop.LOGGER.info("SeasonManager initialized - Current season: SS{}, Last processed: {}", 
			globalSeasonNumber, lastProcessedMonth);
		
		initialized = true;
	}
	
	/**
	 * Called when server starts - check if month has changed
	 */
	private static void onServerStart(MinecraftServer server) {
		checkAndProcessMonthRollover();
	}
	
	/**
	 * Check if month has rolled over and process decay if needed
	 * Handles case where server was offline during month boundary
	 */
	public static void checkAndProcessMonthRollover() {
		String currentMonth = getCurrentMonthKey();
		
		// If lastProcessedMonth is empty (first time), set it to current month
		if (lastProcessedMonth.isEmpty()) {
			lastProcessedMonth = currentMonth;
			saveSeasonState();
			FocusTimerShop.LOGGER.info("SEASON: First run - set lastProcessedMonth to {}", currentMonth);
			return;
		}
		
		// If month hasn't changed, nothing to do
		if (lastProcessedMonth.equals(currentMonth)) {
			FocusTimerShop.LOGGER.debug("SEASON: Month unchanged ({}), no decay needed", currentMonth);
			return;
		}
		
		// Month has changed! Process season decay
		FocusTimerShop.LOGGER.info("SEASON: Month changed from {} to {} - Processing season decay!", 
			lastProcessedMonth, currentMonth);
		
		processSeasonDecay();
		
		// Update state
		globalSeasonNumber++;
		lastProcessedMonth = currentMonth;
		saveSeasonState();
		
		FocusTimerShop.LOGGER.info("SEASON: Decay complete - Advanced to season SS{}", globalSeasonNumber);
	}
	
	/**
	 * Process season decay for all players
	 * Reduces seasonRankXp by 95% (keeps 5%)
	 * NEVER touches totalXpEarned
	 */
	private static void processSeasonDecay() {
		File statsDir = DatabaseManager.getStatsDir().toFile();
		
		if (!statsDir.exists() || !statsDir.isDirectory()) {
			FocusTimerShop.LOGGER.warn("SEASON: Stats directory not found, skipping decay");
			return;
		}
		
		File[] statFiles = statsDir.listFiles((dir, name) -> name.endsWith("_stats.json"));
		if (statFiles == null || statFiles.length == 0) {
			FocusTimerShop.LOGGER.info("SEASON: No player stats found, skipping decay");
			return;
		}
		
		int playersProcessed = 0;
		int playersDecayed = 0;
		
		for (File statFile : statFiles) {
			try {
				// Extract UUID from filename
				String filename = statFile.getName();
				String uuidStr = filename.replace("_stats.json", "");
				UUID playerId = UUID.fromString(uuidStr);
				
				// Load stats
				PlayerStatsData stats = DatabaseManager.getPlayerStats(playerId);
				if (stats == null) {
					continue;
				}
				
				playersProcessed++;
				
				// Store pre-decay values for summary
				long oldSeasonXp = stats.getSeasonRankXp();
				long oldTotalXp = stats.getTotalXpEarned(); // Should NEVER change
				
				// Apply 95% decay (keep 5%)
				long newSeasonXp = (long) Math.floor(oldSeasonXp * 0.05);
				stats.setSeasonRankXp(newSeasonXp);
				
				// Update season number
				stats.setCurrentSeasonNumber(globalSeasonNumber);
				
				// Create end-of-season summary snapshot
				SeasonSummary summary = new SeasonSummary(
					globalSeasonNumber - 1, // Previous season number
					oldSeasonXp,
					newSeasonXp,
					System.currentTimeMillis() / 1000
				);
				saveSeasonSummary(playerId, summary);
				
				// Save stats
				DatabaseManager.savePlayerStats(stats);
				
				// Verify totalXpEarned unchanged
				if (stats.getTotalXpEarned() != oldTotalXp) {
					FocusTimerShop.LOGGER.error("CRITICAL BUG: totalXpEarned was modified during decay! " +
						"Player: {}, Old: {}, New: {}", 
						playerId, oldTotalXp, stats.getTotalXpEarned());
				}
				
				if (oldSeasonXp > 0) {
					playersDecayed++;
					FocusTimerShop.LOGGER.info("SEASON: Decayed player {} - {} → {} XP (kept 5%)", 
						playerId, oldSeasonXp, newSeasonXp);
				}
				
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("SEASON: Failed to process decay for {}", statFile.getName(), e);
			}
		}
		
		FocusTimerShop.LOGGER.info("SEASON: Processed {} players, decayed {} players with XP", 
			playersProcessed, playersDecayed);
	}
	
	/**
	 * Get current month key (yyyy-MM format)
	 */
	private static String getCurrentMonthKey() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
		return sdf.format(new Date());
	}
	
	/**
	 * Load season state from file
	 */
	private static void loadSeasonState() {
		if (!seasonStateFile.toFile().exists()) {
			// First time - initialize with current month
			globalSeasonNumber = 1;
			lastProcessedMonth = ""; // Will be set on first check
			FocusTimerShop.LOGGER.info("SEASON: No state file found, starting fresh at SS1");
			return;
		}
		
		try {
			java.util.List<String> lines = Files.readAllLines(seasonStateFile);
			if (lines.size() >= 2) {
				globalSeasonNumber = Integer.parseInt(lines.get(0).trim());
				lastProcessedMonth = lines.get(1).trim();
				FocusTimerShop.LOGGER.info("SEASON: Loaded state - SS{}, last processed: {}", 
					globalSeasonNumber, lastProcessedMonth);
			}
		} catch (IOException | NumberFormatException e) {
			FocusTimerShop.LOGGER.error("SEASON: Failed to load season state, resetting", e);
			globalSeasonNumber = 1;
			lastProcessedMonth = "";
		}
	}
	
	/**
	 * Save season state to file
	 */
	private static void saveSeasonState() {
		try {
			String content = globalSeasonNumber + "\n" + lastProcessedMonth;
			Files.writeString(seasonStateFile, content);
			FocusTimerShop.LOGGER.debug("SEASON: Saved state - SS{}, {}", globalSeasonNumber, lastProcessedMonth);
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("SEASON: Failed to save season state", e);
		}
	}
	
	/**
	 * Save end-of-season summary for player
	 */
	private static void saveSeasonSummary(UUID playerId, SeasonSummary summary) {
		Path summaryFile = DatabaseManager.getFCTMSRoot()
			.resolve("season_summaries")
			.resolve(playerId.toString() + "_last_season.json");
		
		try {
			Files.createDirectories(summaryFile.getParent());
			
			com.google.gson.Gson gson = new com.google.gson.GsonBuilder().setPrettyPrinting().create();
			String json = gson.toJson(summary);
			Files.writeString(summaryFile, json);
			
			FocusTimerShop.LOGGER.debug("SEASON: Saved summary for player {}", playerId);
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("SEASON: Failed to save summary for player {}", playerId, e);
		}
	}
	
	/**
	 * Load end-of-season summary for player (returns null if none exists)
	 */
	public static SeasonSummary loadSeasonSummary(UUID playerId) {
		Path summaryFile = DatabaseManager.getFCTMSRoot()
			.resolve("season_summaries")
			.resolve(playerId.toString() + "_last_season.json");
		
		if (!summaryFile.toFile().exists()) {
			return null;
		}
		
		try {
			String json = Files.readString(summaryFile);
			com.google.gson.Gson gson = new com.google.gson.Gson();
			return gson.fromJson(json, SeasonSummary.class);
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("SEASON: Failed to load summary for player {}", playerId, e);
			return null;
		}
	}
	
	/**
	 * Delete season summary after it's been shown to player
	 */
	public static void deleteSeasonSummary(UUID playerId) {
		Path summaryFile = DatabaseManager.getFCTMSRoot()
			.resolve("season_summaries")
			.resolve(playerId.toString() + "_last_season.json");
		
		try {
			Files.deleteIfExists(summaryFile);
			FocusTimerShop.LOGGER.debug("SEASON: Deleted summary for player {}", playerId);
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("SEASON: Failed to delete summary for player {}", playerId, e);
		}
	}
	
	/**
	 * Get current global season number
	 */
	public static int getCurrentSeasonNumber() {
		return globalSeasonNumber;
	}
	
	/**
	 * Manual decay trigger for testing (admin command)
	 */
	public static void forceSeasonDecay() {
		FocusTimerShop.LOGGER.warn("SEASON: FORCED DECAY TRIGGERED (admin command)");
		processSeasonDecay();
		globalSeasonNumber++;
		lastProcessedMonth = getCurrentMonthKey();
		saveSeasonState();
		FocusTimerShop.LOGGER.info("SEASON: Forced decay complete - Advanced to season SS{}", globalSeasonNumber);
	}
}
