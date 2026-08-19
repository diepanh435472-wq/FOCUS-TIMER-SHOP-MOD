package com.focustimershop.title;

/**
 * Title definition loaded from titles.json (v1.0.6)
 * 67 curated titles, cosmetic-only
 */
public class TitleDefinition {
	
	private String id;
	private String name;
	private String displayPrefix; // Shown next to player name
	private String description;
	private String unlockedByAchievementId; // null = has custom condition
	private String customCondition; // For 3 titles without achievement link
	
	public TitleDefinition() {}
	
	// Getters/Setters
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public String getName() { return name; }
	public void setName(String name) { this.name = name; }
	
	public String getDisplayPrefix() { return displayPrefix; }
	public void setDisplayPrefix(String displayPrefix) { this.displayPrefix = displayPrefix; }
	
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	
	public String getUnlockedByAchievementId() { return unlockedByAchievementId; }
	public void setUnlockedByAchievementId(String id) { this.unlockedByAchievementId = id; }
	
	public String getCustomCondition() { return customCondition; }
	public void setCustomCondition(String condition) { this.customCondition = condition; }
	
	/**
	 * Check if unlocked by achievement
	 */
	public boolean hasAchievementUnlock() {
		return unlockedByAchievementId != null && !unlockedByAchievementId.isEmpty();
	}
}
