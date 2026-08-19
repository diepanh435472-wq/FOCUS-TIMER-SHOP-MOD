package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import com.focustimershop.profile.RankTier;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import org.joml.Quaternionf;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Profile tab - Comprehensive UI (v1.0.6 Phases 2-6)
 * Shows: avatar, stats, rank progress, ETA, personal bests, weekly goal, focus history, achievements, missions
 * v1.0.6 Phase 5: Added 4-tab navigation (Tổng quát, Thống kê, Thành tựu, Danh hiệu)
 * v1.0.6 Phase 0.3: Added scroll support for all tabs
 */
public class ProfileTabScreen {
	
	private final MainMenuScreen parent;
	
	// Layout constants
	private static final int SECTION_PADDING = 12;
	private static final int CARD_PADDING = 8;
	private static final int TAB_BAR_HEIGHT = 30;
	
	// Current active tab
	private ProfileTab currentTab = ProfileTab.OVERVIEW;
	
	// Tab screens
	private DetailedStatsTabScreen statsScreen;
	private AchievementsTabScreen achievementsScreen;
	private TitlesTabScreen titlesScreen;
	
	// Scroll offset for Overview tab (v1.0.6 Phase 0.3)
	private int overviewScrollOffset = 0;
	private int overviewContentHeight = 0; // Track total content height (Bug Fix 2)
	private int overviewViewportHeight = 0; // Track viewport height (Bug Fix 2)
	
	// Rank card position tracking (v1.0.6 Phase 3)
	private int rankCardX = 0;
	private int rankCardY = 0;
	private int rankCardWidth = 0;
	private int rankCardHeight = 90;
	
	public ProfileTabScreen(MainMenuScreen parent) {
		this.parent = parent;
		this.statsScreen = new DetailedStatsTabScreen(parent);
		this.achievementsScreen = new AchievementsTabScreen(parent);
		this.titlesScreen = new TitlesTabScreen(parent);
	}
	
	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		ClientPlayerEntity player = MinecraftClient.getInstance().player;
		if (player == null) {
			return;
		}
		
		// Get profile data from cache
		long totalXp = com.focustimershop.client.ClientProfileCache.getTotalFocusXpEarned();
		RankTier rank = com.focustimershop.profile.RankManager.resolveRank(totalXp);
		
		int contentX = x + 10;
		int contentY = y + 10;
		int contentWidth = width - 20;
		
		// === FIXED HEADER SECTION (never scrolls) ===
		// Render tab navigation bar at FIXED position
		renderTabBar(context, contentX, contentY, contentWidth, mouseX, mouseY);
		
		// Calculate scrollable content area
		int scrollableY = contentY + TAB_BAR_HEIGHT + 8;
		int scrollableHeight = height - (scrollableY - y) - 10;
		
		// Store viewport height for scroll clamping (Bug Fix 2)
		overviewViewportHeight = scrollableHeight;
		
		// === SCROLLABLE CONTENT SECTION ===
		// Enable scissor to clip content to scrollable area
		context.enableScissor(x, scrollableY, x + width, scrollableY + scrollableHeight);
		
		// Render tab content with scroll offset applied
		switch (currentTab) {
			case OVERVIEW:
				renderOverviewTab(context, contentX, scrollableY, contentWidth, scrollableHeight, player, rank, mouseX, mouseY);
				break;
			case STATS:
				statsScreen.render(context, contentX, scrollableY, contentWidth, scrollableHeight, mouseX, mouseY, delta);
				break;
			case ACHIEVEMENTS:
				achievementsScreen.render(context, contentX, scrollableY, contentWidth, scrollableHeight, mouseX, mouseY, delta);
				break;
			case TITLES:
				titlesScreen.render(context, contentX, scrollableY, contentWidth, scrollableHeight, mouseX, mouseY, delta);
				break;
		}
		
