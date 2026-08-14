package com.focustimershop.client.gui;

import com.focustimershop.client.ClientRentalCache;
import com.focustimershop.database.RentalData;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;

/**
 * Rental tab - allows renting Netherite tools with custom stats
 * Phase 1: UI shell only - 3 tools with THUÊ buttons
 * v1.0.5: Shows "Cấu hình" if tool already rented
 */
public class RentalTabScreen {
	private final MainMenuScreen parent;
	
	// Tool types available for rent
	private static final String[] TOOL_NAMES = {"Cuốc Amethyst", "Rìu Amethyst", "Xẻng Amethyst"};
	private static final String[] TOOL_TYPES = {"PICKAXE", "AXE", "SHOVEL"};
	private static final ItemStack[] TOOL_ICONS = {
		new ItemStack(Items.NETHERITE_PICKAXE),
		new ItemStack(Items.NETHERITE_AXE),
		new ItemStack(Items.NETHERITE_SHOVEL)
	};
	
	public RentalTabScreen(MainMenuScreen parent) {
		this.parent = parent;
	}
	
	public void render(DrawContext context, int x, int y, int width, int height, int mouseX, int mouseY, float delta) {
		// Title
		context.drawText(parent.getTextRenderer(), "§l§6THUÊ CÔNG CỤ", x + 10, y + 10, 0xFFFFD700, true);
		context.drawText(parent.getTextRenderer(), "§7Thuê công cụ Netherite với stat tùy chỉnh", x + 10, y + 25, 0xFFAAAAAA, false);
		
		// Render 3 tool cards
		int cardY = y + 50;
		int cardHeight = 80;
		int cardSpacing = 10;
		
		for (int i = 0; i < 3; i++) {
			renderToolCard(context, x + 10, cardY + i * (cardHeight + cardSpacing), width - 20, cardHeight, i, mouseX, mouseY);
		}
	}
	
	private void renderToolCard(DrawContext context, int x, int y, int width, int height, int toolIndex, int mouseX, int mouseY) {
		// Check if this tool is already rented
		boolean isRented = false;
		RentalData.SingleRental rental = null;
		RentalData rentalData = ClientRentalCache.getRentalData();
		if (rentalData != null) {
			rental = rentalData.getRentalByType(TOOL_TYPES[toolIndex]);
			isRented = (rental != null && rental.isActive());
		}
		
		// Card background
		context.fill(x, y, x + width, y + height, 0xFF2A2A2A);
		context.fill(x, y, x + width, y + 2, isRented ? 0xFF4AFF4A : 0xFF5ABAFF); // Green if rented
		
		// Tool icon (left side)
		int iconX = x + 15;
		int iconY = y + (height - 16) / 2;
		context.drawItem(TOOL_ICONS[toolIndex], iconX, iconY);
		
		// Tool name
		String nameColor = isRented ? "§l§a" : "§l§f";
		context.drawText(parent.getTextRenderer(), nameColor + TOOL_NAMES[toolIndex], 
			x + 50, y + 15, 0xFFFFFFFF, true);
		
		// Description
		if (isRented && rental != null) {
			long remaining = rental.getRemainingTimeMillis();
			int hours = (int)(remaining / 3600000);
			int minutes = (int)((remaining % 3600000) / 60000);
			context.drawText(parent.getTextRenderer(), "§7Còn lại: §e" + hours + "h " + minutes + "m", 
				x + 50, y + 30, 0xFF888888, false);
			context.drawText(parent.getTextRenderer(), "§7Click để xem/chỉnh cấu hình", 
				x + 50, y + 42, 0xFF888888, false);
		} else {
			context.drawText(parent.getTextRenderer(), "§7Công cụ Netherite với stat tùy chỉnh", 
				x + 50, y + 30, 0xFF888888, false);
			context.drawText(parent.getTextRenderer(), "§7Thời hạn: Tối thiểu 30 phút", 
				x + 50, y + 42, 0xFF888888, false);
		}
		
		// Button (right side) - "THUÊ" or "CẤU HÌNH"
		int btnWidth = 100;
		int btnHeight = 30;
		int btnX = x + width - btnWidth - 15;
		int btnY = y + (height - btnHeight) / 2;
		
		boolean hovered = mouseX >= btnX && mouseX <= btnX + btnWidth &&
		                  mouseY >= btnY && mouseY <= btnY + btnHeight;
		
		int btnColor = isRented ? 
			(hovered ? 0xFF5AFF5A : 0xFF4AFF4A) :  // Green if rented
			(hovered ? 0xFF5ABAFF : 0xFF4A9EFF);   // Blue if not rented
		context.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
		
		if (hovered) {
			context.fill(btnX, btnY, btnX + btnWidth, btnY + 2, 0xFF6ABAFF);
			context.fill(btnX, btnY + btnHeight - 2, btnX + btnWidth, btnY + btnHeight, 0xFF6ABAFF);
		}
		
		String buttonText = isRented ? "§l§fCẤU HÌNH" : "§l§fTHUÊ";
		context.drawCenteredTextWithShadow(parent.getTextRenderer(), buttonText, 
			btnX + btnWidth / 2, btnY + 10, 0xFFFFFFFF);
	}
	
	public boolean mouseClicked(double mouseX, double mouseY, int button, int contentX, int contentY, int contentWidth, int contentHeight) {
		if (button != 0) return false;
		
		// Check button clicks
		int cardY = contentY + 50;
		int cardHeight = 80;
		int cardSpacing = 10;
		int cardWidth = contentWidth - 20;
		
		for (int i = 0; i < 3; i++) {
			int y = cardY + i * (cardHeight + cardSpacing);
			
			// Button bounds
			int btnWidth = 100;
			int btnHeight = 30;
			int btnX = contentX + 10 + cardWidth - btnWidth - 15;
			int btnY = y + (cardHeight - btnHeight) / 2;
			
			if (mouseX >= btnX && mouseX <= btnX + btnWidth &&
			    mouseY >= btnY && mouseY <= btnY + btnHeight) {
				
				// Check if tool is already rented
				boolean isRented = false;
				RentalData rentalData = ClientRentalCache.getRentalData();
				if (rentalData != null) {
					RentalData.SingleRental rental = rentalData.getRentalByType(TOOL_TYPES[i]);
					isRented = (rental != null && rental.isActive());
				}
				
				if (isRented) {
					// Open config popup (reuse RentalConfigPopup but in "edit mode")
					com.focustimershop.FocusTimerShop.LOGGER.info("Opening config for rented tool: {}", TOOL_NAMES[i]);
					// TODO: Create RentalConfigEditPopup or reuse existing one with edit mode
					net.minecraft.client.MinecraftClient.getInstance().player.sendMessage(
						net.minecraft.text.Text.literal("§eChức năng cấu hình đang phát triển..."), false);
				} else {
					// Open rental popup to rent new tool
					com.focustimershop.FocusTimerShop.LOGGER.info("Opening rental config for tool: {}", TOOL_NAMES[i]);
					net.minecraft.client.MinecraftClient.getInstance().setScreen(
						new RentalConfigPopup(parent, TOOL_NAMES[i], i)
					);
				}
				
				return true;
			}
		}
		
		return false;
	}
}
