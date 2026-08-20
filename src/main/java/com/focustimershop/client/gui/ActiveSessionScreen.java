package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;
import net.minecraft.client.gui.DrawContext;

/**
 * v1.0.7-beta Timer UI Overhaul - Active session screen
 * Displays running timer with End Session button (Pause button hidden per spec)
 */
public class ActiveSessionScreen {
	private final TimerTabScreenV2 parent;

	public ActiveSessionScreen(TimerTabScreenV2 parent) {
		this.parent = parent;
	}

	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		TimerType type = ClientDataCache.getCurrentTimerType();
		TimerState state = ClientDataCache.getCurrentTimerState();
		int elapsed = ClientDataCache.getElapsedSeconds();
		int target = ClientDataCache.getTargetSeconds();

		// Title
		context.drawText(parent.getTextRenderer(), getTimerLabel(type), x + 10, y + 10, 0xFF4A9EFF, true);

		// Timer display (large centered)
		String timeDisplay = formatTime(elapsed);
		if (type != TimerType.STOPWATCH) {
			int remaining = Math.max(0, target - elapsed);
			timeDisplay = formatTime(remaining);
		}

		int timerX = x + width / 2;
		int timerY = y + height / 3;
		
		// Scale up the time text (draw it bigger)
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), timeDisplay, timerX, timerY, 0xFFFFFFFF);

		// State info
		String stateText = state == TimerState.RUNNING ? "§a● Đang chạy - Game bị tạm dừng" : "§e● Tạm ngưng";
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), stateText, timerX, timerY + 30, 0xFFFFFFFF);

		// Progress bar for countdown
		if (type != TimerType.STOPWATCH && target > 0) {
			int barWidth = width - 100;
			int barX = x + 50;
			int barY = timerY + 70;
			float progress = Math.min(1.0f, (float) elapsed / target);
			
			context.fill(barX, barY, barX + barWidth, barY + 10, 0xFF2A2A2A);
			context.fill(barX, barY, barX + (int)(barWidth * progress), barY + 10, 0xFF4A9EFF);
		}

		// Rewards info
		int rewardY = y + 50;
		int earnedSilver = elapsed / 45;
		int earnedXp = elapsed / 90;
		context.drawText(parent.getTextRenderer(), "§eĐã kiếm: " + earnedSilver + " Silver, " + earnedXp + " XP", 
			x + 20, rewardY, 0xFFFFAA00, false);
		
		// v1.0.7-beta: Stopwatch mission cap warning
		if (type == TimerType.STOPWATCH && elapsed >= 7200) { // 120 minutes = 7200 seconds
			context.drawText(parent.getTextRenderer(), "⚠ Đã vượt 120 phút - nhiệm vụ không tính thêm", 
				x + 20, rewardY + 20, 0xFFFF4444, false);
		}

		// v1.0.7-beta: Only "End Session" button - NO PAUSE BUTTON
		int btnY = y + height - 80;
		int btnWidth = 140;
		int btnX = x + width / 2 - btnWidth / 2;

		context.fill(btnX, btnY, btnX + btnWidth, btnY + 40, 0xFFFF4444);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "KẾT THÚC PHIÊN", 
			btnX + btnWidth / 2, btnY + 14, 0xFFFFFFFF);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
		int x = contentX;
		int y = contentY;
		int width = contentWidth;
		int height = contentHeight;

		// End Session button
		int btnY = y + height - 80;
		int btnWidth = 140;
		int btnX = x + width / 2 - btnWidth / 2;
		
		if (mouseX >= btnX && mouseX <= btnX + btnWidth && 
		    mouseY >= btnY && mouseY <= btnY + 40) {
			// Determine if abandoned
			int elapsedSeconds = ClientDataCache.getElapsedSeconds();
			int targetSeconds = ClientDataCache.getTargetSeconds();
			TimerType type = ClientDataCache.getCurrentTimerType();
			
			boolean tooShort = elapsedSeconds < 60;
			boolean incompleteTarget = (type != TimerType.STOPWATCH && targetSeconds > 0 && elapsedSeconds < targetSeconds);
			boolean abandoned = tooShort || incompleteTarget;
			
			ModNetworking.sendTimerStop(abandoned);
			return true;
		}

		return false;
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

	private String getTimerLabel(TimerType type) {
		switch (type) {
			case STOPWATCH:
				return "Bấm Giờ";
			case POMODORO_FOCUS:
				return "Tập Trung";
			case POMODORO_SHORT_BREAK:
				return "Nghỉ Ngắn";
			case POMODORO_LONG_BREAK:
				return "Nghỉ Dài";
			case COUNTDOWN:
			default:
				return "Đếm Ngược";
		}
	}
}
