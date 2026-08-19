package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.client.ClientProfileCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * Detailed Stats Tab (Thống kê) - comprehensive stats view (v1.0.6 Phase 5)
 * v1.0.6 Phase 0.3: Added scroll support
 */
public class DetailedStatsTabScreen {
	
	private final MainMenuScreen parent;
	private int scrollOffset = 0; // v1.0.6 Phase 0.3
	private int contentHeight = 0; // Track total content height (Bug Fix 2)
	private int viewportHeight = 0; // Track viewport height (Bug Fix 2)
	
	public DetailedStatsTabScreen(MainMenuScreen parent) {
		this.parent = parent;
	}
	
	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		viewportHeight = height; // Store for scroll clamping (Bug Fix 2)
		int startY = y;
		
		int contentX = x + 10;
		int contentY = y + 10 - scrollOffset; // Apply scroll offset
		int contentWidth = width - 20;
		
		int currentY = contentY;
		
		// Section 1: Tập trung
		currentY = renderSection(context, contentX, currentY, contentWidth, "🎯 TẬP TRUNG", new String[][] {
			{"Tổng giờ", formatHours(ClientProfileCache.getTotalFocusTimeSeconds())},
			{"Tổng số phiên", String.valueOf(ClientProfileCache.getTotalSessionsCompleted())},
			{"Phiên trung bình", formatAvgSession()},
			{"Kỷ lục 1 phiên", formatTime(ClientProfileCache.getLongestSingleSessionSeconds())}
		});
		currentY += 12;
		
		// Section 2: Kinh tế
		currentY = renderSection(context, contentX, currentY, contentWidth, "💰 KINH TẾ", new String[][] {
			{"Tổng Silver kiếm được", formatNumber(ClientDataCache.getTotalSilverEarned())},
			{"Tổng Silver đã quy đổi Gold", formatNumber(ClientDataCache.getTotalSilverConvertedToGold())},
			{"Tổng Focus XP (trọn đời)", formatNumber(ClientProfileCache.getTotalFocusXpEarned())},
			{"Silver hiện tại", formatNumber(ClientDataCache.getSilverCoins())},
			{"Gold hiện tại", formatNumber(ClientDataCache.getGoldCoins())},
			{"Focus XP hiện tại", formatNumber(ClientDataCache.getFocusXp())}
		});
		currentY += 12;
		
		// Section 3: Hoạt động khác
		currentY = renderSection(context, contentX, currentY, contentWidth, "📊 HOẠT ĐỘNG KHÁC", new String[][] {
			{"Tổng lượt mua hàng", String.valueOf(ClientDataCache.getTotalItemsPurchased())},
			{"Tổng Lucky Chest đã mở", String.valueOf(ClientDataCache.getTotalChestsOpened())},
			{"Tổng khối đã đào", formatNumber(ClientDataCache.getTotalBlocksMined())}
		});
		currentY += 12;
		
		// Section 4: Streak
		currentY = renderSection(context, contentX, currentY, contentWidth, "🔥 STREAK", new String[][] {
			{"Streak hiện tại", ClientProfileCache.getCurrentStreakDays() + " ngày"},
			{"Streak dài nhất", ClientProfileCache.getLongestStreakDays() + " ngày"},
			{"Ngày tập trung gần nhất", formatEpochDate(ClientProfileCache.getLastFocusDate())}
		});
		
		// Store total content height (Bug Fix 2)
		contentHeight = (currentY + scrollOffset) - startY;
	}
	
	/**
	 * Render a section card
	 */
	private int renderSection(DrawContext context, int x, int y, int width, String title, String[][] rows) {
		int rowHeight = 16;
		int cardHeight = 30 + (rows.length * rowHeight);
		
		// Card background + border
		context.fill(x, y, x + width, y + cardHeight, 0xD0000000);
		context.drawBorder(x, y, width, cardHeight, 0xFF555555);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§l" + title, x + 8, y + 8, 0xFF00FFFF, false);
		
		// Rows (two-column: label | value)
		int rowY = y + 26;
		for (String[] row : rows) {
			String label = row[0];
			String value = row[1];
			
			// Label (left)
			context.drawText(parent.getTextRenderer(), "§7" + label + ":", x + 12, rowY, 0xFFAAAAAA, false);
			
			// Value (right-aligned)
			int valueWidth = parent.getTextRenderer().getWidth(value);
			context.drawText(parent.getTextRenderer(), "§f" + value, 
				x + width - valueWidth - 12, rowY, 0xFFFFFFFF, false);
			
			rowY += rowHeight;
		}
		
		return y + cardHeight;
	}
	
	// ===== UTILITY METHODS =====
	
	private String formatHours(long seconds) {
		double hours = seconds / 3600.0;
		return String.format("%.1f giờ", hours);
	}
	
	private String formatAvgSession() {
		long total = ClientProfileCache.getTotalFocusTimeSeconds();
		long sessions = ClientProfileCache.getTotalSessionsCompleted();
		if (sessions == 0) return "0 phút";
		
		long avgSeconds = total / sessions;
		int minutes = (int)(avgSeconds / 60);
		return minutes + " phút";
	}
	
	private String formatTime(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		if (hours > 0) {
			return String.format("%dh %02dm %02ds", hours, minutes, secs);
		}
		return String.format("%dm %02ds", minutes, secs);
	}
	
	private String formatNumber(long number) {
		if (number >= 1_000_000_000) {
			return String.format("%.1fB", number / 1_000_000_000.0);
		} else if (number >= 1_000_000) {
			return String.format("%.1fM", number / 1_000_000.0);
		} else if (number >= 1_000) {
			return String.format("%.1fK", number / 1_000.0);
		}
		return String.valueOf(number);
	}
	
	private String formatEpochDate(long epochSeconds) {
		if (epochSeconds == 0) {
			return "Chưa có";
		}
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
		return sdf.format(new java.util.Date(epochSeconds * 1000));
	}
	
	/**
	 * Handle mouse scroll (v1.0.6 Phase 0.3)
	 * v1.0.6 Bug Fix 2: Clamp scroll to actual content height
	 */
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		scrollOffset -= (int)(amount * 20);
		
		// Clamp to valid range (Bug Fix 2)
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		if (scrollOffset < 0) scrollOffset = 0;
		if (scrollOffset > maxScroll) scrollOffset = maxScroll;
		
		return true;
	}
}
