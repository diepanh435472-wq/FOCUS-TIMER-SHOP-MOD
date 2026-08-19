package com.focustimershop.timer;

import com.focustimershop.FocusTimerShop;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents an active timer session for a player
 * Server-authoritative to prevent cheating
 * 
 * PHASE 2 FIXES:
 * - Accurate reward calculation with finalTick() improvements (BUG #4)
 * - Prevention of double-counting elapsed time
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
		// PHASE 2: Force final tick BEFORE changing state to capture accurate elapsed time
		finalTick();
		this.state = TimerState.PAUSED;
		// lastTickTime is already updated by finalTick(), no need to reset here
	}

	public void resume() {
		this.state = TimerState.RUNNING;
		// PHASE 2: Reset lastTickTime to NOW to start fresh timing from resume
		// This prevents counting the pause duration
		this.lastTickTime = System.currentTimeMillis();
	}

	/**
	 * Update timer - called every server tick
	 * Returns true if timer completed naturally
	 * 
	 * PHASE 2: Added protection against negative time from clock skew
	 */
	public boolean tick() {
		if (state != TimerState.RUNNING) {
			return false;
		}

		long currentTime = System.currentTimeMillis();
		long deltaMs = currentTime - lastTickTime;
		
		// PHASE 2: Protect against clock skew (negative delta or huge jump)
		if (deltaMs < 0) {
			// System clock went backwards!
			FocusTimerShop.LOGGER.warn("PHASE2_REWARD: Clock skew detected! deltaMs: {} (resetting lastTickTime)", deltaMs);
			lastTickTime = currentTime;
			return false;
		}
		
		// PHASE 2: Protect against huge time jumps (more than 5 seconds = likely clock adjustment)
		if (deltaMs > 5000) {
			FocusTimerShop.LOGGER.warn("PHASE2_REWARD: Large time jump detected: {}ms (capping to 5000ms)", deltaMs);
			deltaMs = 5000; // Cap at 5 seconds
		}
		
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
	 * PHASE 2 FIX: FINAL TICK BEFORE PAUSE/STOP
	 * Force update elapsed time with remaining milliseconds
	 * Called when timer is paused/stopped to get accurate final time
	 * 
	 * CRITICAL: Resets lastTickTime after updating to prevent double-counting
	 */
	public void finalTick() {
		// Only tick if currently RUNNING and has valid lastTickTime
		if (state != TimerState.RUNNING || lastTickTime == 0) {
			return;
		}
		
		long currentTime = System.currentTimeMillis();
		long deltaMs = currentTime - lastTickTime;
		
		// Only add time if there's actually a delta (prevent double-count on rapid pause/resume)
		if (deltaMs > 0) {
			// Add any remaining seconds (even if < 1000ms, round up if >= 0.5s)
			if (deltaMs >= 500) {
				int secondsPassed = (int) Math.ceil(deltaMs / 1000.0);
				elapsedTime += secondsPassed;
				
				FocusTimerShop.LOGGER.debug("PHASE2_REWARD: finalTick() added {}s (deltaMs: {})", 
					secondsPassed, deltaMs);
			}
			
			// CRITICAL: Reset lastTickTime to prevent double-counting
			// If we pause/resume quickly, we don't want to count the same time twice
			lastTickTime = currentTime;
		}
	}

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
	
	// PHASE 2: Setter for state (for persistence restore)
	public void setState(TimerState state) {
		this.state = state;
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
	
	// PHASE 2: Setter for elapsed time (for persistence restore)
	public void setElapsedTime(int elapsedTime) {
		this.elapsedTime = elapsedTime;
	}
	
	// PHASE 2: Getter for lastTickTime (for persistence)
	public long getLastTickTime() {
		return lastTickTime;
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
