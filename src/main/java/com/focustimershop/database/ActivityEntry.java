package com.focustimershop.database;

/**
 * Single activity log entry (v1.0.6 Phase B)
 * Used for "Recent Activity" feed
 */
public class ActivityEntry {
	
	public enum Type {
		TIMER_COMPLETE,
		CHEST_OPEN,
		SHOP_PURCHASE,
		RANK_UP
	}
	
	private String type; // Enum name as string for JSON
	private long timestampEpochSeconds;
	private String summaryText;
	
	public ActivityEntry() {}
	
	public ActivityEntry(Type type, long timestampEpochSeconds, String summaryText) {
		this.type = type.name();
		this.timestampEpochSeconds = timestampEpochSeconds;
		this.summaryText = summaryText;
	}
	
	// Getters/Setters
	public String getType() { return type; }
	public void setType(String type) { this.type = type; }
	
	public long getTimestampEpochSeconds() { return timestampEpochSeconds; }
	public void setTimestampEpochSeconds(long timestampEpochSeconds) { 
		this.timestampEpochSeconds = timestampEpochSeconds; 
	}
	
	public String getSummaryText() { return summaryText; }
	public void setSummaryText(String summaryText) { 
		this.summaryText = summaryText; 
	}
}
