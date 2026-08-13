package com.focustimershop.client;

import com.focustimershop.shop.ShopItem;
import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;

import java.util.*;

/**
 * Client-side cache for displaying server-authoritative data
 * Never used for validation, only for UI rendering
 */
public class ClientDataCache {
	
	// Economy data
	private static int silverCoins = 0;
	private static int goldCoins = 0;
	private static int focusXp = 0;

	// Timer state
	private static boolean hasActiveTimer = false;
	private static TimerType currentTimerType = null;
	private static TimerState currentTimerState = TimerState.IDLE;
	private static int elapsedSeconds = 0;
	private static int targetSeconds = 0;
	private static int pomodoroRounds = 0;
	
	// Client-side timer tracking for smooth animation
	private static long lastServerUpdateTime = 0;
	private static int lastServerElapsed = 0;
	
	// Shop data (synced from server)
	private static final Map<String, ShopItem> shopItems = new HashMap<>();
	private static boolean shopDataLoaded = false;

	// Economy getters
	public static int getSilverCoins() {
		return silverCoins;
	}

	public static int getGoldCoins() {
		return goldCoins;
	}

	public static int getFocusXp() {
		return focusXp;
	}

	public static void updateEconomy(int silver, int gold, int xp) {
		silverCoins = silver;
		goldCoins = gold;
		focusXp = xp;
	}

	// Timer getters
	public static boolean hasActiveTimer() {
		return hasActiveTimer;
	}

	public static TimerType getCurrentTimerType() {
		return currentTimerType;
	}

	public static TimerState getCurrentTimerState() {
		return currentTimerState;
	}

	/**
	 * Get current elapsed time with client-side interpolation for smooth display
	 */
	public static int getElapsedSeconds() {
		// If timer is not running, return last known value
		if (currentTimerState != TimerState.RUNNING) {
			return elapsedSeconds;
		}
		
		// Calculate client-side elapsed time based on last server update
		if (lastServerUpdateTime > 0) {
			long now = System.currentTimeMillis();
			long deltaMs = now - lastServerUpdateTime;
			int clientElapsed = lastServerElapsed + (int)(deltaMs / 1000);
			
			// Clamp to target for countdown timers
			if (currentTimerType != TimerType.STOPWATCH && targetSeconds > 0) {
				clientElapsed = Math.min(clientElapsed, targetSeconds);
			}
			
			return clientElapsed;
		}
		
		return elapsedSeconds;
	}

	public static int getTargetSeconds() {
		return targetSeconds;
	}

	public static int getPomodoroRounds() {
		return pomodoroRounds;
	}

	public static void updateTimerState(TimerType type, TimerState state, int elapsed, int target, int rounds) {
		hasActiveTimer = true;
		currentTimerType = type;
		currentTimerState = state;
		targetSeconds = target;
		pomodoroRounds = rounds;
		
		// Update client-side tracking
		lastServerElapsed = elapsed;
		lastServerUpdateTime = System.currentTimeMillis();
		elapsedSeconds = elapsed;
	}
	
	/**
	 * Optimistic state update for instant UI feedback
	 * Server update will override if different
	 */
	public static void setTimerStateOptimistic(TimerState newState) {
		currentTimerState = newState;
		
		// Reset client tracking when pausing
		if (newState == TimerState.PAUSED) {
			lastServerUpdateTime = 0;
		}
		// Restart tracking when resuming
		else if (newState == TimerState.RUNNING) {
			lastServerUpdateTime = System.currentTimeMillis();
			lastServerElapsed = elapsedSeconds;
		}
	}

	public static void clearTimerState() {
		hasActiveTimer = false;
		currentTimerType = null;
		currentTimerState = TimerState.IDLE;
		elapsedSeconds = 0;
		targetSeconds = 0;
		pomodoroRounds = 0;
		lastServerUpdateTime = 0;
		lastServerElapsed = 0;
	}

	public static boolean isGameFrozen() {
		return hasActiveTimer && currentTimerState == TimerState.RUNNING;
	}
	
	// Shop data methods
	public static void setShopItems(Collection<ShopItem> items) {
		shopItems.clear();
		for (ShopItem item : items) {
			shopItems.put(item.getItemId(), item);
		}
		shopDataLoaded = true;
		System.out.println("[ClientDataCache] Loaded " + shopItems.size() + " shop items from server");
	}
	
	public static Collection<ShopItem> getAllShopItems() {
		return new ArrayList<>(shopItems.values());
	}
	
	public static ShopItem getShopItem(String itemId) {
		return shopItems.get(itemId);
	}
	
	public static boolean isShopDataLoaded() {
		return shopDataLoaded;
	}
	
	public static void clearShopData() {
		shopItems.clear();
		shopDataLoaded = false;
	}
}
