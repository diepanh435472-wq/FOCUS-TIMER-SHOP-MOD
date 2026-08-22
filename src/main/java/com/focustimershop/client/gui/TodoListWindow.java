package com.focustimershop.client.gui;

import com.focustimershop.todo.FloatingWindowState;
import com.focustimershop.todo.TodoManager;
import com.focustimershop.todo.TodoTask;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * v1.0.7-beta - To-Do List Floating Window
 * Draggable floating window for managing player's to-do list
 * 
 * Features:
 * - Draggable by title bar
 * - Minimize/restore via [-]/[+] button
 * - Close via [X] button
 * - Add tasks via text field + Enter
 * - Edit tasks via pencil button
 * - Delete tasks via X button
 * - Toggle completion via checkbox
 * - Reorder incomplete tasks via drag handle
 * - Scroll support for long lists
 */
public class TodoListWindow {
	private static final int DEFAULT_WIDTH = 400;
	private static final int DEFAULT_HEIGHT = 350;
	private static final int MIN_WIDTH = 300;
	private static final int MIN_HEIGHT = 200;
	private static final int TITLE_BAR_HEIGHT = 30;
	private static final int TASK_ROW_HEIGHT = 25;
	private static final int SCROLL_BAR_WIDTH = 8;
	private static final int RESIZE_MARGIN = 5; // Pixel margin for resize detection
	
	private final MinecraftClient client;
	private final FloatingWindowState windowState;
	
	// Window dimensions
	private int x, y, width, height;
	
	// Dragging state
	private boolean isDraggingWindow = false;
	private double dragStartMouseX, dragStartMouseY;
	private int dragStartWindowX, dragStartWindowY;
	
	// Resizing state
	private ResizeEdge resizingEdge = ResizeEdge.NONE;
	private int resizeStartWidth, resizeStartHeight;
	private int resizeStartX, resizeStartY;
	
	// Resize edge enum
	private enum ResizeEdge {
		NONE, LEFT, RIGHT, TOP, BOTTOM, 
		TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT
	}
	
	// Task reordering
	private String draggingTaskId = null;
	private int draggingTaskStartY = 0;
	private int draggingTaskCurrentY = 0;
	
	// Task editing
	private String editingTaskId = null;
	private TextFieldWidget editField = null;
	
	// Add task field
	private TextFieldWidget addTaskField = null;
	
	// Scrolling
	private int scrollOffset = 0;
	private int maxScrollOffset = 0;
	
	// UI state
	private String hoveredButton = null; // "minimize", "close", "edit_taskId", "delete_taskId", "checkbox_taskId"
	
	public TodoListWindow(MinecraftClient client) {
		this.client = client;
		this.windowState = TodoManager.getWindowState(
			client.player.getUuid(), 
			"todo_list"
		);
		
		// Initialize dimensions
		this.width = DEFAULT_WIDTH;
		this.height = DEFAULT_HEIGHT;
		
		// Set position from saved state
		this.x = windowState.getPosX();
		this.y = windowState.getPosY();
		
		// Clamp to screen
		clampToScreen();
		
		// Initialize add task field
		initAddTaskField();
	}
	
	private void initAddTaskField() {
		if (addTaskField == null) {
			addTaskField = new TextFieldWidget(
				client.textRenderer,
				x + 10,
				y + TITLE_BAR_HEIGHT + 10,
				width - 20,
				20,
				Text.literal("")
			);
			addTaskField.setMaxLength(200);
			addTaskField.setPlaceholder(Text.literal("Thêm công việc... (Enter)"));
			addTaskField.setEditable(true);
		} else {
			// Update position
			addTaskField.setX(x + 10);
			addTaskField.setY(y + TITLE_BAR_HEIGHT + 10);
			addTaskField.setWidth(width - 20);
		}
	}
	
	private void clampToScreen() {
		int screenWidth = client.getWindow().getScaledWidth();
		int screenHeight = client.getWindow().getScaledHeight();
		
		// Keep at least 50px of window visible
		int minVisible = 50;
		x = Math.max(minVisible - width, Math.min(screenWidth - minVisible, x));
		y = Math.max(0, Math.min(screenHeight - TITLE_BAR_HEIGHT, y));
	}
	
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// v1.0.7-beta FIX: Only render on ActiveSessionScreen (timer running screen)
		// NOT on CategorySelectionScreen (timer setup screen)
		net.minecraft.client.gui.screen.Screen currentScreen = client.currentScreen;
		boolean isActiveSessionScreen = currentScreen instanceof ActiveSessionScreen;
		
