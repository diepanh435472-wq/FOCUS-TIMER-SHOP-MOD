package com.focustimershop.client;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Renders rental tool HUD overlay
 */
public class RentalHudRenderer implements HudRenderCallback {
	
	@Override
	public void onHudRender(DrawContext context, float tickDelta) {
		if (!ClientRentalCache.hasActiveRental()) {
			return;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.debugEnabled) {
			return;
		}
		
		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		
		// Display rental info below hotbar (centered)
		String timeText = "§dRental: §f" + ClientRentalCache.getFormattedTime();
		int textWidth = client.textRenderer.getWidth(timeText);
		int x = (screenWidth - textWidth) / 2;
		int y = screenHeight - 70; // Above hotbar
		
		// Background
		context.fill(x - 5, y - 2, x + textWidth + 5, y + 10, 0x80000000);
		
		// Text
		context.drawText(client.textRenderer, timeText, x, y, 0xFFFFFFFF, true);
		
		// Warning if less than 60 seconds
		int remaining = ClientRentalCache.getRemainingSeconds();
		if (remaining <= 60 && remaining > 0) {
			String warning = "§c⚠ Expiring soon!";
			int warnWidth = client.textRenderer.getWidth(warning);
			int warnX = (screenWidth - warnWidth) / 2;
			context.drawText(client.textRenderer, warning, warnX, y + 12, 0xFFFF4444, true);
		}
	}
}
