package com.focustimershop.database;

import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;

import java.util.UUID;

/**
 * Player data stored in players/<uuid>.json
 * Includes economy data and current timer state
 */
public class PlayerData {
	
	private String version = "1.0.2";
	private String playerUuid;
	private String playerName;
	
	// Economy
	private int silverCoins = 0;
	private int goldCoins = 0;
	private int focusXp = 0;
	
	// Timer state (persisted across restarts)
	private String timerType = null;  // null if no active timer
	private String timerState = "IDLE";
	private int elapsedSeconds = 0;
	private int targetSeconds = 0;
	private int pomodoroRounds = 0;
	private long lastUpdateTimestamp = 0;  // System.currentTimeMillis()
	
	public PlayerData() {}
	
	public PlayerData(UUID uuid, String name) {
		this.playerUuid = uuid.toString();
		this.playerName = name;
	}
	
	// Economy getters/setters
	public String getVersion() { return version; }
	public void setVersion(String version) { this.version = version; }
	
	public String getPlayerUuid() { return playerUuid; }
	public void setPlayerUuid(String playerUuid) { this.playerUuid = playerUuid; }
	
	public String getPlayerName() { return playerName; }
	public void setPlayerName(String playerName) { this.playerName = playerName; }
	
	public int getSilverCoins() { return silverCoins; }
	public void setSilverCoins(int silverCoins) { this.silverCoins = silverCoins; }
	
	public int getGoldCoins() { return goldCoins; }
	public void setGoldCoins(int goldCoins) { this.goldCoins = goldCoins; }
	
	public int getFocusXp() { return focusXp; }
	public void setFocusXp(int focusXp) { this.focusXp = focusXp; }
	
	// Timer getters/setters
	public String getTimerType() { return timerType; }
	public void setTimerType(String timerType) { this.timerType = timerType; }
	
	public String getTimerState() { return timerState; }
	public void setTimerState(String timerState) { this.timerState = timerState; }
	
	public int getElapsedSeconds() { return elapsedSeconds; }
	public void setElapsedSeconds(int elapsedSeconds) { this.elapsedSeconds = elapsedSeconds; }
	
	public int getTargetSeconds() { return targetSeconds; }
	public void setTargetSeconds(int targetSeconds) { this.targetSeconds = targetSeconds; }
	
	public int getPomodoroRounds() { return pomodoroRounds; }
	public void setPomodoroRounds(int pomodoroRounds) { this.pomodoroRounds = pomodoroRounds; }
	
	public long getLastUpdateTimestamp() { return lastUpdateTimestamp; }
	public void setLastUpdateTimestamp(long lastUpdateTimestamp) { this.lastUpdateTimestamp = lastUpdateTimestamp; }
	
	// Helper methods
	public void addSilver(int amount) {
		this.silverCoins += amount;
	}
	
	public void addGold(int amount) {
		this.goldCoins += amount;
	}
	
	public void addXp(int amount) {
		this.focusXp += amount;
	}
	
	public boolean canAfford(int silverCost, int goldCost) {
		return this.silverCoins >= silverCost && this.goldCoins >= goldCost;
	}
	
	public void deductCurrency(int silverCost, int goldCost) {
		this.silverCoins -= silverCost;
		this.goldCoins -= goldCost;
	}
}
