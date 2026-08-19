package com.focustimershop.mission;

/**
 * Daily or Weekly mission definition (v1.0.6 Phase 6)
 * Server-wide, config-driven
 */
public class Mission {
	
	private String id;
	private MissionType type; // DAILY or WEEKLY
	private MetricType metricType;
	private int targetValue;
	private String description;
	
	public Mission() {}
	
	public Mission(String id, MissionType type, MetricType metricType, 
	               int targetValue, String description) {
		this.id = id;
		this.type = type;
		this.metricType = metricType;
		this.targetValue = targetValue;
		this.description = description;
	}
	
	// Getters/Setters
	public String getId() { return id; }
	public void setId(String id) { this.id = id; }
	
	public MissionType getType() { return type; }
	public void setType(MissionType type) { this.type = type; }
	
	public MetricType getMetricType() { return metricType; }
	public void setMetricType(MetricType metricType) { this.metricType = metricType; }
	
	public int getTargetValue() { return targetValue; }
	public void setTargetValue(int targetValue) { this.targetValue = targetValue; }
	
	public String getDescription() { return description; }
	public void setDescription(String description) { this.description = description; }
	
	public enum MissionType {
		DAILY,
		WEEKLY
	}
	
	public enum MetricType {
		FOCUS_MINUTES,
		SESSION_COUNT,
		XP_EARNED
	}
	
	/**
	 * Check current progress for a player
	 */
	public int getProgress(com.focustimershop.database.PlayerStatsData stats) {
		switch (metricType) {
			case FOCUS_MINUTES:
				if (type == MissionType.DAILY) {
					// Today's focus time
					var dailyStats = stats.getDailyStats();
					if (dailyStats == null) return 0;
					String today = new java.text.SimpleDateFormat("yyyy-MM-dd")
						.format(new java.util.Date());
					var stat = dailyStats.get(today);
					return (stat != null) ? (stat.getFocusSeconds() / 60) : 0;
				} else {
					// Last 7 days
					return stats.getTotalFocusSecondsLastNDays(7) / 60;
				}
				
			case SESSION_COUNT:
				if (type == MissionType.DAILY) {
					// Today's session count
					var dailyStats = stats.getDailyStats();
					if (dailyStats == null) return 0;
					String today = new java.text.SimpleDateFormat("yyyy-MM-dd")
						.format(new java.util.Date());
					var stat = dailyStats.get(today);
					return (stat != null) ? stat.getSessionCount() : 0;
				} else {
					// Last 7 days - sum session counts
					var dailyStats = stats.getDailyStats();
					if (dailyStats == null) return 0;
					var sortedDates = new java.util.ArrayList<>(dailyStats.keySet());
					sortedDates.sort(java.util.Collections.reverseOrder());
					int count = 0;
					for (int i = 0; i < Math.min(7, sortedDates.size()); i++) {
						var stat = dailyStats.get(sortedDates.get(i));
						if (stat != null) {
							count += stat.getSessionCount();
						}
					}
					return count;
				}
				
			case XP_EARNED:
				if (type == MissionType.DAILY) {
					// Today's XP
					var dailyStats = stats.getDailyStats();
					if (dailyStats == null) return 0;
					String today = new java.text.SimpleDateFormat("yyyy-MM-dd")
						.format(new java.util.Date());
					var stat = dailyStats.get(today);
					return (stat != null) ? stat.getXpEarned() : 0;
				} else {
					// Last 7 days
					var dailyStats = stats.getDailyStats();
					if (dailyStats == null) return 0;
					var sortedDates = new java.util.ArrayList<>(dailyStats.keySet());
					sortedDates.sort(java.util.Collections.reverseOrder());
					int xp = 0;
					for (int i = 0; i < Math.min(7, sortedDates.size()); i++) {
						var stat = dailyStats.get(sortedDates.get(i));
						if (stat != null) {
							xp += stat.getXpEarned();
						}
					}
					return xp;
				}
				
			default:
				return 0;
		}
	}
	
	/**
	 * Check if mission is complete
	 */
	public boolean isComplete(com.focustimershop.database.PlayerStatsData stats) {
		return getProgress(stats) >= targetValue;
	}
}
