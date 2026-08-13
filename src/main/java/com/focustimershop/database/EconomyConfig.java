package com.focustimershop.database;

/**
 * Economy configuration - conversion rates, rewards, etc.
 */
public class EconomyConfig {
	
	private String version = "1.0.2";
	
	// Timer reward rates
	private int silverPerSecond = 1;  // 1 silver per 45 seconds = earn 1 every 45s
	private int secondsPerSilver = 45;
	private int xpPerSecond = 1;      // 1 XP per 90 seconds
	private int secondsPerXp = 90;
	
	// Conversion rates
	private int silverToGoldRate = 100;  // 100 silver = 1 gold
	
	// Keybinds (for reference, actual binding in client code)
	private String openMenuKey = "RIGHT_SHIFT";
	
	public static EconomyConfig createDefault() {
		return new EconomyConfig();
	}
	
	// Getters
	public String getVersion() { return version; }
	public int getSecondsPerSilver() { return secondsPerSilver; }
	public int getSecondsPerXp() { return secondsPerXp; }
	public int getSilverToGoldRate() { return silverToGoldRate; }
	public String getOpenMenuKey() { return openMenuKey; }
	
	// Setters
	public void setVersion(String version) { this.version = version; }
	public void setSecondsPerSilver(int secondsPerSilver) { this.secondsPerSilver = secondsPerSilver; }
	public void setSecondsPerXp(int secondsPerXp) { this.secondsPerXp = secondsPerXp; }
	public void setSilverToGoldRate(int silverToGoldRate) { this.silverToGoldRate = silverToGoldRate; }
	public void setOpenMenuKey(String openMenuKey) { this.openMenuKey = openMenuKey; }
}
