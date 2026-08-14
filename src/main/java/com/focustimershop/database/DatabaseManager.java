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
		
		// Natural blocks prices - NEW CATEGORY v1.0.4
		File naturalPrices = PRICES_DIR.resolve("natural_blocks.json").toFile();
		boolean needRegenerateNatural = !naturalPrices.exists();
		
		if (naturalPrices.exists()) {
			PriceList existing = readJson(naturalPrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 150) {
				FocusTimerShop.LOGGER.warn("natural_blocks.json outdated (version={}, entries={}), regenerating...",
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateNatural = true;
			}
		}
		
		if (needRegenerateNatural) {
			PriceList defaultPrices = PriceList.createNaturalBlocksDefaults();
			writeJson(naturalPrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated natural_blocks.json with {} entries", defaultPrices.getPrices().size());
		}
		
		// Functional blocks prices - NEW CATEGORY v1.0.4
		File functionalPrices = PRICES_DIR.resolve("functional_blocks.json").toFile();
		boolean needRegenerateFunctional = !functionalPrices.exists();
		
		if (functionalPrices.exists()) {
			PriceList existing = readJson(functionalPrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 80) {
				FocusTimerShop.LOGGER.warn("functional_blocks.json outdated (version={}, entries={}), regenerating...",
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateFunctional = true;
			}
		}
		
		if (needRegenerateFunctional) {
			PriceList defaultPrices = PriceList.createFunctionalBlocksDefaults();
			writeJson(functionalPrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated functional_blocks.json with {} entries", defaultPrices.getPrices().size());
		}
		
		// Redstone blocks prices - NEW CATEGORY v1.0.4
		File redstonePrices = PRICES_DIR.resolve("redstone_blocks.json").toFile();
		boolean needRegenerateRedstone = !redstonePrices.exists();
		
		if (redstonePrices.exists()) {
			PriceList existing = readJson(redstonePrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 50) {
				FocusTimerShop.LOGGER.warn("redstone_blocks.json outdated (version={}, entries={}), regenerating...",
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateRedstone = true;
			}
		}
		
		if (needRegenerateRedstone) {
			PriceList defaultPrices = PriceList.createRedstoneBlocksDefaults();
			writeJson(redstonePrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated redstone_blocks.json with {} entries", defaultPrices.getPrices().size());
		}
		
		// Tools & utilities prices - NEW CATEGORY v1.0.4
		File toolsPrices = PRICES_DIR.resolve("tools_utilities.json").toFile();
		boolean needRegenerateTools = !toolsPrices.exists();
		
		if (toolsPrices.exists()) {
			PriceList existing = readJson(toolsPrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 100) {
				FocusTimerShop.LOGGER.warn("tools_utilities.json outdated (version={}, entries={}), regenerating...",
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateTools = true;
			}
		}
		
		if (needRegenerateTools) {
			PriceList defaultPrices = PriceList.createToolsUtilitiesDefaults();
			writeJson(toolsPrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated tools_utilities.json with {} entries", defaultPrices.getPrices().size());
		}
		
		// Food & drinks prices - NEW CATEGORY v1.0.4
		File foodPrices = PRICES_DIR.resolve("food_drinks.json").toFile();
		boolean needRegenerateFood = !foodPrices.exists();
		
		if (foodPrices.exists()) {
			PriceList existing = readJson(foodPrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 30) {
				FocusTimerShop.LOGGER.warn("food_drinks.json outdated (version={}, entries={}), regenerating...",
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateFood = true;
			}
		}
		
		if (needRegenerateFood) {
			PriceList defaultPrices = PriceList.createFoodDrinksDefaults();
			writeJson(foodPrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated food_drinks.json with {} entries", defaultPrices.getPrices().size());
		}
		
		// Ingredients prices - NEW CATEGORY v1.0.4
		File ingredientsPrices = PRICES_DIR.resolve("ingredients.json").toFile();
		boolean needRegenerateIngredients = !ingredientsPrices.exists();
		
		if (ingredientsPrices.exists()) {
			PriceList existing = readJson(ingredientsPrices, PriceList.class);
			if (existing == null || !existing.getVersion().equals("1.0.2") || existing.getPrices().size() < 80) {
				FocusTimerShop.LOGGER.warn("ingredients.json outdated (version={}, entries={}), regenerating...",
					existing != null ? existing.getVersion() : "null",
					existing != null ? existing.getPrices().size() : 0);
				needRegenerateIngredients = true;
			}
		}
		
		if (needRegenerateIngredients) {
			PriceList defaultPrices = PriceList.createIngredientsDefaults();
			writeJson(ingredientsPrices, defaultPrices);
			FocusTimerShop.LOGGER.info("Generated ingredients.json with {} entries", defaultPrices.getPrices().size());
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
