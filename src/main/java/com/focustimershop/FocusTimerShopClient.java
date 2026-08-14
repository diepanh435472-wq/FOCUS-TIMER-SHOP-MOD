package com.focustimershop;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.client.gui.MainMenuScreen;
import com.focustimershop.network.ModNetworking;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class FocusTimerShopClient implements ClientModInitializer {
	
	public static KeyBinding openMenuKey;

	@Override
	public void onInitializeClient() {
		FocusTimerShop.LOGGER.info("[CLIENT INIT] Starting Focus Timer Shop Client initialization");

		try {
			// Initialize Music Player
			FocusTimerShop.LOGGER.info("[CLIENT INIT] Initializing Music Player");
			com.focustimershop.music.MusicPlayerManager.initialize();
			
			// DISABLED: Client-side lore updater (causes flickering)
			// Server updates NBT directly, client just displays it
			// com.focustimershop.client.RentalToolLoreUpdater.register();
			
			// Register keybinding - Right Shift
			FocusTimerShop.LOGGER.info("[CLIENT INIT] Registering keybinding");
			openMenuKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
				"key.focustimershop.open_menu",
				InputUtil.Type.KEYSYM,
				GLFW.GLFW_KEY_RIGHT_SHIFT,
				"category.focustimershop.general"
			));

			// Register client-side packet handlers
			FocusTimerShop.LOGGER.info("[CLIENT INIT] Registering client packets");
			ModNetworking.registerClientPackets();
			
			// Register HUD renderer for rental info
			FocusTimerShop.LOGGER.info("[CLIENT INIT] Registering HUD renderer");
			net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				new com.focustimershop.client.RentalHudRenderer()
			);
			
			// Register HUD renderer for timer with focus lock indicator
			net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback.EVENT.register(
				new com.focustimershop.client.TimerHudRenderer()
			);

			// Handle keybinding
			FocusTimerShop.LOGGER.info("[CLIENT INIT] Registering tick events");
			ClientTickEvents.END_CLIENT_TICK.register(client -> {
				// Open main menu with Right Shift
				while (openMenuKey.wasPressed()) {
					if (client.player != null && client.currentScreen == null) {
						client.setScreen(new MainMenuScreen());
					}
				}
				
				// Close inventory/container screens when timer is running
				if (ClientDataCache.isGameFrozen() && client.currentScreen != null) {
					// Only close screens that are player-controllable (inventory, chests, etc)
					if (client.currentScreen instanceof net.minecraft.client.gui.screen.ingame.HandledScreen) {
						// Don't close MainMenuScreen or other mod screens
						if (!(client.currentScreen instanceof MainMenuScreen)) {
							client.setScreen(null);
						}
					}
				}
			});

			FocusTimerShop.LOGGER.info("[CLIENT INIT] Focus Timer Shop Client initialized successfully");
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("[CLIENT INIT] Failed to initialize client", e);
			throw e;
		}
	}
}
