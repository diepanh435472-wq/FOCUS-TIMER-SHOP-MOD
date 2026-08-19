package com.focustimershop.database;

import java.util.UUID;

/**
 * Player statistics stored in stats/<uuid>_stats.json
 * Placeholder for future statistics tracking
 */
public class PlayerStatsData {
	
	private String version = "1.0.6"; // v1.0.6 Phase 0 - Changed to long to prevent overflow
	private String playerUuid;
	
	// Statistics (Phase 0 - all changed to long)
	private long totalTimerSessionsCompleted = 0;
	private long totalFocusTimeSeconds = 0;
	private long totalSilverEarned = 0;
	private long totalSilverConvertedToGold = 0; // v1.0.6 Phase A - renamed from totalGoldEarned
	private long totalXpEarned = 0;
	private long totalChestsOpened = 0;
	private long totalItemsPurchased = 0;
	private long totalBlocksMined = 0; // Track blocks mined with rental tools
	
	// v1.0.6 Phase 1 - Shared daily stats (last 30 days max)
	// Replaces old dailyFocusSeconds map - now tracks focus time, session count, and XP per day
	private java.util.Map<String, DailyStat> dailyStats = new java.util.HashMap<>();
	
	// v1.0.6 Phase B - Recent activity feed (last 20 entries max)
	private java.util.List<ActivityEntry> recentActivity = new java.util.ArrayList<>();
	
	public PlayerStatsData() {}
	
	public PlayerStatsData(UUID uuid) {
		this.playerUuid = uuid.toString();
	}
	
