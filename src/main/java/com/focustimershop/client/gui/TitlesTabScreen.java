package com.focustimershop.client.gui;

import com.focustimershop.client.ClientProfileCache;
import com.focustimershop.network.NetworkHandler;
import com.focustimershop.network.EquipTitleC2SPacket;
import com.focustimershop.title.TitleDefinition;
import com.focustimershop.title.TitleSystemManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Titles Tab (Danh hiệu) - 67 titles with equip functionality (v1.0.6 Phase 5)
 */
public class TitlesTabScreen {
	
	private final MainMenuScreen parent;
	private List<String> unlockedTitleIds;
	private String equippedTitleId;
	private int scrollOffset = 0;
	private int contentHeight = 0; // Track total content height (Bug Fix 2)
	private int viewportHeight = 0; // Track viewport height (Bug Fix 2)
	
	public TitlesTabScreen(MainMenuScreen parent) {
		this.parent = parent;
		this.unlockedTitleIds = new ArrayList<>();
		this.equippedTitleId = null;
	}
	
	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		viewportHeight = height; // Store for scroll clamping (Bug Fix 2)
		int startY = y;
		
		int contentX = x + 10;
		int contentY = y + 10 - scrollOffset;
		int contentWidth = width - 20;
		
		// Get unlocked titles (Map<String, Long>) with fallback
		java.util.Map<String, Long> unlockedTitles = new java.util.HashMap<>();
		try {
			unlockedTitles = ClientProfileCache.getUnlockedTitlesMap();
			if (unlockedTitles == null) {
				unlockedTitles = new java.util.HashMap<>();
			}
		} catch (Exception e) {
			// Method not implemented yet, use empty map
		}
		
		equippedTitleId = ClientProfileCache.getEquippedTitleId();
		
		// Header
		context.drawText(parent.getTextRenderer(), 
			"§d§lDANH HIỆU  §7(" + unlockedTitles.size() + " / 67)", 
			contentX, contentY, 0xFFFF00FF, false);
		
		int currentY = contentY + 20;
		
		// Currently equipped title (if any)
		if (equippedTitleId != null) {
			TitleDefinition equipped = TitleSystemManager.getTitleById(equippedTitleId);
			if (equipped != null) {
				currentY = renderEquippedTitle(context, contentX, currentY, contentWidth, equipped);
				currentY += 12;
			}
		}
		
		// Title list
		List<TitleDefinition> allTitles = TitleSystemManager.getAllTitles();
		
		// Safety check: if titles not loaded, show error message
		if (allTitles == null || allTitles.isEmpty()) {
			context.drawText(parent.getTextRenderer(), 
				"§c✖ Không tải được titles.json", 
				contentX, currentY, 0xFFFF5555, false);
			context.drawText(parent.getTextRenderer(), 
				"§7Kiểm tra file config/focustimershop/titles.json", 
				contentX, currentY + 12, 0xFF888888, false);
			return;
		}
		
		for (TitleDefinition title : allTitles) {
			if (title == null) continue; // Skip null entries
			
			Long unlockTime = unlockedTitles.get(title.getId());
			boolean unlocked = (unlockTime != null);
			boolean equipped = title.getId().equals(equippedTitleId);
			
			currentY = renderTitleCard(context, contentX, currentY, contentWidth, title, 
				unlocked, equipped, unlockTime, mouseX, mouseY);
			currentY += 6;
		}
		
