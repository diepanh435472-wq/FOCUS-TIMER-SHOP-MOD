package com.focustimershop.achievement;

import com.google.gson.annotations.SerializedName;

/**
 * Achievement definition loaded from achievements.json (v1.0.6)
 * 220 achievements with rarity tiers and condition types
 */
public class AchievementDefinition {
	
	private String id;
	
	@SerializedName(value = "name", alternate = {"displayName"})
	private String name;
	
	private String description;
	private String category;
	private String rarity; // Phổ biến, Không phổ biến, Hiếm, Cực hiếm, Huyền thoại
	
	@SerializedName(value = "conditionType", alternate = {"condition_type"})
	private String conditionType;
	
	@SerializedName(value = "conditionValue", alternate = {"condition_value"})
	private Object conditionValue; // Can be int, String, or array
	
	@SerializedName(value = "icon", alternate = {"iconAssetId"})
	private String icon; // Icon asset ID
	
	public AchievementDefinition() {}
	
	// Getters/Setters
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	
	public String getCategory() { return category; }
	public void setCategory(String category) { this.category = category; }
	
	public String getRarity() { return rarity; }
	public void setRarity(String rarity) { this.rarity = rarity; }
	
	public String getConditionType() { return conditionType; }
	public void setConditionType(String conditionType) { this.conditionType = conditionType; }
	
	public Object getConditionValue() { return conditionValue; }
	public void setConditionValue(Object conditionValue) { this.conditionValue = conditionValue; }
	
	public String getIcon() { return icon; }
	public void setIcon(String icon) { this.icon = icon; }
	
	/**
	 * Get rarity color
	 */
	public int getRarityColor() {
		switch (rarity) {
			case "Phổ biến": return 0xFFCCCCCC; // Gray
			case "Không phổ biến": return 0xFF00FF00; // Green
			case "Hiếm": return 0xFF4A9EFF; // Blue
			case "Cực hiếm": return 0xFFAA00FF; // Purple
			case "Huyền thoại": return 0xFFFFD700; // Gold
			default: return 0xFFFFFFFF; // White
		}
	}
	
	/**
	 * v1.0.7-beta - Check if achievement condition is met
	 * Supports TIMER_TYPE_USES for legacy timer type achievements
	 */
	public boolean checkCondition(com.focustimershop.database.PlayerStatsData stats) {
		if (conditionType == null) {
			return false;
		}
		
		switch (conditionType) {
			case "TIMER_TYPE_USES":
				// conditionValue is [typeName, count] array
				if (conditionValue instanceof java.util.List) {
					java.util.List<?> list = (java.util.List<?>) conditionValue;
					if (list.size() >= 2) {
						String typeName = list.get(0).toString();
						int requiredCount = ((Number) list.get(1)).intValue();
						int actualCount = stats.getTimerTypeUse(typeName);
						return actualCount >= requiredCount;
					}
				}
				return false;
				
			case "TOTAL_SESSIONS":
				if (conditionValue instanceof Number) {
					long required = ((Number) conditionValue).longValue();
					return stats.getTotalTimerSessionsCompleted() >= required;
				}
				return false;
				
			case "TOTAL_FOCUS_HOURS":
				if (conditionValue instanceof Number) {
					long required = ((Number) conditionValue).longValue();
					long totalHours = stats.getTotalFocusTimeSeconds() / 3600;
					return totalHours >= required;
				}
				return false;
				
			default:
				return false;
		}
	}
}
