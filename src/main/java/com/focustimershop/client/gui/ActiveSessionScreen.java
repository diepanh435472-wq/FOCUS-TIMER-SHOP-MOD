package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.network.ModNetworking;
import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * v1.0.7-beta Timer UI Overhaul - FULLSCREEN active session screen
 * Features: draggable ring, swipe-to-cancel, larger countdown
 */
public class ActiveSessionScreen extends Screen {
	private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");
	
	// Ring geometry - REDUCED for performance
	private static final int RING_SEGMENTS = 90; // Reduced from 180 for smoother performance
	private static final float RING_OUTER_RADIUS = 120f;
	private static final float RING_INNER_RADIUS = 105f;
	private static final float GLOW_HEAD_SIZE = 8f;
	
	// Performance optimization - only re-render ring when needed
	private int lastRenderedSecond = -1;
	
	// Draggable ring position (offset from center)
	private int ringOffsetX = 0;
	private int ringOffsetY = 0;
	private boolean isDraggingRing = false;
	private int dragStartMouseX = 0;
	private int dragStartMouseY = 0;
	private int dragStartOffsetX = 0;
	private int dragStartOffsetY = 0;
	
	// Swipe-to-cancel slider
	private static final int SLIDER_WIDTH = 300;
	private static final int SLIDER_HEIGHT = 50;
	private static final int SLIDER_DOT_SIZE = 40;
	private int sliderDotX = 0; // 0 to (SLIDER_WIDTH - SLIDER_DOT_SIZE)
	private boolean isDraggingSlider = false;
	private int sliderDragStartX = 0;

	public ActiveSessionScreen() {
		super(Text.literal("Timer Session"));
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		this.renderBackground(context);
		
		TimerType type = ClientDataCache.getCurrentTimerType();
		int elapsed = ClientDataCache.getElapsedSeconds();
		int target = ClientDataCache.getTargetSeconds();

		// Safety check
		if (type == null) {
			String loadingText = "§7Đang tải...";
			int textWidth = this.textRenderer.getWidth(loadingText);
			context.drawText(this.textRenderer, loadingText, 
				this.width / 2 - textWidth / 2, this.height / 2, 0xFFAAAAAA, false);
			super.render(context, mouseX, mouseY, delta);
			return;
		}

		// Calculate ring center with offset (clamped to screen bounds)
		int defaultCenterX = this.width / 2;
		int defaultCenterY = this.height / 2 - 60; // Moved up to make room for slider
		int centerX = defaultCenterX + ringOffsetX;
		int centerY = defaultCenterY + ringOffsetY;
		
		// Clamp to screen boundaries (keep ring fully visible)
		int margin = (int)RING_OUTER_RADIUS + 10;
		centerX = Math.max(margin, Math.min(this.width - margin, centerX));
		centerY = Math.max(margin, Math.min(this.height - margin - 100, centerY)); // Extra margin for slider
		
		// Update offset based on clamped position
		ringOffsetX = centerX - defaultCenterX;
		ringOffsetY = centerY - defaultCenterY;
		
		// Calculate progress
		float progress = calculateProgress(type, elapsed, target);
		int[] ringColor = getRingColor(type);
		
		// Render ring
		renderProgressRing(context, centerX, centerY, progress, ringColor, type);
		
		// Time display inside ring
		String timeDisplay;
		if (type == TimerType.STOPWATCH) {
			timeDisplay = formatTime(elapsed);
		} else {
			int remaining = Math.max(0, target - elapsed);
			timeDisplay = formatTime(remaining);
		}
		
		// Large countdown (4x scale, removed label)
		context.getMatrices().push();
		context.getMatrices().translate(centerX, centerY - 20, 0);
		context.getMatrices().scale(4.0f, 4.0f, 1.0f);
		
		int timeWidth = this.textRenderer.getWidth(timeDisplay);
		context.drawText(this.textRenderer, "§l" + timeDisplay, 
			-timeWidth / 2, 0, 0xFFFFFFFF, true);
		
		context.getMatrices().pop();
		
		// Real-world clock time below (smaller)
		String realTime = LocalTime.now().format(TIME_FORMATTER);
		int clockWidth = this.textRenderer.getWidth(realTime);
		context.drawText(this.textRenderer, "§7" + realTime, 
			centerX - clockWidth / 2, centerY + 40, 0xFF888888, false);

		// Swipe-to-cancel slider at bottom
		renderSwipeSlider(context, mouseX, mouseY);
		
		super.render(context, mouseX, mouseY, delta);
	}
	
