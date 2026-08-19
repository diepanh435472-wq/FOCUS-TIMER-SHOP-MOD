package com.focustimershop.database;

/**
 * Daily statistics for a single calendar day
 * Used for focus history, personal bests, weekly goals, etc.
 * v1.0.6 Phase 1
 */
public class DailyStat {
	
	private int focusSeconds;
	private int sessionCount;
	private int xpEarned;
	
	public DailyStat() {
		this.focusSeconds = 0;
		this.sessionCount = 0;
		this.xpEarned = 0;
	}
	
	public DailyStat(int focusSeconds, int sessionCount, int xpEarned) {
		this.focusSeconds = focusSeconds;
		this.sessionCount = sessionCount;
		this.xpEarned = xpEarned;
	}
	
	// Getters
	public int getFocusSeconds() {
		return focusSeconds;
	}
	
	public int getSessionCount() {
		return sessionCount;
	}
	
	public int getXpEarned() {
		return xpEarned;
	}
	
	// Setters
	public void setFocusSeconds(int focusSeconds) {
		this.focusSeconds = focusSeconds;
	}
	
	public void setSessionCount(int sessionCount) {
		this.sessionCount = sessionCount;
	}
	
	public void setXpEarned(int xpEarned) {
		this.xpEarned = xpEarned;
	}
	
	/**
	 * Add session data to this day's totals
	 */
	public void addSession(int elapsedSeconds, int xpAwarded) {
		this.focusSeconds += elapsedSeconds;
		this.sessionCount += 1;
		this.xpEarned += xpAwarded;
	}
	
	@Override
	public String toString() {
		return String.format("DailyStat{focus=%ds, sessions=%d, xp=%d}", 
			focusSeconds, sessionCount, xpEarned);
	}
}
