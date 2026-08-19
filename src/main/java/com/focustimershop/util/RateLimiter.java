package com.focustimershop.util;

import com.focustimershop.FocusTimerShop;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Rate limiting infrastructure for dangerous operations
 * Phase 1 - Prevents abuse of expensive/sensitive operations
 * 
 * Uses sliding window algorithm with per-player tracking
 */
public class RateLimiter {
	
	/**
	 * Rate limit configuration
	 */
	public static class RateLimit {
		public final int maxRequests;
		public final long windowMs;
		
		public RateLimit(int maxRequests, long windowMs) {
			this.maxRequests = maxRequests;
			this.windowMs = windowMs;
		}
	}
	
	/**
	 * Predefined rate limits for different operation types
	 */
	public static class Limits {
		// Bulk operations - expensive, should be rare
		public static final RateLimit CHEST_BULK_OPEN = new RateLimit(5, 60_000); // 5 per minute
		
		// Shop operations - moderate cost
		public static final RateLimit SHOP_CHECKOUT = new RateLimit(20, 60_000); // 20 per minute
		
		// Currency conversion - low cost but can be spammed
		public static final RateLimit CURRENCY_CONVERT = new RateLimit(30, 60_000); // 30 per minute
		
		// Rental requests - expensive calculations
		public static final RateLimit RENTAL_REQUEST = new RateLimit(10, 60_000); // 10 per minute
		
		// Profile updates - low cost but should be limited
		public static final RateLimit CUSTOM_NAME_UPDATE = new RateLimit(5, 60_000); // 5 per minute
		public static final RateLimit EQUIP_TITLE = new RateLimit(20, 60_000); // 20 per minute
		
		// Timer operations - should be limited to prevent spam
		public static final RateLimit TIMER_START = new RateLimit(10, 60_000); // 10 per minute
	}
	
	/**
	 * Request timestamp tracking for a single player+operation
	 */
	private static class RequestHistory {
		private final java.util.LinkedList<Long> timestamps = new java.util.LinkedList<>();
		
		/**
		 * Check if request is allowed and record if so
		 * @param rateLimit Rate limit configuration
		 * @return true if allowed, false if rate limited
		 */
		public synchronized boolean tryRequest(RateLimit rateLimit) {
			long now = System.currentTimeMillis();
			long windowStart = now - rateLimit.windowMs;
			
			// Remove timestamps outside window
			while (!timestamps.isEmpty() && timestamps.getFirst() < windowStart) {
				timestamps.removeFirst();
			}
			
			// Check if limit exceeded
			if (timestamps.size() >= rateLimit.maxRequests) {
				return false; // Rate limited
			}
			
			// Record request
			timestamps.addLast(now);
			return true;
		}
		
		/**
		 * Get time until rate limit resets
		 * @param rateLimit Rate limit configuration
		 * @return milliseconds until next request allowed, or 0 if allowed now
		 */
		public synchronized long getResetTimeMs(RateLimit rateLimit) {
			if (timestamps.isEmpty()) {
				return 0;
			}
			
			long now = System.currentTimeMillis();
			long windowStart = now - rateLimit.windowMs;
			
			// Clean old timestamps
			while (!timestamps.isEmpty() && timestamps.getFirst() < windowStart) {
				timestamps.removeFirst();
			}
			
			if (timestamps.size() < rateLimit.maxRequests) {
				return 0; // Not rate limited
			}
			
			// Time until oldest request exits window
			long oldestTimestamp = timestamps.getFirst();
			return (oldestTimestamp + rateLimit.windowMs) - now;
		}
	}
	
	/**
	 * Rate limit tracking: Map<OperationType, Map<PlayerId, RequestHistory>>
	 */
	private static final Map<String, Map<UUID, RequestHistory>> rateLimits = new ConcurrentHashMap<>();
	
