package com.focustimershop.achievement;

/**
 * Achievement definition (v1.0.6 Phase 5)
 * Config-driven, cosmetic-only
 */
public class Achievement {
	
	private String id;
	private String displayName;
	private String iconAssetId;
	private ConditionType conditionType;
	private long conditionValue;
	
	public Achievement() {}
	
	public Achievement(String id, String displayName, String iconAssetId, 
	                   ConditionType conditionType, long conditionValue) {
		this.id = id;
		this.displayName = displayName;
		this.iconAssetId = iconAssetId;
		this.conditionType = conditionType;
		this.conditionValue = conditionValue;
	}
	
	// Getters/Setters
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public String getDisplayName() { return displayName; }
	public void setDisplayName(String displayName) { this.displayName = displayName; }
	
	public String getIconAssetId() { return iconAssetId; }
	public void setIconAssetId(String iconAssetId) { this.iconAssetId = iconAssetId; }
	
	public ConditionType getConditionType() { return conditionType; }
	public void setConditionType(ConditionType conditionType) { this.conditionType = conditionType; }
	
	public long getConditionValue() { return conditionValue; }
	public void setConditionValue(long conditionValue) { this.conditionValue = conditionValue; }
	
	/**
	 * Condition types for achievements
	 */
	public enum ConditionType {
		TOTAL_SESSIONS,      // Total sessions completed
		TOTAL_FOCUS_HOURS,   // Total focus hours (convert from seconds)
		STREAK_DAYS,         // Current streak days
		RANK_TIER_REACHED    // Reached specific rank tier (by name)
	}
	
	/**
	 * Check if achievement condition is met
	 */
	public boolean checkCondition(com.focustimershop.database.PlayerStatsData stats, 
	                               com.focustimershop.profile.PlayerProfile profile,
	                               com.focustimershop.profile.RankTier rank) {
		switch (conditionType) {
			case TOTAL_SESSIONS:
				return stats.getTotalTimerSessionsCompleted() >= conditionValue;
				
			case TOTAL_FOCUS_HOURS:
				long totalHours = stats.getTotalFocusTimeSeconds() / 3600;
				return totalHours >= conditionValue;
				
			case STREAK_DAYS:
				return profile.getCurrentStreakDays() >= conditionValue;
				
			case RANK_TIER_REACHED:
				// For rank achievements, conditionValue is rank index
				// (Not implementing rank-based achievements in this phase - placeholder)
				return false;
				
			default:
				return false;
		}
	}
}
