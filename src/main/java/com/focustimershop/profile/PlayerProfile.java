package com.focustimershop.profile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Player profile data
 * Persisted at FCTMS/profiles/<uuid>.json
 */
public class PlayerProfile {
	
	private String version = "1.0.6";
	private String playerUuid;
	private String inGameName;          // Synced from vanilla on login
	private String customName;          // Player-set display name, null = show inGameName
	
	// Rank progression (derived from PlayerStatsData.totalXpEarned via resolveRank())
	// DO NOT store totalFocusXpEarned here - read from PlayerStatsData instead
	
	// Profile-specific stats (NOT duplicated in PlayerStatsData)
	private int currentStreakDays;
	private int longestStreakDays;
	private long lastFocusSessionEpochSeconds; // For streak calc + "last focus: today, 07:42"
	private int longestSingleSessionSeconds;
	
	// Meta
	private String favoriteTimerType;   // "POMODORO", "STOPWATCH", "COUNTDOWN"
	private long profileCreatedAtEpochSeconds;
	
	// v1.0.6 Phase 4 - Weekly goal (minutes)
	private int weeklyGoalMinutes = 0; // 0 = no goal set
	private long lastLoginEpochSeconds = 0; // For login summary (Phase 4.4)
	
	// v1.0.6 Phase 6 - Mission progress
	private com.focustimershop.mission.MissionProgress missionProgress = new com.focustimershop.mission.MissionProgress();
	
	// Optional features (§5 upgrade ideas)
	private List<String> unlockedTitles;
	private String equippedTitle;       // null = no title
	
	// v1.0.6 Phase 1 - Track unlock timestamps
	private java.util.Map<String, Long> unlockedAchievements = new java.util.HashMap<>(); // achievementId -> epochSeconds
	private java.util.Map<String, Long> unlockedTitlesMap = new java.util.HashMap<>(); // titleId -> epochSeconds (Phase 2)
	
	public PlayerProfile() {
		this.unlockedTitles = new ArrayList<>();
	}
	
	public PlayerProfile(UUID uuid, String inGameName) {
		this.playerUuid = uuid.toString();
		this.inGameName = inGameName;
		this.profileCreatedAtEpochSeconds = System.currentTimeMillis() / 1000;
		this.lastFocusSessionEpochSeconds = 0; // No sessions yet
		this.unlockedTitles = new ArrayList<>();
	}
	
	// Getters
	public String getVersion() { return version; }
	public String getPlayerUuid() { return playerUuid; }
	public String getInGameName() { return inGameName; }
	public String getCustomName() { return customName; }
	public int getCurrentStreakDays() { return currentStreakDays; }
	public int getLongestStreakDays() { return longestStreakDays; }
	public long getLastFocusSessionEpochSeconds() { return lastFocusSessionEpochSeconds; }
	public int getLongestSingleSessionSeconds() { return longestSingleSessionSeconds; }
	public String getFavoriteTimerType() { return favoriteTimerType; }
	public long getProfileCreatedAtEpochSeconds() { return profileCreatedAtEpochSeconds; }
	public List<String> getUnlockedTitles() { return unlockedTitles; }
	public String getEquippedTitle() { return equippedTitle; }
	public int getWeeklyGoalMinutes() { return weeklyGoalMinutes; }
	public long getLastLoginEpochSeconds() { return lastLoginEpochSeconds; }
	public com.focustimershop.mission.MissionProgress getMissionProgress() { return missionProgress; }
	
	// Setters
	public void setVersion(String version) { this.version = version; }
	public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
	public void setInGameName(String inGameName) { this.inGameName = inGameName; }
	public void setCustomName(String customName) { this.customName = customName; }
	public void setCurrentStreakDays(int currentStreakDays) { this.currentStreakDays = currentStreakDays; }
	public void setLongestStreakDays(int longestStreakDays) { this.longestStreakDays = longestStreakDays; }
	public void setLastFocusSessionEpochSeconds(long lastFocusSessionEpochSeconds) { 
		this.lastFocusSessionEpochSeconds = lastFocusSessionEpochSeconds; 
	}
	public void setLongestSingleSessionSeconds(int longestSingleSessionSeconds) { 
		this.longestSingleSessionSeconds = longestSingleSessionSeconds; 
	}
	public void setFavoriteTimerType(String favoriteTimerType) { 
		this.favoriteTimerType = favoriteTimerType; 
	}
	public void setProfileCreatedAtEpochSeconds(long profileCreatedAtEpochSeconds) { 
		this.profileCreatedAtEpochSeconds = profileCreatedAtEpochSeconds; 
	}
	public void setUnlockedTitles(List<String> unlockedTitles) { 
		this.unlockedTitles = unlockedTitles; 
	}
	public void setEquippedTitle(String equippedTitle) { 
		this.equippedTitle = equippedTitle; 
	}
	public void setEquippedTitleId(String equippedTitleId) { 
		this.equippedTitle = equippedTitleId; 
	}
	public String getEquippedTitleId() { 
		return equippedTitle; 
	}
	
