package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

/**
 * Timer tab - handles Pomodoro, Stopwatch, and Countdown timers
 */
public class TimerTabScreen {
	private final MainMenuScreen parent;
	private TimerMode selectedMode = TimerMode.POMODORO;
	private int pomodoroMinutes = 25;
	private int countdownMinutes = 25;

	public TimerTabScreen(MainMenuScreen parent) {
		this.parent = parent;
	}

	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// Title
		context.drawText(parent.getTextRenderer(), "Focus Timer", x + 10, y + 10, 0xFFFFFFFF, true);

		// Timer mode selector
		int modeY = y + 40;
		renderModeButtons(context, x + 10, modeY);

		// Display based on current state
		if (ClientDataCache.hasActiveTimer()) {
			renderActiveTimer(context, x, y + 100, width, height - 100);
		} else {
			renderTimerSetup(context, x, y + 100, width, height - 100);
		}
	}

	private void renderModeButtons(DrawContext context, int x, int y) {
		String[] modes = {"Pomodoro", "Stopwatch", "Countdown"};
		int buttonWidth = 100;
		int spacing = 10;

		for (int i = 0; i < modes.length; i++) {
			int btnX = x + i * (buttonWidth + spacing);
			boolean selected = (i == selectedMode.ordinal());
			
			int color = selected ? 0xFF4A9EFF : 0xFF2A2A2A;
			context.fill(btnX, y, btnX + buttonWidth, y + 30, color);
			
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), modes[i], 
				btnX + buttonWidth / 2, y + 10, 0xFFFFFFFF);
		}
	}

	private void renderTimerSetup(DrawContext context, int x, int y, int width, int height) {
		String info = "";
		
		switch (selectedMode) {
			case POMODORO:
				info = "Focus for " + pomodoroMinutes + " minutes";
				context.drawText(parent.getTextRenderer(), "Set duration (1-120 min):", x + 20, y + 20, 0xFFAAAAAA, false);
				context.drawText(parent.getTextRenderer(), pomodoroMinutes + " minutes", x + 20, y + 40, 0xFFFFFFFF, true);
				break;
			case STOPWATCH:
				info = "Count up from zero - stop when ready";
				break;
			case COUNTDOWN:
				info = "Count down " + countdownMinutes + " minutes";
				context.drawText(parent.getTextRenderer(), "Set duration (1-120 min):", x + 20, y + 20, 0xFFAAAAAA, false);
				context.drawText(parent.getTextRenderer(), countdownMinutes + " minutes", x + 20, y + 40, 0xFFFFFFFF, true);
				break;
		}

		context.drawText(parent.getTextRenderer(), info, x + 20, y, 0xFFFFFFFF, false);

		// Start button
		int btnY = y + height - 50;
		context.fill(x + 20, btnY, x + 120, btnY + 35, 0xFF4A9EFF);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "START", x + 70, btnY + 12, 0xFFFFFFFF);
	}

	private void renderActiveTimer(DrawContext context, int x, int y, int width, int height) {
		TimerType type = ClientDataCache.getCurrentTimerType();
		TimerState state = ClientDataCache.getCurrentTimerState();
		int elapsed = ClientDataCache.getElapsedSeconds();
		int target = ClientDataCache.getTargetSeconds();

		// Timer display
		String timeDisplay = formatTime(elapsed);
		if (type != TimerType.STOPWATCH) {
			int remaining = Math.max(0, target - elapsed);
			timeDisplay = formatTime(remaining);
		}

		// Large timer text
		int timerX = x + width / 2;
		int timerY = y + height / 3;
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), timeDisplay, timerX, timerY, 0xFF4A9EFF);

		// State info
		String stateText = state == TimerState.RUNNING ? "§aRunning - Game Paused" : "§ePaused";
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), stateText, timerX, timerY + 30, 0xFFFFFFFF);

		// Progress bar for countdown/pomodoro
		if (type != TimerType.STOPWATCH && target > 0) {
			int barWidth = width - 100;
			int barX = x + 50;
			int barY = timerY + 60;
			float progress = Math.min(1.0f, (float) elapsed / target);
			
			context.fill(barX, barY, barX + barWidth, barY + 10, 0xFF2A2A2A);
			context.fill(barX, barY, barX + (int)(barWidth * progress), barY + 10, 0xFF4A9EFF);
		}

		// Control buttons
		int btnY = y + height - 80;
		int btnWidth = 100;
		int spacing = 20;
		int startX = x + width / 2 - (btnWidth * 2 + spacing) / 2;

		if (state == TimerState.RUNNING) {
			// Pause button
			context.fill(startX, btnY, startX + btnWidth, btnY + 35, 0xFFFFAA00);
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), "PAUSE", 
				startX + btnWidth / 2, btnY + 12, 0xFFFFFFFF);
		} else {
			// Resume button
			context.fill(startX, btnY, startX + btnWidth, btnY + 35, 0xFF4A9EFF);
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), "RESUME", 
				startX + btnWidth / 2, btnY + 12, 0xFFFFFFFF);
		}

		// Stop/Abandon button
		int stopX = startX + btnWidth + spacing;
		context.fill(stopX, btnY, stopX + btnWidth, btnY + 35, 0xFFFF4444);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "ABANDON", 
			stopX + btnWidth / 2, btnY + 12, 0xFFFFFFFF);

		// Rewards info
		int rewardY = y + 20;
		int earnedSilver = elapsed / 45;
		int earnedXp = elapsed / 90;
		context.drawText(parent.getTextRenderer(), "§eEarned so far: " + earnedSilver + " Silver, " + earnedXp + " XP", 
			x + 20, rewardY, 0xFFFFAA00, false);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
		// Handle button clicks
		int x = contentX;
		int y = contentY;

		// Mode selection buttons
		int modeY = y + 40;
		int buttonWidth = 100;
		int spacing = 10;
		
		for (int i = 0; i < 3; i++) {
			int btnX = x + 10 + i * (buttonWidth + spacing);
			if (mouseX >= btnX && mouseX <= btnX + buttonWidth && mouseY >= modeY && mouseY <= modeY + 30) {
				if (!ClientDataCache.hasActiveTimer()) {
					selectedMode = TimerMode.values()[i];
					return true;
				}
			}
		}

		if (!ClientDataCache.hasActiveTimer()) {
			// Start button
			int btnY = y + contentHeight - 50;
			if (mouseX >= x + 20 && mouseX <= x + 120 && mouseY >= btnY && mouseY <= btnY + 35) {
				startTimer();
				return true;
			}
		} else {
			// Control buttons when timer is active
			TimerState state = ClientDataCache.getCurrentTimerState();
			int btnY = y + contentHeight - 80;
			int btnWidth = 100;
			int btnSpacing = 20;
			int startX = x + contentWidth / 2 - (btnWidth * 2 + btnSpacing) / 2;

			// Pause/Resume button
			if (mouseX >= startX && mouseX <= startX + btnWidth && mouseY >= btnY && mouseY <= btnY + 35) {
				if (state == TimerState.RUNNING) {
					// Optimistic update - pause immediately on client
					ClientDataCache.setTimerStateOptimistic(TimerState.PAUSED);
					ModNetworking.sendTimerPause();
				} else {
					// Optimistic update - resume immediately on client
					ClientDataCache.setTimerStateOptimistic(TimerState.RUNNING);
					ModNetworking.sendTimerResume();
				}
				return true;
			}

			// Stop button
			int stopX = startX + btnWidth + btnSpacing;
			if (mouseX >= stopX && mouseX <= stopX + btnWidth && mouseY >= btnY && mouseY <= btnY + 35) {
				// Only mark as abandoned if timer ran for less than 60 seconds
				int elapsedSeconds = ClientDataCache.getElapsedSeconds();
				boolean abandoned = elapsedSeconds < 60;
				
				System.out.println("[DEBUG-TIMER-STOP] Elapsed: " + elapsedSeconds + "s, abandoned: " + abandoned);
				ModNetworking.sendTimerStop(abandoned);
				return true;
			}
		}

		return false;
	}

	private void startTimer() {
		TimerType type;
		int targetSeconds = 0;

		switch (selectedMode) {
			case POMODORO:
				type = TimerType.POMODORO_FOCUS;
				targetSeconds = pomodoroMinutes * 60;
				break;
			case STOPWATCH:
				type = TimerType.STOPWATCH;
				targetSeconds = 0; // No limit
				break;
			case COUNTDOWN:
				type = TimerType.COUNTDOWN;
				targetSeconds = countdownMinutes * 60;
				break;
			default:
				return;
		}

		ModNetworking.sendTimerStart(type, targetSeconds);
	}

	private String formatTime(int totalSeconds) {
		int hours = totalSeconds / 3600;
		int minutes = (totalSeconds % 3600) / 60;
		int seconds = totalSeconds % 60;

		if (hours > 0) {
			return String.format("%02d:%02d:%02d", hours, minutes, seconds);
		} else {
			return String.format("%02d:%02d", minutes, seconds);
		}
	}

	enum TimerMode {
		POMODORO,
		STOPWATCH,
		COUNTDOWN
	}
}
