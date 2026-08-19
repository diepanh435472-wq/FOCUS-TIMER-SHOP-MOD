package com.focustimershop.mission;

/**
 * Player's mission completion state (v1.0.6 Phase 6)
 * Stored in PlayerProfile
 */
public class MissionProgress {
	
	private String currentDailyMissionId;
	private boolean dailyCompleted;
	private String dailyCompletedDate; // yyyy-MM-dd
	
	private String currentWeeklyMissionId;
	private boolean weeklyCompleted;
	private int weeklyCompletedWeek; // Week number (year*100 + week)
	
	private int totalMissionsCompleted; // Running personal count
	
	public MissionProgress() {
		this.currentDailyMissionId = "";
		this.dailyCompleted = false;
		this.dailyCompletedDate = "";
		this.currentWeeklyMissionId = "";
		this.weeklyCompleted = false;
		this.weeklyCompletedWeek = 0;
		this.totalMissionsCompleted = 0;
	}
	
	// Getters/Setters
	public String getCurrentDailyMissionId() { return currentDailyMissionId; }
	public void setCurrentDailyMissionId(String id) { this.currentDailyMissionId = id; }
	
	public boolean isDailyCompleted() { return dailyCompleted; }
	public void setDailyCompleted(boolean completed) { this.dailyCompleted = completed; }
	
	public String getDailyCompletedDate() { return dailyCompletedDate; }
	public void setDailyCompletedDate(String date) { this.dailyCompletedDate = date; }
	
	public String getCurrentWeeklyMissionId() { return currentWeeklyMissionId; }
	public void setCurrentWeeklyMissionId(String id) { this.currentWeeklyMissionId = id; }
	
	public boolean isWeeklyCompleted() { return weeklyCompleted; }
	public void setWeeklyCompleted(boolean completed) { this.weeklyCompleted = completed; }
	
	public int getWeeklyCompletedWeek() { return weeklyCompletedWeek; }
	public void setWeeklyCompletedWeek(int week) { this.weeklyCompletedWeek = week; }
	
	public int getTotalMissionsCompleted() { return totalMissionsCompleted; }
	public void setTotalMissionsCompleted(int count) { this.totalMissionsCompleted = count; }
	
	public void incrementTotalCompleted() {
		this.totalMissionsCompleted++;
	}
}
