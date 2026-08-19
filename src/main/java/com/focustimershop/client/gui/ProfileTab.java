package com.focustimershop.client.gui;

/**
 * Profile tab enum (v1.0.6)
 * 4 tabs: Overview, Stats, Achievements, Titles
 */
public enum ProfileTab {
	OVERVIEW("Tổng quát"),
	STATS("Thống kê"),
	ACHIEVEMENTS("Thành tựu"),
	TITLES("Danh hiệu");
	
	private final String displayName;
	
	ProfileTab(String displayName) {
		this.displayName = displayName;
	}
	
	public String getDisplayName() {
		return displayName;
	}
}
