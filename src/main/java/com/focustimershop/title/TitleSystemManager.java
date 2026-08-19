package com.focustimershop.title;

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
 * Title system manager - loads titles from JSON (v1.0.6 Phase 5)
 */
public class TitleSystemManager {
	
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static List<TitleDefinition> allTitles = new ArrayList<>();
	private static Map<String, TitleDefinition> titlesById = new HashMap<>();
	private static boolean initialized = false;
	
	/**
	 * Initialize title system - load from titles.json
	 */
	public static void initialize() {
		if (initialized) {
			return;
		}
		
		File configFile = DatabaseManager.getConfigDir().resolve("titles.json").toFile();
		
		if (!configFile.exists()) {
			FocusTimerShop.LOGGER.warn("titles.json not found at {}", configFile.getAbsolutePath());
			initialized = true;
			return;
		}
		
		try (FileReader reader = new FileReader(configFile)) {
			allTitles = GSON.fromJson(reader, new TypeToken<List<TitleDefinition>>(){}.getType());
			
			// Build ID map
			titlesById.clear();
			for (TitleDefinition def : allTitles) {
				titlesById.put(def.getId(), def);
			}
			
			FocusTimerShop.LOGGER.info("Loaded {} titles from JSON", allTitles.size());
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to load titles.json", e);
		}
		
		initialized = true;
	}
	
	/**
	 * Get all titles
	 */
	public static List<TitleDefinition> getAllTitles() {
		if (!initialized) {
			initialize();
		}
		return new ArrayList<>(allTitles);
	}
	
	/**
	 * Get title by ID
	 */
	public static TitleDefinition getTitleById(String id) {
		if (!initialized) {
			initialize();
		}
		return titlesById.get(id);
	}
	
	/**
	 * Get titles that unlock by achievement
	 */
	public static List<TitleDefinition> getTitlesByAchievementId(String achievementId) {
		if (!initialized) {
			initialize();
		}
		
		List<TitleDefinition> result = new ArrayList<>();
		for (TitleDefinition def : allTitles) {
			if (achievementId.equals(def.getUnlockedByAchievementId())) {
				result.add(def);
			}
		}
		return result;
	}
}