	/**
	 * Check if operation is allowed for player
	 * @param playerId Player UUID
	 * @param operationType Operation type identifier
	 * @param rateLimit Rate limit configuration
	 * @return true if allowed, false if rate limited
	 */
	public static boolean tryRequest(UUID playerId, String operationType, RateLimit rateLimit) {
		Map<UUID, RequestHistory> operationMap = rateLimits.computeIfAbsent(
			operationType, k -> new ConcurrentHashMap<>()
		);
		
		RequestHistory history = operationMap.computeIfAbsent(
			playerId, k -> new RequestHistory()
		);
		
		boolean allowed = history.tryRequest(rateLimit);
		
		if (!allowed) {
			long resetMs = history.getResetTimeMs(rateLimit);
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} rate limited for operation '{}' (reset in {}ms)", 
				playerId, operationType, resetMs);
		}
		
		return allowed;
	}
	
	/**
	 * Check if operation is allowed for player (with player name for logging)
	 * @param playerId Player UUID
	 * @param playerName Player name for logging
	 * @param operationType Operation type identifier
	 * @param rateLimit Rate limit configuration
	 * @return true if allowed, false if rate limited
	 */
	public static boolean tryRequest(UUID playerId, String playerName, String operationType, RateLimit rateLimit) {
		Map<UUID, RequestHistory> operationMap = rateLimits.computeIfAbsent(
			operationType, k -> new ConcurrentHashMap<>()
		);
		
		RequestHistory history = operationMap.computeIfAbsent(
			playerId, k -> new RequestHistory()
		);
		
		boolean allowed = history.tryRequest(rateLimit);
		
		if (!allowed) {
			long resetMs = history.getResetTimeMs(rateLimit);
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} ({}) rate limited for operation '{}' (reset in {}ms)", 
				playerName, playerId, operationType, resetMs);
		}
		
		return allowed;
	}
	
	/**
	 * Get time until rate limit resets for player
	 * @param playerId Player UUID
	 * @param operationType Operation type identifier
	 * @param rateLimit Rate limit configuration
	 * @return milliseconds until next request allowed, or 0 if allowed now
	 */
	public static long getResetTimeMs(UUID playerId, String operationType, RateLimit rateLimit) {
		Map<UUID, RequestHistory> operationMap = rateLimits.get(operationType);
		if (operationMap == null) {
			return 0;
		}
		
		RequestHistory history = operationMap.get(playerId);
		if (history == null) {
			return 0;
		}
		
		return history.getResetTimeMs(rateLimit);
	}
	
	/**
	 * Clear rate limit history for a player (e.g., on disconnect)
	 * @param playerId Player UUID
	 */
	public static void clearPlayer(UUID playerId) {
		for (Map<UUID, RequestHistory> operationMap : rateLimits.values()) {
			operationMap.remove(playerId);
		}
	}
	
	/**
	 * Clear all rate limit history (e.g., server shutdown)
	 */
	public static void clearAll() {
		rateLimits.clear();
	}
	
	/**
	 * Get human-readable time remaining message
	 * @param ms Milliseconds remaining
	 * @return Formatted message
	 */
	public static String formatResetTime(long ms) {
		if (ms <= 0) {
			return "now";
		}
		
		long seconds = ms / 1000;
		if (seconds < 60) {
			return seconds + "s";
		}
		
		long minutes = seconds / 60;
		seconds = seconds % 60;
		return minutes + "m " + seconds + "s";
	}
	
	/**
	 * Cleanup old entries periodically (call from server tick or scheduled task)
	 * Removes players with no recent requests to prevent memory leak
	 */
	public static void cleanup() {
		long now = System.currentTimeMillis();
		long maxAge = 5 * 60_000; // 5 minutes
		
		for (Map<UUID, RequestHistory> operationMap : rateLimits.values()) {
			operationMap.entrySet().removeIf(entry -> {
				RequestHistory history = entry.getValue();
				synchronized (history) {
					return history.timestamps.isEmpty() || 
					       (now - history.timestamps.getLast()) > maxAge;
				}
			});
		}
	}
}
