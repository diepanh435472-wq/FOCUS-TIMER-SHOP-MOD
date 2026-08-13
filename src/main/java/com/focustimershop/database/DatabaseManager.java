package com.focustimershop.database;

import com.focustimershop.FocusTimerShop;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Central database manager for FCTMS (Focus Timer Shop Management System)
 * Handles all file I/O with atomic writes and automatic directory creation
 */
public class DatabaseManager {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static Path FCTMS_ROOT;
	
	// Directory paths
	private static Path CONFIG_DIR;
	private static Path PRICES_DIR;
	private static Path LOOTCHESTS_DIR;
	private static Path PLAYERS_DIR;
	private static Path STATS_DIR;
	private static Path RENTALS_DIR;
	
	/**
	 * Initialize FCTMS database structure
	 * Called once at mod initialization
	 */
	public static void initialize() {
		// Base directory: .minecraft/FCTMS/
		FCTMS_ROOT = FabricLoader.getInstance().getGameDir().resolve("FCTMS");
		
		// Subdirectories
		CONFIG_DIR = FCTMS_ROOT.resolve("config");
		PRICES_DIR = FCTMS_ROOT.resolve("prices");
		LOOTCHESTS_DIR = FCTMS_ROOT.resolve("lootchests");
		PLAYERS_DIR = FCTMS_ROOT.resolve("players");
		STATS_DIR = FCTMS_ROOT.resolve("stats");
		RENTALS_DIR = FCTMS_ROOT.resolve("rentals");
		
		// Create all directories
		try {
			Files.createDirectories(CONFIG_DIR);
			Files.createDirectories(PRICES_DIR);
			Files.createDirectories(LOOTCHESTS_DIR);
			Files.createDirectories(PLAYERS_DIR);
			Files.createDirectories(STATS_DIR);
			Files.createDirectories(RENTALS_DIR);
			
			FocusTimerShop.LOGGER.info("FCTMS database initialized at: {}", FCTMS_ROOT);
			
			// Generate default files if they don't exist
			generateDefaultFiles();
			
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to create FCTMS directories", e);
		}
	}
	
	/**
	 * Generate default configuration and data files if they don't exist
	 */
	private static void generateDefaultFiles() {
		// Economy config
		File economyConfig = CONFIG_DIR.resolve("economy_config.json").toFile();
		if (!economyConfig.exists()) {
			EconomyConfig defaultConfig = EconomyConfig.createDefault();
			writeJson(economyConfig, defaultConfig);
			FocusTimerShop.LOGGER.info("Generated default economy_config.json");
		}
		
		// Building blocks prices - FORCE REGENERATE if version mismatch
		File buildingPrices = PRICES_DIR.resolve("building_blocks.json").toFile();
		boolean needRegenerateBuilding = !buildingPrices.exists();
		
		if (buildingPrices.exists()) {
			// Check version to force update
			PriceList existing = readJson(buildingPrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 200) {
				FocusTimerShop.LOGGER.warn("building_blocks.json outdated (version={}, entries={}), regenerating...", 
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateBuilding = true;
			}
		}
		
		if (needRegenerateBuilding) {
			PriceList defaultPrices = PriceList.createBuildingBlocksDefaults();
			writeJson(buildingPrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated building_blocks.json with {} entries", defaultPrices.getPrices().size());
		}
		
		// Colored blocks prices - same check
		File coloredPrices = PRICES_DIR.resolve("colored_blocks.json").toFile();
		boolean needRegenerateColored = !coloredPrices.exists();
		
		if (coloredPrices.exists()) {
			PriceList existing = readJson(coloredPrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 190) {
				FocusTimerShop.LOGGER.warn("colored_blocks.json outdated (version={}, entries={}), regenerating...",
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateColored = true;
			}
		}
		
		if (needRegenerateColored) {
			PriceList defaultPrices = PriceList.createColoredBlocksDefaults();
			writeJson(coloredPrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated colored_blocks.json with {} entries", defaultPrices.getPrices().size());
		}
		
		// Chest definitions
		File chestDefs = LOOTCHESTS_DIR.resolve("chest_definitions.json").toFile();
		if (!chestDefs.exists()) {
			ChestDefinitions defaultDefs = ChestDefinitions.createDefault();
			writeJson(chestDefs, defaultDefs);
			FocusTimerShop.LOGGER.info("Generated default chest_definitions.json");
		}
	}
	
	/**
	 * Write JSON with atomic file operation
	 * Writes to .tmp file first, then renames to prevent corruption on crash
	 */
	public static <T> void writeJson(File file, T data) {
		File tmpFile = new File(file.getAbsolutePath() + ".tmp");
		
		try (FileWriter writer = new FileWriter(tmpFile)) {
			GSON.toJson(data, writer);
			writer.flush();
			
			// Atomic rename
			Files.move(tmpFile.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING);
			
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to write JSON file: {}", file.getName(), e);
			// Clean up tmp file on failure
			tmpFile.delete();
		}
	}
	
	/**
	 * Read JSON file
	 */
	public static <T> T readJson(File file, Class<T> clazz) {
		if (!file.exists()) {
			return null;
		}
		
		try (FileReader reader = new FileReader(file)) {
			return GSON.fromJson(reader, clazz);
		} catch (IOException e) {
			FocusTimerShop.LOGGER.error("Failed to read JSON file: {}", file.getName(), e);
			return null;
		}
	}
	
	// Getters for directory paths
	public static Path getConfigDir() { return CONFIG_DIR; }
	public static Path getPricesDir() { return PRICES_DIR; }
	public static Path getLootChestsDir() { return LOOTCHESTS_DIR; }
	public static Path getPlayersDir() { return PLAYERS_DIR; }
	public static Path getStatsDir() { return STATS_DIR; }
	public static Path getRentalsDir() { return RENTALS_DIR; }
	public static Path getRoot() { return FCTMS_ROOT; }
}
