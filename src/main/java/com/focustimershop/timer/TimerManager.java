package com.focustimershop.timer;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.economy.EconomyManager;
import com.focustimershop.network.ModNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side manager for all active timer sessions
 * Validates all timer operations to prevent cheating
 * 
 * PHASE 2 FIXES:
 * - Thread-safe using ConcurrentHashMap (BUG #5)
 * - Proper freeze detection enforcement (BUG #2)
 */
public class TimerManager {
	// PHASE 2 FIX: Use ConcurrentHashMap for thread-safety
	private static final Map<UUID, TimerSession> activeSessions = new ConcurrentHashMap<>();

	/**
	 * Start a new timer session
	 * PHASE 2: Now saves state for persistence
	 */
	public static boolean startTimer(ServerPlayerEntity player, TimerType type, int targetSeconds) {
		UUID playerId = player.getUuid();
		
		// Cancel existing session if any
		if (activeSessions.containsKey(playerId)) {
			FocusTimerShop.LOGGER.warn("Player {} tried to start timer while one is active", 
				player.getName().getString());
			return false;
		}

		// Validate target time
		if (type != TimerType.STOPWATCH && (targetSeconds <= 0 || targetSeconds > 7200)) {
			FocusTimerShop.LOGGER.warn("Invalid target time: {}", targetSeconds);
			return false;
		}

		// Create session
		TimerSession session = new TimerSession(playerId, type, targetSeconds);
		session.start();
		activeSessions.put(playerId, session);

		// PHASE 2: Save state immediately
		TimerPersistence.saveTimer(playerId, session);

		// Sync to client
		ModNetworking.sendTimerStateUpdate(player, session);

		FocusTimerShop.LOGGER.info("Player {} started {} timer for {}s", 
			player.getName().getString(), type, targetSeconds);

		return true;
	}

	/**
	 * Pause active timer
	 * PHASE 2: Now saves state for persistence
	 */
	public static boolean pauseTimer(ServerPlayerEntity player) {
		TimerSession session = activeSessions.get(player.getUuid());
		if (session == null || session.getState() != TimerState.RUNNING) {
			return false;
		}

		session.pause();
		
		// PHASE 2: Save state when pausing
		TimerPersistence.saveTimer(player.getUuid(), session);
		
		ModNetworking.sendTimerStateUpdate(player, session);
		return true;
	}

	/**
	 * Resume paused timer
	 * PHASE 2: Now saves state for persistence
	 */
	public static boolean resumeTimer(ServerPlayerEntity player) {
		TimerSession session = activeSessions.get(player.getUuid());
		if (session == null || session.getState() != TimerState.PAUSED) {
			return false;
		}

		session.resume();
		
		// PHASE 2: Save state when resuming
		TimerPersistence.saveTimer(player.getUuid(), session);
		
		ModNetworking.sendTimerStateUpdate(player, session);
		return true;
	}

	/**
	 * Stop timer and award rewards if applicable
	 * PHASE 2: Now deletes save file after completion
	 * PHASE 2: Added validation to prevent negative/zero rewards
	 */
	public static boolean stopTimer(ServerPlayerEntity player, boolean abandoned) {
		TimerSession session = activeSessions.get(player.getUuid());
		if (session == null) {
			return false;
		}

		// PHASE 2: GET FINAL ELAPSED TIME with final tick
		session.finalTick();
		int elapsedSeconds = session.getElapsedTime();
		
		// PHASE 2: Validate elapsed time is reasonable (not negative, not zero if claiming rewards)
		if (elapsedSeconds < 0) {
			FocusTimerShop.LOGGER.warn("PHASE2_REWARD: Negative elapsed time for player {}: {}s (resetting to 0)", 
				player.getName().getString(), elapsedSeconds);
			elapsedSeconds = 0;
		}
		
		// PHASE 2: Minimum time for rewards (at least 1 minute = 60 seconds)
		boolean shouldReward = !abandoned && session.shouldAwardRewards() && elapsedSeconds >= 60;
		
		if (!shouldReward && !abandoned && elapsedSeconds < 60) {
			player.sendMessage(Text.literal("§cTimer quá ngắn! Cần ít nhất 1 phút để nhận rewards."), false);
		}

		// Award rewards if completed successfully
		if (shouldReward) {
			EconomyManager.awardTimerReward(player, elapsedSeconds);
			
			// Record stats (v1.0.6 - Phase A)
			com.focustimershop.database.PlayerStatsData stats = 
				com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
			stats.setTotalTimerSessionsCompleted(stats.getTotalTimerSessionsCompleted() + 1);
			stats.setTotalFocusTimeSeconds(stats.getTotalFocusTimeSeconds() + elapsedSeconds);
			
			// v1.0.7-beta - Track timer type uses for achievements
			String legacyTypeName = session.getLegacyTypeNameForAchievements();
			stats.incrementTimerTypeUse(legacyTypeName);
			
			// Phase B - Activity log
			int silverEarned = elapsedSeconds / 45;
			int xpEarned = elapsedSeconds / 90;
			int minutes = elapsedSeconds / 60;
			stats.addActivity(
				com.focustimershop.database.ActivityEntry.Type.TIMER_COMPLETE,
				String.format("🔥 Hoàn thành phiên tập trung %d phút        +%d Silver +%d XP", 
					minutes, silverEarned, xpEarned)
			);
			
			com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
			
			// Record session in profile (v1.0.6)
			com.focustimershop.profile.ProfileManager.recordSession(
				player, elapsedSeconds, session.getType().name());
			
			// Increment pomodoro round counter
			if (session.getType() == TimerType.POMODORO_FOCUS) {
				session.incrementPomodoroRound();
			}

			// Play success sound
			player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
			
			// Send message
			player.sendMessage(Text.literal(String.format("§aTimer hoàn thành! +%d Silver +%d XP (%d phút)", 
				silverEarned, xpEarned, minutes)), false);
				
			FocusTimerShop.LOGGER.info("PHASE2_REWARD: Player {} earned {} silver, {} xp for {}s timer", 
				player.getName().getString(), silverEarned, xpEarned, elapsedSeconds);
				
		} else if (abandoned) {
			player.sendMessage(Text.literal("§cTimer bị hủy. Không nhận được rewards."), false);
		}

		// Clean up
		activeSessions.remove(player.getUuid());
		
		// PHASE 2: Delete save file (timer stopped)
		TimerPersistence.deleteTimer(player.getUuid());
		
		ModNetworking.sendTimerStateUpdate(player, null);

		FocusTimerShop.LOGGER.info("Player {} stopped timer (abandoned: {}, time: {}s, rewarded: {})", 
			player.getName().getString(), abandoned, elapsedSeconds, shouldReward);

		return true;
	}

	/**
	 * Get active session for player
	 */
	public static TimerSession getSession(UUID playerId) {
		return activeSessions.get(playerId);
	}

	/**
	 * Check if player has active timer
	 */
	public static boolean hasActiveTimer(UUID playerId) {
		TimerSession session = activeSessions.get(playerId);
		return session != null && session.getState() != TimerState.IDLE;
	}

	/**
	 * Check if player's game should be frozen
	 * PHASE 2: Now properly used by RentalManager to prevent rental timers from ticking during focus
	 * 
	 * @return true if player has timer RUNNING (not just paused), false otherwise
	 */
	public static boolean isPlayerFrozen(UUID playerId) {
		TimerSession session = activeSessions.get(playerId);
		return session != null && session.getState() == TimerState.RUNNING;
	}
	
	/**
	 * PHASE 2 NEW: Validate that operation is allowed (player not frozen)
	 * Used for server-side validation of actions that should be blocked during focus
	 * 
	 * @param player Player attempting the operation
	 * @param operationName Name of operation for logging
	 * @return true if allowed (not frozen), false if blocked (frozen)
	 */
	public static boolean validateNotFrozen(ServerPlayerEntity player, String operationName) {
		if (isPlayerFrozen(player.getUuid())) {
			FocusTimerShop.LOGGER.warn("PHASE2_FREEZE: Player {} attempted {} while frozen (timer running)", 
				player.getName().getString(), operationName);
			player.sendMessage(Text.literal("§cKhông thể làm điều này khi đang tập trung!"), false);
			return false;
		}
		return true;
	}

	/**
	 * Server tick - update all active timers
	 * PHASE 2: Added periodic auto-save every 30 seconds
	 */
	public static void tick(MinecraftServer server) {
		// PHASE 2: Auto-save counter (save every 30 seconds = 600 ticks)
		long currentTick = server.getTicks();
		boolean shouldAutoSave = (currentTick % 600 == 0);
		
		activeSessions.entrySet().removeIf(entry -> {
			UUID playerId = entry.getKey();
			TimerSession session = entry.getValue();
			
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
			if (player == null) {
				// Player disconnected - save timer before abandoning
				TimerPersistence.saveTimer(playerId, session);
				FocusTimerShop.LOGGER.info("PHASE2_PERSIST: Saved timer for disconnected player {}", playerId);
				return true;
			}

			// Update timer
			boolean completed = session.tick();
			
			// PHASE 2: Auto-save running/paused timers every 30 seconds
			if (shouldAutoSave && session.getState() != TimerState.IDLE) {
				TimerPersistence.saveTimer(playerId, session);
			}
			
			// Send state update to client every second (20 ticks)
			// This ensures the UI displays current elapsed time
			if (server.getTicks() % 20 == 0) {
				ModNetworking.sendTimerStateUpdate(player, session);
			}
			
			if (completed) {
				// Timer completed naturally
				stopTimer(player, false);
				
				// Play completion sound
				player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), 
					SoundCategory.PLAYERS, 1.0f, 1.0f);
				
				return true; // Remove from map (already handled in stopTimer)
			}

			return false;
		});
	}

	/**
	 * Clear all sessions (server shutdown)
	 * PHASE 2: Now saves all sessions before clearing
	 */
	public static void clearAll() {
		// PHASE 2: Save all active sessions before shutdown
		for (Map.Entry<UUID, TimerSession> entry : activeSessions.entrySet()) {
			TimerPersistence.saveTimer(entry.getKey(), entry.getValue());
		}
		
		FocusTimerShop.LOGGER.info("PHASE2_PERSIST: Saved {} timer sessions on server shutdown", 
			activeSessions.size());
		
		activeSessions.clear();
	}

	/**
	 * Handle player disconnect - pause and save timer
	 * v1.0.7-beta: Timer pauses on disconnect, resumes on rejoin
	 */
	public static void onPlayerDisconnect(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		TimerSession session = activeSessions.get(playerId);
		
		if (session != null) {
			// Force pause if running
			if (session.getState() == TimerState.RUNNING) {
				session.pause();
				FocusTimerShop.LOGGER.info("Auto-paused timer for {} on disconnect", 
					player.getName().getString());
			}
			
			// Save paused timer for restoration
			TimerPersistence.saveTimer(playerId, session);
			
			FocusTimerShop.LOGGER.info("Saved timer for player {} on disconnect (state: PAUSED, elapsed: {}s)", 
				player.getName().getString(), session.getElapsedTime());
			
			activeSessions.remove(playerId);
		}
	}
	
	/**
	 * Handle player join - restore and resume timer
	 * v1.0.7-beta: Timer resumes automatically on rejoin
	 */
	public static void onPlayerJoin(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		
		// Check if player has saved timer
		TimerPersistence.TimerSaveData saveData = TimerPersistence.loadTimer(playerId);
		
		if (saveData != null) {
			// Restore timer session
			TimerSession session = TimerPersistence.restoreTimer(playerId, saveData);
			
			// Auto-resume if it was paused (from disconnect)
			if (session.getState() == TimerState.PAUSED) {
				session.resume();
				FocusTimerShop.LOGGER.info("Auto-resumed timer for {} on rejoin", 
					player.getName().getString());
			}
			
			activeSessions.put(playerId, session);
			
			// Sync to client - will open ActiveSessionScreen fullscreen
			ModNetworking.sendTimerStateUpdate(player, session);
			
			// Notify player
			int minutes = session.getElapsedTime() / 60;
			player.sendMessage(
				Text.literal(String.format("§aTimer tiếp tục! (%d phút đã trôi qua)", minutes)), 
				false
			);
			
			FocusTimerShop.LOGGER.info("Restored and resumed timer for player {} (type: {}, elapsed: {}s)",
				player.getName().getString(), session.getType(), session.getElapsedTime());
		}
	}
	
	/**
	 * PHASE 2 NEW: Initialize persistence system
	 * Called on server startup
	 */
	public static void initializePersistence() {
		// Clean up old saves (> 7 days)
		TimerPersistence.cleanupOldSaves();
	}
}
