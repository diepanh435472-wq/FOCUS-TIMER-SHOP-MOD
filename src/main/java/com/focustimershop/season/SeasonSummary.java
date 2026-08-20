package com.focustimershop.season;

/**
 * End-of-season summary snapshot
 * Stored per player to show on next login after season rollover
 */
public class SeasonSummary {
	
	private int seasonNumber; // Which season just ended (e.g., 1 for SS1)
	private long oldSeasonXp; // XP before decay
	private long newSeasonXp; // XP after decay (5% of old)
	private long decayTimestamp; // When decay occurred (epoch seconds)
	
	// No-arg constructor for Gson
	public SeasonSummary() {}
	
	public SeasonSummary(int seasonNumber, long oldSeasonXp, long newSeasonXp, long decayTimestamp) {
		this.seasonNumber = seasonNumber;
		this.oldSeasonXp = oldSeasonXp;
		this.newSeasonXp = newSeasonXp;
		this.decayTimestamp = decayTimestamp;
	}
	
	// Getters
	public int getSeasonNumber() { return seasonNumber; }
	public long getOldSeasonXp() { return oldSeasonXp; }
	public long getNewSeasonXp() { return newSeasonXp; }
	public long getDecayTimestamp() { return decayTimestamp; }
	
	// Setters (for Gson)
	public void setSeasonNumber(int seasonNumber) { this.seasonNumber = seasonNumber; }
	public void setOldSeasonXp(long oldSeasonXp) { this.oldSeasonXp = oldSeasonXp; }
	public void setNewSeasonXp(long newSeasonXp) { this.newSeasonXp = newSeasonXp; }
	public void setDecayTimestamp(long decayTimestamp) { this.decayTimestamp = decayTimestamp; }
}
