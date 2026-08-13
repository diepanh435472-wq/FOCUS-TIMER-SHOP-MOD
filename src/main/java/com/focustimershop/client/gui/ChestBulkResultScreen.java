package com.focustimershop.client.gui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Grid display for x10+1 chest opening results
 * Shows 11 rewards with fade-in animation
 */
public class ChestBulkResultScreen extends Screen {
	
	private final String chestName;
	private final List<ItemStack> rewards;
	private int ticksOpen = 0;
	private static final int ITEM_REVEAL_INTERVAL = 4; // ~0.2s per item at 20 ticks/sec
	private boolean allRevealed = false;
	private boolean itemsGiven = false;

	public ChestBulkResultScreen(String chestName, List<ItemStack> rewards) {
		super(Text.literal("Bulk Opening Results"));
		this.chestName = chestName;
		this.rewards = rewards;
	}

	@Override
	protected void init() {
		super.init();
	}

	@Override
	public void tick() {
		super.tick();
		ticksOpen++;
		
		// Check if all items revealed
		int revealedCount = Math.min(rewards.size(), ticksOpen / ITEM_REVEAL_INTERVAL);
		if (revealedCount >= rewards.size() && !allRevealed) {
			allRevealed = true;
			
			// Give items to player after all revealed
			if (client != null && client.player != null && !itemsGiven) {
				for (ItemStack stack : rewards) {
					client.player.dropItem(stack.copy(), false);
				}
				itemsGiven = true;
			}
		}
	}

	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Dark background
		renderBackground(context);
		
		int screenWidth = this.width;
		int screenHeight = this.height;
		
		// Title
		String title = "§6§l" + chestName + " x11";
		int titleWidth = textRenderer.getWidth(title);
		context.drawText(textRenderer, title, (screenWidth - titleWidth) / 2, 30, 0xFFFFFFFF, true);
		
		// Grid layout: 4-4-3 (11 total)
		int gridStartY = 70;
		int cellSize = 48;
		int spacing = 8;
		int totalGridWidth = (cellSize + spacing) * 4 - spacing;
		int gridStartX = (screenWidth - totalGridWidth) / 2;
		
		int revealedCount = Math.min(rewards.size(), ticksOpen / ITEM_REVEAL_INTERVAL);
		
		for (int i = 0; i < rewards.size(); i++) {
			int row = i / 4;
			int col = i % 4;
			
			// Center last row (3 items)
			int cellX;
			if (row == 2) { // Last row
				int lastRowWidth = (cellSize + spacing) * 3 - spacing;
				int lastRowStartX = (screenWidth - lastRowWidth) / 2;
				cellX = lastRowStartX + col * (cellSize + spacing);
			} else {
				cellX = gridStartX + col * (cellSize + spacing);
			}
			
			int cellY = gridStartY + row * (cellSize + spacing);
			
			// Draw cell background
			int bgColor = i < revealedCount ? 0xFF2A2A2A : 0xFF1A1A1A;
			context.fill(cellX, cellY, cellX + cellSize, cellY + cellSize, bgColor);
			
			// Draw border
			int borderColor = i < revealedCount ? 0xFF4A9EFF : 0xFF3A3A3A;
			context.fill(cellX, cellY, cellX + cellSize, cellY + 2, borderColor);
			context.fill(cellX, cellY + cellSize - 2, cellX + cellSize, cellY + cellSize, borderColor);
			context.fill(cellX, cellY, cellX + 2, cellY + cellSize, borderColor);
			context.fill(cellX + cellSize - 2, cellY, cellX + cellSize, cellY + cellSize, borderColor);
			
			// Reveal item with fade-in
			if (i < revealedCount) {
				ItemStack stack = rewards.get(i);
				
				// Draw item icon
				int iconX = cellX + (cellSize - 16) / 2;
				int iconY = cellY + 8;
				context.drawItem(stack, iconX, iconY);
				context.drawItemInSlot(textRenderer, stack, iconX, iconY);
				
				// Draw item name
				String itemName = stack.getName().getString();
				if (itemName.length() > 10) {
					itemName = itemName.substring(0, 9) + "...";
				}
				int nameWidth = textRenderer.getWidth(itemName);
				context.drawText(textRenderer, itemName, cellX + (cellSize - nameWidth) / 2, cellY + cellSize - 12, 0xFFFFFFFF, true);
				
				// Play sound on reveal
				if (i == revealedCount - 1 && client != null) {
					client.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 0.5f, 1.0f);
				}
			} else {
				// Not yet revealed - show "?"
				context.drawCenteredTextWithShadow(textRenderer, "§7?", cellX + cellSize / 2, cellY + cellSize / 2 - 4, 0xFFAAAAAA);
			}
		}
		
		// Close button (only show after all revealed)
		if (allRevealed) {
			int btnWidth = 100;
			int btnHeight = 30;
			int btnX = (screenWidth - btnWidth) / 2;
			int btnY = screenHeight - 60;
			
			boolean hovered = mouseX >= btnX && mouseX <= btnX + btnWidth &&
			                  mouseY >= btnY && mouseY <= btnY + btnHeight;
			
			int btnColor = hovered ? 0xFF4A9EFF : 0xFF2A2A2A;
			context.fill(btnX, btnY, btnX + btnWidth, btnY + btnHeight, btnColor);
			context.fill(btnX, btnY, btnX + btnWidth, btnY + 2, 0xFF6ABAFF);
			
			String btnText = "§lĐóng";
			int btnTextWidth = textRenderer.getWidth(btnText);
			context.drawText(textRenderer, btnText, btnX + (btnWidth - btnTextWidth) / 2, btnY + 10, 0xFFFFFFFF, true);
		}
		
		super.render(context, mouseX, mouseY, delta);
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (!allRevealed) {
			return false;
		}
		
		int screenWidth = this.width;
		int screenHeight = this.height;
		int btnWidth = 100;
		int btnHeight = 30;
		int btnX = (screenWidth - btnWidth) / 2;
		int btnY = screenHeight - 60;
		
		if (mouseX >= btnX && mouseX <= btnX + btnWidth &&
		    mouseY >= btnY && mouseY <= btnY + btnHeight) {
			this.close();
			return true;
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean shouldPause() {
		return false;
	}
}
