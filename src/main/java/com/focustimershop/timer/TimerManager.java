package com.focustimershop.timer;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.economy.EconomyManager;
import com.focustimershop.network.ModNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side manager for all active timer sessions
 * Validates all timer operations to prevent cheating
 */
public class TimerManager {
	private static final Map<UUID, TimerSession> activeSessions = new HashMap<>();

	/**
	 * Start a new timer session
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

		// Sync to client
		ModNetworking.sendTimerStateUpdate(player, session);

		FocusTimerShop.LOGGER.info("Player {} started {} timer for {}s", 
			player.getName().getString(), type, targetSeconds);

		return true;
	}

	/**
	 * Pause active timer
	 */
	public static boolean pauseTimer(ServerPlayerEntity player) {
		TimerSession session = activeSessions.get(player.getUuid());
		if (session == null || session.getState() != TimerState.RUNNING) {
			return false;
		}

		session.pause();
		ModNetworking.sendTimerStateUpdate(player, session);
		return true;
	}

	/**
	 * Resume paused timer
	 */
	public static boolean resumeTimer(ServerPlayerEntity player) {
		TimerSession session = activeSessions.get(player.getUuid());
		if (session == null || session.getState() != TimerState.PAUSED) {
			return false;
		}

		session.resume();
		ModNetworking.sendTimerStateUpdate(player, session);
		return true;
	}

	/**
	 * Stop timer and award rewards if applicable
	 */
	public static boolean stopTimer(ServerPlayerEntity player, boolean abandoned) {
		TimerSession session = activeSessions.get(player.getUuid());
		if (session == null) {
			return false;
		}

		// ===== FIX: GET FINAL ELAPSED TIME =====
		// Force final tick to update elapsed time before getting it
		session.finalTick();
		int elapsedSeconds = session.getElapsedTime();
		// ========================================
		
		boolean shouldReward = !abandoned && session.shouldAwardRewards();

		// Award rewards if completed successfully
		if (shouldReward && elapsedSeconds > 0) {
			EconomyManager.awardTimerReward(player, elapsedSeconds);
			
			// Increment pomodoro round counter
			if (session.getType() == TimerType.POMODORO_FOCUS) {
				session.incrementPomodoroRound();
			}

			// Play success sound
			player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 0.5f, 1.0f);
			
			// Send message
			player.sendMessage(Text.literal("§aTimer completed! Earned rewards."), false);
		} else if (abandoned) {
			player.sendMessage(Text.literal("§cTimer abandoned. No rewards earned."), false);
		}

		// Clean up
		activeSessions.remove(player.getUuid());
		ModNetworking.sendTimerStateUpdate(player, null);

		FocusTimerShop.LOGGER.info("Player {} stopped timer (abandoned: {}, time: {}s)", 
			player.getName().getString(), abandoned, elapsedSeconds);

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
	 */
	public static boolean isPlayerFrozen(UUID playerId) {
		TimerSession session = activeSessions.get(playerId);
		return session != null && session.getState() == TimerState.RUNNING;
	}

	/**
	 * Server tick - update all active timers
	 */
	public static void tick(MinecraftServer server) {
		activeSessions.entrySet().removeIf(entry -> {
			UUID playerId = entry.getKey();
			TimerSession session = entry.getValue();
			
			ServerPlayerEntity player = server.getPlayerManager().getPlayer(playerId);
			if (player == null) {
				// Player disconnected - abandon timer
				FocusTimerShop.LOGGER.info("Abandoning timer for disconnected player {}", playerId);
				return true;
			}

			// Update timer
			boolean completed = session.tick();
			
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
	 */
	public static void clearAll() {
		activeSessions.clear();
	}

	/**
	 * Handle player disconnect - abandon their timer
	 */
	public static void onPlayerDisconnect(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		if (activeSessions.containsKey(playerId)) {
			FocusTimerShop.LOGGER.info("Player {} disconnected with active timer - abandoning", 
				player.getName().getString());
			activeSessions.remove(playerId);
		}
	}
}
