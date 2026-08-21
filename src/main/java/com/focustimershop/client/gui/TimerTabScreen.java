package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Timer tab - handles Pomodoro, Stopwatch, and Countdown timers
 * v1.0.7-beta: Added drag-to-adjust time + To-Do List
 */
public class TimerTabScreen {
	private final MainMenuScreen parent;
	private TimerMode selectedMode = TimerMode.POMODORO;
	private int pomodoroMinutes = 25;
	private int countdownMinutes = 25;
	
	// Drag-to-adjust time
	private boolean isDraggingTime = false;
	private double dragStartY = 0;
	private int dragStartValue = 0;
	private static final double DRAG_SENSITIVITY = 0.5; // pixels per minute
	
	// To-Do List
	private TextFieldWidget todoInputField = null;
	private List<TodoItem> todoItems = new ArrayList<>();
	private static final int MAX_TODO_ITEMS = 10;

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
			renderActiveTimer(context, x, y + 100, width, height - 100, mouseX, mouseY);
		} else {
			renderTimerSetup(context, x, y + 100, width, height - 100, mouseX, mouseY);
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

	private void renderTimerSetup(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		String info = "";
		
		switch (selectedMode) {
			case POMODORO:
				info = "Focus for " + pomodoroMinutes + " minutes";
				context.drawText(parent.getTextRenderer(), "Set duration (drag up/down to adjust):", x + 20, y + 20, 0xFFAAAAAA, false);
				
				// Draggable time display
				int timeY = y + 45;
				int timeBoxWidth = 150;
				int timeBoxHeight = 50;
				boolean hovered = mouseX >= x + 20 && mouseX <= x + 20 + timeBoxWidth && 
				                  mouseY >= timeY && mouseY <= timeY + timeBoxHeight;
				
				int bgColor = hovered || isDraggingTime ? 0xFF4A5A6A : 0xFF2A2A2A;
				context.fill(x + 20, timeY, x + 20 + timeBoxWidth, timeY + timeBoxHeight, bgColor);
				
				context.drawCenteredTextWithShadow(parent.getTextRenderer(), 
					"§l" + pomodoroMinutes + " min", 
					x + 20 + timeBoxWidth / 2, timeY + 20, 0xFFFFFFFF);
				
				if (hovered) {
					context.drawText(parent.getTextRenderer(), "§7(drag)", x + 25, timeY + 5, 0xFF888888, false);
				}
				break;
			case STOPWATCH:
				info = "Count up from zero - stop when ready";
				break;
			case COUNTDOWN:
				info = "Count down " + countdownMinutes + " minutes";
				context.drawText(parent.getTextRenderer(), "Set duration (drag up/down to adjust):", x + 20, y + 20, 0xFFAAAAAA, false);
				
				// Draggable time display
				int cdTimeY = y + 45;
				int cdTimeBoxWidth = 150;
				int cdTimeBoxHeight = 50;
				boolean cdHovered = mouseX >= x + 20 && mouseX <= x + 20 + cdTimeBoxWidth && 
				                    mouseY >= cdTimeY && mouseY <= cdTimeY + cdTimeBoxHeight;
				
				int cdBgColor = cdHovered || isDraggingTime ? 0xFF4A5A6A : 0xFF2A2A2A;
				context.fill(x + 20, cdTimeY, x + 20 + cdTimeBoxWidth, cdTimeY + cdTimeBoxHeight, cdBgColor);
				
				context.drawCenteredTextWithShadow(parent.getTextRenderer(), 
					"§l" + countdownMinutes + " min", 
					x + 20 + cdTimeBoxWidth / 2, cdTimeY + 20, 0xFFFFFFFF);
				
				if (cdHovered) {
					context.drawText(parent.getTextRenderer(), "§7(drag)", x + 25, cdTimeY + 5, 0xFF888888, false);
				}
				break;
		}

		context.drawText(parent.getTextRenderer(), info, x + 20, y, 0xFFFFFFFF, false);

		// Start button
		int btnY = y + 120;
		context.fill(x + 20, btnY, x + 120, btnY + 35, 0xFF4A9EFF);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "START", x + 70, btnY + 12, 0xFFFFFFFF);
		
		// To-Do List section
		int todoY = y + 180;
		renderTodoList(context, x + 20, todoY, width - 40, height - 200, mouseX, mouseY);
	}

	private void renderActiveTimer(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
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
	
	private void renderTodoList(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Title
		context.drawText(parent.getTextRenderer(), "§lTo-Do List", x, y, 0xFFFFFFFF, true);
		
		// Input field
		int inputY = y + 20;
		if (todoInputField == null) {
			todoInputField = new TextFieldWidget(parent.getTextRenderer(), x, inputY, width - 10, 18, Text.literal(""));
			todoInputField.setMaxLength(100);
			todoInputField.setPlaceholder(Text.literal("Add a task... (press Enter)"));
		}
		todoInputField.setPosition(x, inputY);
		todoInputField.setWidth(width - 10);
		todoInputField.render(context, mouseX, mouseY, 0);
		
		// Todo items
		int itemY = inputY + 30;
		int lineHeight = 20;
		for (int i = 0; i < todoItems.size(); i++) {
			TodoItem item = todoItems.get(i);
			int currentY = itemY + i * lineHeight;
			
			// Checkbox
			int checkboxSize = 12;
			int checkboxX = x + 5;
			boolean checkboxHovered = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
			                          mouseY >= currentY && mouseY <= currentY + checkboxSize;
			
			int checkboxColor = checkboxHovered ? 0xFF5A5A5A : 0xFF3A3A3A;
			context.fill(checkboxX, currentY, checkboxX + checkboxSize, currentY + checkboxSize, checkboxColor);
			
			// Check mark if completed
			if (item.completed) {
				context.drawText(parent.getTextRenderer(), "§a✓", checkboxX + 2, currentY + 2, 0xFF00FF00, false);
			}
			
			// Task text
			int textX = checkboxX + checkboxSize + 5;
			String displayText = item.text;
			
			if (item.completed) {
				// Strikethrough effect with dimmed color
				context.drawText(parent.getTextRenderer(), "§8§m" + displayText, textX, currentY + 2, 0xFF666666, false);
			} else {
				// Normal bright text
				context.drawText(parent.getTextRenderer(), displayText, textX, currentY + 2, 0xFFFFFFFF, false);
			}
		}
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
		// Check todo input field first
		if (todoInputField != null && todoInputField.mouseClicked(mouseX, mouseY, button)) {
			todoInputField.setFocused(true);
			return true;
		}
		
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
			// Check if clicking on draggable time display
			int setupY = y + 100;
			if (selectedMode == TimerMode.POMODORO || selectedMode == TimerMode.COUNTDOWN) {
				int timeY = setupY + 45;
				int timeBoxWidth = 150;
				int timeBoxHeight = 50;
				
				if (mouseX >= x + 20 && mouseX <= x + 20 + timeBoxWidth && 
				    mouseY >= timeY && mouseY <= timeY + timeBoxHeight) {
					isDraggingTime = true;
					dragStartY = mouseY;
					dragStartValue = (selectedMode == TimerMode.POMODORO) ? pomodoroMinutes : countdownMinutes;
					return true;
				}
			}
			
			// Start button
			int btnY = y + 100 + 120;
			if (mouseX >= x + 20 && mouseX <= x + 120 && mouseY >= btnY && mouseY <= btnY + 35) {
				startTimer();
				return true;
			}
			
			// To-Do list checkboxes
			int todoY = y + 100 + 180;
			int inputY = todoY + 20;
			int itemY = inputY + 30;
			int lineHeight = 20;
			int checkboxSize = 12;
			int checkboxX = x + 25;
			
			for (int i = 0; i < todoItems.size(); i++) {
				int currentY = itemY + i * lineHeight;
				if (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
				    mouseY >= currentY && mouseY <= currentY + checkboxSize) {
					todoItems.get(i).completed = !todoItems.get(i).completed;
					return true;
				}
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
				// PHASE 4: BUG #23 FIX - Proper abandoned detection
				// Abandoned if: too short (<60s) OR didn't complete target
				int elapsedSeconds = ClientDataCache.getElapsedSeconds();
				int targetSeconds = ClientDataCache.getTargetSeconds();
				TimerType type = ClientDataCache.getCurrentTimerType();
				
				boolean tooShort = elapsedSeconds < 60;
				boolean incompleteTarget = (type != TimerType.STOPWATCH && targetSeconds > 0 && elapsedSeconds < targetSeconds);
				boolean abandoned = tooShort || incompleteTarget;
				
				System.out.println("[DEBUG-TIMER-STOP] Elapsed: " + elapsedSeconds + "s, target: " + targetSeconds + "s, abandoned: " + abandoned);
				ModNetworking.sendTimerStop(abandoned);
				return true;
			}
		}

		return false;
	}
	
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (isDraggingTime) {
			double dragDelta = dragStartY - mouseY;
			int minutesDelta = (int)(dragDelta * DRAG_SENSITIVITY);
			
			int newValue = Math.max(1, Math.min(120, dragStartValue + minutesDelta));
			
			if (selectedMode == TimerMode.POMODORO) {
				pomodoroMinutes = newValue;
			} else if (selectedMode == TimerMode.COUNTDOWN) {
				countdownMinutes = newValue;
			}
			
			return true;
		}
		return false;
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingTime) {
			isDraggingTime = false;
			return true;
		}
		return false;
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Handle todo input field
		if (todoInputField != null && todoInputField.isFocused()) {
			if (keyCode == 257 || keyCode == 335) { // Enter or NumpadEnter
				String text = todoInputField.getText().trim();
				if (!text.isEmpty() && todoItems.size() < MAX_TODO_ITEMS) {
					todoItems.add(new TodoItem(text));
					todoInputField.setText("");
				}
				return true;
			}
			return todoInputField.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}
	
	public boolean charTyped(char chr, int modifiers) {
		if (todoInputField != null && todoInputField.isFocused()) {
			return todoInputField.charTyped(chr, modifiers);
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
	
	static class TodoItem {
		String text;
		boolean completed;
		
		TodoItem(String text) {
			this.text = text;
			this.completed = false;
		}
	}
}
