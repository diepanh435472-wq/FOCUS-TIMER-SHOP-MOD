package com.focustimershop.database;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Rental data stored in rentals/<uuid>.json
 * Tracks active tool rentals with real-time expiration
 * v1.0.5: Supports multiple simultaneous rentals
 */
public class RentalData {
	
	private String version = "1.0.5";
	private String playerUuid;
	
	// LEGACY: Single rental (kept for backward compatibility)
	private String rentalType = null;  // "UNIVERSAL_PICKAXE", etc. null if no rental
	private long rentalStartTime = 0;  // System.currentTimeMillis()
	private long rentalEndTime = 0;    // System.currentTimeMillis()
	private int rentalDurationSeconds = 0;
	private int fortuneLevel = 0;
	private int unbreakingLevel = 0;
	private int efficiencyLevel = 0;
	private boolean hasMending = false;
	
	// NEW v1.0.5: Multiple rentals support
	private List<SingleRental> rentals = new ArrayList<>();
	
	/**
	 * Single rental entry (v1.0.6+ with pausable timer)
	 */
	public static class SingleRental {
		private String rentalType;        // "PICKAXE", "AXE", "SHOVEL"
		private long rentalStartTime;     // When rental was created (for tracking)
		private int remainingSeconds;     // Remaining time (pauses when timer running or offline)
		private int totalDurationSeconds; // Original duration
		private int fortuneLevel;
		private int unbreakingLevel;
		private int efficiencyLevel;
		private int mendingLevel;
		private boolean useSilkTouch;
		
		public SingleRental() {}
		
		public SingleRental(String type, long start, int duration,
		                    int fortune, int efficiency, int unbreaking, int mending, boolean silkTouch) {
			this.rentalType = type;
			this.rentalStartTime = start;
			this.remainingSeconds = duration;
			this.totalDurationSeconds = duration;
			this.fortuneLevel = fortune;
			this.efficiencyLevel = efficiency;
			this.unbreakingLevel = unbreaking;
			this.mendingLevel = mending;
			this.useSilkTouch = silkTouch;
		}
		
		public boolean isActive() {
			return remainingSeconds > 0;
		}
		
		/**
		 * Tick the rental timer down by 1 second
		 * Only call when player is online and game is not frozen
		 */
		public void tickDown() {
			if (remainingSeconds > 0) {
				remainingSeconds--;
			}
		}
		
		// Getters/Setters
		public String getRentalType() { return rentalType; }
		public void setRentalType(String rentalType) { this.rentalType = rentalType; }
		
		public long getRentalStartTime() { return rentalStartTime; }
		public void setRentalStartTime(long rentalStartTime) { this.rentalStartTime = rentalStartTime; }
		
		public int getRemainingSeconds() { return remainingSeconds; }
		public void setRemainingSeconds(int remainingSeconds) { this.remainingSeconds = remainingSeconds; }
		
		public int getTotalDurationSeconds() { return totalDurationSeconds; }
		public void setTotalDurationSeconds(int totalDurationSeconds) { this.totalDurationSeconds = totalDurationSeconds; }
		
		public int getFortuneLevel() { return fortuneLevel; }
		public void setFortuneLevel(int fortuneLevel) { this.fortuneLevel = fortuneLevel; }
		
		public int getUnbreakingLevel() { return unbreakingLevel; }
		public void setUnbreakingLevel(int unbreakingLevel) { this.unbreakingLevel = unbreakingLevel; }
		
		public int getEfficiencyLevel() { return efficiencyLevel; }
		public void setEfficiencyLevel(int efficiencyLevel) { this.efficiencyLevel = efficiencyLevel; }
		
		public int getMendingLevel() { return mendingLevel; }
		public void setMendingLevel(int mendingLevel) { this.mendingLevel = mendingLevel; }
		
		public boolean useSilkTouch() { return useSilkTouch; }
		public void setUseSilkTouch(boolean useSilkTouch) { this.useSilkTouch = useSilkTouch; }
		
		// Backward compatibility methods
		public long getRemainingTimeMillis() {
			return remainingSeconds * 1000L;
		}
		
		public long getRentalEndTime() {
			return rentalStartTime + (remainingSeconds * 1000L);
		}
	}
	
	public RentalData() {}
	
	public RentalData(UUID uuid) {
		this.playerUuid = uuid.toString();
	}
	
	// NEW: Add a rental (v1.0.6+ with remainingSeconds)
	public void addRental(String type, long start, int duration,
	                      int fortune, int efficiency, int unbreaking, int mending, boolean silkTouch) {
		// Remove any existing rental of same type
		rentals.removeIf(r -> r.getRentalType().equals(type));
		
		// Add new rental
		SingleRental rental = new SingleRental(type, start, duration, fortune, efficiency, unbreaking, mending, silkTouch);
		rentals.add(rental);
	}
	
	// NEW: Get rental by type
	public SingleRental getRentalByType(String type) {
		return rentals.stream()
			.filter(r -> r.getRentalType().equals(type) && r.isActive())
			.findFirst()
			.orElse(null);
	}
	
	// NEW: Get all active rentals
	public List<SingleRental> getActiveRentals() {
		List<SingleRental> active = new ArrayList<>();
		for (SingleRental rental : rentals) {
			if (rental.isActive()) {
				active.add(rental);
			}
		}
		return active;
	}
	
	// NEW: Check if has any active rental
	public boolean hasAnyActiveRental() {
		return rentals.stream().anyMatch(SingleRental::isActive);
	}
	
	// NEW: Remove expired rentals
	public void cleanupExpired() {
		rentals.removeIf(r -> !r.isActive());
	}
	
	// Getters/Setters (keep for backward compatibility)
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
		// Check both legacy and new format
		boolean legacyActive = (rentalType != null && System.currentTimeMillis() < rentalEndTime);
		boolean newActive = hasAnyActiveRental();
		return legacyActive || newActive;
	}
	
	public long getRemainingTimeMillis() {
		if (!hasActiveRental()) {
			return 0;
		}
		
		// Check legacy first
		if (rentalType != null) {
			return Math.max(0, rentalEndTime - System.currentTimeMillis());
		}
		
		// Find longest remaining time in new format
		long maxRemaining = 0;
		for (SingleRental rental : rentals) {
			if (rental.isActive()) {
				maxRemaining = Math.max(maxRemaining, rental.getRemainingTimeMillis());
			}
		}
		return maxRemaining;
	}
	
	public int getRemainingTimeSeconds() {
		return (int) (getRemainingTimeMillis() / 1000);
	}
	
	public void clearRental() {
		// Clear legacy
		this.rentalType = null;
		this.rentalStartTime = 0;
		this.rentalEndTime = 0;
		this.rentalDurationSeconds = 0;
		this.fortuneLevel = 0;
		this.unbreakingLevel = 0;
		this.efficiencyLevel = 0;
		this.hasMending = false;
		
		// Clear new format
		this.rentals.clear();
	}
	
	// NEW: Get rentals list (for serialization)
	public List<SingleRental> getRentals() {
		return rentals;
	}
	
	public void setRentals(List<SingleRental> rentals) {
		this.rentals = rentals != null ? rentals : new ArrayList<>();
	}
}
