package com.focustimershop.client.gui;

import com.focustimershop.network.ModNetworking;
import com.focustimershop.timer.ClockMode;
import com.focustimershop.timer.SessionCategory;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * v1.0.7-beta Timer UI Overhaul - Clock configuration screen
 * Added: Drag-to-adjust time + To-Do List with persistence
 */
public class ClockConfigScreen {
	private final TimerTabScreenV2 parent;
	private final SessionCategory category;
	private ClockMode clockMode = ClockMode.COUNTDOWN;
	
	private int minutes = 25;
	private int seconds = 0;
	
	// Drag-to-adjust time
	private boolean isDraggingTime = false;
	private boolean draggingMinutes = false; // true = minutes, false = seconds
	private double dragStartY = 0;
	private int dragStartValue = 0;
	private static final double DRAG_SENSITIVITY = 0.3; // pixels per unit
	
	// 90+ minute warning
	private boolean showingWarningPopup = false;
	
	// To-Do List with persistence
	private TextFieldWidget todoInputField = null;
	private List<TodoItem> todoItems = new ArrayList<>();
	private static final int MAX_TODO_ITEMS = 20;
	private static final String TODO_FILE = "focustimershop_todo.txt";
	
	// Edit mode
	private int editingIndex = -1;
	private TextFieldWidget editField = null;
	
	// Drag to reorder
	private boolean isDraggingTodo = false;
	private int draggingTodoIndex = -1;
	private double todoDragStartY = 0;
	
	// Cache absolute coordinates for drag calculations
	private int cachedTodoItemY = 0;
	private int cachedTodoLineHeight = 24;
	
	private boolean showingNoteInput = false; // Deprecated but kept for compatibility
	private boolean showingTodoPopup = false;

	public ClockConfigScreen(TimerTabScreenV2 parent, SessionCategory category) {
		this.parent = parent;
		this.category = category;
		loadTodoList();
	}
	
