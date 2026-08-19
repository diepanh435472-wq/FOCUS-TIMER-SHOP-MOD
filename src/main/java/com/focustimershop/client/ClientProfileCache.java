package com.focustimershop.client;

/**
 * Client-side cache for player profile data (v1.0.6)
 * Synced from server via PROFILE_SYNC packet
 */
public class ClientProfileCache {
	
	private static String inGameName = "";
	private static String customName = "";
	private static long totalFocusXpEarned = 0;
	private static int currentStreakDays = 0;
	private static int longestStreakDays = 0;
	private static int longestSingleSessionSeconds = 0;
	private static long totalSessionsCompleted = 0; // Phase 0 - long
	private static long totalFocusTimeSeconds = 0;
	private static String favoriteTimerType = "POMODORO";
	private static long profileCreatedAtEpochSeconds = 0;
	private static long lastFocusDate = 0;
	
	// Achievement & Title data (v1.0.6 Phase 5)
	private static java.util.List<String> unlockedAchievementIds = new java.util.ArrayList<>();
	private static java.util.List<String> unlockedTitleIds = new java.util.ArrayList<>();
	private static String equippedTitleId = null;
	
	// v1.0.6 Phase 1 & 2 - Track unlock timestamps
	private static java.util.Map<String, Long> unlockedAchievementsMap = new java.util.HashMap<>();
	private static java.util.Map<String, Long> unlockedTitlesMap = new java.util.HashMap<>();
	
	/**
	 * Update profile from server sync (Phase 0 - long)
	 */
	public static void updateProfile(String inGameName, String customName, long totalXp,
	                                  int currentStreak, int longestStreak, int longestSession,
	                                  long totalSessions, long totalFocusTime, String favoriteTimer,
	                                  long profileCreated) {
		ClientProfileCache.inGameName = inGameName;
		ClientProfileCache.customName = customName;
		ClientProfileCache.totalFocusXpEarned = totalXp;
		ClientProfileCache.currentStreakDays = currentStreak;
		ClientProfileCache.longestStreakDays = longestStreak;
		ClientProfileCache.longestSingleSessionSeconds = longestSession;
		ClientProfileCache.totalSessionsCompleted = totalSessions;
		ClientProfileCache.totalFocusTimeSeconds = totalFocusTime;
		ClientProfileCache.favoriteTimerType = favoriteTimer;
		ClientProfileCache.profileCreatedAtEpochSeconds = profileCreated;
	}
	
	// Getters
	public static String getInGameName() { return inGameName; }
	public static String getCustomName() { return customName; }
	public static long getTotalFocusXpEarned() { return totalFocusXpEarned; }
	public static int getCurrentStreakDays() { return currentStreakDays; }
	public static int getLongestStreakDays() { return longestStreakDays; }
	public static int getLongestSingleSessionSeconds() { return longestSingleSessionSeconds; }
	public static long getTotalSessionsCompleted() { return totalSessionsCompleted; }
	public static long getTotalFocusTimeSeconds() { return totalFocusTimeSeconds; }
	public static String getFavoriteTimerType() { return favoriteTimerType; }
	public static long getProfileCreatedAtEpochSeconds() { return profileCreatedAtEpochSeconds; }
	public static long getLastFocusDate() { return lastFocusDate; }
	
	// Achievement & Title getters (v1.0.6 Phase 5)
	public static java.util.List<String> getUnlockedAchievementIds() { 
		return new java.util.ArrayList<>(unlockedAchievementIds); 
	}
	public static java.util.List<String> getUnlockedTitleIds() { 
		return new java.util.ArrayList<>(unlockedTitleIds); 
	}
	public static String getEquippedTitleId() { return equippedTitleId; }
	
	// Setters for achievement & title data
	public static void setUnlockedAchievementIds(java.util.List<String> ids) {
		unlockedAchievementIds = new java.util.ArrayList<>(ids);
	}
	public static void setUnlockedTitleIds(java.util.List<String> ids) {
		unlockedTitleIds = new java.util.ArrayList<>(ids);
	}
	public static void setEquippedTitleId(String id) {
		equippedTitleId = id;
	}
	public static void setLastFocusDate(long date) {
		lastFocusDate = date;
	}
	
	// v1.0.6 Phase 1 & 2 - Map-based unlock tracking
	public static java.util.Map<String, Long> getUnlockedAchievementsMap() {
		return new java.util.HashMap<>(unlockedAchievementsMap);
	}
	public static void setUnlockedAchievementsMap(java.util.Map<String, Long> map) {
		unlockedAchievementsMap = new java.util.HashMap<>(map);
	}
	public static java.util.Map<String, Long> getUnlockedTitlesMap() {
		return new java.util.HashMap<>(unlockedTitlesMap);
	}
	public static void setUnlockedTitlesMap(java.util.Map<String, Long> map) {
		unlockedTitlesMap = new java.util.HashMap<>(map);
	}
	
	/**
	 * Get display name (custom if set, otherwise in-game)
	 */
	public static String getDisplayName() {
		return (customName != null && !customName.isEmpty()) ? customName : inGameName;
	}
}