		// Store total content height (Bug Fix 2)
		contentHeight = (currentY + scrollOffset) - startY;
	}
	
	/**
	 * Render currently equipped title banner
	 */
	private int renderEquippedTitle(DrawContext context, int x, int y, int width, TitleDefinition title) {
		int cardHeight = 40;
		
		// Background (purple gradient)
		context.fill(x, y, x + width, y + cardHeight, 0xD0440044);
		context.drawBorder(x, y, width, cardHeight, 0xFFFF00FF);
		
		// Label
		context.drawText(parent.getTextRenderer(), "§d§lHIỆN TẠI:", x + 8, y + 8, 0xFFFF00FF, false);
		
		// Title display
		String displayText = "§f[" + title.getDisplayPrefix() + "]";
		context.drawText(parent.getTextRenderer(), displayText, x + 8, y + 22, 0xFFFFFFFF, true);
		
		return y + cardHeight;
	}
	
	/**
	 * Render a title card (Phase 2 format with ?????? for locked titles)
	 */
	private int renderTitleCard(DrawContext context, int x, int y, int width, 
	                             TitleDefinition title, boolean unlocked, boolean equipped, Long unlockTime,
	                             int mouseX, int mouseY) {
		int cardHeight = 50;
		
		// Background + border
		int bgColor = unlocked ? 0xD0000000 : 0xD0111111;
		int borderColor = equipped ? 0xFFFF00FF : (unlocked ? 0xFF555555 : 0xFF333333);
		context.fill(x, y, x + width, y + cardHeight, bgColor);
		context.drawBorder(x, y, width, cardHeight, borderColor);
		
		if (unlocked) {
			// Title name
			context.drawText(parent.getTextRenderer(), "§f§l" + title.getName(), 
				x + 8, y + 8, 0xFFFFFFFF, false);
			
			// Unlock date
			if (unlockTime != null) {
				String dateStr = formatEpochDate(unlockTime);
				context.drawText(parent.getTextRenderer(), "§7Đạt được: " + dateStr, 
					x + 8, y + 22, 0xFFAAAAAA, false);
			}
			
			// Display prefix
			String prefixText = "§7[" + title.getDisplayPrefix() + "]";
			context.drawText(parent.getTextRenderer(), prefixText, x + 8, y + 36, 
				0xFFAAAAAA, false);
			
			// Equip button (if not currently equipped)
			if (!equipped) {
				int buttonX = x + width - 70;
				int buttonY = y + cardHeight / 2 - 10;
				int buttonWidth = 60;
				int buttonHeight = 20;
				
				boolean hovered = mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
				                  mouseY >= buttonY && mouseY < buttonY + buttonHeight;
				
				int buttonColor = hovered ? 0xFF44AA44 : 0xFF226622;
				context.fill(buttonX, buttonY, buttonX + buttonWidth, buttonY + buttonHeight, buttonColor);
				context.drawBorder(buttonX, buttonY, buttonWidth, buttonHeight, 0xFF00FF00);
				
				context.drawCenteredTextWithShadow(parent.getTextRenderer(), "§aTrang bị", 
					buttonX + buttonWidth / 2, buttonY + 6, 0xFFFFFFFF);
			} else {
				// "Equipped" label
				int labelX = x + width - 80;
				context.drawText(parent.getTextRenderer(), "§d✓ Đã trang bị", 
					labelX, y + cardHeight / 2 - 4, 0xFFFF00FF, false);
			}
		} else {
			// Locked title - show "???????" only
			context.drawText(parent.getTextRenderer(), "§8§l???????", 
				x + 8, y + cardHeight / 2 - 4, 0xFF444444, false);
			
			// Lock icon
			context.drawText(parent.getTextRenderer(), "§c🔒 Chưa mở khóa", 
				x + width - 100, y + cardHeight / 2 - 4, 0xFFFF5555, false);
		}
		
		return y + cardHeight;
	}
	
	/**
	 * Format epoch timestamp
	 */
	private String formatEpochDate(long epochSeconds) {
		java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy");
		return sdf.format(new java.util.Date(epochSeconds * 1000));
	}
	
	/**
	 * Handle mouse clicks (equip button)
	 */
	public boolean mouseClicked(double mouseX, double mouseY, int button, 
	                            int contentX, int contentY, int contentWidth) {
		int currentY = contentY + 10 - scrollOffset + 20;
		
		// Get unlocked titles map
		java.util.Map<String, Long> unlockedTitles = ClientProfileCache.getUnlockedTitlesMap();
		if (unlockedTitles == null) {
			unlockedTitles = new java.util.HashMap<>();
		}
		
		// Skip equipped title banner if present
		if (equippedTitleId != null) {
			currentY += 40 + 12;
		}
		
		List<TitleDefinition> allTitles = TitleSystemManager.getAllTitles();
		
		for (TitleDefinition title : allTitles) {
			Long unlockTime = unlockedTitles.get(title.getId());
			boolean unlocked = (unlockTime != null);
			boolean equipped = title.getId().equals(equippedTitleId);
			
			int cardHeight = 50;
			
			// Check equip button click
			if (unlocked && !equipped) {
				int buttonX = contentX + 10 + contentWidth - 20 - 70;
				int buttonY = currentY + cardHeight / 2 - 10;
				int buttonWidth = 60;
				int buttonHeight = 20;
				
				if (mouseX >= buttonX && mouseX < buttonX + buttonWidth &&
				    mouseY >= buttonY && mouseY < buttonY + buttonHeight) {
					// Open confirmation dialog (v1.0.6 Phase 2)
					MinecraftClient client = MinecraftClient.getInstance();
					client.setScreen(new TitleEquipConfirmScreen(
						client.currentScreen, title
					));
					return true;
				}
			}
			
			currentY += cardHeight + 6;
		}
		
		return false;
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
