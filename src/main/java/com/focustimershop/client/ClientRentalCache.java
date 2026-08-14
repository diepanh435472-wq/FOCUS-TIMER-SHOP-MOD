package com.focustimershop.client;

import com.focustimershop.database.RentalData;

/**
 * Client-side cache for rental data (display only)
 * v1.0.5: Supports multiple rentals
 */
public class ClientRentalCache {
	
	// NEW v1.0.5: Store full rental data
	private static RentalData rentalData = null;
	
	// LEGACY: Single rental fields (kept for backward compatibility)
	private static boolean hasActiveRental = false;
	private static String rentalType = null;
	private static long rentalEndTime = 0;
	
	/**
	 * Get full rental data (v1.0.5+)
	 */
	public static RentalData getRentalData() {
		return rentalData;
	}
	
	/**
	 * Update full rental data (v1.0.5+)
	 */
	public static void setRentalData(RentalData data) {
		rentalData = data;
		
		// Update legacy fields for backward compatibility
		if (data != null && data.hasAnyActiveRental()) {
			hasActiveRental = true;
			// Get first active rental for legacy support
			var activeRentals = data.getActiveRentals();
			if (!activeRentals.isEmpty()) {
				RentalData.SingleRental first = activeRentals.get(0);
				rentalType = first.getRentalType();
				rentalEndTime = first.getRentalEndTime();
			}
		} else {
			hasActiveRental = false;
			rentalType = null;
			rentalEndTime = 0;
		}
	}
	
	public static boolean hasActiveRental() {
		return hasActiveRental && System.currentTimeMillis() < rentalEndTime;
	}
	
	public static String getRentalType() {
		return rentalType;
	}
	
	public static long getRentalEndTime() {
		return rentalEndTime;
	}
	
	public static int getRemainingSeconds() {
		if (!hasActiveRental()) {
			return 0;
		}
		long remaining = rentalEndTime - System.currentTimeMillis();
		return (int) Math.max(0, remaining / 1000);
	}
	
	public static String getFormattedTime() {
		int seconds = getRemainingSeconds();
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		
		if (hours > 0) {
			return String.format("%dh %dm %ds", hours, minutes, secs);
		} else if (minutes > 0) {
			return String.format("%dm %ds", minutes, secs);
		} else {
			return String.format("%ds", secs);
		}
	}
	
	public static void updateRental(String type, long endTime) {
		hasActiveRental = true;
		rentalType = type;
		rentalEndTime = endTime;
	}
	
	public static void clearRental() {
		hasActiveRental = false;
		rentalType = null;
		rentalEndTime = 0;
	}
}
