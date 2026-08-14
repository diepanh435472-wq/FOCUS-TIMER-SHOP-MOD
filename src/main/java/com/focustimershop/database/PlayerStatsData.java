package com.focustimershop.database;

import java.util.UUID;

/**
 * Player statistics stored in stats/<uuid>_stats.json
 * Placeholder for future statistics tracking
 */
public class PlayerStatsData {
	
	private String version = "1.0.2";
	private String playerUuid;
	
	// Statistics (placeholder for future features)
	private int totalTimerSessionsCompleted = 0;
	private int totalFocusTimeSeconds = 0;
	private int totalSilverEarned = 0;
	private int totalGoldEarned = 0;
	private int totalXpEarned = 0;
	private int totalChestsOpened = 0;
	private int totalItemsPurchased = 0;
	private int totalBlocksMined = 0; // Track blocks mined with rental tools
	
	public PlayerStatsData() {}
	
	public PlayerStatsData(UUID uuid) {
		this.playerUuid = uuid.toString();
	}
	
	// Getters/Setters
	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }
	
	public String getPlayerUuid() { return playerUuid; }
	public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
	
	public int getTotalTimerSessionsCompleted() { return totalTimerSessionsCompleted; }
	public void setTotalTimerSessionsCompleted(int totalTimerSessionsCompleted) { 
		this.totalTimerSessionsCompleted = totalTimerSessionsCompleted; 
	}
	
	public int getTotalFocusTimeSeconds() { return totalFocusTimeSeconds; }
	public void setTotalFocusTimeSeconds(int totalFocusTimeSeconds) { 
		this.totalFocusTimeSeconds = totalFocusTimeSeconds; 
	}
	
	public int getTotalSilverEarned() { return totalSilverEarned; }
	public void setTotalSilverEarned(int totalSilverEarned) { 
		this.totalSilverEarned = totalSilverEarned; 
	}
	
	public int getTotalGoldEarned() { return totalGoldEarned; }
	public void setTotalGoldEarned(int totalGoldEarned) { 
		this.totalGoldEarned = totalGoldEarned; 
	}
	
	public int getTotalXpEarned() { return totalXpEarned; }
	public void setTotalXpEarned(int totalXpEarned) { 
		this.totalXpEarned = totalXpEarned; 
	}
	
	public int getTotalChestsOpened() { return totalChestsOpened; }
	public void setTotalChestsOpened(int totalChestsOpened) { 
		this.totalChestsOpened = totalChestsOpened; 
	}
	
	public int getTotalItemsPurchased() { return totalItemsPurchased; }
	public void setTotalItemsPurchased(int totalItemsPurchased) { 
		this.totalItemsPurchased = totalItemsPurchased; 
	}
	
	public int getTotalBlocksMined() { return totalBlocksMined; }
	public void setTotalBlocksMined(int totalBlocksMined) { 
		this.totalBlocksMined = totalBlocksMined; 
	}
	
	/**
	 * Add blocks mined count (for 3x3 mining stats tracking)
	 */
	public void addBlocksMined(int count) {
		this.totalBlocksMined += count;
	}
}
