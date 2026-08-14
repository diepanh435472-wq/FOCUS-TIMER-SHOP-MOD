package com.focustimershop.timer;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an active timer session for a player
 * Server-authoritative to prevent cheating
 */
public class TimerSession {
	private final UUID playerId;
	private TimerType type;
	private TimerState state;
	
	// Time tracking (in seconds)
	private int targetTime;      // For countdown/pomodoro (0 = no target for stopwatch)
	private int elapsedTime;     // Current elapsed time
	private long lastTickTime;   // System time of last update
	
	// Pomodoro-specific
	private int pomodoroRounds;  // Completed focus rounds in current session
	
	// Stopwatch lap times
	private List<Integer> lapTimes;

	public TimerSession(UUID playerId, TimerType type, int targetTimeSeconds) {
		this.playerId = playerId;
		this.type = type;
		this.state = TimerState.IDLE;
		this.targetTime = targetTimeSeconds;
		this.elapsedTime = 0;
		this.lastTickTime = 0;
		this.pomodoroRounds = 0;
		this.lapTimes = new ArrayList<>();
	}

	public void start() {
		this.state = TimerState.RUNNING;
		this.lastTickTime = System.currentTimeMillis();
	}

	public void pause() {
		// Force final tick to capture accurate elapsed time
		finalTick();
		this.state = TimerState.PAUSED;
	}

	public void resume() {
		this.state = TimerState.RUNNING;
		this.lastTickTime = System.currentTimeMillis();
	}

	/**
	 * Update timer - called every server tick
	 * Returns true if timer completed naturally
	 */
	public boolean tick() {
		if (state != TimerState.RUNNING) {
			return false;
		}

		long currentTime = System.currentTimeMillis();
		long deltaMs = currentTime - lastTickTime;
		
		// Update every second
		if (deltaMs >= 1000) {
			int secondsPassed = (int) (deltaMs / 1000);
			
			if (type == TimerType.STOPWATCH) {
				// Count up
				elapsedTime += secondsPassed;
			} else {
				// Count down (Pomodoro or Countdown)
				elapsedTime += secondsPassed;
				
				// Check if completed
				if (elapsedTime >= targetTime) {
					return true; // Timer completed
				}
			}
			
			lastTickTime = currentTime;
		}

		return false;
	}
	
	/**
	 * ===== FIX: FINAL TICK BEFORE STOP =====
	 * Force update elapsed time with remaining milliseconds
	 * Called when timer is stopped to get accurate final time
	 */
	public void finalTick() {
		if (state != TimerState.RUNNING || lastTickTime == 0) {
			return;
		}
		
		long currentTime = System.currentTimeMillis();
		long deltaMs = currentTime - lastTickTime;
		
		// Add any remaining seconds (even if < 1000ms, round up)
		if (deltaMs >= 500) { // Round up if >= 0.5 seconds
			int secondsPassed = (int) Math.ceil(deltaMs / 1000.0);
			elapsedTime += secondsPassed;
		}
	}
	// ========================================

	public void addLap() {
		if (type == TimerType.STOPWATCH) {
			lapTimes.add(elapsedTime);
		}
	}

	public void incrementPomodoroRound() {
		if (type == TimerType.POMODORO_FOCUS) {
			pomodoroRounds++;
		}
	}

	// Getters
	public UUID getPlayerId() {
		return playerId;
	}

	public TimerType getType() {
		return type;
	}

	public void setType(TimerType type) {
		this.type = type;
	}

	public TimerState getState() {
		return state;
	}

	public int getTargetTime() {
		return targetTime;
	}

	public void setTargetTime(int targetTime) {
		this.targetTime = targetTime;
	}

	public int getElapsedTime() {
		return elapsedTime;
	}

	public int getRemainingTime() {
		if (type == TimerType.STOPWATCH) {
			return 0; // No limit
		}
		return Math.max(0, targetTime - elapsedTime);
	}

	public int getPomodoroRounds() {
		return pomodoroRounds;
	}

	public List<Integer> getLapTimes() {
		return new ArrayList<>(lapTimes);
	}

	/**
	 * Check if this timer type should award rewards on completion
	 */
	public boolean shouldAwardRewards() {
		return type == TimerType.POMODORO_FOCUS || 
		       type == TimerType.STOPWATCH || 
		       type == TimerType.COUNTDOWN;
	}

	public void reset() {
		this.state = TimerState.IDLE;
		this.elapsedTime = 0;
		this.lastTickTime = 0;
	}
}