		if (!isActiveSessionScreen) {
			return; // Only render on ActiveSessionScreen (ảnh 2 - timer running)
		}
		
		// Don't render if closed
		if (windowState.getState() == FloatingWindowState.State.CLOSED) {
			System.out.println("[TodoListWindow] Not rendering - window is CLOSED");
			return;
		}
		
		System.out.println("[TodoListWindow] Rendering - state=" + windowState.getState());
		
		// Update field positions
		initAddTaskField();
		
		// Update cursor for resize
		updateResizeCursor(mouseX, mouseY);
		
		// Render based on state
		if (windowState.getState() == FloatingWindowState.State.MINIMIZED) {
			renderMinimized(context, mouseX, mouseY);
		} else {
			renderNormal(context, mouseX, mouseY, delta);
		}
	}
	
	private void renderMinimized(DrawContext context, int mouseX, int mouseY) {
		// Just title bar
		renderTitleBar(context, mouseX, mouseY, true);
	}
	
	private void renderNormal(DrawContext context, int mouseX, int mouseY, float delta) {
		// Window background
		context.fill(x, y, x + width, y + height, 0xEE2D2D2D);
		
		// Title bar
		renderTitleBar(context, mouseX, mouseY, false);
		
		// Content area border
		context.fill(x, y + TITLE_BAR_HEIGHT, x + width, y + TITLE_BAR_HEIGHT + 1, 0xFF4A4A4A);
		
		// Add task field
		addTaskField.render(context, mouseX, mouseY, 0);
		
		// Task list
		renderTaskList(context, mouseX, mouseY);
		
		// Scroll bar
		if (maxScrollOffset > 0) {
			renderScrollBar(context, mouseX, mouseY);
		}
	}
	
	private void renderTitleBar(DrawContext context, int mouseX, int mouseY, boolean minimized) {
		// Title bar background
		boolean isDraggingOrHovering = isDraggingWindow || 
			(mouseX >= x && mouseX <= x + width - 60 && mouseY >= y && mouseY <= y + TITLE_BAR_HEIGHT);
		int titleBarColor = isDraggingOrHovering ? 0xFF3A5A9E : 0xFF2A4A8E;
		context.fill(x, y, x + width, y + TITLE_BAR_HEIGHT, titleBarColor);
		
		// Title text
		context.drawText(client.textRenderer, "§lTo-do List", 
			x + 10, y + 10, 0xFFFFFFFF, true);
		
		// Minimize button [-] or [+]
		int minBtnX = x + width - 55;
		int minBtnY = y + 7;
		boolean minHovered = mouseX >= minBtnX && mouseX <= minBtnX + 20 &&
		                     mouseY >= minBtnY && mouseY <= minBtnY + 16;
		int minBtnColor = minHovered ? 0xFF5A5A5A : 0xFF3A3A3A;
		context.fill(minBtnX, minBtnY, minBtnX + 20, minBtnY + 16, minBtnColor);
		context.drawText(client.textRenderer, minimized ? "§l+" : "§l-", 
			minBtnX + 7, minBtnY + 4, 0xFFFFFFFF, false);
		
		// Close button [X]
		int closeBtnX = x + width - 30;
		int closeBtnY = y + 7;
		boolean closeHovered = mouseX >= closeBtnX && mouseX <= closeBtnX + 20 &&
		                       mouseY >= closeBtnY && mouseY <= closeBtnY + 16;
		int closeBtnColor = closeHovered ? 0xFFAA3333 : 0xFF883333;
		context.fill(closeBtnX, closeBtnY, closeBtnX + 20, closeBtnY + 16, closeBtnColor);
		context.drawText(client.textRenderer, "§lX", 
			closeBtnX + 6, closeBtnY + 4, 0xFFFFFFFF, false);
	}
	
	private void renderTaskList(DrawContext context, int mouseX, int mouseY) {
		if (client.player == null) return;
		
		List<TodoTask> tasks = TodoManager.getTasks(client.player.getUuid());
		
		// Debug logging
		if (tasks.isEmpty()) {
			System.out.println("[TodoListWindow] No tasks found for player " + client.player.getUuid());
		} else {
			System.out.println("[TodoListWindow] Rendering " + tasks.size() + " tasks for player " + client.player.getUuid());
		}
		
		int listY = y + TITLE_BAR_HEIGHT + 40; // Below add field
		int listHeight = height - TITLE_BAR_HEIGHT - 50;
		int visibleTasks = listHeight / TASK_ROW_HEIGHT;
		
		// Calculate max scroll
		maxScrollOffset = Math.max(0, tasks.size() * TASK_ROW_HEIGHT - listHeight);
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
		
		// Scissor/clip to list area
		context.enableScissor(x + 5, listY, x + width - 15, listY + listHeight);
		
		int taskY = listY - scrollOffset;
		for (int i = 0; i < tasks.size(); i++) {
			TodoTask task = tasks.get(i);
			
			// Skip if outside visible area
			if (taskY + TASK_ROW_HEIGHT < listY || taskY > listY + listHeight) {
				taskY += TASK_ROW_HEIGHT;
				continue;
			}
			
			renderTaskRow(context, task, i + 1, x + 10, taskY, width - 30, mouseX, mouseY);
			taskY += TASK_ROW_HEIGHT;
		}
		
		context.disableScissor();
	}
	
	private void renderTaskRow(DrawContext context, TodoTask task, int number, 
	                           int rowX, int rowY, int rowWidth, int mouseX, int mouseY) {
		boolean isCompleted = task.isCompleted();
		boolean isEditing = task.getId().equals(editingTaskId);
		
		int currentX = rowX;
		
		// Drag handle [::] - only for incomplete tasks
		if (!isCompleted) {
			String handle = "§7::";
			boolean handleHovered = mouseX >= currentX && mouseX <= currentX + 15 &&
			                        mouseY >= rowY && mouseY <= rowY + TASK_ROW_HEIGHT;
			if (handleHovered || task.getId().equals(draggingTaskId)) {
				handle = "§f::";
			}
			context.drawText(client.textRenderer, handle, currentX, rowY + 5, 0xFFFFFFFF, false);
		}
		currentX += 20;
		
		// Checkbox [ ] or [V]
		int checkboxX = currentX;
		boolean checkboxHovered = mouseX >= checkboxX && mouseX <= checkboxX + 15 &&
		                          mouseY >= rowY && mouseY <= rowY + TASK_ROW_HEIGHT;
		String checkbox = isCompleted ? "§a[✓]" : (checkboxHovered ? "§7[ ]" : "§8[ ]");
		context.drawText(client.textRenderer, checkbox, checkboxX, rowY + 5, 0xFFFFFFFF, false);
		currentX += 20;
		
		// Task number
		String numStr = String.format("§7%d ", number);
		context.drawText(client.textRenderer, numStr, currentX, rowY + 5, 0xFFFFFFFF, false);
		currentX += client.textRenderer.getWidth(numStr);
		
		// Task text or edit field
		int textWidth = rowWidth - (currentX - rowX) - 60; // Leave space for buttons
		if (isEditing && editField != null) {
			editField.setX(currentX);
			editField.setY(rowY + 2);
			editField.setWidth(textWidth);
			editField.render(context, mouseX, mouseY, 0);
		} else {
			String displayText = task.getText();
			if (isCompleted) {
				displayText = "§7§m" + displayText; // Gray + strikethrough
			}
			
			// Truncate if too long
			String truncated = client.textRenderer.trimToWidth(displayText, textWidth);
			context.drawText(client.textRenderer, truncated, currentX, rowY + 5, 
				0xFFFFFFFF, false);
		}
		currentX += textWidth + 5;
		
		// Edit button [✎]
		int editBtnX = currentX;
		boolean editHovered = mouseX >= editBtnX && mouseX <= editBtnX + 15 &&
		                      mouseY >= rowY && mouseY <= rowY + TASK_ROW_HEIGHT;
		String editIcon = editHovered ? "§f✎" : "§7✎";
		context.drawText(client.textRenderer, editIcon, editBtnX, rowY + 5, 0xFFFFFFFF, false);
		currentX += 20;
		
		// Delete button [X]
		int deleteBtnX = currentX;
		boolean deleteHovered = mouseX >= deleteBtnX && mouseX <= deleteBtnX + 15 &&
		                        mouseY >= rowY && mouseY <= rowY + TASK_ROW_HEIGHT;
		String deleteIcon = deleteHovered ? "§cX" : "§7X";
		context.drawText(client.textRenderer, deleteIcon, deleteBtnX, rowY + 5, 0xFFFFFFFF, false);
	}
	
	private void renderScrollBar(DrawContext context, int mouseX, int mouseY) {
		int scrollBarX = x + width - SCROLL_BAR_WIDTH - 5;
		int scrollBarY = y + TITLE_BAR_HEIGHT + 40;
		int scrollBarHeight = height - TITLE_BAR_HEIGHT - 50;
		
		// Track
		context.fill(scrollBarX, scrollBarY, scrollBarX + SCROLL_BAR_WIDTH, 
			scrollBarY + scrollBarHeight, 0xFF2A2A2A);
		
		// Thumb
		if (maxScrollOffset > 0) {
			int contentHeight = maxScrollOffset + scrollBarHeight;
			float thumbRatio = (float) scrollBarHeight / contentHeight;
			int thumbHeight = Math.max(20, (int)(scrollBarHeight * thumbRatio));
			float scrollRatio = (float) scrollOffset / maxScrollOffset;
			int thumbY = scrollBarY + (int)((scrollBarHeight - thumbHeight) * scrollRatio);
			
			context.fill(scrollBarX, thumbY, scrollBarX + SCROLL_BAR_WIDTH, 
				thumbY + thumbHeight, 0xFF5A5A5A);
		}
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return false;
		if (windowState.getState() == FloatingWindowState.State.CLOSED) return false;
		
		int mx = (int) mouseX;
		int my = (int) mouseY;
		
		// Check minimize button
		int minBtnX = x + width - 55;
		int minBtnY = y + 7;
		if (mx >= minBtnX && mx <= minBtnX + 20 && my >= minBtnY && my <= minBtnY + 16) {
			toggleMinimize();
			return true;
		}
		
		// Check close button
		int closeBtnX = x + width - 30;
		int closeBtnY = y + 7;
		if (mx >= closeBtnX && mx <= closeBtnX + 20 && my >= closeBtnY && my <= closeBtnY + 16) {
			close();
			return true;
		}
		
		// If minimized, nothing else to check except title bar drag
		if (windowState.getState() == FloatingWindowState.State.MINIMIZED) {
			if (mx >= x && mx <= x + width - 60 && my >= y && my <= y + TITLE_BAR_HEIGHT) {
				isDraggingWindow = true;
				dragStartMouseX = mouseX;
				dragStartMouseY = mouseY;
				dragStartWindowX = x;
				dragStartWindowY = y;
				return true;
			}
			return false;
		}
		
		// Check resize edges (only when not minimized)
		ResizeEdge edge = getResizeEdge(mx, my);
		if (edge != ResizeEdge.NONE) {
			resizingEdge = edge;
			dragStartMouseX = mouseX;
			dragStartMouseY = mouseY;
			resizeStartX = x;
			resizeStartY = y;
			resizeStartWidth = width;
			resizeStartHeight = height;
			return true;
		}
		
		// Check title bar drag
		if (mx >= x && mx <= x + width - 60 && my >= y && my <= y + TITLE_BAR_HEIGHT) {
			isDraggingWindow = true;
			dragStartMouseX = mouseX;
			dragStartMouseY = mouseY;
			dragStartWindowX = x;
			dragStartWindowY = y;
			return true;
		}
		
		// Check add task field - IMPORTANT: Set focus explicitly
		if (addTaskField != null) {
			boolean clickedField = addTaskField.mouseClicked(mouseX, mouseY, button);
			if (clickedField) {
				addTaskField.setFocused(true);
				return true;
			}
		}
		
		// Check task interactions
		return handleTaskClick(mx, my);
	}
	
	private boolean handleTaskClick(int mouseX, int mouseY) {
		List<TodoTask> tasks = TodoManager.getTasks(client.player.getUuid());
		int listY = y + TITLE_BAR_HEIGHT + 40;
		int listHeight = height - TITLE_BAR_HEIGHT - 50;
		
		int taskY = listY - scrollOffset;
		for (int i = 0; i < tasks.size(); i++) {
			TodoTask task = tasks.get(i);
			
			if (taskY + TASK_ROW_HEIGHT < listY || taskY > listY + listHeight) {
				taskY += TASK_ROW_HEIGHT;
				continue;
			}
			
			int rowX = x + 10;
			int currentX = rowX;
			
			// Drag handle (incomplete only)
			if (!task.isCompleted()) {
				if (mouseX >= currentX && mouseX <= currentX + 15 &&
				    mouseY >= taskY && mouseY <= taskY + TASK_ROW_HEIGHT) {
					draggingTaskId = task.getId();
					draggingTaskStartY = taskY;
					draggingTaskCurrentY = taskY;
					return true;
				}
			}
			currentX += 20;
			
			// Checkbox
			int checkboxX = currentX;
			if (mouseX >= checkboxX && mouseX <= checkboxX + 15 &&
			    mouseY >= taskY && mouseY <= taskY + TASK_ROW_HEIGHT) {
				TodoManager.toggleTaskCompletion(client.player.getUuid(), task.getId());
				return true;
			}
			currentX += 20;
			
			// Skip number
			String numStr = String.format("§7%d ", i + 1);
			currentX += client.textRenderer.getWidth(numStr);
			
			// Text width
			int textWidth = width - 30 - (currentX - rowX) - 60;
			currentX += textWidth + 5;
			
			// Edit button
			int editBtnX = currentX;
			if (mouseX >= editBtnX && mouseX <= editBtnX + 15 &&
			    mouseY >= taskY && mouseY <= taskY + TASK_ROW_HEIGHT) {
				startEditingTask(task);
				return true;
			}
			currentX += 20;
			
			// Delete button
			int deleteBtnX = currentX;
			if (mouseX >= deleteBtnX && mouseX <= deleteBtnX + 15 &&
			    mouseY >= taskY && mouseY <= taskY + TASK_ROW_HEIGHT) {
				TodoManager.deleteTask(client.player.getUuid(), task.getId());
				return true;
			}
			
			taskY += TASK_ROW_HEIGHT;
		}
		
		return false;
	}
	
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (isDraggingWindow) {
			// Use proven direct position calculation pattern from rental sliders
			double dx = mouseX - dragStartMouseX;
			double dy = mouseY - dragStartMouseY;
			x = dragStartWindowX + (int) dx;
			y = dragStartWindowY + (int) dy;
			clampToScreen();
			return true;
		}
		
		if (resizingEdge != ResizeEdge.NONE) {
			handleResize(mouseX, mouseY);
			return true;
		}
		
		if (draggingTaskId != null) {
			draggingTaskCurrentY = (int) mouseY;
			return true;
		}
		
		return false;
	}
	
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingWindow) {
			isDraggingWindow = false;
			// Save position
			windowState.setPosition(x, y);
			TodoManager.saveWindowState(client.player.getUuid(), windowState);
			return true;
		}
		
		if (resizingEdge != ResizeEdge.NONE) {
			resizingEdge = ResizeEdge.NONE;
			// Position/size already saved during resize
			return true;
		}
		
		if (draggingTaskId != null) {
			finishTaskReorder();
			draggingTaskId = null;
			return true;
		}
		
		return false;
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		if (windowState.getState() != FloatingWindowState.State.NORMAL) return false;
		
		// Check if mouse is over task list area
		int listY = y + TITLE_BAR_HEIGHT + 40;
		int listHeight = height - TITLE_BAR_HEIGHT - 50;
		
		if (mouseX >= x && mouseX <= x + width && 
		    mouseY >= listY && mouseY <= listY + listHeight) {
			scrollOffset -= (int)(verticalAmount * 20);
			scrollOffset = Math.max(0, Math.min(scrollOffset, maxScrollOffset));
			return true;
		}
		
		return false;
	}
	
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		System.out.println("[TodoListWindow] keyPressed: keyCode=" + keyCode + ", addFieldFocused=" + (addTaskField != null && addTaskField.isFocused()));
		
		// Enter key in add field
		if (addTaskField != null && addTaskField.isFocused() && keyCode == 257) { // Enter
			String text = addTaskField.getText().trim();
			System.out.println("[TodoListWindow] Enter pressed, text='" + text + "'");
			if (!text.isEmpty() && client.player != null) {
				System.out.println("[TodoListWindow] Adding task: " + text);
				TodoTask task = TodoManager.addTask(client.player.getUuid(), text);
				System.out.println("[TodoListWindow] Task added with ID: " + task.getId());
				addTaskField.setText("");
				// Clear focus to prevent multiple submissions
				addTaskField.setFocused(false);
			}
			return true;
		}
		
		// Enter key in edit field
		if (editField != null && editField.isFocused() && keyCode == 257) {
			finishEditing();
			return true;
		}
		
		// Escape key cancels edit
		if (editField != null && editField.isFocused() && keyCode == 256) { // Escape
			cancelEditing();
			return true;
		}
		
		// Pass to focused field
		if (addTaskField != null && addTaskField.isFocused()) {
			return addTaskField.keyPressed(keyCode, scanCode, modifiers);
		}
		if (editField != null && editField.isFocused()) {
			return editField.keyPressed(keyCode, scanCode, modifiers);
		}
		
		return false;
	}
	
	public boolean charTyped(char chr, int modifiers) {
		if (addTaskField != null && addTaskField.isFocused()) {
			return addTaskField.charTyped(chr, modifiers);
		}
		if (editField != null && editField.isFocused()) {
			return editField.charTyped(chr, modifiers);
		}
		return false;
	}
	
	private void toggleMinimize() {
		if (windowState.getState() == FloatingWindowState.State.MINIMIZED) {
			windowState.setState(FloatingWindowState.State.NORMAL);
		} else {
			windowState.setState(FloatingWindowState.State.MINIMIZED);
		}
		TodoManager.saveWindowState(client.player.getUuid(), windowState);
	}
	
	private void close() {
		windowState.setState(FloatingWindowState.State.CLOSED);
		TodoManager.saveWindowState(client.player.getUuid(), windowState);
	}
	
	public void open() {
		windowState.setState(FloatingWindowState.State.NORMAL);
		TodoManager.saveWindowState(client.player.getUuid(), windowState);
	}
	
	private void startEditingTask(TodoTask task) {
		editingTaskId = task.getId();
		editField = new TextFieldWidget(
			client.textRenderer,
			0, 0, 100, 20,
			Text.literal("")
		);
		editField.setMaxLength(200);
		editField.setText(task.getText());
		editField.setFocused(true);
		editField.setEditable(true);
	}
	
	private void finishEditing() {
		if (editingTaskId != null && editField != null) {
			String newText = editField.getText().trim();
			if (!newText.isEmpty()) {
				TodoManager.updateTaskText(client.player.getUuid(), editingTaskId, newText);
			}
		}
		cancelEditing();
	}
	
	private void cancelEditing() {
		editingTaskId = null;
		editField = null;
	}
	
	private void finishTaskReorder() {
		if (draggingTaskId == null) return;
		
		// Calculate new order based on Y position
		List<TodoTask> tasks = TodoManager.getTasks(client.player.getUuid());
		int listY = y + TITLE_BAR_HEIGHT + 40;
		int relativeY = draggingTaskCurrentY - listY + scrollOffset;
		int newOrder = relativeY / TASK_ROW_HEIGHT;
		
		// Clamp to valid range
		long incompleteCount = tasks.stream().filter(t -> !t.isCompleted()).count();
		newOrder = Math.max(0, Math.min(newOrder, (int) incompleteCount - 1));
		
		TodoManager.reorderTasks(client.player.getUuid(), draggingTaskId, newOrder);
	}
	
	public boolean isVisible() {
		return windowState.getState() != FloatingWindowState.State.CLOSED;
	}
	
	public FloatingWindowState.State getState() {
		return windowState.getState();
	}
	
	/**
	 * v1.0.7-beta - Detect which edge/corner the mouse is hovering over for resizing
	 */
	private ResizeEdge getResizeEdge(int mouseX, int mouseY) {
		if (windowState.getState() != FloatingWindowState.State.NORMAL) {
			return ResizeEdge.NONE;
		}
		
		boolean nearLeft = mouseX >= x - RESIZE_MARGIN && mouseX <= x + RESIZE_MARGIN;
		boolean nearRight = mouseX >= x + width - RESIZE_MARGIN && mouseX <= x + width + RESIZE_MARGIN;
		boolean nearTop = mouseY >= y - RESIZE_MARGIN && mouseY <= y + RESIZE_MARGIN;
		boolean nearBottom = mouseY >= y + height - RESIZE_MARGIN && mouseY <= y + height + RESIZE_MARGIN;
		
		// Don't allow resizing from title bar area (except corners)
		boolean inTitleBar = mouseY >= y && mouseY <= y + TITLE_BAR_HEIGHT;
		
		// Corners have priority
		if (nearTop && nearLeft) return ResizeEdge.TOP_LEFT;
		if (nearTop && nearRight) return ResizeEdge.TOP_RIGHT;
		if (nearBottom && nearLeft) return ResizeEdge.BOTTOM_LEFT;
		if (nearBottom && nearRight) return ResizeEdge.BOTTOM_RIGHT;
		
		// Edges (but not in title bar middle area)
		if (nearLeft && !inTitleBar) return ResizeEdge.LEFT;
		if (nearRight && !inTitleBar) return ResizeEdge.RIGHT;
		if (nearTop) return ResizeEdge.TOP;
		if (nearBottom) return ResizeEdge.BOTTOM;
		
		return ResizeEdge.NONE;
	}
	
	/**
	 * v1.0.7-beta - Update mouse cursor based on hover position
	 */
	private void updateResizeCursor(int mouseX, int mouseY) {
		// Don't show resize cursor when dragging/resizing
		if (isDraggingWindow || resizingEdge != ResizeEdge.NONE || draggingTaskId != null) {
			return;
		}
		
		ResizeEdge edge = getResizeEdge(mouseX, mouseY);
		
		// Note: Minecraft doesn't have native cursor change API in 1.20.1
		// We could use GLFW directly via client.getWindow().getHandle()
		// For now, visual feedback is provided by edge detection
		// Future: Implement GLFW cursor change if needed
	}
	
	/**
	 * v1.0.7-beta - Handle window resizing
	 */
	private void handleResize(double mouseX, double mouseY) {
		double dx = mouseX - dragStartMouseX;
		double dy = mouseY - dragStartMouseY;
		
		int newX = resizeStartX;
		int newY = resizeStartY;
		int newWidth = resizeStartWidth;
		int newHeight = resizeStartHeight;
		
		switch (resizingEdge) {
			case LEFT:
				newX = resizeStartX + (int) dx;
				newWidth = resizeStartWidth - (int) dx;
				break;
			case RIGHT:
				newWidth = resizeStartWidth + (int) dx;
				break;
			case TOP:
				newY = resizeStartY + (int) dy;
				newHeight = resizeStartHeight - (int) dy;
				break;
			case BOTTOM:
				newHeight = resizeStartHeight + (int) dy;
				break;
			case TOP_LEFT:
				newX = resizeStartX + (int) dx;
				newY = resizeStartY + (int) dy;
				newWidth = resizeStartWidth - (int) dx;
				newHeight = resizeStartHeight - (int) dy;
				break;
			case TOP_RIGHT:
				newY = resizeStartY + (int) dy;
				newWidth = resizeStartWidth + (int) dx;
				newHeight = resizeStartHeight - (int) dy;
				break;
			case BOTTOM_LEFT:
				newX = resizeStartX + (int) dx;
				newWidth = resizeStartWidth - (int) dx;
				newHeight = resizeStartHeight + (int) dy;
				break;
			case BOTTOM_RIGHT:
				newWidth = resizeStartWidth + (int) dx;
				newHeight = resizeStartHeight + (int) dy;
				break;
		}
		
		// Enforce minimum size
		if (newWidth < MIN_WIDTH) {
			if (resizingEdge == ResizeEdge.LEFT || resizingEdge == ResizeEdge.TOP_LEFT || resizingEdge == ResizeEdge.BOTTOM_LEFT) {
				newX = x + width - MIN_WIDTH;
			}
			newWidth = MIN_WIDTH;
		}
		
		if (newHeight < MIN_HEIGHT) {
			if (resizingEdge == ResizeEdge.TOP || resizingEdge == ResizeEdge.TOP_LEFT || resizingEdge == ResizeEdge.TOP_RIGHT) {
				newY = y + height - MIN_HEIGHT;
			}
			newHeight = MIN_HEIGHT;
		}
		
		// Apply changes
		x = newX;
		y = newY;
		width = newWidth;
		height = newHeight;
		
		// Update window state position
		windowState.setPosition(x, y);
		TodoManager.saveWindowState(client.player.getUuid(), windowState);
		
		clampToScreen();
	}
}
