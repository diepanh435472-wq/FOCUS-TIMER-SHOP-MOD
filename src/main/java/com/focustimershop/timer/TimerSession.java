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
	
	// v1.0.7-beta Timer UI Overhaul - New category + mode system
	private SessionCategory category;
	private ClockMode clockMode;
	
	// Time tracking (in seconds)
	private int targetTime;      // For countdown/pomodoro (0 = no target for stopwatch)
	private int elapsedTime;     // Current elapsed time
	private long lastTickTime;   // System time of last update
	
	// Pomodoro-specific
	private int pomodoroRounds;  // Completed focus rounds in current session
	
	// Stopwatch lap times
	private List<Integer> lapTimes;
	
	// v1.0.7-beta - Encouragement note and to-do list
	private String encouragementNote;
	private List<String> todoList;

	// Old constructor for backwards compatibility
	public TimerSession(UUID playerId, TimerType type, int targetTimeSeconds) {
		this.playerId = playerId;
		this.type = type;
		this.state = TimerState.IDLE;
		this.targetTime = targetTimeSeconds;
		this.elapsedTime = 0;
		this.lastTickTime = 0;
		this.pomodoroRounds = 0;
		this.lapTimes = new ArrayList<>();
		this.todoList = new ArrayList<>();
		
		// Map old TimerType to new category/mode for backwards compatibility
		mapLegacyTypeToNewSystem();
	}
	
	// v1.0.7-beta - New constructor using category + clock mode
	public TimerSession(UUID playerId, SessionCategory category, ClockMode clockMode, 
	                    int targetTimeSeconds, String encouragementNote, List<String> todoList) {
		this.playerId = playerId;
		this.category = category;
		this.clockMode = clockMode;
		this.state = TimerState.IDLE;
		this.targetTime = targetTimeSeconds;
		this.elapsedTime = 0;
		this.lastTickTime = 0;
		this.pomodoroRounds = 0;
		this.lapTimes = new ArrayList<>();
		this.encouragementNote = encouragementNote;
		this.todoList = todoList != null ? new ArrayList<>(todoList) : new ArrayList<>();
		
		// Set legacy TimerType based on category + mode
		this.type = mapNewSystemToLegacyType(category, clockMode);
	}
	
	/**
	 * v1.0.7-beta - Map legacy TimerType to new category/mode system
	 */
	private void mapLegacyTypeToNewSystem() {
		switch (type) {
			case POMODORO_FOCUS:
				this.category = SessionCategory.TAP_TRUNG;
				this.clockMode = ClockMode.COUNTDOWN;
				break;
			case POMODORO_SHORT_BREAK:
				this.category = SessionCategory.NGHI_NGAN;
				this.clockMode = ClockMode.COUNTDOWN;
				break;
			case POMODORO_LONG_BREAK:
				this.category = SessionCategory.NGHI_DAI;
				this.clockMode = ClockMode.COUNTDOWN;
				break;
			case STOPWATCH:
				this.category = SessionCategory.TAP_TRUNG; // Default to focus
				this.clockMode = ClockMode.STOPWATCH;
				break;
			case COUNTDOWN:
				this.category = SessionCategory.TAP_TRUNG; // Default to focus
				this.clockMode = ClockMode.COUNTDOWN;
				break;
		}
	}
	
	/**
	 * v1.0.7-beta - Map new category/mode system to legacy TimerType
	 * Used for maintaining compatibility with existing systems
	 */
	private TimerType mapNewSystemToLegacyType(SessionCategory category, ClockMode clockMode) {
		if (clockMode == ClockMode.STOPWATCH) {
			return TimerType.STOPWATCH;
		}
		
		// Countdown mode
		switch (category) {
			case TAP_TRUNG:
				return TimerType.POMODORO_FOCUS;
			case NGHI_NGAN:
				return TimerType.POMODORO_SHORT_BREAK;
			case NGHI_DAI:
				return TimerType.POMODORO_LONG_BREAK;
			case TAP_LUYEN:
				return TimerType.COUNTDOWN; // Exercise maps to generic countdown
			default:
				return TimerType.COUNTDOWN;
		}
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
	
	// v1.0.7-beta - Getters/setters for new fields
	public SessionCategory getCategory() {
		return category;
	}
	
	public void setCategory(SessionCategory category) {
		this.category = category;
	}
	
	public ClockMode getClockMode() {
		return clockMode;
	}
	
	public void setClockMode(ClockMode clockMode) {
		this.clockMode = clockMode;
	}
	
	public String getEncouragementNote() {
		return encouragementNote;
	}
	
	public void setEncouragementNote(String encouragementNote) {
		this.encouragementNote = encouragementNote;
	}
	
	public List<String> getTodoList() {
		return todoList != null ? new ArrayList<>(todoList) : new ArrayList<>();
	}
	
	public void setTodoList(List<String> todoList) {
		this.todoList = todoList != null ? new ArrayList<>(todoList) : new ArrayList<>();
	}
	
	/**
	 * v1.0.7-beta - Get legacy type name for achievement tracking
	 * Maps category + mode to old achievement type names per spec §1:
	 * - "Pomodoro" = TAP_TRUNG + COUNTDOWN
	 * - "Stopwatch" = Any category + STOPWATCH (Bấm giờ mode)
	 * - "Countdown" = (NGHI_NGAN | NGHI_DAI | TAP_LUYEN) + COUNTDOWN (excluding TAP_TRUNG to avoid double-trigger)
	 */
	public String getLegacyTypeNameForAchievements() {
		// New system: use category + mode
		if (category != null && clockMode != null) {
			if (clockMode == ClockMode.STOPWATCH) {
				return "Stopwatch"; // Any category with Stopwatch mode
			}
			
			// Countdown mode
			if (category == SessionCategory.TAP_TRUNG) {
				return "Pomodoro"; // Focus + Countdown = old Pomodoro
			} else {
				return "Countdown"; // Other categories + Countdown = old generic Countdown
			}
		}
		
		// Fallback: map legacy TimerType to achievement name
		switch (type) {
			case POMODORO_FOCUS:
			case POMODORO_SHORT_BREAK:
			case POMODORO_LONG_BREAK:
				return "Pomodoro";
			case STOPWATCH:
				return "Stopwatch";
			case COUNTDOWN:
			default:
				return "Countdown";
		}
	}
}
