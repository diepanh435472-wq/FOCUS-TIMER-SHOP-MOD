package com.focustimershop.client;

/**
 * Client-side cache for rental data (display only)
 */
public class ClientRentalCache {
	
	private static boolean hasActiveRental = false;
	private static String rentalType = null;
	private static long rentalEndTime = 0;
	
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
