package com.focustimershop.database;

import java.util.UUID;

/**
 * Rental data stored in rentals/<uuid>.json
 * Tracks active tool rentals with real-time expiration
 */
public class RentalData {
	
	private String version = "1.0.2";
	private String playerUuid;
	
	// Active rental info
	private String rentalType = null;  // "UNIVERSAL_PICKAXE", etc. null if no rental
	private long rentalStartTime = 0;  // System.currentTimeMillis()
	private long rentalEndTime = 0;    // System.currentTimeMillis()
	private int rentalDurationSeconds = 0;
	
	// Rental tier/stats
	private int fortuneLevel = 0;
	private int unbreakingLevel = 0;
	private int efficiencyLevel = 0;
	private boolean hasMending = false;
	
	public RentalData() {}
	
	public RentalData(UUID uuid) {
		this.playerUuid = uuid.toString();
	}
	
	// Getters/Setters
	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }
	
	public String getPlayerUuid() { return playerUuid; }
	public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
	
	public String getRentalType() { return rentalType; }
	public void setRentalType(String rentalType) { this.rentalType = rentalType; }
	
	public long getRentalStartTime() { return rentalStartTime; }
	public void setRentalStartTime(long rentalStartTime) { this.rentalStartTime = rentalStartTime; }
	
	public long getRentalEndTime() { return rentalEndTime; }
	public void setRentalEndTime(long rentalEndTime) { this.rentalEndTime = rentalEndTime; }
	
	public int getRentalDurationSeconds() { return rentalDurationSeconds; }
	public void setRentalDurationSeconds(int rentalDurationSeconds) { this.rentalDurationSeconds = rentalDurationSeconds; }
	
	public int getFortuneLevel() { return fortuneLevel; }
	public void setFortuneLevel(int fortuneLevel) { this.fortuneLevel = fortuneLevel; }
	
	public int getUnbreakingLevel() { return unbreakingLevel; }
	public void setUnbreakingLevel(int unbreakingLevel) { this.unbreakingLevel = unbreakingLevel; }
	
	public int getEfficiencyLevel() { return efficiencyLevel; }
	public void setEfficiencyLevel(int efficiencyLevel) { this.efficiencyLevel = efficiencyLevel; }
	
	public boolean hasMending() { return hasMending; }
	public void setMending(boolean hasMending) { this.hasMending = hasMending; }
	
	// Helper methods
	public boolean hasActiveRental() {
		return rentalType != null && System.currentTimeMillis() < rentalEndTime;
	}
	
	public long getRemainingTimeMillis() {
		if (!hasActiveRental()) {
			return 0;
		}
		return Math.max(0, rentalEndTime - System.currentTimeMillis());
	}
	
	public int getRemainingTimeSeconds() {
		return (int) (getRemainingTimeMillis() / 1000);
	}
	
	public void clearRental() {
		this.rentalType = null;
		this.rentalStartTime = 0;
		this.rentalEndTime = 0;
		this.rentalDurationSeconds = 0;
		this.fortuneLevel = 0;
		this.unbreakingLevel = 0;
		this.efficiencyLevel = 0;
		this.hasMending = false;
	}
}