		// Disable scissor
		context.disableScissor();
	}
	
	/**
	 * Render tab navigation bar
	 */
	private void renderTabBar(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		ProfileTab[] tabs = ProfileTab.values();
		int tabWidth = width / tabs.length;
		
		for (int i = 0; i < tabs.length; i++) {
			ProfileTab tab = tabs[i];
			int tabX = x + (i * tabWidth);
			boolean active = tab == currentTab;
			boolean hovered = mouseX >= tabX && mouseX < tabX + tabWidth &&
			                  mouseY >= y && mouseY < y + TAB_BAR_HEIGHT;
			
			// Background
			int bgColor = active ? 0xFF2A4A6A : (hovered ? 0xFF1A3A5A : 0xFF0A2A4A);
			context.fill(tabX, y, tabX + tabWidth, y + TAB_BAR_HEIGHT, bgColor);
			
			// Border
			int borderColor = active ? 0xFF4A9EFF : 0xFF555555;
			context.drawBorder(tabX, y, tabWidth, TAB_BAR_HEIGHT, borderColor);
			
			// Tab name
			String tabName = tab.getDisplayName();
			int textColor = active ? 0xFFFFFFFF : 0xFFAAAAAA;
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), tabName, 
				tabX + tabWidth / 2, y + 10, textColor);
		}
	}
	
	/**
	 * Render overview tab (original Profile content)
	 * v1.0.6 Phase 0.3: Added scroll offset
	 * v1.0.6 Bug Fix 1: Scroll offset applied HERE, not affecting tab bar
	 * v1.0.6 Bug Fix 2: Track content height for proper scroll clamping
	 */
	private void renderOverviewTab(DrawContext context, int contentX, int contentY, 
	                                int contentWidth, int scrollableHeight, 
	                                ClientPlayerEntity player, RankTier rank,
	                                int mouseX, int mouseY) {
		// Apply scroll offset to content ONLY
		int startY = contentY;
		int currentY = contentY - overviewScrollOffset;
		
		// Row 1: Avatar + Stats Grid (side by side)
		int avatarWidth = 160;
		int statsGridX = contentX + avatarWidth + SECTION_PADDING;
		int statsGridWidth = contentWidth - avatarWidth - SECTION_PADDING;
		
		renderAvatarCard(context, contentX, currentY, avatarWidth, 200, player, rank);
		renderStatsGrid(context, statsGridX, currentY, statsGridWidth, 200);
		currentY += 200 + SECTION_PADDING;
		
		// Row 2: Rank Progress + Next Rank ETA
		// Store world coordinates (before scroll adjustment) for click detection (Bug Fix 6)
		rankCardX = contentX;
		rankCardY = currentY + overviewScrollOffset; // Convert back to world coords
		rankCardWidth = contentWidth;
		renderRankSection(context, contentX, currentY, contentWidth, rank);
		currentY += 90 + SECTION_PADDING;
		
		// Row 3: Personal Bests + Weekly Goal (side by side)
		int halfWidth = (contentWidth - SECTION_PADDING) / 2;
		renderPersonalBests(context, contentX, currentY, halfWidth, 120);
		renderWeeklyGoal(context, contentX + halfWidth + SECTION_PADDING, currentY, halfWidth, 120);
		currentY += 120 + SECTION_PADDING;
		
		// Row 4: Focus History (7-day chart)
		renderFocusHistory(context, contentX, currentY, contentWidth, 100);
		currentY += 100 + SECTION_PADDING;
		
		// Row 5: Achievements + Missions (side by side)
		renderAchievements(context, contentX, currentY, halfWidth, 80);
		renderMissions(context, contentX + halfWidth + SECTION_PADDING, currentY, halfWidth, 80);
		currentY += 80 + SECTION_PADDING;
		
		// Row 6: Balance Bar (full width at bottom)
		renderBalanceBar(context, contentX, currentY, contentWidth);
		currentY += 40; // Balance bar height
		
		// Store total content height (Bug Fix 2)
		overviewContentHeight = (currentY + overviewScrollOffset) - startY;
	}
	
	/**
	 * Avatar card - player render + names + rank-colored border
	 */
	private void renderAvatarCard(DrawContext context, int x, int y, int width, int height, 
	                               ClientPlayerEntity player, RankTier rank) {
		// Get rank border color
		int borderColor = parseBorderColor(rank.getFrameColor());
		if (rank.getTier() != null && rank.getTier().contains("Legend")) {
			borderColor = getLegendGradientColor();
		}
		
		// Card background + border
		context.fill(x, y, x + width, y + height, 0xD0000000);
		context.drawBorder(x, y, width, height, borderColor);
		
		// Render 3D player entity (centered)
		int playerRenderY = y + 100;
		int playerRenderX = x + width / 2;
		renderPlayerEntity(context, playerRenderX, playerRenderY, 45, player);
		
		// In-game name (below player)
		String inGameName = com.focustimershop.client.ClientProfileCache.getInGameName();
		if (inGameName.isEmpty()) {
			inGameName = player.getName().getString();
		}
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), 
			"§f" + inGameName, x + width / 2, y + 130, 0xFFFFFFFF);
		
		// Custom name line (with edit icon)
		String customName = com.focustimershop.client.ClientProfileCache.getCustomName();
		String customDisplay = (customName != null && !customName.isEmpty()) ? customName : "[Chưa đặt]";
		String customLine = "§7" + customDisplay + "  §e✎";
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), 
			customLine, x + width / 2, y + 145, 0xFFAAAAAA);
		
		// Rank name (bottom of card)
		String rankText = "§b" + rank.getDisplayName();
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), 
			rankText, x + width / 2, y + height - 20, 0xFF4A9EFF);
	}
	
	/**
	 * Stats grid - 2x3 small cards
	 */
	private void renderStatsGrid(DrawContext context, int x, int y, int gridWidth, int gridHeight) {
		int currentStreak = com.focustimershop.client.ClientProfileCache.getCurrentStreakDays();
		int longestSession = com.focustimershop.client.ClientProfileCache.getLongestSingleSessionSeconds();
		long totalFocusTime = com.focustimershop.client.ClientProfileCache.getTotalFocusTimeSeconds();
		long totalSessions = com.focustimershop.client.ClientProfileCache.getTotalSessionsCompleted();
		String favoriteTimer = com.focustimershop.client.ClientProfileCache.getFavoriteTimerType();
		long profileCreated = com.focustimershop.client.ClientProfileCache.getProfileCreatedAtEpochSeconds();
		
		int cardWidth = (gridWidth - CARD_PADDING) / 2;
		int cardHeight = (gridHeight - CARD_PADDING * 2) / 3;
		
		int col1X = x;
		int col2X = x + cardWidth + CARD_PADDING;
		
		// Row 1
		renderSmallStatCard(context, col1X, y, cardWidth, cardHeight, "🔥 Streak", currentStreak + " ngày");
		renderSmallStatCard(context, col2X, y, cardWidth, cardHeight, "⏱ Kỷ lục phiên", formatTime(longestSession));
		
		// Row 2
		int row2Y = y + cardHeight + CARD_PADDING;
		renderSmallStatCard(context, col1X, row2Y, cardWidth, cardHeight, "⏳ Tổng tập trung", formatDuration(totalFocusTime));
		renderSmallStatCard(context, col2X, row2Y, cardWidth, cardHeight, "✅ Số phiên", String.valueOf(totalSessions));
		
		// Row 3
		int row3Y = row2Y + cardHeight + CARD_PADDING;
		renderSmallStatCard(context, col1X, row3Y, cardWidth, cardHeight, "⭐ Timer yêu thích", favoriteTimer);
		renderSmallStatCard(context, col2X, row3Y, cardWidth, cardHeight, "📅 Thành viên từ", formatEpochDate(profileCreated));
	}
	
	private void renderSmallStatCard(DrawContext context, int x, int y, int width, int height, String label, String value) {
		// Background + border
		context.fill(x, y, x + width, y + height, 0xD0000000);
		context.drawBorder(x, y, width, height, 0xFF555555);
		
		// Label (small, top)
		context.drawText(parent.getTextRenderer(), "§7" + label, x + 4, y + 4, 0xFFAAAAAA, false);
		
		// Value (large, centered)
		int valueWidth = parent.getTextRenderer().getWidth(value);
		context.drawText(parent.getTextRenderer(), "§f" + value, 
			x + (width - valueWidth) / 2, y + height - 16, 0xFFFFFFFF, true);
	}
	
	/**
	 * Rank section - progress bar + next rank ETA (Phase 2)
	 */
	private void renderRankSection(DrawContext context, int x, int y, int width, RankTier rank) {
		// Background
		context.fill(x, y, x + width, y + 90, 0xD0000000);
		context.drawBorder(x, y, width, 90, 0xFF555555);
		
		// Rank name (large)
		String rankDisplay = "–  " + rank.getDisplayName().toUpperCase();
		context.drawText(parent.getTextRenderer(), "§l§b" + rankDisplay, x + 10, y + 8, 0xFF4A9EFF, false);
		
		// XP display (right side)
		long xpInto = rank.getXpIntoLevel();
		long xpNeeded = rank.getXpNeededForLevel();
		int percent = rank.getPercent();
		String xpText = String.format("%d / %d XP    %d%%", xpInto, xpNeeded, percent);
		int xpTextWidth = parent.getTextRenderer().getWidth(xpText);
		context.drawText(parent.getTextRenderer(), "§7" + xpText, 
			x + width - xpTextWidth - 10, y + 8, 0xFFAAAAAA, false);
		
		// Next rank info (Phase 2.1)
		if (!rank.isMaxRank() && rank.getNextRankName() != null) {
			String nextRankText = "§7TIẾP THEO: §f" + rank.getNextRankName();
			context.drawText(parent.getTextRenderer(), nextRankText, x + 10, y + 24, 0xFFAAAAAA, false);
			
			// ETA calculation (simplified - show XP remaining)
			long xpRemaining = rank.getXpToNextRank();
			String etaText = String.format("§7Còn §e%d XP", xpRemaining);
			context.drawText(parent.getTextRenderer(), etaText, x + 10, y + 36, 0xFFAAAAAA, false);
		} else {
			context.drawText(parent.getTextRenderer(), "§6✦ ĐÃ ĐẠT RANK TỐI ĐA ✦", x + 10, y + 24, 0xFFFFD700, false);
		}
		
		// Progress bar (colored with tier color)
		int barY = y + 55;
		int barHeight = 24;
		int barPadding = 10;
		int barWidth = width - (barPadding * 2);
		renderColoredProgressBar(context, x + barPadding, barY, barWidth, barHeight, percent, rank);
	}
	
	private void renderColoredProgressBar(DrawContext context, int x, int y, int width, int height, 
	                                      int percent, RankTier rank) {
		// Background
		context.fill(x, y, x + width, y + height, 0xFF1A1A1A);
		
		// Get tier color
		int tierColor = parseBorderColor(rank.getFrameColor());
		if (rank.getTier() != null && rank.getTier().contains("Legend")) {
			tierColor = getLegendGradientColor();
		}
		
		// Fill (progress)
		int fillWidth = (int)(width * (percent / 100.0f));
		if (fillWidth > 0) {
			context.fill(x, y, x + fillWidth, y + height, tierColor);
		}
		
		// Border
		context.drawBorder(x, y, width, height, 0xFF666666);
		
		// Percentage text
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), 
			percent + "%", x + width / 2, y + 8, 0xFFFFFFFF);
	}
	
	/**
	 * Personal Bests card (Phase 4.1)
	 */
	private void renderPersonalBests(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0xD0000000);
		context.drawBorder(x, y, width, height, 0xFF555555);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§e§lKỷ Lục Cá Nhân", x + 8, y + 6, 0xFFFFFF00, false);
		
		// Stats (4 lines) - using existing cached data
		int currentStreak = com.focustimershop.client.ClientProfileCache.getCurrentStreakDays();
		int longestStreak = com.focustimershop.client.ClientProfileCache.getLongestStreakDays();
		int longestSession = com.focustimershop.client.ClientProfileCache.getLongestSingleSessionSeconds();
		
		int lineY = y + 24;
		context.drawText(parent.getTextRenderer(), "§7Longest session: §f" + formatTime(longestSession), 
			x + 8, lineY, 0xFFAAAAAA, false);
		lineY += 12;
		
		context.drawText(parent.getTextRenderer(), "§7Longest streak: §f" + longestStreak + " ngày", 
			x + 8, lineY, 0xFFAAAAAA, false);
		lineY += 12;
		
		// Placeholder for data we don't have in cache yet
		context.drawText(parent.getTextRenderer(), "§7Most XP/day: §f?? XP", 
			x + 8, lineY, 0xFFAAAAAA, false);
		lineY += 12;
		
		context.drawText(parent.getTextRenderer(), "§7Most sessions/day: §f??", 
			x + 8, lineY, 0xFFAAAAAA, false);
	}
	
	/**
	 * Weekly Goal card (Phase 4.2)
	 */
	private void renderWeeklyGoal(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0xD0000000);
		context.drawBorder(x, y, width, height, 0xFF555555);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§a§lMục Tiêu Tuần", x + 8, y + 6, 0xFF00FF00, false);
		
		// Placeholder - no goal set UI for now
		context.drawText(parent.getTextRenderer(), "§7Chưa đặt mục tiêu", x + 8, y + 24, 0xFFAAAAAA, false);
		context.drawText(parent.getTextRenderer(), "§7(Tính năng đang phát triển)", x + 8, y + 38, 0xFF666666, false);
		
		// Simple progress bar (0%)
		int barY = y + height - 30;
		int barWidth = width - 16;
		context.fill(x + 8, barY, x + 8 + barWidth, barY + 16, 0xFF1A1A1A);
		context.drawBorder(x + 8, barY, barWidth, 16, 0xFF666666);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), "0%", 
			x + width / 2, barY + 4, 0xFFAAAAAA);
	}
	
	/**
	 * Focus History - 7-day bar chart (Phase 3)
	 */
	private void renderFocusHistory(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0xD0000000);
		context.drawBorder(x, y, width, height, 0xFF555555);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§b§lLịch Sử Tập Trung (7 Ngày)", x + 8, y + 6, 0xFF4A9EFF, false);
		
		// Placeholder chart
		context.drawText(parent.getTextRenderer(), "§7Chưa có dữ liệu", x + 8, y + 24, 0xFFAAAAAA, false);
		context.drawText(parent.getTextRenderer(), "§7Hoàn thành phiên để bắt đầu tracking", 
			x + 8, y + 38, 0xFF666666, false);
		
		// Simple bars (mock data for visual)
		int barAreaY = y + height - 40;
		int barAreaWidth = width - 16;
		int barWidth = barAreaWidth / 7 - 4;
		
		for (int i = 0; i < 7; i++) {
			int barX = x + 8 + (i * (barWidth + 4));
			int barHeight = 5 + (i * 3); // Mock increasing height
			context.fill(barX, barAreaY + 30 - barHeight, barX + barWidth, barAreaY + 30, 0xFF4A9EFF);
		}
	}
	
	/**
	 * Achievements display (Phase 5)
	 */
	private void renderAchievements(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0xD0000000);
		context.drawBorder(x, y, width, height, 0xFF555555);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§6§lThành Tựu", x + 8, y + 6, 0xFFFFD700, false);
		
		// Placeholder
		context.drawText(parent.getTextRenderer(), "§7Hoàn thành phiên đầu tiên", x + 8, y + 24, 0xFFAAAAAA, false);
		context.drawText(parent.getTextRenderer(), "§7để mở khóa thành tựu!", x + 8, y + 38, 0xFF666666, false);
		
		// Achievement icons (placeholders)
		int iconY = y + height - 24;
		for (int i = 0; i < 5; i++) {
			int iconX = x + 8 + (i * 20);
			context.fill(iconX, iconY, iconX + 16, iconY + 16, 0xFF333333);
			context.drawBorder(iconX, iconY, 16, 16, 0xFF666666);
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), "?", 
				iconX + 8, iconY + 4, 0xFF666666);
		}
	}
	
	/**
	 * Missions display (Phase 6)
	 */
	private void renderMissions(DrawContext context, int x, int y, int width, int height) {
		context.fill(x, y, x + width, y + height, 0xD0000000);
		context.drawBorder(x, y, width, height, 0xFF555555);
		
		// Title
		context.drawText(parent.getTextRenderer(), "§d§lNhiệm Vụ", x + 8, y + 6, 0xFFFF00FF, false);
		
		// Daily mission (placeholder)
		context.drawText(parent.getTextRenderer(), "§eNgày: §7Tập trung 15 phút", x + 8, y + 24, 0xFFAAAAAA, false);
		context.drawText(parent.getTextRenderer(), "§70 / 15 phút", x + 8, y + 36, 0xFF666666, false);
		
		// Weekly mission (placeholder)
		context.drawText(parent.getTextRenderer(), "§aTuần: §7Hoàn thành 10 phiên", x + 8, y + 52, 0xFFAAAAAA, false);
		context.drawText(parent.getTextRenderer(), "§70 / 10 phiên", x + 8, y + 64, 0xFF666666, false);
	}
	
	/**
	 * Balance bar (full width at bottom)
	 */
	private void renderBalanceBar(DrawContext context, int x, int y, int width) {
		int barHeight = 30;
		
		context.fill(x, y, x + width, y + barHeight, 0xD0000000);
		context.drawBorder(x, y, width, barHeight, 0xFF555555);
		
		// Get balances
		long silver = ClientDataCache.getSilverCoins();
		long gold = ClientDataCache.getGoldCoins();
		long xp = ClientDataCache.getFocusXp();
		
		// Evenly spaced
		int col1X = x + width / 6;
		int col2X = x + width / 2;
		int col3X = x + (width * 5) / 6;
		int textY = y + 10;
		
		// Silver
		String silverText = "§f🪙 " + formatNumber(silver);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), silverText, col1X, textY, 0xFFFFFFFF);
		
		// Gold
		String goldText = "§e💰 " + formatNumber(gold);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), goldText, col2X, textY, 0xFFFFFFFF);
		
		// XP
		String xpText = "§b✦ " + formatNumber(xp);
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), xpText, col3X, textY, 0xFFFFFFFF);
	}
	
	/**
	 * Render 3D player entity
	 */
	private void renderPlayerEntity(DrawContext context, int x, int y, int size, LivingEntity entity) {
		try {
			context.getMatrices().push();
			context.getMatrices().translate(x, y, 50);
			
			context.getMatrices().scale(size, size, -size);
			Quaternionf rotation = new Quaternionf().rotateZ((float) Math.PI);
			Quaternionf rotationX = new Quaternionf().rotateX(0.2f);
			rotation.mul(rotationX);
			context.getMatrices().multiply(rotation);
			
			DiffuseLighting.method_34742();
			
			EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
			dispatcher.setRenderShadows(false);
			dispatcher.render(entity, 0, 0, 0, 0, 1, context.getMatrices(), 
				MinecraftClient.getInstance().getBufferBuilders().getEntityVertexConsumers(), 0xF000F0);
			dispatcher.setRenderShadows(true);
			
			context.getMatrices().pop();
			DiffuseLighting.enableGuiDepthLighting();
		} catch (Exception e) {
			// Fallback: text
			context.drawCenteredTextWithShadow(parent.getTextRenderer(), "👤", x, y - 20, 0xFFFFFFFF);
		}
	}
	
	// ===== UTILITY METHODS =====
	
	private int parseBorderColor(String hexColor) {
		try {
			if (hexColor != null && hexColor.startsWith("#")) {
				return (int) Long.parseLong(hexColor.substring(1), 16) | 0xFF000000;
			}
		} catch (Exception e) {
			// Ignore
		}
		return 0xFF666666;
	}
	
	private int getLegendGradientColor() {
		long currentTime = System.currentTimeMillis();
		double cycle = (currentTime % 3000) / 3000.0;
		double sine = Math.sin(cycle * Math.PI * 2);
		double t = (sine + 1.0) / 2.0;
		
		int r = (int) (0xFF * (1 - t) + 0xFF * t);
		int g = (int) (0x44 * (1 - t) + 0xD7 * t);
		int b = (int) (0x44 * (1 - t) + 0x00 * t);
		
		return 0xFF000000 | (r << 16) | (g << 8) | b;
	}
	
	private String formatTime(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		int secs = seconds % 60;
		if (hours > 0) {
			return String.format("%dh %02dm", hours, minutes);
		}
		return String.format("%dm %02ds", minutes, secs);
	}
	
	private String formatDuration(long seconds) {
		long hours = seconds / 3600;
		long minutes = (seconds % 3600) / 60;
		
		if (hours > 0) {
			return String.format("%dh %02dm", hours, minutes);
		}
		return String.format("%dm", minutes);
	}
	
	private String formatEpochDate(long epochSeconds) {
		if (epochSeconds == 0) {
			SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
			return sdf.format(new Date());
		}
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
		return sdf.format(new Date(epochSeconds * 1000));
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
	
	/**
	 * Handle mouse clicks (custom name editor + tab switching + rank map)
	 */
	public boolean mouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, 
	                            int contentWidth, int contentHeight) {
		// Check tab bar clicks
		int tabBarY = contentY + 10;
		if (mouseY >= tabBarY && mouseY < tabBarY + TAB_BAR_HEIGHT) {
			ProfileTab[] tabs = ProfileTab.values();
			int tabWidth = contentWidth / tabs.length;
			
			for (int i = 0; i < tabs.length; i++) {
				int tabX = contentX + 10 + (i * tabWidth);
				if (mouseX >= tabX && mouseX < tabX + tabWidth) {
					currentTab = tabs[i];
					return true;
				}
			}
		}
		
		// Tab-specific clicks
		int contentStartY = contentY + 10 + TAB_BAR_HEIGHT + 8;
		
		if (currentTab == ProfileTab.TITLES) {
			return titlesScreen.mouseClicked(mouseX, mouseY, button, 
				contentX + 10, contentStartY, contentWidth - 20);
		}
		
		// Overview tab - custom name editor + rank map
		if (currentTab == ProfileTab.OVERVIEW) {
			// v1.0.6 Phase 3 - Check rank card click
			int adjustedRankCardY = rankCardY - overviewScrollOffset;
			if (mouseX >= rankCardX && mouseX < rankCardX + rankCardWidth &&
			    mouseY >= adjustedRankCardY && mouseY < adjustedRankCardY + rankCardHeight) {
				// Open rank map
				MinecraftClient client = MinecraftClient.getInstance();
				client.setScreen(new RankMapScreen(client.currentScreen));
				return true;
			}
			
			// Avatar card position
			int avatarX = contentX + 10;
			int avatarY = contentStartY;
			int customNameY = avatarY + 145;
			
			// Check if click is within custom name line
			if (mouseX >= avatarX && mouseX <= avatarX + 160 &&
			    mouseY >= customNameY - 5 && mouseY <= customNameY + 15) {
				MinecraftClient client = MinecraftClient.getInstance();
				if (client != null) {
					client.setScreen(new CustomNameEditorScreen(parent));
				}
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Handle mouse scroll (for scrollable tabs)
	 * v1.0.6 Bug Fix 2: Clamp scroll to actual content height
	 */
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		if (currentTab == ProfileTab.OVERVIEW) {
			overviewScrollOffset -= (int)(amount * 20);
			
			// Clamp to valid range (Bug Fix 2)
			int maxScroll = Math.max(0, overviewContentHeight - overviewViewportHeight);
			if (overviewScrollOffset < 0) overviewScrollOffset = 0;
			if (overviewScrollOffset > maxScroll) overviewScrollOffset = maxScroll;
			
			return true;
		} else if (currentTab == ProfileTab.ACHIEVEMENTS) {
			return achievementsScreen.mouseScrolled(mouseX, mouseY, amount);
		} else if (currentTab == ProfileTab.TITLES) {
			return titlesScreen.mouseScrolled(mouseX, mouseY, amount);
		} else if (currentTab == ProfileTab.STATS) {
			return statsScreen.mouseScrolled(mouseX, mouseY, amount);
		}
		return false;
	}
}
