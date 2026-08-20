package com.focustimershop.client.gui;

import com.focustimershop.client.ClientProfileCache;
import com.focustimershop.profile.RankConfig;
import com.focustimershop.profile.RankManager;
import com.focustimershop.profile.RankTier;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Rank Map overlay - shows all 58 ranks in scrollable list (v1.0.6 Phase 3)
 * Displays 12 tiers with 5 levels each (Legend has 3)
 */
public class RankMapScreen extends Screen {
	
	private final Screen parent;
	private int scrollOffset = 0;
	private RankTier currentRank;
	
	// Track content height for proper scroll clamping (fix infinite scroll bug)
	private int contentHeight = 0;
	private int viewportHeight = 0;
	
	public RankMapScreen(Screen parent) {
		super(Text.literal("Sơ Đồ Rank"));
		this.parent = parent;
		
		// Get current rank (v1.0.6-beta Season System - use seasonRankXp)
		long seasonXp = ClientProfileCache.getSeasonRankXp();
		this.currentRank = RankManager.resolveRank(seasonXp);
	}
	
	@Override
	protected void init() {
		super.init();
		
		// Close button
		this.addDrawableChild(ButtonWidget.builder(
			Text.literal("§cĐóng"),
			button -> this.close()
		).dimensions(width / 2 - 50, height - 35, 100, 20).build());
		
		// Auto-scroll to current rank
		autoScrollToCurrentRank();
	}
	
