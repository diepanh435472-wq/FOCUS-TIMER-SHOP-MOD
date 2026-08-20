package com.focustimershop.client.gui;

import com.focustimershop.network.ModNetworking;
import com.focustimershop.timer.ClockMode;
import com.focustimershop.timer.SessionCategory;
import net.minecraft.client.gui.DrawContext;

import java.util.ArrayList;
import java.util.List;

/**
 * v1.0.7-beta Timer UI Overhaul - Clock configuration screen
 */
public class ClockConfigScreen {
	private final TimerTabScreenV2 parent;
	private final SessionCategory category;
	private ClockMode clockMode = ClockMode.COUNTDOWN;
	
	private int hours = 0;
	private int minutes = 25;
	
	private String encouragementNote = "";
	private boolean showingNoteInput = false;
	
	private List<String> todoList = new ArrayList<>();
	private boolean showingTodoPopup = false;

	public ClockConfigScreen(TimerTabScreenV2 parent, SessionCategory category) {
		this.parent = parent;
		this.category = category;
	}
	
	public boolean isShowingPopup() {
		return showingNoteInput || showingTodoPopup;
	}

	public void renderContent(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// If popups are open, ONLY render the popup (don't render normal content)
		if (showingNoteInput) {
			renderNoteInputPopup(context);
			return;
		}
		if (showingTodoPopup) {
			renderTodoPopup(context);
			return;
		}
		
		// Normal content (only rendered when no popup is open)
		// Time picker at top
		int pickerY = y + 10;
		if (clockMode == ClockMode.COUNTDOWN) {
			renderCountdownPicker(context, x, pickerY, width);
		} else {
			renderStopwatchInfo(context, x, pickerY, width);
		}
		
		// Mode toggle on right
		int toggleX = x + width / 2 + 100;
		int toggleY = pickerY + 30;
		renderModeToggle(context, toggleX, toggleY);
		
		// Note + Todo buttons
		int noteY = y + 150;
		context.fill(x + 20, noteY, x + width - 70, noteY + 35, 0xFF2A2A2A);
		String noteLabel = encouragementNote.isEmpty() ? 
			"Một Vài Lời Tiếp Thêm Năng Lượng" : 
			encouragementNote.substring(0, Math.min(35, encouragementNote.length()));
		context.drawText(parent.getTextRenderer(), noteLabel, x + 30, noteY + 12, 0xFFAAAAAA, false);
		
		int vButtonX = x + width - 60;
		context.fill(vButtonX, noteY, vButtonX + 40, noteY + 35, 0xFF2A2A2A);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "V", 
			vButtonX + 20, noteY + 12, 0xFFAAAAAA);
		
