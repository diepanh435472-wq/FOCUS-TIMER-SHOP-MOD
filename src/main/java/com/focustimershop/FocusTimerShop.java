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
		
		// Initialize rental system
		com.focustimershop.rental.RentalManager.initialize();

		// Initialize networking
		ModNetworking.registerServerPackets();

		// Register server tick handler for timer updates
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			TimerManager.tick(server);
			com.focustimershop.rental.RentalManager.tick();
		});

		// Handle server shutdown - clear all data
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> {
			TimerManager.clearAll();
			com.focustimershop.rental.RentalManager.clearAll();
		});

		// Handle player join
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			EconomyManager.onPlayerJoin(handler.player);
			// Send shop data to client so they can view items
			ModNetworking.sendShopData(handler.player);
		});

		// Handle player disconnect
		net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
			EconomyManager.onPlayerDisconnect(handler.player);
			TimerManager.onPlayerDisconnect(handler.player);
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