	/**
	 * Render swipe-to-cancel slider with rounded rect + large circle inside
	 */
	private void renderSwipeSlider(DrawContext context, int mouseX, int mouseY) {
		int sliderX = this.width / 2 - SLIDER_WIDTH / 2;
		int sliderY = this.height - 100;
		
		// Rounded rectangle background track (using fill approximation)
		int cornerRadius = 25;
		drawRoundedRect(context, sliderX, sliderY, SLIDER_WIDTH, SLIDER_HEIGHT, cornerRadius, 0xFF2A2A2A);
		
		// Progress fill (red gradient) - also rounded
		float fillProgress = (float)sliderDotX / (SLIDER_WIDTH - SLIDER_DOT_SIZE);
		int fillWidth = (int)(SLIDER_WIDTH * fillProgress);
		if (fillWidth > cornerRadius * 2) { // Only draw if wide enough
			int startColor = 0xFF4A0000;
			int endColor = 0xFFFF0000;
			drawRoundedRect(context, sliderX, sliderY, fillWidth, SLIDER_HEIGHT, cornerRadius,
				interpolateColor(startColor, endColor, fillProgress));
		}
		
		// Large circular dot inside track
		int dotRadius = SLIDER_DOT_SIZE / 2; // Circle radius
		int dotCenterX = sliderX + sliderDotX + SLIDER_DOT_SIZE / 2;
		int dotCenterY = sliderY + SLIDER_HEIGHT / 2;
		
		boolean dotHovered = Math.sqrt(Math.pow(mouseX - dotCenterX, 2) + Math.pow(mouseY - dotCenterY, 2)) <= dotRadius;
		
		int dotColor = dotHovered || isDraggingSlider ? 0xFFFFFFFF : 0xFFCCCCCC;
		drawCircle(context, dotCenterX, dotCenterY, dotRadius, dotColor);
		
		// Dot icon (arrow or >>>)
		String icon = "§l»»»";
		int iconWidth = this.textRenderer.getWidth(icon);
		context.drawText(this.textRenderer, icon,
			dotCenterX - iconWidth / 2, 
			dotCenterY - this.textRenderer.fontHeight / 2,
			0xFF000000, false);
		
		// Instruction text
		if (sliderDotX < SLIDER_WIDTH - SLIDER_DOT_SIZE - 40) {
			String instruction = fillProgress > 0.3f ? "§cKéo tiếp để hủy timer" : "§7Kéo sang phải để hủy";
			int instructionWidth = this.textRenderer.getWidth(instruction);
			context.drawText(this.textRenderer, instruction,
				sliderX + SLIDER_WIDTH / 2 - instructionWidth / 2,
				sliderY + SLIDER_HEIGHT / 2 - this.textRenderer.fontHeight / 2,
				0xFFFFFFFF, true);
		}
	}
	
	/**
	 * Draw rounded rectangle
	 */
	private void drawRoundedRect(DrawContext context, int x, int y, int width, int height, int radius, int color) {
		// Top and bottom rectangles
		context.fill(x + radius, y, x + width - radius, y + height, color);
		// Left and right rectangles
		context.fill(x, y + radius, x + radius, y + height - radius, color);
		context.fill(x + width - radius, y + radius, x + width, y + height - radius, color);
		
		// Four corner circles (simplified - just fill squares for performance)
		context.fill(x, y + radius, x + radius, y + height - radius, color); // Left
		context.fill(x + width - radius, y + radius, x + width, y + height - radius, color); // Right
		context.fill(x + radius, y, x + width - radius, y + radius, color); // Top
		context.fill(x + radius, y + height - radius, x + width - radius, y + height, color); // Bottom
	}
	
