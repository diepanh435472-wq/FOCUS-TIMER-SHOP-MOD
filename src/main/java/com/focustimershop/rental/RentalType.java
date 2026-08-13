package com.focustimershop.rental;

/**
 * Types of rentable tools
 */
public enum RentalType {
	UNIVERSAL_PICKAXE("Cúp Vạn Năng", "3×3 mining pickaxe with Fortune");
	
	private final String displayName;
	private final String description;
	
	RentalType(String displayName, String description) {
		this.displayName = displayName;
		this.description = description;
	}
	
	public String getDisplayName() {
		return displayName;
	}
	
	public String getDescription() {
		return description;
	}
}