	// Getters/Setters
	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }
	
	public String getPlayerUuid() { return playerUuid; }
	public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
	
	public long getTotalTimerSessionsCompleted() { return totalTimerSessionsCompleted; }
	public void setTotalTimerSessionsCompleted(long totalTimerSessionsCompleted) { 
		this.totalTimerSessionsCompleted = Math.max(0, totalTimerSessionsCompleted); 
	}
	
	public long getTotalFocusTimeSeconds() { return totalFocusTimeSeconds; }
	public void setTotalFocusTimeSeconds(long totalFocusTimeSeconds) { 
		this.totalFocusTimeSeconds = Math.max(0, totalFocusTimeSeconds); 
	}
	
	public long getTotalSilverEarned() { return totalSilverEarned; }
	public void setTotalSilverEarned(long totalSilverEarned) { 
		this.totalSilverEarned = Math.max(0, totalSilverEarned); 
	}
	
	public long getTotalSilverConvertedToGold() { return totalSilverConvertedToGold; }
	public void setTotalSilverConvertedToGold(long totalSilverConvertedToGold) { 
		this.totalSilverConvertedToGold = Math.max(0, totalSilverConvertedToGold); 
	}
	
	public long getTotalXpEarned() { return totalXpEarned; }
	public void setTotalXpEarned(long totalXpEarned) { 
		this.totalXpEarned = Math.max(0, totalXpEarned); 
	}
	
	public long getTotalChestsOpened() { return totalChestsOpened; }
	public void setTotalChestsOpened(long totalChestsOpened) { 
		this.totalChestsOpened = Math.max(0, totalChestsOpened); 
	}
	
	public long getTotalItemsPurchased() { return totalItemsPurchased; }
	public void setTotalItemsPurchased(long totalItemsPurchased) { 
		this.totalItemsPurchased = Math.max(0, totalItemsPurchased); 
	}
	
	public long getTotalBlocksMined() { return totalBlocksMined; }
	public void setTotalBlocksMined(long totalBlocksMined) { 
		this.totalBlocksMined = Math.max(0, totalBlocksMined); 
	}
	
	public java.util.Map<String, DailyStat> getDailyStats() { 
		return dailyStats; 
	}
	public void setDailyStats(java.util.Map<String, DailyStat> dailyStats) { 
		this.dailyStats = dailyStats; 
	}
	
	public java.util.List<ActivityEntry> getRecentActivity() { 
		return recentActivity; 
	}
	public void setRecentActivity(java.util.List<ActivityEntry> recentActivity) { 
		this.recentActivity = recentActivity; 
	}
	
	/**
	 * Add blocks mined count (for 3x3 mining stats tracking)
	 */
	public void addBlocksMined(long count) {
		this.totalBlocksMined += count;
	}
	
	/**
	 * Add session data to today's daily stats (v1.0.6 Phase 1)
	 * @param elapsedSeconds Focus time in seconds
	 * @param xpAwarded XP earned from this session
	 */
	public void addDailyStat(int elapsedSeconds, int xpAwarded) {
		if (dailyStats == null) {
			dailyStats = new java.util.HashMap<>();
		}
		
		// Get today's date key (yyyy-MM-dd)
		String today = new java.text.SimpleDateFormat("yyyy-MM-dd")
			.format(new java.util.Date());
		
		// Get or create today's stat
		DailyStat stat = dailyStats.get(today);
		if (stat == null) {
			stat = new DailyStat();
			dailyStats.put(today, stat);
		}
		
		// Add this session's data
		stat.addSession(elapsedSeconds, xpAwarded);
		
		// Trim old entries (keep last 30 days only)
		trimOldDailyStats();
	}
	
	/**
	 * Trim daily stats to last 30 days (bounded storage)
	 */
	private void trimOldDailyStats() {
		if (dailyStats == null || dailyStats.size() <= 30) {
			return;
		}
		
		try {
			long cutoffEpoch = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
			java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd");
			
			dailyStats.entrySet().removeIf(entry -> {
				try {
					long entryEpoch = sdf.parse(entry.getKey()).getTime();
					return entryEpoch < cutoffEpoch;
				} catch (Exception e) {
					return false; // Keep if parse fails
				}
			});
		} catch (Exception e) {
			// Ignore trim errors
		}
	}

	
	/**
	 * Add activity entry (v1.0.6 Phase B)
	 * Keeps last 20 entries max (FIFO)
	 */
	public void addActivity(ActivityEntry.Type type, String summaryText) {
		if (recentActivity == null) {
			recentActivity = new java.util.ArrayList<>();
		}
		
		long now = System.currentTimeMillis() / 1000;
		ActivityEntry entry = new ActivityEntry(type, now, summaryText);
		
		// Add to front (newest first)
		recentActivity.add(0, entry);
		
		// Trim to last 20
		while (recentActivity.size() > 20) {
			recentActivity.remove(recentActivity.size() - 1);
		}
	}

	/**
	 * Get average XP per day over last N active days (v1.0.6 Phase 2)
	 * Used for Next Rank ETA calculation
	 * @param dayCount Number of days to look back (e.g. 7)
	 * @return Average XP/day, or 0 if no data
	 */
	public int getAverageXpPerDay(int dayCount) {
		if (dailyStats == null || dailyStats.isEmpty()) {
			return 0;
		}
		
		// Get sorted list of dates (newest first)
		java.util.List<String> sortedDates = new java.util.ArrayList<>(dailyStats.keySet());
		sortedDates.sort(java.util.Collections.reverseOrder());
		
		// Sum XP from last N days (only count days with activity)
		int totalXp = 0;
		int activeDays = 0;
		
		for (int i = 0; i < Math.min(dayCount, sortedDates.size()); i++) {
			DailyStat stat = dailyStats.get(sortedDates.get(i));
			if (stat != null && stat.getXpEarned() > 0) {
				totalXp += stat.getXpEarned();
				activeDays++;
			}
		}
		
		return activeDays > 0 ? (totalXp / activeDays) : 0;
	}
	
	/**
	 * Get most focus time in a single day (v1.0.6 Phase 4 - Personal Bests)
	 */
	public int getMostFocusSecondsInDay() {
		if (dailyStats == null || dailyStats.isEmpty()) {
			return 0;
		}
		
		int max = 0;
		for (DailyStat stat : dailyStats.values()) {
			if (stat.getFocusSeconds() > max) {
				max = stat.getFocusSeconds();
			}
		}
		return max;
	}
	
	/**
	 * Get most XP earned in a single day (v1.0.6 Phase 4 - Personal Bests)
	 */
	public int getMostXpInDay() {
		if (dailyStats == null || dailyStats.isEmpty()) {
			return 0;
		}
		
		int max = 0;
		for (DailyStat stat : dailyStats.values()) {
			if (stat.getXpEarned() > max) {
				max = stat.getXpEarned();
			}
		}
		return max;
	}
	
	/**
	 * Get most sessions in a single day (v1.0.6 Phase 4 - Personal Bests)
	 */
	public int getMostSessionsInDay() {
		if (dailyStats == null || dailyStats.isEmpty()) {
			return 0;
		}
		
		int max = 0;
		for (DailyStat stat : dailyStats.values()) {
			if (stat.getSessionCount() > max) {
				max = stat.getSessionCount();
			}
		}
		return max;
	}
	
	/**
	 * Get total focus time for last N days (v1.0.6 Phase 4 - Weekly)
	 */
	public int getTotalFocusSecondsLastNDays(int dayCount) {
		if (dailyStats == null || dailyStats.isEmpty()) {
			return 0;
		}
		
		// Get sorted list of dates (newest first)
		java.util.List<String> sortedDates = new java.util.ArrayList<>(dailyStats.keySet());
		sortedDates.sort(java.util.Collections.reverseOrder());
		
		int total = 0;
		for (int i = 0; i < Math.min(dayCount, sortedDates.size()); i++) {
			DailyStat stat = dailyStats.get(sortedDates.get(i));
			if (stat != null) {
				total += stat.getFocusSeconds();
			}
		}
		return total;
	}

	/**
	 * Get focus history for last 7 days (v1.0.6 Phase 3)
	 * Returns list of [dateKey, focusSeconds] ordered oldest to newest
	 */
	public java.util.List<java.util.Map.Entry<String, Integer>> getLast7DaysFocusHistory() {
		if (dailyStats == null || dailyStats.isEmpty()) {
			return new java.util.ArrayList<>();
		}
		
		// Get sorted dates (newest first)
		java.util.List<String> sortedDates = new java.util.ArrayList<>(dailyStats.keySet());
		sortedDates.sort(java.util.Collections.reverseOrder());
		
		// Take last 7 days
		java.util.List<String> last7Days = new java.util.ArrayList<>();
		for (int i = 0; i < Math.min(7, sortedDates.size()); i++) {
			last7Days.add(sortedDates.get(i));
		}
		
		// Reverse to get oldest-to-newest for chart display
		java.util.Collections.reverse(last7Days);
		
		// Build result list
		java.util.List<java.util.Map.Entry<String, Integer>> result = new java.util.ArrayList<>();
		for (String date : last7Days) {
			DailyStat stat = dailyStats.get(date);
			int seconds = (stat != null) ? stat.getFocusSeconds() : 0;
			result.add(new java.util.AbstractMap.SimpleEntry<>(date, seconds));
		}
		
		return result;
	}
}