		// Start button
		int startBtnY = y + height - 60;
		int startBtnX = x + width / 2 - 80;
		context.fill(startBtnX, startBtnY, startBtnX + 160, startBtnY + 45, 0xFF4AFF4A);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "BẮT ĐẦU", 
			startBtnX + 80, startBtnY + 16, 0xFF000000);
	}
	
	private void renderModeToggle(DrawContext context, int x, int y) {
		int btnWidth = 50;
		int btnHeight = 80;
		context.fill(x, y, x + btnWidth, y + btnHeight, 0xFF4A9EFF);
		
		String modeText = clockMode == ClockMode.COUNTDOWN ? "⏱" : "⏲";
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), modeText, 
			x + btnWidth / 2, y + 15, 0xFFFFFFFF);
		
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "↑", 
			x + btnWidth / 2, y + 35, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "↓", 
			x + btnWidth / 2, y + 55, 0xFFFFFFFF);
	}
	
	private void renderCountdownPicker(DrawContext context, int x, int y, int width) {
		int centerX = x + width / 2 - 80;
		
		int hoursX = centerX - 40;
		renderTimeUnitWheel(context, hoursX, y, hours, 23);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Giờ", 
			hoursX + 25, y + 95, 0xFFAAAAAA);
		
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), ":", 
			centerX + 35, y + 40, 0xFFFFFFFF);
		
		int minsX = centerX + 60;
		renderTimeUnitWheel(context, minsX, y, minutes, 59);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Phút", 
			minsX + 25, y + 95, 0xFFAAAAAA);
	}
	
	private void renderTimeUnitWheel(DrawContext context, int x, int y, int currentValue, int maxValue) {
		int unitWidth = 50;
		int rowHeight = 25;
		
		int prevValue = currentValue > 0 ? currentValue - 1 : maxValue;
		String prevStr = String.format("%02d", prevValue);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), prevStr, 
			x + unitWidth / 2, y + 5, 0xFF666666);
		
		context.fill(x - 5, y + rowHeight - 2, x + unitWidth + 5, y + rowHeight * 2 + 2, 0xFF4A9EFF);
		String currentStr = String.format("%02d", currentValue);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), currentStr, 
			x + unitWidth / 2, y + rowHeight + 5, 0xFFFFFFFF);
		
		int nextValue = currentValue < maxValue ? currentValue + 1 : 0;
		String nextStr = String.format("%02d", nextValue);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), nextStr, 
			x + unitWidth / 2, y + rowHeight * 2 + 5, 0xFF666666);
	}
	
	private void renderStopwatchInfo(DrawContext context, int x, int y, int width) {
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Bấm giờ từ 00:00", 
			x + width / 2, y + 20, 0xFFFFFFFF);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "(Dừng khi bạn muốn)", 
			x + width / 2, y + 40, 0xFFAAAAAA);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "⚠ 120 phút đầu được tính vào nhiệm vụ", 
			x + width / 2, y + 70, 0xFFFFAA00);
	}
	
	private void renderNoteInputPopup(DrawContext context) {
		// Get actual screen dimensions
		int screenWidth = parent.getParent().width;
		int screenHeight = parent.getParent().height;
		
		// NOTE: Dim overlay is now drawn by MainMenuScreen BEFORE this is called
		// This ensures it covers the sidebar too
		
		// Center modal on screen
		int popupWidth = 300;
		int popupHeight = 150;
		int popupX = (screenWidth - popupWidth) / 2;
		int popupY = (screenHeight - popupHeight) / 2;
		
		// Modal box with border effect (matching Lucky Chest style)
		context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF1A1A1A);
		context.fill(popupX + 2, popupY + 2, popupX + popupWidth - 2, popupY + popupHeight - 2, 0xFF2A2A2A);
		
		// Title
		context.drawText(parent.getTextRenderer(), "Lời nhắc động viên:", 
			popupX + 10, popupY + 10, 0xFFFFFFFF, false);
		
		// Close button [X] at top-right (matching Lucky Chest style)
		String closeText = "§l§c[X]";
		int closeWidth = parent.getTextRenderer().getWidth(closeText);
		int closeX = popupX + popupWidth - closeWidth - 10;
		int closeY = popupY + 10;
		context.drawText(parent.getTextRenderer(), closeText, closeX, closeY, 0xFFFF5555, false);
		
		// Input field
		context.fill(popupX + 10, popupY + 35, popupX + popupWidth - 10, popupY + 75, 0xFF0A0A0A);
		context.drawText(parent.getTextRenderer(), encouragementNote, 
			popupX + 15, popupY + 45, 0xFFFFFFFF, false);
		
		// OK button (keep for functional purposes)
		int okBtnX = popupX + popupWidth / 2 - 40;
		int okBtnY = popupY + popupHeight - 40;
		context.fill(okBtnX, okBtnY, okBtnX + 80, okBtnY + 30, 0xFF4A9EFF);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "OK", 
			okBtnX + 40, okBtnY + 10, 0xFFFFFFFF);
	}
	
	private void renderTodoPopup(DrawContext context) {
		// Get actual screen dimensions
		int screenWidth = parent.getParent().width;
		int screenHeight = parent.getParent().height;
		
		// NOTE: Dim overlay is now drawn by MainMenuScreen BEFORE this is called
		
		// Center modal on screen
		int popupWidth = 350;
		int popupHeight = 300;
		int popupX = (screenWidth - popupWidth) / 2;
		int popupY = (screenHeight - popupHeight) / 2;
		
		// Modal box with border effect (matching Lucky Chest style)
		context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF1A1A1A);
		context.fill(popupX + 2, popupY + 2, popupX + popupWidth - 2, popupY + popupHeight - 2, 0xFF2A2A2A);
		
		// Title
		context.drawText(parent.getTextRenderer(), "Danh sách công việc:", 
			popupX + 10, popupY + 10, 0xFFFFFFFF, false);
		
		// Close button [X] at top-right (matching Lucky Chest style)
		String closeText = "§l§c[X]";
		int closeWidth = parent.getTextRenderer().getWidth(closeText);
		int closeX = popupX + popupWidth - closeWidth - 10;
		int closeY = popupY + 10;
		context.drawText(parent.getTextRenderer(), closeText, closeX, closeY, 0xFFFF5555, false);
		
		// Task list
		int listY = popupY + 35;
		for (int i = 0; i < todoList.size() && i < 8; i++) {
			String task = todoList.get(i);
			context.drawText(parent.getTextRenderer(), "☐ " + task, 
				popupX + 15, listY + i * 20, 0xFFAAAAAA, false);
		}
		
		// Close button at bottom (keep for functional purposes)
		int closeBtnX = popupX + popupWidth / 2 - 40;
		int closeBtnY = popupY + popupHeight - 40;
		context.fill(closeBtnX, closeBtnY, closeBtnX + 80, closeBtnY + 30, 0xFF4A9EFF);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "ĐÓNG", 
			closeBtnX + 40, closeBtnY + 10, 0xFFFFFFFF);
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
		int x = contentX;
		int y = contentY;
		int width = contentWidth;
		int height = contentHeight;
		
		// Handle popups with screen coordinates
		if (showingNoteInput) {
			int screenWidth = parent.getParent().width;
			int screenHeight = parent.getParent().height;
			int popupWidth = 300;
			int popupHeight = 150;
			int popupX = (screenWidth - popupWidth) / 2;
			int popupY = (screenHeight - popupHeight) / 2;
			
			// Check [X] close button (top-right)
			String closeText = "§l§c[X]";
			int closeWidth = parent.getTextRenderer().getWidth(closeText);
			int closeX = popupX + popupWidth - closeWidth - 10;
			int closeY = popupY + 10;
			int closeHeight = parent.getTextRenderer().fontHeight;
			
			if (mouseX >= closeX && mouseX <= closeX + closeWidth && 
			    mouseY >= closeY && mouseY <= closeY + closeHeight) {
				showingNoteInput = false;
				return true;
			}
			
			// Check OK button
			int okBtnX = popupX + popupWidth / 2 - 40;
			int okBtnY = popupY + popupHeight - 40;
			if (mouseX >= okBtnX && mouseX <= okBtnX + 80 && 
			    mouseY >= okBtnY && mouseY <= okBtnY + 30) {
				showingNoteInput = false;
				return true;
			}
			return true; // Consume all clicks when popup is open
		}
		
		if (showingTodoPopup) {
			int screenWidth = parent.getParent().width;
			int screenHeight = parent.getParent().height;
			int popupWidth = 350;
			int popupHeight = 300;
			int popupX = (screenWidth - popupWidth) / 2;
			int popupY = (screenHeight - popupHeight) / 2;
			
			// Check [X] close button (top-right)
			String closeText = "§l§c[X]";
			int closeWidth = parent.getTextRenderer().getWidth(closeText);
			int closeX = popupX + popupWidth - closeWidth - 10;
			int closeY = popupY + 10;
			int closeHeight = parent.getTextRenderer().fontHeight;
			
			if (mouseX >= closeX && mouseX <= closeX + closeWidth && 
			    mouseY >= closeY && mouseY <= closeY + closeHeight) {
				showingTodoPopup = false;
				return true;
			}
			
			// Check ĐÓNG button
			int closeBtnX = popupX + popupWidth / 2 - 40;
			int closeBtnY = popupY + popupHeight - 40;
			if (mouseX >= closeBtnX && mouseX <= closeBtnX + 80 && 
			    mouseY >= closeBtnY && mouseY <= closeBtnY + 30) {
				showingTodoPopup = false;
				return true;
			}
			return true; // Consume all clicks when popup is open
		}
		
		// Mode toggle
		int toggleX = x + width / 2 + 100;
		int toggleY = y + 10 + 30;
		if (mouseX >= toggleX && mouseX <= toggleX + 50 && 
		    mouseY >= toggleY && mouseY <= toggleY + 80) {
			clockMode = (clockMode == ClockMode.COUNTDOWN) ? ClockMode.STOPWATCH : ClockMode.COUNTDOWN;
			return true;
		}
		
		// Time picker clicks
		if (clockMode == ClockMode.COUNTDOWN) {
			int pickerY = y + 10;
			int centerX = x + width / 2 - 80;
			int rowHeight = 25;
			int unitWidth = 50;
			
			int hoursX = centerX - 40;
			if (mouseX >= hoursX && mouseX <= hoursX + unitWidth && 
			    mouseY >= pickerY && mouseY <= pickerY + rowHeight * 3) {
				int relativeY = (int)(mouseY - pickerY);
				if (relativeY < rowHeight) {
					hours = hours > 0 ? hours - 1 : 23;
				} else if (relativeY >= rowHeight * 2) {
					hours = hours < 23 ? hours + 1 : 0;
				}
				return true;
			}
			
			int minsX = centerX + 60;
			if (mouseX >= minsX && mouseX <= minsX + unitWidth && 
			    mouseY >= pickerY && mouseY <= pickerY + rowHeight * 3) {
				int relativeY = (int)(mouseY - pickerY);
				if (relativeY < rowHeight) {
					minutes = minutes > 0 ? minutes - 1 : 59;
				} else if (relativeY >= rowHeight * 2) {
					minutes = minutes < 59 ? minutes + 1 : 0;
				}
				return true;
			}
		}
		
		// Note button
		int noteY = y + 150;
		if (mouseX >= x + 20 && mouseX <= x + width - 70 && 
		    mouseY >= noteY && mouseY <= noteY + 35) {
			showingNoteInput = true;
			return true;
		}
		
		// V button
		int vButtonX = x + width - 60;
		if (mouseX >= vButtonX && mouseX <= vButtonX + 40 && 
		    mouseY >= noteY && mouseY <= noteY + 35) {
			showingTodoPopup = true;
			return true;
		}
		
		// Start button
		int startBtnY = y + height - 60;
		int startBtnX = x + width / 2 - 80;
		if (mouseX >= startBtnX && mouseX <= startBtnX + 160 && 
		    mouseY >= startBtnY && mouseY <= startBtnY + 45) {
			startTimer();
			return true;
		}
		
		return false;
	}
	
	private void startTimer() {
		int targetSeconds = 0;
		
		if (clockMode == ClockMode.COUNTDOWN) {
			targetSeconds = hours * 3600 + minutes * 60;
			if (targetSeconds == 0) {
				return;
			}
		}
		
		com.focustimershop.timer.TimerType legacyType = mapToLegacyType();
		ModNetworking.sendTimerStart(legacyType, targetSeconds);
		parent.onTimerStarted();
	}
	
	private com.focustimershop.timer.TimerType mapToLegacyType() {
		if (clockMode == ClockMode.STOPWATCH) {
			return com.focustimershop.timer.TimerType.STOPWATCH;
		}
		
		switch (category) {
			case TAP_TRUNG:
				return com.focustimershop.timer.TimerType.POMODORO_FOCUS;
			case NGHI_NGAN:
				return com.focustimershop.timer.TimerType.POMODORO_SHORT_BREAK;
			case NGHI_DAI:
				return com.focustimershop.timer.TimerType.POMODORO_LONG_BREAK;
			case TAP_LUYEN:
			default:
				return com.focustimershop.timer.TimerType.COUNTDOWN;
		}
	}
}
