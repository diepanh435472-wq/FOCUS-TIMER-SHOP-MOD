package com.focustimershop.client.gui;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.music.MusicPlayerManager;
import com.focustimershop.music.MusicTrack;
import com.focustimershop.music.Playlist;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Music Player popup UI
 * Shows playlist, playback controls, and add music button
 */
public class MusicPlayerScreen extends Screen {
	private final Screen parent;
	private int popupX, popupY, popupWidth, popupHeight;
	private int playlistScrollOffset = 0;
	private int maxScroll = 0;
	private boolean isDraggingScrollbar = false;
	
	// UI regions
	private int playlistY, playlistHeight;
	private int controlsY;
	
	public MusicPlayerScreen(Screen parent) {
		super(Text.literal("Music Player"));
		this.parent = parent;
	}
	
	@Override
	protected void init() {
		super.init();
		
		// Center popup
		popupWidth = Math.min(500, width - 40);
		popupHeight = Math.min(400, height - 40);
		popupX = (width - popupWidth) / 2;
		popupY = (height - popupHeight) / 2;
		
		// Layout
		playlistY = popupY + 60;
		playlistHeight = popupHeight - 140;
		controlsY = popupY + popupHeight - 70;
		
		// Calculate max scroll
		updateMaxScroll();
	}
	
	private void updateMaxScroll() {
		Playlist playlist = MusicPlayerManager.getPlaylist();
		int itemHeight = 25;
		int totalHeight = playlist.size() * itemHeight;
		maxScroll = Math.max(0, totalHeight - playlistHeight);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Darken background
		context.fill(0, 0, width, height, 0xAA000000);
		
		// Popup background
		context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF2D2D2D);
		context.fill(popupX, popupY, popupX + popupWidth, popupY + 2, 0xFF5ABAFF);
		
		// Title bar
		context.drawCenteredTextWithShadow(textRenderer, "§l§6♪ Music Player", 
			popupX + popupWidth / 2, popupY + 10, 0xFFFFFFFF);
		
		// Close button (X)
		int closeX = popupX + popupWidth - 30;
		int closeY = popupY + 5;
		boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 20 && 
		                        mouseY >= closeY && mouseY <= closeY + 20;
		context.drawText(textRenderer, closeHovered ? "§c§lX" : "§7X", closeX + 6, closeY + 6, 0xFFFFFFFF, false);
		
		// Current track display
		renderCurrentTrack(context, popupX + 10, popupY + 30);
		
		// Playlist
		renderPlaylist(context, popupX + 10, playlistY, popupWidth - 20, playlistHeight, mouseX, mouseY);
		
		// Controls
		renderControls(context, popupX + 10, controlsY, popupWidth - 20, mouseX, mouseY);
		
		// Open folder button (replaced Add Music)
		renderOpenFolderButton(context, popupX + 10, popupY + popupHeight - 30, mouseX, mouseY);
		
		// Rescan button
		renderRescanButton(context, popupX + 140, popupY + popupHeight - 30, mouseX, mouseY);
		