	// v1.0.6 Phase 1 - Achievement unlock tracking
	public java.util.Map<String, Long> getUnlockedAchievements() { 
		return unlockedAchievements != null ? unlockedAchievements : new java.util.HashMap<>(); 
	}
	public void setUnlockedAchievements(java.util.Map<String, Long> map) {
		this.unlockedAchievements = map;
	}
	public void unlockAchievement(String achievementId) {
		if (unlockedAchievements == null) {
			unlockedAchievements = new java.util.HashMap<>();
		}
		if (!unlockedAchievements.containsKey(achievementId)) {
			unlockedAchievements.put(achievementId, System.currentTimeMillis() / 1000);
		}
	}
	public boolean hasAchievement(String achievementId) {
		return unlockedAchievements != null && unlockedAchievements.containsKey(achievementId);
	}
	public Long getAchievementUnlockTime(String achievementId) {
		return unlockedAchievements != null ? unlockedAchievements.get(achievementId) : null;
	}
	
	// v1.0.6 Phase 2 - Title unlock tracking
	public java.util.Map<String, Long> getUnlockedTitlesMap() {
		return unlockedTitlesMap != null ? unlockedTitlesMap : new java.util.HashMap<>();
	}
	public void setUnlockedTitlesMap(java.util.Map<String, Long> map) {
		this.unlockedTitlesMap = map;
	}
	public void unlockTitle(String titleId) {
		if (unlockedTitlesMap == null) {
			unlockedTitlesMap = new java.util.HashMap<>();
		}
		if (!unlockedTitlesMap.containsKey(titleId)) {
			unlockedTitlesMap.put(titleId, System.currentTimeMillis() / 1000);
		}
	}
	public boolean hasTitle(String titleId) {
		return unlockedTitlesMap != null && unlockedTitlesMap.containsKey(titleId);
	}
	public Long getTitleUnlockTime(String titleId) {
		return unlockedTitlesMap != null ? unlockedTitlesMap.get(titleId) : null;
	}
	public void setWeeklyGoalMinutes(int weeklyGoalMinutes) {
		this.weeklyGoalMinutes = weeklyGoalMinutes;
	}
	public void setLastLoginEpochSeconds(long lastLoginEpochSeconds) {
		this.lastLoginEpochSeconds = lastLoginEpochSeconds;
	}
	public void setMissionProgress(com.focustimershop.mission.MissionProgress progress) {
		this.missionProgress = progress;
	}
	
	/**
	 * Get display name (custom if set, otherwise in-game name)
	 */
	public String getDisplayName() {
		return (customName != null && !customName.isEmpty()) ? customName : inGameName;
	}
	
	/**
	 * Record a completed session (Phase A - simplified)
	 */
	public void recordSession(int sessionSeconds, String timerType) {
		// Update longest single session
		if (sessionSeconds > longestSingleSessionSeconds) {
			this.longestSingleSessionSeconds = sessionSeconds;
		}
		
		// Update favorite timer type (simple: most recent)
		// TODO: Track counts per type for true "favorite"
		this.favoriteTimerType = timerType;
		
		// Update last session time for streak calc
		this.lastFocusSessionEpochSeconds = System.currentTimeMillis() / 1000;
		
		// Update streak
		updateStreak(this.lastFocusSessionEpochSeconds);
	}
	
	/**
	 * Update streak on session completion (Phase A - simplified)
	 */
	public void updateStreak(long currentEpochSeconds) {
		long lastSessionDays = lastFocusSessionEpochSeconds / 86400;
		long currentDays = currentEpochSeconds / 86400;
		long daysSinceLastSession = currentDays - lastSessionDays;
		
		if (lastFocusSessionEpochSeconds == 0 || daysSinceLastSession == 0) {
			// First session or same day - start/continue streak
			if (currentStreakDays == 0) {
				currentStreakDays = 1;
			}
		} else if (daysSinceLastSession == 1) {
			// Next day - increment streak
			currentStreakDays++;
			if (currentStreakDays > longestStreakDays) {
				longestStreakDays = currentStreakDays;
			}
		} else {
			// Missed days - reset streak to 1 (current session counts)
			currentStreakDays = 1;
		}
	}
}