	/**
	 * Draw filled circle (optimized with fewer segments)
	 */
	private void drawCircle(DrawContext context, int centerX, int centerY, int radius, int color) {
		int segments = 24; // Reduced for performance
		for (int i = 0; i < segments; i++) {
			float angle1 = (float)(i * 2 * Math.PI / segments);
			float angle2 = (float)((i + 1) * 2 * Math.PI / segments);
			
			int x1 = (int)(centerX + Math.cos(angle1) * radius);
			int y1 = (int)(centerY + Math.sin(angle1) * radius);
			int x2 = (int)(centerX + Math.cos(angle2) * radius);
			int y2 = (int)(centerY + Math.sin(angle2) * radius);
			
			// Triangle: center -> p1 -> p2
			drawTriangle(context, centerX, centerY, x1, y1, x2, y2, color);
		}
	}
	
	private int interpolateColor(int color1, int color2, float t) {
		int a1 = (color1 >> 24) & 0xFF;
		int r1 = (color1 >> 16) & 0xFF;
		int g1 = (color1 >> 8) & 0xFF;
		int b1 = color1 & 0xFF;
		
		int a2 = (color2 >> 24) & 0xFF;
		int r2 = (color2 >> 16) & 0xFF;
		int g2 = (color2 >> 8) & 0xFF;
		int b2 = color2 & 0xFF;
		
		int a = (int)(a1 + (a2 - a1) * t);
		int r = (int)(r1 + (r2 - r1) * t);
		int g = (int)(g1 + (g2 - g1) * t);
		int b = (int)(b1 + (b2 - b1) * t);
		
		return (a << 24) | (r << 16) | (g << 8) | b;
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0) { // Left click
			// Check slider circle click
			int sliderX = this.width / 2 - SLIDER_WIDTH / 2;
			int sliderY = this.height - 100;
			int dotRadius = SLIDER_DOT_SIZE / 2;
			int dotCenterX = sliderX + sliderDotX + SLIDER_DOT_SIZE / 2;
			int dotCenterY = sliderY + SLIDER_HEIGHT / 2;
			
			double distToDot = Math.sqrt(Math.pow(mouseX - dotCenterX, 2) + Math.pow(mouseY - dotCenterY, 2));
			
			if (distToDot <= dotRadius) {
				isDraggingSlider = true;
				sliderDragStartX = (int)mouseX - sliderDotX;
				return true;
			}
			
			// Check ring drag (click inside ring area)
			int defaultCenterX = this.width / 2;
			int defaultCenterY = this.height / 2 - 60;
			int centerX = defaultCenterX + ringOffsetX;
			int centerY = defaultCenterY + ringOffsetY;
			
			double distFromCenter = Math.sqrt(
				Math.pow(mouseX - centerX, 2) + Math.pow(mouseY - centerY, 2)
			);
			
			if (distFromCenter <= RING_OUTER_RADIUS) {
				isDraggingRing = true;
				dragStartMouseX = (int)mouseX;
				dragStartMouseY = (int)mouseY;
				dragStartOffsetX = ringOffsetX;
				dragStartOffsetY = ringOffsetY;
				return true;
			}
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (isDraggingSlider) {
			// Update slider position
			int sliderX = this.width / 2 - SLIDER_WIDTH / 2;
			sliderDotX = (int)mouseX - sliderDragStartX;
			sliderDotX = Math.max(0, Math.min(SLIDER_WIDTH - SLIDER_DOT_SIZE, sliderDotX));
			return true;
		}
		
		if (isDraggingRing) {
			// Update ring offset
			int deltaMouseX = (int)mouseX - dragStartMouseX;
			int deltaMouseY = (int)mouseY - dragStartMouseY;
			
			ringOffsetX = dragStartOffsetX + deltaMouseX;
			ringOffsetY = dragStartOffsetY + deltaMouseY;
			
			return true;
		}
		
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (isDraggingSlider) {
			isDraggingSlider = false;
			
			// Check if slider was dragged far enough to cancel (>80%)
			float progress = (float)sliderDotX / (SLIDER_WIDTH - SLIDER_DOT_SIZE);
			if (progress >= 0.8f) {
				// Cancel timer
				int elapsedSeconds = ClientDataCache.getElapsedSeconds();
				int targetSeconds = ClientDataCache.getTargetSeconds();
				TimerType type = ClientDataCache.getCurrentTimerType();
				
				boolean tooShort = elapsedSeconds < 60;
				boolean incompleteTarget = (type != TimerType.STOPWATCH && targetSeconds > 0 && elapsedSeconds < targetSeconds);
				boolean abandoned = tooShort || incompleteTarget;
				
				ModNetworking.sendTimerStop(abandoned);
				
				// Return to main menu
				if (this.client != null) {
					this.client.setScreen(new MainMenuScreen());
				}
			} else {
				// Reset slider
				sliderDotX = 0;
			}
			
			return true;
		}
		
		if (isDraggingRing) {
			isDraggingRing = false;
			return true;
		}
		
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override
	public boolean shouldPause() {
		return false;
	}
	
	@Override
	public boolean shouldCloseOnEsc() {
		// Prevent ESC from closing
		return false;
	}
	
	@Override
	public void close() {
		// Do nothing - must use swipe slider
	}
	
	private float calculateProgress(TimerType type, int elapsed, int target) {
		if (type == TimerType.STOPWATCH) {
			int cap = 120 * 60;
			return Math.min(1.0f, (float)elapsed / cap);
		} else {
			if (target <= 0) return 0.0f;
			int remaining = Math.max(0, target - elapsed);
			return (float)remaining / target;
		}
	}
	
	private int[] getRingColor(TimerType type) {
		switch (type) {
			case POMODORO_FOCUS:
			case COUNTDOWN:
				return new int[]{100, 200, 255, 74, 158, 255};
			case POMODORO_SHORT_BREAK:
				return new int[]{100, 255, 150, 50, 200, 100};
			case POMODORO_LONG_BREAK:
				return new int[]{200, 150, 255, 150, 100, 200};
			case STOPWATCH:
			default:
				return new int[]{100, 255, 255, 50, 200, 200};
		}
	}
	
	private void renderProgressRing(DrawContext context, int centerX, int centerY, 
	                                 float progress, int[] color, TimerType type) {
		// 1. Dim background track
		for (int i = 0; i < RING_SEGMENTS; i++) {
			float angle1 = (float)(i * 2 * Math.PI / RING_SEGMENTS);
			float angle2 = (float)((i + 1) * 2 * Math.PI / RING_SEGMENTS);
			
			drawRingSegmentSimple(context, centerX, centerY, 
				angle1, angle2, RING_INNER_RADIUS, RING_OUTER_RADIUS,
				0x50282828);
		}
		
		// 2. Colored progress arc
		if (progress > 0) {
			int progressSegments = (int)(RING_SEGMENTS * progress);
			if (progressSegments > 0) {
				float startAngle = -(float)Math.PI / 2;
				
				for (int i = 0; i < progressSegments; i++) {
					float t = (float)i / progressSegments;
					
					int r, g, b;
					if (type == TimerType.STOPWATCH) {
						r = (int)(color[0] + (color[3] - color[0]) * t);
						g = (int)(color[1] + (color[4] - color[1]) * t);
						b = (int)(color[2] + (color[5] - color[2]) * t);
					} else {
						r = (int)(color[3] + (color[0] - color[3]) * (1 - t));
						g = (int)(color[4] + (color[1] - color[4]) * (1 - t));
						b = (int)(color[5] + (color[2] - color[5]) * (1 - t));
					}
					
					int arcColor = 0xFF000000 | (r << 16) | (g << 8) | b;
					
					float angle1 = startAngle + (float)(i * 2 * Math.PI / RING_SEGMENTS);
					float angle2 = startAngle + (float)((i + 1) * 2 * Math.PI / RING_SEGMENTS);
					
					drawRingSegmentSimple(context, centerX, centerY,
						angle1, angle2, RING_INNER_RADIUS, RING_OUTER_RADIUS,
						arcColor);
				}
				
				// 3. Glowing head
				float headAngle = startAngle + (float)(progressSegments * 2 * Math.PI / RING_SEGMENTS);
				float headX = centerX + (float)Math.cos(headAngle) * ((RING_INNER_RADIUS + RING_OUTER_RADIUS) / 2);
				float headY = centerY + (float)Math.sin(headAngle) * ((RING_INNER_RADIUS + RING_OUTER_RADIUS) / 2);
				
				int headColor = 0xFF000000 | (color[0] << 16) | (color[1] << 8) | color[2];
				for (int i = 0; i < 16; i++) {
					float a1 = (float)(i * 2 * Math.PI / 16);
					float a2 = (float)((i + 1) * 2 * Math.PI / 16);
					
					int x1 = (int)(headX + Math.cos(a1) * GLOW_HEAD_SIZE);
					int y1 = (int)(headY + Math.sin(a1) * GLOW_HEAD_SIZE);
					int x2 = (int)(headX + Math.cos(a2) * GLOW_HEAD_SIZE);
					int y2 = (int)(headY + Math.sin(a2) * GLOW_HEAD_SIZE);
					
					drawTriangle(context, (int)headX, (int)headY, x1, y1, x2, y2, headColor);
				}
			}
		}
	}
	
	private void drawRingSegmentSimple(DrawContext context, float centerX, float centerY,
	                                   float angle1, float angle2, 
	                                   float innerRadius, float outerRadius,
	                                   int color) {
		float cos1 = (float)Math.cos(angle1);
		float sin1 = (float)Math.sin(angle1);
		float cos2 = (float)Math.cos(angle2);
		float sin2 = (float)Math.sin(angle2);
		
		int x1Inner = (int)(centerX + cos1 * innerRadius);
		int y1Inner = (int)(centerY + sin1 * innerRadius);
		int x1Outer = (int)(centerX + cos1 * outerRadius);
		int y1Outer = (int)(centerY + sin1 * outerRadius);
		
		int x2Inner = (int)(centerX + cos2 * innerRadius);
		int y2Inner = (int)(centerY + sin2 * innerRadius);
		int x2Outer = (int)(centerX + cos2 * outerRadius);
		int y2Outer = (int)(centerY + sin2 * outerRadius);
		
		drawTriangle(context, x1Inner, y1Inner, x1Outer, y1Outer, x2Outer, y2Outer, color);
		drawTriangle(context, x1Inner, y1Inner, x2Outer, y2Outer, x2Inner, y2Inner, color);
	}
	
	private void drawTriangle(DrawContext context, int x1, int y1, int x2, int y2, int x3, int y3, int color) {
		int minX = Math.min(x1, Math.min(x2, x3));
		int maxX = Math.max(x1, Math.max(x2, x3));
		int minY = Math.min(y1, Math.min(y2, y3));
		int maxY = Math.max(y1, Math.max(y2, y3));
		
		for (int y = minY; y <= maxY; y++) {
			for (int x = minX; x <= maxX; x++) {
				if (isPointInTriangle(x, y, x1, y1, x2, y2, x3, y3)) {
					context.fill(x, y, x + 1, y + 1, color);
				}
			}
		}
	}
	
	private boolean isPointInTriangle(int px, int py, int x1, int y1, int x2, int y2, int x3, int y3) {
		float d1 = sign(px, py, x1, y1, x2, y2);
		float d2 = sign(px, py, x2, y2, x3, y3);
		float d3 = sign(px, py, x3, y3, x1, y1);
		
		boolean hasNeg = (d1 < 0) || (d2 < 0) || (d3 < 0);
		boolean hasPos = (d1 > 0) || (d2 > 0) || (d3 > 0);
		
		return !(hasNeg && hasPos);
	}
	
	private float sign(int p1x, int p1y, int p2x, int p2y, int p3x, int p3y) {
		return (p1x - p3x) * (p2y - p3y) - (p2x - p3x) * (p1y - p3y);
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
}
