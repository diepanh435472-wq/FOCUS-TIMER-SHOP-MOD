package com.focustimershop.profile;

/**
 * Rank configuration entry - one per level
 * 58 total ranks across 12 tiers
 */
public class RankConfig {
	
	private String tier;           // e.g. "Đồng", "Legend"
	private int level;             // 1-5 (I-V), except Legend has only 1-3
	private String symbol;         // Icon reference: e.g. "bronze", "legend_flame"
	private int requiredXP;        // XP needed for THIS level only
	private long cumulativeXP;     // Total XP needed to reach this level
	private String frameColor;     // Hex color for UI borders
	private boolean animated;      // Only true for Legend tiers
	
	public RankConfig() {}
	
	public RankConfig(String tier, int level, String symbol, int requiredXP, 
	                  long cumulativeXP, String frameColor, boolean animated) {
		this.tier = tier;
		this.level = level;
		this.symbol = symbol;
		this.requiredXP = requiredXP;
		this.cumulativeXP = cumulativeXP;
		this.frameColor = frameColor;
		this.animated = animated;
	}
	
	// Getters
	public String getTier() { return tier; }
	public int getLevel() { return level; }
	public String getSymbol() { return symbol; }
	public int getRequiredXP() { return requiredXP; }
	public long getCumulativeXP() { return cumulativeXP; }
	public String getFrameColor() { return frameColor; }
	public boolean isAnimated() { return animated; }
	
	// Setters for GSON
	public void setTier(String tier) { this.tier = tier; }
	public void setLevel(int level) { this.level = level; }
	public void setSymbol(String symbol) { this.symbol = symbol; }
	public void setRequiredXP(int requiredXP) { this.requiredXP = requiredXP; }
	public void setCumulativeXP(long cumulativeXP) { this.cumulativeXP = cumulativeXP; }
	public void setFrameColor(String frameColor) { this.frameColor = frameColor; }
	public void setAnimated(boolean animated) { this.animated = animated; }
	
	/**
	 * Get full display name with Roman numeral
	 * E.g. "Đồng III 🥉"
	 */
	public String getDisplayName() {
		return tier + " " + toRoman(level) + " " + getSymbolDisplay();
	}
	
	/**
	 * Get symbol display (placeholder until textures ready)
	 */
	private String getSymbolDisplay() {
		// Map icon IDs to emoji placeholders
		switch (symbol) {
			case "unranked": return "–";
			case "bronze": return "🥉";
			case "iron": return "🔩";
			case "steel": return "⚔";
			case "silver": return "🥈";
			case "gold": return "🥇";
			case "platinum": return "💠";
			case "titanium": return "🔷";
			case "diamond": return "💎";
			case "elite": return "⭐";
			case "master": return "👑";
			case "legend_flame": return "🔥";
			default: return "?";
		}
	}
	
	/**
	 * Convert level to Roman numeral
	 */
	private String toRoman(int num) {
		switch (num) {
			case 1: return "I";
			case 2: return "II";
			case 3: return "III";
			case 4: return "IV";
			case 5: return "V";
			default: return String.valueOf(num);
		}
	}
}
