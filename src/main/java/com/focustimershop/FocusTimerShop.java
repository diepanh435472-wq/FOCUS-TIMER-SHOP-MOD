package com.focustimershop;

import com.focustimershop.economy.EconomyManager;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.timer.TimerManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FocusTimerShop implements ModInitializer {
	public static final String MOD_ID = "focustimershop";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Focus Timer Shop");

		// Initialize FCTMS database
		com.focustimershop.database.DatabaseManager.initialize();
		
		// Initialize rank system (v1.0.6)
		com.focustimershop.profile.RankManager.initialize();
		
		// Initialize profile system (v1.0.6)
		com.focustimershop.profile.ProfileManager.initialize();
		
		// Initialize achievement system (v1.0.6 Phase 5)
		com.focustimershop.achievement.AchievementManager.initialize();
		com.focustimershop.achievement.AchievementSystemManager.initialize();
		
		// Initialize title system (v1.0.6 Phase 5)
		com.focustimershop.title.TitleSystemManager.initialize();
		
		// Initialize mission system (v1.0.6 Phase 6)
		com.focustimershop.mission.MissionManager.initialize();
		
		// Initialize rental system
		com.focustimershop.rental.RentalManager.initialize();
		
		// Register area mining handler for rental tools
		com.focustimershop.rental.AreaMiningHandler.register();
		
		// Initialize bulk order system (v1.0.6-beta)
		com.focustimershop.bulkorder.BulkOrderManager.initialize();
		
		// Initialize season system (v1.0.6-beta)
		com.focustimershop.season.SeasonManager.initialize();

		// Initialize networking
		ModNetworking.registerServerPackets();
		
		// PHASE 2: Initialize timer persistence system
		TimerManager.initializePersistence();
		
		// Register admin commands
		net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
			(dispatcher, registryAccess, environment) -> {
				com.focustimershop.command.AdminCommands.register(dispatcher, registryAccess, environment);
				com.focustimershop.command.AdminCommands.registerRankTest(dispatcher); // v1.0.6 Phase 0.1
				com.focustimershop.command.ProfileTestCommand.register(dispatcher, registryAccess, environment);
			}
		);

		// Register server tick handler for timer updates
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TimerManager.tick(server);
			com.focustimershop.rental.RentalManager.tick();
			// Check for expired rental tools every tick (will remove from inventory)
			com.focustimershop.rental.RentalManager.checkAndRemoveExpiredTools(server);
		});
		
		// Register second-based tick for rental timers (only tick down when not frozen)
		final int[] tickCounter = {0};
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			tickCounter[0]++;
			if (tickCounter[0] >= 20) { // Every second
				tickCounter[0] = 0;
				com.focustimershop.rental.RentalManager.tickRentalTimers(server);
			}
		});
		
		// PHASE 3: Periodic cleanup for memory leak prevention (BUG #13)
		// Cleanup every minute (1200 ticks)
		final int[] cleanupCounter = {0};
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			cleanupCounter[0]++;
			if (cleanupCounter[0] >= 1200) { // Every minute
				cleanupCounter[0] = 0;
				com.focustimershop.luckychest.LuckyChestManager.cleanupOldIdempotencyRecords();
			}
		});

		// Handle server shutdown - clear all data
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			TimerManager.clearAll();
			com.focustimershop.rental.RentalManager.clearAll();
			com.focustimershop.profile.ProfileManager.clearAll();
		});

		// Handle player join
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			EconomyManager.onPlayerJoin(handler.player);
			// Send shop data to client so they can view items
			ModNetworking.sendShopData(handler.player);
			// Load player profile (v1.0.6)
			com.focustimershop.profile.ProfileManager.onPlayerJoin(handler.player);
			// PHASE 2: Restore saved timer
			TimerManager.onPlayerJoin(handler.player);
		});

		// Handle player disconnect
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			EconomyManager.onPlayerDisconnect(handler.player);
			// PHASE 2: Save timer before disconnect
			TimerManager.onPlayerDisconnect(handler.player);
			com.focustimershop.profile.ProfileManager.onPlayerDisconnect(handler.player);
			// PHASE 4: Remove expired rental tools on logout (BUG #19 fix)
			com.focustimershop.rental.RentalManager.checkPlayerInventoryForExpiredTools(handler.player);
			LOGGER.debug("PHASE4_RENTAL: Checked expired tools for {} on logout", handler.player.getName().getString());
		});
		
		// ===== PER-WORLD ECONOMY: Handle dimension change =====
		net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			// This fires when player changes dimension or respawns
			// Check if dimension actually changed
			if (!oldPlayer.getWorld().getRegistryKey().equals(newPlayer.getWorld().getRegistryKey())) {
				// Save old world's economy (oldPlayer is about to be discarded)
				EconomyManager.savePlayerData(oldPlayer);
				
				// Load and sync new world's economy
				EconomyManager.onPlayerChangeDimension(newPlayer);
			}
		});
		// ======================================================

		LOGGER.info("Focus Timer Shop initialized successfully");
	}
}
