package com.focustimershop.client;

import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Renders timer HUD overlay with focus lock indicator
 */
public class TimerHudRenderer implements HudRenderCallback {
	
	@Override
	public void onHudRender(DrawContext context, float tickDelta) {
		if (!ClientDataCache.hasActiveTimer()) {
			return;
		}
		
		MinecraftClient client = MinecraftClient.getInstance();
		if (client.player == null || client.options.debugEnabled) {
			return;
		}
		
		int screenWidth = context.getScaledWindowWidth();
		int screenHeight = context.getScaledWindowHeight();
		
		TimerType type = ClientDataCache.getCurrentTimerType();
		TimerState state = ClientDataCache.getCurrentTimerState();
		int elapsed = ClientDataCache.getElapsedSeconds();
		int target = ClientDataCache.getTargetSeconds();
		
		// Format time display
		String timeText = formatTime(elapsed);
		if (type != TimerType.STOPWATCH) {
			int remaining = Math.max(0, target - elapsed);
			timeText = formatTime(remaining);
		}
		
		// Timer display (top center)
		String timerLabel = getTimerLabel(type);
		String fullText = "§6" + timerLabel + ": §f" + timeText;
		int textWidth = client.textRenderer.getWidth(fullText);
		int x = (screenWidth - textWidth) / 2;
		int y = 10;
		
		// Background
		int bgColor = state == TimerState.PAUSED ? 0x80444444 : 0x80001100;
		context.fill(x - 5, y - 2, x + textWidth + 5, y + 10, bgColor);
		
		// Text
		context.drawText(client.textRenderer, fullText, x, y, 0xFFFFFFFF, true);
		
		// Focus lock indicator when running
		if (state == TimerState.RUNNING) {
			String lockText = "§c⚠ FOCUS MODE: Game actions locked";
			int lockWidth = client.textRenderer.getWidth(lockText);
			int lockX = (screenWidth - lockWidth) / 2;
			int lockY = y + 12;
			
			// Pulsing effect
			long time = System.currentTimeMillis();
			int alpha = (int) (128 + 64 * Math.sin(time / 500.0));
			int bgAlpha = (alpha << 24) | 0x440000;
			
			context.fill(lockX - 3, lockY - 1, lockX + lockWidth + 3, lockY + 9, bgAlpha);
			context.drawText(client.textRenderer, lockText, lockX, lockY, 0xFFFF4444, true);
			
			// Hint text
			String hintText = "§7Press M to pause";
			int hintWidth = client.textRenderer.getWidth(hintText);
			int hintX = (screenWidth - hintWidth) / 2;
			context.drawText(client.textRenderer, hintText, hintX, lockY + 12, 0xFFAAAAAA, false);
		} else if (state == TimerState.PAUSED) {
			String pausedText = "§e⏸ PAUSED - Press M to resume";
			int pausedWidth = client.textRenderer.getWidth(pausedText);
			int pausedX = (screenWidth - pausedWidth) / 2;
			context.drawText(client.textRenderer, pausedText, pausedX, y + 12, 0xFFFFAA00, true);
		}
	}
	
	private String getTimerLabel(TimerType type) {
		switch (type) {
			case STOPWATCH:
				return "Stopwatch";
			case POMODORO_FOCUS:
				return "Pomodoro Focus";
			case POMODORO_SHORT_BREAK:
				return "Short Break";
			case POMODORO_LONG_BREAK:
				return "Long Break";
			case COUNTDOWN:
				return "Countdown";
			default:
				return "Timer";
		}
	}
	
	private String formatTime(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		
		if (hours > 0) {
			return String.format("%d:%02d:%02d", hours, minutes, secs);
		} else {
			return String.format("%d:%02d", minutes, secs);
		}
	}
}