		super.render(context, mouseX, mouseY, delta);
	}
	
	private void renderCurrentTrack(DrawContext context, int x, int y) {
		MusicTrack track = MusicPlayerManager.getCurrentTrack();
		if (track == null) {
			context.drawText(textRenderer, "§7No track playing", x, y, 0xFFAAAAAA, false);
			return;
		}
		
		String status = MusicPlayerManager.isPlaying() ? "§a▶" : (MusicPlayerManager.isPaused() ? "§e❚❚" : "§7■");
		String title = status + " §f" + track.getTitle();
		
		context.drawText(textRenderer, title, x, y, 0xFFFFFFFF, true);
		
		// Progress bar
		int pos = MusicPlayerManager.getCurrentPosition();
		int duration = track.getDurationSeconds();
		if (duration > 0) {
			int barWidth = 200;
			int barX = x + 250;
			float progress = Math.min(1.0f, (float)pos / duration);
			
			context.fill(barX, y + 2, barX + barWidth, y + 6, 0xFF444444);
			context.fill(barX, y + 2, barX + (int)(barWidth * progress), y + 6, 0xFF5ABAFF);
			
			String timeText = String.format("%02d:%02d / %s", pos / 60, pos % 60, track.getFormattedDuration());
			context.drawText(textRenderer, timeText, barX + barWidth + 10, y, 0xFFAAAAAA, false);
		}
	}
	
	private void renderPlaylist(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY) {
		// Title
		context.drawText(textRenderer, "§7Playlist:", x, y - 15, 0xFFAAAAAA, false);
		
		// Playlist background
		context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
		
		// Enable scissor (clipping)
		context.enableScissor(x, y, x + width - 10, y + height);
		
		Playlist playlist = MusicPlayerManager.getPlaylist();
		if (playlist.isEmpty()) {
			context.drawText(textRenderer, "§7No tracks. Click 'Add Music' to add songs.", 
				x + 10, y + height / 2 - 5, 0xFFAAAAAA, false);
		} else {
			int itemHeight = 25;
			int currentIndex = playlist.getCurrentTrackIndex();
			
			for (int i = 0; i < playlist.getTracks().size(); i++) {
				MusicTrack track = playlist.getTracks().get(i);
				int itemY = y + i * itemHeight - playlistScrollOffset;
				
				// Skip if out of view
				if (itemY + itemHeight < y || itemY > y + height) continue;
				
				boolean isHovered = mouseX >= x && mouseX <= x + width - 10 && 
				                    mouseY >= itemY && mouseY <= itemY + itemHeight;
				boolean isCurrent = (i == currentIndex);
				
				// Background
				int bgColor = isCurrent ? 0xFF3A5A7A : (isHovered ? 0xFF2A2A2A : 0xFF1A1A1A);
				context.fill(x + 2, itemY + 2, x + width - 12, itemY + itemHeight - 2, bgColor);
				
				// Track number
				String numText = (i + 1) + ".";
				context.drawText(textRenderer, numText, x + 8, itemY + 8, 0xFF888888, false);
				
				// Track title
				String titleText = track.getTitle();
				if (isCurrent) titleText = "§b" + titleText;
				context.drawText(textRenderer, titleText, x + 30, itemY + 8, 0xFFFFFFFF, false);
				
				// Duration
				context.drawText(textRenderer, "§7" + track.getFormattedDuration(), 
					x + width - 70, itemY + 8, 0xFFAAAAAA, false);
			}
		}
		
		context.disableScissor();
		
		// Scrollbar
		if (maxScroll > 0) {
			renderScrollbar(context, x + width - 8, y, 6, height);
		}
	}
	
	private void renderScrollbar(DrawContext context, int x, int y, int width, int height) {
		// Scrollbar track
		context.fill(x, y, x + width, y + height, 0xFF333333);
		
		// Scrollbar thumb
		float scrollRatio = (float)playlistScrollOffset / maxScroll;
		float thumbHeight = Math.max(20, height * 0.3f);
		float thumbY = y + (height - thumbHeight) * scrollRatio;
		
		context.fill(x, (int)thumbY, x + width, (int)(thumbY + thumbHeight), 0xFF5ABAFF);
	}
	
	private void renderControls(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		int btnSize = 40;
		int spacing = 10;
		int startX = x + width / 2 - (4 * btnSize + 3 * spacing) / 2;
		
		// Previous button
		renderControlButton(context, "⏮", startX, y, btnSize, mouseX, mouseY);
		
		// Play/Pause button
		String playIcon = MusicPlayerManager.isPlaying() ? "⏸" : "▶";
		renderControlButton(context, playIcon, startX + btnSize + spacing, y, btnSize, mouseX, mouseY);
		
		// Stop button
		renderControlButton(context, "⏹", startX + 2 * (btnSize + spacing), y, btnSize, mouseX, mouseY);
		
		// Next button
		renderControlButton(context, "⏭", startX + 3 * (btnSize + spacing), y, btnSize, mouseX, mouseY);
	}
	
	private void renderControlButton(DrawContext context, String icon, int x, int y, int size, int mouseX, int mouseY) {
		boolean hovered = mouseX >= x && mouseX <= x + size && 
		                  mouseY >= y && mouseY <= y + size;
		
		int bgColor = hovered ? 0xFF4A9EFF : 0xFF3A3A3A;
		context.fill(x, y, x + size, y + size, bgColor);
		
		// Icon
		int textWidth = textRenderer.getWidth(icon);
		context.drawText(textRenderer, icon, x + (size - textWidth) / 2, y + 14, 0xFFFFFFFF, false);
	}
	
	private void renderOpenFolderButton(DrawContext context, int x, int y, int mouseX, int mouseY) {
		int btnWidth = 120;
		int btnHeight = 25;
		
		boolean hovered = mouseX >= x && mouseX <= x + btnWidth && 
		                  mouseY >= y && mouseY <= y + btnHeight;
		
		int bgColor = hovered ? 0xFF5ABAFF : 0xFF4A9EFF;
		context.fill(x, y, x + btnWidth, y + btnHeight, bgColor);
		
		context.drawCenteredTextWithShadow(textRenderer, "§l📁 Open Folder", 
			x + btnWidth / 2, y + 8, 0xFFFFFFFF);
		
		// Instruction text
		context.drawText(textRenderer, "§7Put your .ogg/.wav files here, then click Rescan:", x, y + 30, 0xFF888888, false);
		String dirPath = MusicPlayerManager.getMusicDirectory();
		context.drawText(textRenderer, "§e" + dirPath, x, y + 42, 0xFFFFAA00, false);
	}
	
	private void renderRescanButton(DrawContext context, int x, int y, int mouseX, int mouseY) {
		int btnWidth = 100;
		int btnHeight = 25;
		
		boolean hovered = mouseX >= x && mouseX <= x + btnWidth && 
		                  mouseY >= y && mouseY <= y + btnHeight;
		
		int bgColor = hovered ? 0xFF6ABA6A : 0xFF5AAA5A;
		context.fill(x, y, x + btnWidth, y + btnHeight, bgColor);
		
		context.drawCenteredTextWithShadow(textRenderer, "§l↻ Rescan", 
			x + btnWidth / 2, y + 8, 0xFFFFFFFF);
	}
	
	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button != 0) return false;
		
		int mx = (int)mouseX;
		int my = (int)mouseY;
		
		// Close button
		int closeX = popupX + popupWidth - 30;
		int closeY = popupY + 5;
		if (mx >= closeX && mx <= closeX + 20 && my >= closeY && my <= closeY + 20) {
			close();
			return true;
		}
		
		// Open folder button (replaced Add Music with reliable alternative)
		int addBtnX = popupX + 10;
		int addBtnY = popupY + popupHeight - 30;
		if (mx >= addBtnX && mx <= addBtnX + 120 && my >= addBtnY && my <= addBtnY + 25) {
			MusicPlayerManager.openMusicDirectory();
			return true;
		}
		
		// Rescan button (next to Open Folder)
		int rescanBtnX = addBtnX + 130;
		if (mx >= rescanBtnX && mx <= rescanBtnX + 100 && my >= addBtnY && my <= addBtnY + 25) {
			MusicPlayerManager.rescanMusicDirectory();
			updateMaxScroll();
			return true;
		}
		
		// Control buttons
		int btnSize = 40;
		int spacing = 10;
		int startX = popupX + 10 + (popupWidth - 20) / 2 - (4 * btnSize + 3 * spacing) / 2;
		
		if (my >= controlsY && my <= controlsY + btnSize) {
			// Previous
			if (mx >= startX && mx <= startX + btnSize) {
				MusicPlayerManager.previous();
				return true;
			}
			// Play/Pause
			if (mx >= startX + btnSize + spacing && mx <= startX + 2 * btnSize + spacing) {
				if (MusicPlayerManager.isPlaying()) {
					MusicPlayerManager.pause();
				} else {
					MusicPlayerManager.play();
				}
				return true;
			}
			// Stop
			if (mx >= startX + 2 * (btnSize + spacing) && mx <= startX + 3 * btnSize + 2 * spacing) {
				MusicPlayerManager.stop();
				return true;
			}
			// Next
			if (mx >= startX + 3 * (btnSize + spacing) && mx <= startX + 4 * btnSize + 3 * spacing) {
				MusicPlayerManager.next();
				return true;
			}
		}
		
		// Playlist click (play track)
		int playlistX = popupX + 10;
		if (mx >= playlistX && mx <= playlistX + popupWidth - 20 && 
		    my >= playlistY && my <= playlistY + playlistHeight) {
			int itemHeight = 25;
			int relativeY = my - playlistY + playlistScrollOffset;
			int clickedIndex = relativeY / itemHeight;
			
			Playlist playlist = MusicPlayerManager.getPlaylist();
			if (clickedIndex >= 0 && clickedIndex < playlist.size()) {
				MusicPlayerManager.playTrack(clickedIndex);
				return true;
			}
		}
		
		// Scrollbar click
		int scrollbarX = popupX + popupWidth - 18;
		if (mx >= scrollbarX && mx <= scrollbarX + 6 && 
		    my >= playlistY && my <= playlistY + playlistHeight) {
			isDraggingScrollbar = true;
			return true;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
		// Scroll playlist
		int scrollAmount = (int)(verticalAmount * 25);
		playlistScrollOffset = Math.max(0, Math.min(maxScroll, playlistScrollOffset - scrollAmount));
		updateMaxScroll();
		return true;
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (isDraggingScrollbar) {
			float ratio = (float)(mouseY - playlistY) / playlistHeight;
			playlistScrollOffset = Math.max(0, Math.min(maxScroll, (int)(ratio * maxScroll)));
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		isDraggingScrollbar = false;
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}
	
	@Override
	public boolean shouldPause() {
		return false;  // Don't pause game
	}
}