	public boolean isShowingPopup() {
		return showingNoteInput || showingTodoPopup || showingWarningPopup;
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
		if (showingWarningPopup) {
			renderWarningPopup(context, mouseX, mouseY);
			return;
		}
		
		// Normal content (only rendered when no popup is open)
		// Time picker at top
		int pickerY = y + 10;
		if (clockMode == ClockMode.COUNTDOWN) {
			renderCountdownPicker(context, x, pickerY, width, mouseX, mouseY);
		} else {
			renderStopwatchInfo(context, x, pickerY, width);
		}
		
		// Mode toggle on right
		int toggleX = x + width / 2 + 100;
		int toggleY = pickerY + 30;
		renderModeToggle(context, toggleX, toggleY);
		
		// To-Do List section
		int todoY = y + 150;
		int todoHeight = height - 240; // Reserve space for warning + start button
		renderTodoList(context, x + 20, todoY, width - 40, todoHeight, mouseX, mouseY);
		
		// Warning at bottom (if >90 min)
		int warningY = y + height - 80;
		renderWarningIfNeeded(context, x, warningY, width, mouseX, mouseY);
		
		// Start button
		int startBtnY = y + height - 60;
		int startBtnX = x + width / 2 - 80;
		context.fill(startBtnX, startBtnY, startBtnX + 160, startBtnY + 45, 0xFF4AFF4A);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "BẮT ĐẦU", 
			startBtnX + 80, startBtnY + 16, 0xFF000000);
	}
	
	private void renderTodoList(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Title
		context.drawText(parent.getTextRenderer(), "§lTo-Do List", x, y, 0xFFFFFFFF, true);
		
		// Input field (only show if not editing)
		int inputY = y + 20;
		if (editingIndex == -1) {
			if (todoInputField == null) {
				todoInputField = new TextFieldWidget(parent.getTextRenderer(), x, inputY, width - 10, 18, Text.literal(""));
				todoInputField.setMaxLength(100);
				todoInputField.setPlaceholder(Text.literal("Thêm công việc... (Enter)"));
			}
			todoInputField.setPosition(x, inputY);
			todoInputField.setWidth(width - 10);
			todoInputField.render(context, mouseX, mouseY, 0);
		}
		
		// Todo items
		int itemY = inputY + 30;
		int lineHeight = 24; // Increased for buttons
		
		// Cache for mouseReleased
		cachedTodoItemY = itemY;
		cachedTodoLineHeight = lineHeight;
		for (int i = 0; i < todoItems.size(); i++) {
			TodoItem item = todoItems.get(i);
			int currentY = itemY + i * lineHeight;
			
			// Highlight if dragging over this position
			if (isDraggingTodo && draggingTodoIndex != i) {
				double relativeY = mouseY - currentY;
				if (relativeY >= 0 && relativeY < lineHeight) {
					context.fill(x, currentY, x + width, currentY + lineHeight, 0x404A9EFF);
				}
			}
			
			// Drag handle (≡) - BIGGER for easier grabbing
			int handleX = x + 2;
			int handleY = currentY + 4;
			int handleWidth = 16;
			int handleHeight = 16;
			boolean handleHovered = mouseX >= handleX && mouseX <= handleX + handleWidth &&
			                        mouseY >= handleY && mouseY <= handleY + handleHeight;
			
			// Draw larger drag handle box
			int handleBg = handleHovered ? 0xFF4A5A6A : 0xFF2A2A2A;
			context.fill(handleX, handleY, handleX + handleWidth, handleY + handleHeight, handleBg);
			
			int handleColor = handleHovered ? 0xFFFFFFFF : 0xFF888888;
			// Center the icon in the box
			int iconWidth = parent.getTextRenderer().getWidth("≡");
			int iconX = handleX + (handleWidth - iconWidth) / 2;
			int iconY = handleY + (handleHeight - parent.getTextRenderer().fontHeight) / 2;
			context.drawText(parent.getTextRenderer(), "≡", iconX, iconY, handleColor, false);
			
			// Checkbox
			int checkboxSize = 12;
			int checkboxX = x + 18;
			int checkboxY = currentY + 6;
			boolean checkboxHovered = mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
			                          mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize;
			
			int checkboxColor = checkboxHovered ? 0xFF5A5A5A : 0xFF3A3A3A;
			context.fill(checkboxX, checkboxY, checkboxX + checkboxSize, checkboxY + checkboxSize, checkboxColor);
			
			// Check mark if completed
			if (item.completed) {
				context.drawText(parent.getTextRenderer(), "§a✓", checkboxX + 2, checkboxY + 2, 0xFF00FF00, false);
			}
			
			// Task text or edit field
			int textX = checkboxX + checkboxSize + 5;
			int textWidth = width - 80; // Reserve space for buttons
			
			if (editingIndex == i) {
				// Editing mode - show text field
				if (editField == null) {
					editField = new TextFieldWidget(parent.getTextRenderer(), textX, checkboxY, textWidth, 16, Text.literal(""));
					editField.setMaxLength(100);
					editField.setText(item.text);
					editField.setFocused(true);
				}
				editField.setPosition(textX, checkboxY);
				editField.setWidth(textWidth);
				editField.render(context, mouseX, mouseY, 0);
			} else {
				// Normal mode - show text
				String displayText = item.text;
				if (displayText.length() > 35) {
					displayText = displayText.substring(0, 32) + "...";
				}
				
				if (item.completed) {
					// Strikethrough effect with dimmed color
					context.drawText(parent.getTextRenderer(), "§8§m" + displayText, textX, checkboxY + 2, 0xFF666666, false);
				} else {
					// Normal bright text
					context.drawText(parent.getTextRenderer(), displayText, textX, checkboxY + 2, 0xFFFFFFFF, false);
				}
			}
			
			// Action buttons (right side)
			int btnSize = 14;
			int btnY = currentY + 5;
			
			// Edit button (✎)
			int editBtnX = x + width - 35;
			boolean editHovered = mouseX >= editBtnX && mouseX <= editBtnX + btnSize &&
			                      mouseY >= btnY && mouseY <= btnY + btnSize;
			int editColor = editHovered ? 0xFF4A9EFF : 0xFF2A2A2A;
			context.fill(editBtnX, btnY, editBtnX + btnSize, btnY + btnSize, editColor);
			context.drawText(parent.getTextRenderer(), "§f✎", editBtnX + 2, btnY + 2, 0xFFFFFFFF, false);
			
			// Delete button (🗑)
			int delBtnX = x + width - 18;
			boolean delHovered = mouseX >= delBtnX && mouseX <= delBtnX + btnSize &&
			                     mouseY >= btnY && mouseY <= btnY + btnSize;
			int delColor = delHovered ? 0xFFFF4444 : 0xFF2A2A2A;
			context.fill(delBtnX, btnY, delBtnX + btnSize, btnY + btnSize, delColor);
			context.drawText(parent.getTextRenderer(), "§c✖", delBtnX + 2, btnY + 2, 0xFFFFFFFF, false);
		}
		
		// Show item count
		if (todoItems.size() > 0) {
			String countText = "§7" + todoItems.size() + "/" + MAX_TODO_ITEMS;
			int countWidth = parent.getTextRenderer().getWidth(countText);
			context.drawText(parent.getTextRenderer(), countText, x + width - countWidth, y, 0xFF888888, false);
		}
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
	
	private void renderCountdownPicker(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		int centerX = x + width / 2 - 80;
		
		int minsX = centerX - 40;
		boolean minsHovered = mouseX >= minsX && mouseX <= minsX + 50 && 
		                      mouseY >= y && mouseY <= y + 75;
		renderTimeUnitWheel(context, minsX, y, minutes, 120, minsHovered || (isDraggingTime && draggingMinutes));
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Phút", 
			minsX + 25, y + 95, 0xFFAAAAAA);
		if (minsHovered && !isDraggingTime) {
			context.drawText(parent.getTextRenderer(), "§7(drag)", minsX + 5, y + 80, 0xFF888888, false);
		}
		
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), ":", 
			centerX + 35, y + 40, 0xFFFFFFFF);
		
		int secsX = centerX + 60;
		boolean secsHovered = mouseX >= secsX && mouseX <= secsX + 50 && 
		                      mouseY >= y && mouseY <= y + 75;
		renderTimeUnitWheel(context, secsX, y, seconds, 60, secsHovered || (isDraggingTime && !draggingMinutes));
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Giây", 
			secsX + 25, y + 95, 0xFFAAAAAA);
		if (secsHovered && !isDraggingTime) {
			context.drawText(parent.getTextRenderer(), "§7(drag)", secsX + 5, y + 80, 0xFF888888, false);
		}
		
		// Warning for >90 minutes (render AFTER todo list, at bottom)
	}
	
	private void renderWarningIfNeeded(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		if (minutes > 90) {
			int warningY = y; // Position passed from caller
			String warningText = "§e⚠ Vui lòng đọc lưu ý trước khi bấm giờ ";
			int warningWidth = parent.getTextRenderer().getWidth(warningText);
			
			String linkText = "§e§ntại đây";
			int linkWidth = parent.getTextRenderer().getWidth(linkText);
			
			int totalWidth = warningWidth + linkWidth;
			int startX = x + width / 2 - totalWidth / 2;
			
			context.drawText(parent.getTextRenderer(), warningText, startX, warningY, 0xFFFFAA00, false);
			
			int linkX = startX + warningWidth;
			boolean linkHovered = mouseX >= linkX && mouseX <= linkX + linkWidth &&
			                      mouseY >= warningY && mouseY <= warningY + 10;
			
			if (linkHovered) {
				context.drawText(parent.getTextRenderer(), "§e§n§ltại đây", linkX, warningY, 0xFFFFFF00, false);
			} else {
				context.drawText(parent.getTextRenderer(), linkText, linkX, warningY, 0xFFFFAA00, false);
			}
		}
	}
	
	private void renderTimeUnitWheel(DrawContext context, int x, int y, int currentValue, int maxValue, boolean highlighted) {
		int unitWidth = 50;
		int rowHeight = 25;
		
		int prevValue = currentValue > 0 ? currentValue - 1 : maxValue;
		String prevStr = String.format("%02d", prevValue);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), prevStr, 
			x + unitWidth / 2, y + 5, 0xFF666666);
		
		int bgColor = highlighted ? 0xFF5A7AFF : 0xFF4A9EFF;
		context.fill(x - 5, y + rowHeight - 2, x + unitWidth + 5, y + rowHeight * 2 + 2, bgColor);
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
	
	private void renderWarningPopup(DrawContext context, int mouseX, int mouseY) {
		int screenWidth = parent.getParent().width;
		int screenHeight = parent.getParent().height;
		
		// Center modal
		int popupWidth = 400;
		int popupHeight = 180;
		int popupX = (screenWidth - popupWidth) / 2;
		int popupY = (screenHeight - popupHeight) / 2;
		
		// Modal box
		context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF1A1A1A);
		context.fill(popupX + 2, popupY + 2, popupX + popupWidth - 2, popupY + popupHeight - 2, 0xFF2A2A2A);
		
		// Warning icon
		context.drawText(parent.getTextRenderer(), "§e⚠", popupX + 15, popupY + 15, 0xFFFFAA00, false);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§lCảnh báo sức khỏe", 
			popupX + 35, popupY + 15, 0xFFFFAA00, true);
		
		// Message (word wrap)
		String msg1 = "Theo các nhà nghiên cứu, việc tập trung liên tục";
		String msg2 = "hơn 90 phút có thể làm tăng mệt mỏi và giảm hiệu";
		String msg3 = "suất. Tôi khuyên bạn không nên tập trung trong";
		String msg4 = "thời gian dài để tránh ảnh hưởng sức khỏe!";
		
		context.drawText(parent.getTextRenderer(), msg1, popupX + 20, popupY + 45, 0xFFFFFFFF, false);
		context.drawText(parent.getTextRenderer(), msg2, popupX + 20, popupY + 60, 0xFFFFFFFF, false);
		context.drawText(parent.getTextRenderer(), msg3, popupX + 20, popupY + 75, 0xFFFFFFFF, false);
		context.drawText(parent.getTextRenderer(), msg4, popupX + 20, popupY + 90, 0xFFFFFFFF, false);
		
		// Buttons
		int btnY = popupY + popupHeight - 45;
		int btnHeight = 30;
		
		// "Bỏ qua" button
		int ignoreX = popupX + 30;
		int ignoreWidth = 120;
		boolean ignoreHovered = mouseX >= ignoreX && mouseX <= ignoreX + ignoreWidth &&
		                        mouseY >= btnY && mouseY <= btnY + btnHeight;
		int ignoreColor = ignoreHovered ? 0xFF5A5A5A : 0xFF3A3A3A;
		context.fill(ignoreX, btnY, ignoreX + ignoreWidth, btnY + btnHeight, ignoreColor);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Bỏ qua", 
			ignoreX + ignoreWidth / 2, btnY + 10, 0xFFFFFFFF);
		
		// "Chấp nhận" button (adjust to 90 min)
		int acceptX = popupX + popupWidth - 150;
		int acceptWidth = 120;
		boolean acceptHovered = mouseX >= acceptX && mouseX <= acceptX + acceptWidth &&
		                        mouseY >= btnY && mouseY <= btnY + btnHeight;
		int acceptColor = acceptHovered ? 0xFF5A9EFF : 0xFF4A9EFF;
		context.fill(acceptX, btnY, acceptX + acceptWidth, btnY + btnHeight, acceptColor);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "Chấp nhận", 
			acceptX + acceptWidth / 2, btnY + 10, 0xFFFFFFFF);
	}
	
	private void renderNoteInputPopup(DrawContext context) {
		// Deprecated - no longer used (replaced by To-Do List)
	}
	
	private void renderTodoPopup(DrawContext context) {
		// Deprecated - no longer used (replaced by To-Do List)
	}

	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth, int contentHeight) {
		int x = contentX;
		int y = contentY;
		int width = contentWidth;
		int height = contentHeight;
		
		// Check todo input field first
		if (editingIndex == -1 && todoInputField != null && todoInputField.mouseClicked(mouseX, mouseY, button)) {
			todoInputField.setFocused(true);
			return true;
		}
		
		// Check edit field
		if (editingIndex != -1 && editField != null && editField.mouseClicked(mouseX, mouseY, button)) {
			editField.setFocused(true);
			return true;
		}
		
		// Handle popups with screen coordinates
		if (showingWarningPopup) {
			int screenWidth = parent.getParent().width;
			int screenHeight = parent.getParent().height;
			int popupWidth = 400;
			int popupHeight = 180;
			int popupX = (screenWidth - popupWidth) / 2;
			int popupY = (screenHeight - popupHeight) / 2;
			
			int btnY = popupY + popupHeight - 45;
			int btnHeight = 30;
			
			// "Bỏ qua" button
			int ignoreX = popupX + 30;
			int ignoreWidth = 120;
			if (mouseX >= ignoreX && mouseX <= ignoreX + ignoreWidth &&
			    mouseY >= btnY && mouseY <= btnY + btnHeight) {
				showingWarningPopup = false;
				return true;
			}
			
			// "Chấp nhận" button (set to 90 min, 0 sec)
			int acceptX = popupX + popupWidth - 150;
			int acceptWidth = 120;
			if (mouseX >= acceptX && mouseX <= acceptX + acceptWidth &&
			    mouseY >= btnY && mouseY <= btnY + btnHeight) {
				minutes = 90;
				seconds = 0;
				showingWarningPopup = false;
				return true;
			}
			
			return true; // Consume all clicks when popup is open
		}
		
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
		
		// Time picker clicks - check for drag start
		if (clockMode == ClockMode.COUNTDOWN) {
			int pickerY = y + 10;
			int centerX = x + width / 2 - 80;
			int unitWidth = 50;
			int wheelHeight = 75;
			
			// Check warning link click first (at bottom of screen)
			if (minutes > 90) {
				int warningY = y + height - 80;
				String warningText = "§e⚠ Vui lòng đọc lưu ý trước khi bấm giờ ";
				int warningWidth = parent.getTextRenderer().getWidth(warningText);
				String linkText = "§e§ntại đây";
				int linkWidth = parent.getTextRenderer().getWidth(linkText);
				int totalWidth = warningWidth + linkWidth;
				int startX = x + width / 2 - totalWidth / 2;
				int linkX = startX + warningWidth;
				
				if (mouseX >= linkX && mouseX <= linkX + linkWidth &&
				    mouseY >= warningY && mouseY <= warningY + 10) {
					showingWarningPopup = true;
					return true;
				}
			}
			
			int minsX = centerX - 40;
			if (mouseX >= minsX && mouseX <= minsX + unitWidth && 
			    mouseY >= pickerY && mouseY <= pickerY + wheelHeight) {
				isDraggingTime = true;
				draggingMinutes = true;
				dragStartY = mouseY;
				dragStartValue = minutes;
				return true;
			}
			
			int secsX = centerX + 60;
			if (mouseX >= secsX && mouseX <= secsX + unitWidth && 
			    mouseY >= pickerY && mouseY <= pickerY + wheelHeight) {
				isDraggingTime = true;
				draggingMinutes = false;
				dragStartY = mouseY;
				dragStartValue = seconds;
				return true;
			}
		}
		
		// To-Do list interactions
		int todoY = y + 150;
		int inputY = todoY + 20;
		int itemY = inputY + 30;
		int lineHeight = 24;
		
		for (int i = 0; i < todoItems.size(); i++) {
			int currentY = itemY + i * lineHeight;
			
			// Drag handle (bigger hit area - 16x16)
			int handleX = x + 22;
			int handleWidth = 16;
			int handleHeight = 16;
			if (mouseX >= handleX && mouseX <= handleX + handleWidth &&
			    mouseY >= currentY + 4 && mouseY <= currentY + 4 + handleHeight) {
				isDraggingTodo = true;
				draggingTodoIndex = i;
				todoDragStartY = mouseY;
				return true;
			}
			
			// Checkbox
			int checkboxSize = 12;
			int checkboxX = x + 38;
			int checkboxY = currentY + 6;
			if (mouseX >= checkboxX && mouseX <= checkboxX + checkboxSize &&
			    mouseY >= checkboxY && mouseY <= checkboxY + checkboxSize) {
				todoItems.get(i).completed = !todoItems.get(i).completed;
				saveTodoList();
				return true;
			}
			
			// Edit button
			int btnSize = 14;
			int btnY = currentY + 5;
			int editBtnX = x + width - 55;
			if (mouseX >= editBtnX && mouseX <= editBtnX + btnSize &&
			    mouseY >= btnY && mouseY <= btnY + btnSize) {
				if (editingIndex == i) {
					// Save edit
					if (editField != null) {
						String newText = editField.getText().trim();
						if (!newText.isEmpty()) {
							todoItems.get(i).text = newText;
							saveTodoList();
						}
					}
					editingIndex = -1;
					editField = null;
				} else {
					// Start editing
					editingIndex = i;
					editField = null; // Will be created in render
				}
				return true;
			}
			
			// Delete button
			int delBtnX = x + width - 38;
			if (mouseX >= delBtnX && mouseX <= delBtnX + btnSize &&
			    mouseY >= btnY && mouseY <= btnY + btnSize) {
				todoItems.remove(i);
				saveTodoList();
				if (editingIndex == i) {
					editingIndex = -1;
					editField = null;
				} else if (editingIndex > i) {
					editingIndex--;
				}
				return true;
			}
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
	
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		// Time drag
		if (isDraggingTime) {
			double dragDelta = dragStartY - mouseY;
			int unitsDelta = (int)(dragDelta * DRAG_SENSITIVITY);
			
			if (draggingMinutes) {
				int newValue = dragStartValue + unitsDelta;
				// Smooth loop: allow going negative/over for wrap calculation
				while (newValue < 1) newValue += 120;
				while (newValue > 120) newValue -= 120;
				minutes = newValue;
			} else {
				int newValue = dragStartValue + unitsDelta;
				// Smooth loop: 0-60
				while (newValue < 0) newValue += 61;
				while (newValue > 60) newValue -= 61;
				seconds = newValue;
			}
			
			return true;
		}
		
		// Todo drag (reordering)
		if (isDraggingTodo && draggingTodoIndex >= 0) {
			// Visual feedback handled in render
			return true;
		}
		
		return false;
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingTime) {
			isDraggingTime = false;
			return true;
		}
		
		// Todo reorder on release
		if (isDraggingTodo && draggingTodoIndex >= 0) {
			// Use cached absolute coordinates from renderTodoList
			int itemY = cachedTodoItemY;
			int lineHeight = cachedTodoLineHeight;
			
			// Calculate which item index mouse is over
			double relativeY = mouseY - itemY;
			int newIndex = (int)(relativeY / lineHeight);
			
			// Add half-line offset for better UX (drop in middle = insert after)
			if (relativeY % lineHeight > lineHeight / 2) {
				newIndex++;
			}
			
			// Clamp to valid range
			newIndex = Math.max(0, Math.min(newIndex, todoItems.size() - 1));
			
			// Debug output
			System.out.println("Drag release: oldIndex=" + draggingTodoIndex + ", newIndex=" + newIndex + ", mouseY=" + mouseY + ", itemY=" + itemY);
			
			// Only reorder if actually moved
			if (newIndex != draggingTodoIndex && newIndex >= 0 && newIndex < todoItems.size()) {
				// Remove from old position
				TodoItem item = todoItems.remove(draggingTodoIndex);
				
				// Adjust newIndex if removing item shifted positions
				int insertIndex = newIndex;
				if (draggingTodoIndex < newIndex) {
					// Moving down: after removal, newIndex shifts down by 1
					insertIndex = newIndex - 1;
				}
				
				// Insert at new position
				todoItems.add(insertIndex, item);
				saveTodoList();
				
				System.out.println("Reordered: removed from " + draggingTodoIndex + ", inserted at " + insertIndex);
				
				// Adjust editingIndex if needed
				if (editingIndex == draggingTodoIndex) {
					editingIndex = insertIndex;
				} else if (draggingTodoIndex < insertIndex) {
					// Moved down: items between old and new shift up
					if (editingIndex > draggingTodoIndex && editingIndex <= insertIndex) {
						editingIndex--;
					}
				} else {
					// Moved up: items between new and old shift down
					if (editingIndex >= insertIndex && editingIndex < draggingTodoIndex) {
						editingIndex++;
					}
				}
			}
			
			isDraggingTodo = false;
			draggingTodoIndex = -1;
			return true;
		}
		
		return false;
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// ESC to cancel edit
		if (keyCode == 256 && editingIndex != -1) { // ESC key
			editingIndex = -1;
			editField = null;
			return true;
		}
		
		// Handle edit field
		if (editingIndex != -1 && editField != null && editField.isFocused()) {
			if (keyCode == 257 || keyCode == 335) { // Enter or NumpadEnter
				String newText = editField.getText().trim();
				if (!newText.isEmpty()) {
					todoItems.get(editingIndex).text = newText;
					saveTodoList();
				}
				editingIndex = -1;
				editField = null;
				return true;
			}
			return editField.keyPressed(keyCode, scanCode, modifiers);
		}
		
		// Handle todo input field
		if (todoInputField != null && todoInputField.isFocused()) {
			if (keyCode == 257 || keyCode == 335) { // Enter or NumpadEnter
				String text = todoInputField.getText().trim();
				if (!text.isEmpty() && todoItems.size() < MAX_TODO_ITEMS) {
					todoItems.add(new TodoItem(text));
					todoInputField.setText("");
					saveTodoList();
				}
				return true;
			}
			return todoInputField.keyPressed(keyCode, scanCode, modifiers);
		}
		return false;
	}
	
	public boolean charTyped(char chr, int modifiers) {
		if (editingIndex != -1 && editField != null && editField.isFocused()) {
			return editField.charTyped(chr, modifiers);
		}
		if (todoInputField != null && todoInputField.isFocused()) {
			return todoInputField.charTyped(chr, modifiers);
		}
		return false;
	}
	
	private void startTimer() {
		int targetSeconds = 0;
		
		if (clockMode == ClockMode.COUNTDOWN) {
			targetSeconds = minutes * 60 + seconds;
			if (targetSeconds == 0) {
				return;
			}
		}
		
		com.focustimershop.timer.TimerType legacyType = mapToLegacyType();
		ModNetworking.sendTimerStart(legacyType, targetSeconds);
		
		// Open fullscreen active session screen
		net.minecraft.client.MinecraftClient client = net.minecraft.client.MinecraftClient.getInstance();
		if (client != null) {
			client.setScreen(new ActiveSessionScreen());
		}
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
	
	// ===== PERSISTENCE =====
	
	private void loadTodoList() {
		try {
			Path todoPath = getTodoFilePath();
			if (Files.exists(todoPath)) {
				List<String> lines = Files.readAllLines(todoPath);
				todoItems.clear();
				for (String line : lines) {
					if (line.trim().isEmpty()) continue;
					
					// Format: [x] text or [ ] text
					boolean completed = line.startsWith("[x]") || line.startsWith("[X]");
					String text = line.substring(3).trim(); // Skip "[ ] " or "[x] "
					
					if (!text.isEmpty()) {
						TodoItem item = new TodoItem(text);
						item.completed = completed;
						todoItems.add(item);
					}
				}
			}
		} catch (IOException e) {
			System.err.println("Failed to load todo list: " + e.getMessage());
		}
	}
	
	private void saveTodoList() {
		try {
			Path todoPath = getTodoFilePath();
			List<String> lines = new ArrayList<>();
			
			for (TodoItem item : todoItems) {
				String prefix = item.completed ? "[x] " : "[ ] ";
				lines.add(prefix + item.text);
			}
			
			Files.write(todoPath, lines);
		} catch (IOException e) {
			System.err.println("Failed to save todo list: " + e.getMessage());
		}
	}
	
	private Path getTodoFilePath() {
		// Save in .minecraft folder
		Path minecraftDir = Paths.get(System.getProperty("user.dir"));
		return minecraftDir.resolve(TODO_FILE);
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
