package com.focustimershop.client.gui;

import com.focustimershop.achievement.AchievementDefinition;
import com.focustimershop.achievement.AchievementSystemManager;
import com.focustimershop.client.ClientProfileCache;
import com.focustimershop.client.ClientDataCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Achievements Tab (Thành tựu) - 220 achievements grid (v1.0.6 Phase 5)
 */
public class AchievementsTabScreen {
	
	private final MainMenuScreen parent;
	private List<String> unlockedAchievementIds;
	private int scrollOffset = 0;
	private int contentHeight = 0; // Track total content height (Bug Fix 2)
	private int viewportHeight = 0; // Track viewport height (Bug Fix 2)
	
	public AchievementsTabScreen(MainMenuScreen parent) {
		this.parent = parent;
		this.unlockedAchievementIds = new ArrayList<>();
	}
	
	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		viewportHeight = height; // Store for scroll clamping (Bug Fix 2)
		int startY = y;
		
		int contentX = x + 10;
		int contentY = y + 10 - scrollOffset;
		int contentWidth = width - 20;
		
		// Get unlocked achievements from cache (Map<String, Long>)
		// Fallback to empty map if not synced yet
		java.util.Map<String, Long> unlockedAchievements = new java.util.HashMap<>();
		try {
			unlockedAchievements = ClientProfileCache.getUnlockedAchievementsMap();
			if (unlockedAchievements == null) {
				unlockedAchievements = new java.util.HashMap<>();
			}
		} catch (Exception e) {
			// Method not implemented yet, use empty map
		}
		
		// Header
		context.drawText(parent.getTextRenderer(), 
			"§6§lTHÀNH TỰU  §7(" + unlockedAchievements.size() + " / 220)", 
			contentX, contentY, 0xFFFFD700, false);
		
		int currentY = contentY + 20;
		
		// Get ALL achievements (not grouped - show as list with dates)
		List<AchievementDefinition> allAchievements = AchievementSystemManager.getAllAchievements();
		
		// Safety check: if achievements not loaded, show error message
		if (allAchievements == null || allAchievements.isEmpty()) {
			context.drawText(parent.getTextRenderer(), 
				"§c✖ Không tải được achievements.json", 
				contentX, currentY, 0xFFFF5555, false);
			context.drawText(parent.getTextRenderer(), 
				"§7Kiểm tra file config/focustimershop/achievements.json", 
				contentX, currentY + 12, 0xFF888888, false);
			return;
		}
		
		for (AchievementDefinition def : allAchievements) {
			if (def == null) continue; // Skip null entries
			
			Long unlockTime = unlockedAchievements.get(def.getId());
			boolean unlocked = (unlockTime != null);
			
			// Render achievement row
			currentY = renderAchievementRow(context, contentX, currentY, contentWidth, 
				def, unlocked, unlockTime, mouseX, mouseY);
			currentY += 4; // Small gap between rows
		}
		