	/**
	 * Auto-scroll to show current rank in view
	 */
	private void autoScrollToCurrentRank() {
		List<RankConfig> allRanks = RankManager.getAllRanks();
		
		// Find current rank index
		int currentIndex = -1;
		for (int i = 0; i < allRanks.size(); i++) {
			RankConfig config = allRanks.get(i);
			if (config.getTier().equals(currentRank.getTier()) &&
			    config.getLevel() == currentRank.getLevel()) {
				currentIndex = i;
				break;
			}
		}
		
		if (currentIndex >= 0) {
			// Each tier section is ~120px tall (header + 5 levels)
			// Scroll to center current rank
			int tierIndex = currentIndex / 5; // Approximate tier grouping
			scrollOffset = tierIndex * 120 - height / 3;
			if (scrollOffset < 0) scrollOffset = 0;
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// FIX: Use dark flat background consistent with mod's UI theme
		// Instead of renderBackgroundTexture() which uses dirt/terrain texture
		context.fill(0, 0, width, height, 0xFF0A0A0A); // Very dark background
		context.fill(0, 0, width, height, 0x80000000); // Semi-transparent overlay
		
		// Title
		context.drawCenteredTextWithShadow(textRenderer, "§6§lSƠ ĐỒ RANK", 
			width / 2, 20, 0xFFFFD700);
		
		// Calculate viewport dimensions for scroll clamping
		viewportHeight = height - 100; // Space between title and buttons
		int contentStartY = 50;
		
		// Render rank map (scrollable)
		int contentY = contentStartY - scrollOffset;
		int contentYStart = contentY; // Track where content rendering starts
		int contentX = width / 2 - 200;
		int contentWidth = 400;
		
		List<RankConfig> allRanks = RankManager.getAllRanks();
		
		// Group by tier
		String currentTierName = "";
		int tierStartY = contentY;
		
		for (RankConfig config : allRanks) {
			// New tier - render tier header
			if (!config.getTier().equals(currentTierName)) {
				currentTierName = config.getTier();
				tierStartY = contentY;
				
				// Tier header
				context.drawText(textRenderer, "§l§6" + currentTierName, 
					contentX, contentY, 0xFFFFD700, false);
				contentY += 20;
			}
			
			// Render rank node
			contentY = renderRankNode(context, contentX, contentY, contentWidth, config, mouseX, mouseY);
			contentY += 4; // Small gap
		}
		
		// Render buttons
		super.render(context, mouseX, mouseY, delta);
		
		// Calculate total content height for scroll clamping
		contentHeight = contentY - contentYStart;
		
		// Scroll hint
		context.drawCenteredTextWithShadow(textRenderer, "§7Cuộn chuột để xem thêm", 
			width / 2, height - 55, 0xFF888888);
	}
	
	/**
	 * Render a single rank node
	 */
	private int renderRankNode(DrawContext context, int x, int y, int width, 
	                            RankConfig config, int mouseX, int mouseY) {
		int nodeHeight = 35;
		
		// Check if this is current rank
		boolean isCurrent = config.getTier().equals(currentRank.getTier()) &&
		                    config.getLevel() == currentRank.getLevel();
		
		// Check if achieved
		long playerXp = ClientProfileCache.getTotalFocusXpEarned();
		boolean achieved = playerXp >= config.getCumulativeXP();
		
		// Background color
		int bgColor;
		if (isCurrent) {
			bgColor = 0xFF2A4A6A; // Current - blue glow
		} else if (achieved) {
			bgColor = 0xFF001100; // Achieved - green tint
		} else {
			bgColor = 0xFF222222; // Not achieved - gray
		}
		
		context.fill(x, y, x + width, y + nodeHeight, bgColor);
		
		// Border
		int borderColor;
		if (isCurrent) {
			borderColor = 0xFFFFD700; // Gold for current
		} else if (achieved) {
			int colorInt = parseColor(config.getFrameColor());
			borderColor = colorInt;
		} else {
			borderColor = 0xFF444444;
		}
		
		context.drawBorder(x, y, width, nodeHeight, borderColor);
		
		// Icon area (left side)
		int iconSize = 25;
		int iconX = x + 5;
		int iconY = y + 5;
		
		// Icon background
		context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, borderColor & 0x60FFFFFF);
		
		// Rank symbol/icon
		String iconText = config.getTier().substring(0, Math.min(2, config.getTier().length()));
		context.drawCenteredTextWithShadow(textRenderer, iconText, 
			iconX + iconSize / 2, iconY + iconSize / 2 - 4, 0xFFFFFFFF);
		
		// Rank name
		String rankName = config.getDisplayName();
		int textX = iconX + iconSize + 10;
		int textY = y + 8;
		
		if (isCurrent) {
			context.drawText(textRenderer, "§l§e" + rankName + " §7(BẠN ĐANG Ở ĐÂY)", 
				textX, textY, 0xFFFFD700, false);
		} else if (achieved) {
			context.drawText(textRenderer, "§a✓ §f" + rankName, 
				textX, textY, 0xFFFFFFFF, false);
		} else {
			context.drawText(textRenderer, "§8" + rankName, 
				textX, textY, 0xFF666666, false);
		}
		
		// XP requirement
		String xpText = formatNumber(config.getCumulativeXP()) + " XP";
		context.drawText(textRenderer, "§7" + xpText, 
			textX, textY + 12, 0xFF888888, false);
		
		// Checkmark for achieved
		if (achieved && !isCurrent) {
			context.drawText(textRenderer, "§a✓", 
				x + width - 20, y + nodeHeight / 2 - 4, 0xFF00FF00, false);
		}
		
		return y + nodeHeight;
	}
	
	/**
	 * Parse hex color string to int
	 */
	private int parseColor(String hexColor) {
		try {
			String hex = hexColor.replace("#", "");
			return (int)Long.parseLong("FF" + hex, 16);
		} catch (Exception e) {
			return 0xFFFFFFFF;
		}
	}
	
	/**
	 * Format large numbers
	 */
	private String formatNumber(long number) {
		if (number >= 1000) {
			return String.format("%.1fK", number / 1000.0);
		}
		return String.valueOf(number);
	}
	
	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
		scrollOffset -= (int)(amount * 30);
		
		// FIX: Clamp scroll to actual content bounds (prevent infinite scroll)
		// Same pattern as Lucky Chest and Profile fixes
		int maxScroll = Math.max(0, contentHeight - viewportHeight);
		scrollOffset = Math.max(0, Math.min(scrollOffset, maxScroll));
		
		return true;
	}
	
	@Override
	public void close() {
		client.setScreen(parent);
	}
	
	@Override
	public boolean shouldPause() {
		return false;
	}
}
