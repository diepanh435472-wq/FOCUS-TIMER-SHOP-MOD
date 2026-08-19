package com.focustimershop.profile;

/**
 * Derived rank information for a player
 * Always computed from totalFocusXpEarned, never stored separately
 */
public class RankTier {
	
	private final String tier;
	private final int level;
	private final String displayName;
	private final long xpIntoLevel;      // XP earned into current level
	private final long xpNeededForLevel; // XP needed to complete current level
	private final int percent;           // Progress % for current level
	private final String frameColor;
	private final boolean animated;
	private final boolean maxRank;       // true if Legend III (capped)
	
	// v1.0.6 Phase B - Next rank info
	private final String nextRankName;   // null if maxRank
	private final long xpToNextRank;     // XP needed to reach next rank (0 if maxRank)
	
	public RankTier(String tier, int level, String displayName, 
	                long xpIntoLevel, long xpNeededForLevel, 
	                String frameColor, boolean animated, boolean maxRank,
	                String nextRankName, long xpToNextRank) {
		this.tier = tier;
		this.level = level;
		this.displayName = displayName;
		this.xpIntoLevel = xpIntoLevel;
		this.xpNeededForLevel = xpNeededForLevel;
		this.percent = xpNeededForLevel > 0 ? 
			(int)((xpIntoLevel * 100) / xpNeededForLevel) : 100;
		this.frameColor = frameColor;
		this.animated = animated;
		this.maxRank = maxRank;
		this.nextRankName = nextRankName;
		this.xpToNextRank = xpToNextRank;
	}
	
	public String getTier() { return tier; }
	public int getLevel() { return level; }
	public String getDisplayName() { return displayName; }
	public long getXpIntoLevel() { return xpIntoLevel; }
	public long getXpNeededForLevel() { return xpNeededForLevel; }
	public int getPercent() { return percent; }
	public String getFrameColor() { return frameColor; }
	public boolean isAnimated() { return animated; }
	public boolean isMaxRank() { return maxRank; }
	public String getNextRankName() { return nextRankName; }
	public long getXpToNextRank() { return xpToNextRank; }
}