		// Store total content height (Bug Fix 2)
		contentHeight = (currentY + scrollOffset) - startY;
	}
	
	/**
	 * Render a single achievement as a list row (Phase 1 format)
	 */
	private int renderAchievementRow(DrawContext context, int x, int y, int width,
	                                  AchievementDefinition def, boolean unlocked, Long unlockTime,
	                                  int mouseX, int mouseY) {
		int rowHeight = 50; // Taller to fit progress bar
		
		// Background
		int bgColor = unlocked ? 0xD0001100 : 0xD0110000;
		context.fill(x, y, x + width, y + rowHeight, bgColor);
		
		// Border with rarity color
		int borderColor = unlocked ? def.getRarityColor() : 0xFF333333;
		context.drawBorder(x, y, width, rowHeight, borderColor);
		
		// Icon area (left side, 40x40)
		int iconSize = 40;
		int iconX = x + 5;
		int iconY = y + 5;
		
		// Icon background
		int iconBg = unlocked ? (def.getRarityColor() & 0x40FFFFFF) : 0xFF222222;
		context.fill(iconX, iconY, iconX + iconSize, iconY + iconSize, iconBg);
		
		// Icon text (safe null handling)
		String achievementName = def.getName() != null ? def.getName() : "???";
		String iconText = unlocked ? achievementName.substring(0, Math.min(2, achievementName.length())) : "?";
		int iconTextColor = unlocked ? 0xFFFFFFFF : 0xFF666666;
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), iconText,
			iconX + iconSize / 2, iconY + iconSize / 2 - 4, iconTextColor);
		
		// Content area (right of icon)
		int textX = iconX + iconSize + 8;
		int textY = y + 8;
		
		// Name + rarity (safe null handling)
		String rarityColor = getRarityColorCode(def.getRarity());
		String rarityText = def.getRarity() != null ? def.getRarity() : "???";
		String nameText = rarityColor + "§l" + achievementName + " §7(" + rarityText + ")";
		context.drawText(parent.getTextRenderer(), nameText, textX, textY, 0xFFFFFFFF, false);
		textY += 12;
		
		// Status line
		if (unlocked && unlockTime != null) {
			String dateStr = formatEpochDate(unlockTime);
			context.drawText(parent.getTextRenderer(), "§a✓ Hoàn thành: §f" + dateStr, 
				textX, textY, 0xFF00FF00, false);
		} else {
			// FIX 4b: Show actual unlock condition for locked achievements
			// Format: "§c✗ Chưa hoàn thành - Cần: {condition description}"
			String conditionText = getConditionDescription(def);
			context.drawText(parent.getTextRenderer(), "§c✗ Chưa hoàn thành §7- Cần: §f" + conditionText, 
				textX, textY, 0xFFFF5555, false);
		}
		textY += 12;
		
		// Progress bar (if applicable)
		if (!unlocked && hasProgressTracking(def)) {
			int progressCurrent = getCurrentProgress(def);
			int progressMax = getMaxProgress(def);
			
			if (progressMax > 0) {
				// Progress bar background
				int barWidth = width - (textX - x) - 10;
				int barHeight = 8;
				int barX = textX;
				int barY = textY;
				
				context.fill(barX, barY, barX + barWidth, barY + barHeight, 0xFF222222);
				
				// FIX 4a: Clamp progress percent to [0, 1] to prevent overflow
				// This ensures fill width never exceeds container width
				float progressPercent = Math.max(0.0f, Math.min(1.0f, (float)progressCurrent / progressMax));
				int fillWidth = (int)(barWidth * progressPercent);
				context.fill(barX, barY, barX + fillWidth, barY + barHeight, 0xFF4A9EFF);
				
				// Progress text
				String progressText = progressCurrent + " / " + progressMax + " (" + 
					String.format("%.0f%%", progressPercent * 100) + ")";
				context.drawText(parent.getTextRenderer(), "§7" + progressText, 
					barX + barWidth + 5, barY, 0xFF888888, false);
			}
		}
		
		return y + rowHeight;
	}
	
	/**
	 * Get condition description for locked achievement
	 * FIX 4b: Show actual unlock requirement instead of just "???"
	 */
	private String getConditionDescription(AchievementDefinition def) {
		String type = def.getConditionType();
		Object value = def.getConditionValue();
		
		// If description exists, use it (most detailed)
		if (def.getDescription() != null && !def.getDescription().isEmpty()) {
			return def.getDescription();
		}
		
		// Otherwise, generate from condition type and value
		if (value == null) {
			return "???";
		}
		
		int threshold = 0;
		if (value instanceof Number) {
			threshold = ((Number)value).intValue();
		}
		
		switch (type) {
			case "TOTAL_FOCUS_HOURS":
				return threshold + " giờ tập trung";
			case "TOTAL_SESSIONS":
				return threshold + " phiên học";
			case "STREAK_DAYS":
				return threshold + " ngày liên tiếp";
			case "LONGEST_SESSION_MINUTES":
				return threshold + " phút trong 1 phiên";
			case "TOTAL_ITEMS_PURCHASED":
				return threshold + " vật phẩm đã mua";
			case "TOTAL_CHESTS_OPENED":
				return threshold + " rương đã mở";
			case "TOTAL_SILVER_EARNED":
				return threshold + " silver kiếm được";
			case "TOTAL_SILVER_CONVERTED_TO_GOLD":
				return threshold + " silver đã đổi vàng";
			case "TOTAL_XP_EARNED_LIFETIME":
				return threshold + " XP tích lũy";
			case "TOTAL_BLOCKS_MINED":
				return threshold + " khối đã đào";
			case "SPECIAL":
				return "Điều kiện đặc biệt";
			default:
				return type + ": " + value.toString();
		}
	}
	
	/**
	 * Check if achievement has trackable progress
	 */
	private boolean hasProgressTracking(AchievementDefinition def) {
		String type = def.getConditionType();
		// Most types have progress except special conditions
		return !type.equals("SPECIAL") && def.getConditionValue() != null;
	}
	
	/**
	 * Get current progress for achievement
	 */
	private int getCurrentProgress(AchievementDefinition def) {
		String type = def.getConditionType();
		
		switch (type) {
			case "TOTAL_FOCUS_HOURS":
				return (int)(ClientProfileCache.getTotalFocusTimeSeconds() / 3600);
			case "TOTAL_SESSIONS":
				return (int)ClientProfileCache.getTotalSessionsCompleted();
			case "STREAK_DAYS":
				return ClientProfileCache.getCurrentStreakDays();
			case "LONGEST_SESSION_MINUTES":
				return ClientProfileCache.getLongestSingleSessionSeconds() / 60;
			case "TOTAL_ITEMS_PURCHASED":
				return (int)ClientDataCache.getTotalItemsPurchased();
			case "TOTAL_CHESTS_OPENED":
				return (int)ClientDataCache.getTotalChestsOpened();
			case "TOTAL_SILVER_EARNED":
				return (int)ClientDataCache.getTotalSilverEarned();
			case "TOTAL_SILVER_CONVERTED_TO_GOLD":
				return (int)ClientDataCache.getTotalSilverConvertedToGold();
			case "TOTAL_XP_EARNED_LIFETIME":
				return (int)ClientProfileCache.getTotalFocusXpEarned();
			case "TOTAL_BLOCKS_MINED":
				return (int)ClientDataCache.getTotalBlocksMined();
			default:
				return 0;
		}
	}
	
	/**
	 * Get max progress for achievement
	 */
	private int getMaxProgress(AchievementDefinition def) {
		Object value = def.getConditionValue();
		if (value instanceof Number) {
			return ((Number)value).intValue();
		}
		return 0;
	}
	
	/**
	 * Format epoch timestamp to date string
	 */
	private String formatEpochDate(long epochSeconds) {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
		return sdf.format(new java.util.Date(epochSeconds * 1000));
	}
	
	/**
	 * Get rarity header color
	 */
	private int getRarityHeaderColor(String rarity) {
		if (rarity == null) return 0xFFFFFFFF; // Safety check for null rarity
		
		switch (rarity) {
			case "Phổ biến": return 0xFFCCCCCC;
			case "Không phổ biến": return 0xFF00FF00;
			case "Hiếm": return 0xFF4A9EFF;
			case "Cực hiếm": return 0xFFAA00FF;
			case "Huyền thoại": return 0xFFFFD700;
			default: return 0xFFFFFFFF;
		}
	}
	
	/**
	 * Get rarity color code for text
	 */
	private String getRarityColorCode(String rarity) {
		if (rarity == null) return "§f"; // Safety check for null rarity
		
		switch (rarity) {
			case "Phổ biến": return "§7";
			case "Không phổ biến": return "§a";
			case "Hiếm": return "§9";
			case "Cực hiếm": return "§5";
			case "Huyền thoại": return "§6";
			default: return "§f";
		}
	}
	
	/**
	 * Handle mouse scroll
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
