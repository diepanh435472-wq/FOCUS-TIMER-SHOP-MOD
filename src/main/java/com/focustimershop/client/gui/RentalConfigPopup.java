package com.focustimershop.client.gui;

import com.focustimershop.client.ClientDataCache;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * Rental configuration popup - Phase 2
 * Allows player to customize tool stats and duration with live price calculation
 */
public class RentalConfigPopup extends Screen {
	private final Screen parent;
	private final String toolName;
	private final int toolIndex; // 0=Pickaxe, 1=Axe, 2=Shovel
	
	// Popup dimensions
	private int popupX, popupY, popupWidth, popupHeight;
	
	// Stat configuration
	private boolean useFortuneMode = true; // true=Gia Tài, false=Độ Mềm Mại
	private int fortuneLevel = 0;          // 0-10 (only when useFortuneMode=true)
	private int efficiencyLevel = 0;       // 0-10 (Hiệu Suất)
	private int unbreakingLevel = 0;       // 0-10 (Chậm Hỏng)
	private int mendingLevel = 0;          // 0-10 (Sửa Chữa)
	
	// Duration configuration
	private int durationValue = 30;        // Numeric value
	private TimeUnit timeUnit = TimeUnit.MINUTES; // Unit selector
	private TextFieldWidget durationField;
	
	// Time unit enum
	private enum TimeUnit {
		MINUTES("Phút", 1),
		HOURS("Giờ", 60),
		DAYS("Ngày", 1440);
		
		private final String displayName;
		private final int minutesMultiplier;
		
		TimeUnit(String displayName, int minutesMultiplier) {
			this.displayName = displayName;
			this.minutesMultiplier = minutesMultiplier;
		}
		
		public String getDisplayName() {
			return displayName;
		}
		
		public int toMinutes(int value) {
			return value * minutesMultiplier;
		}
	}
	
	// Payment method (true=Silver, false=Gold)
	private boolean useSilverPayment = false;
	
	// Slider dragging state
	private int draggingSlider = -1; // -1=none, 0=fortune/silk, 1=efficiency, 2=unbreaking, 3=mending
	
	public RentalConfigPopup(Screen parent, String toolName, int toolIndex) {
		super(Text.literal("Cấu hình thuê: " + toolName));
		this.parent = parent;
		this.toolName = toolName;
		this.toolIndex = toolIndex;
	}
	
	@Override
	protected void init() {
		super.init();
		
		// Center popup
		popupWidth = Math.min(500, width - 40);
		popupHeight = Math.min(450, height - 40);
		popupX = (width - popupWidth) / 2;
		popupY = (height - popupHeight) / 2;
		
		// Duration input field
		if (durationField == null) {
			durationField = new TextFieldWidget(textRenderer, popupX + 120, popupY + 270, 60, 20, Text.literal(""));
			durationField.setMaxLength(5);
			durationField.setText(String.valueOf(durationValue));
			durationField.setChangedListener(text -> {
				try {
					int value = Integer.parseInt(text);
					if (value >= 1) {
						durationValue = value;
					}
				} catch (NumberFormatException e) {
					// Ignore invalid input
				}
			});
		} else {
			durationField.setX(popupX + 120);
			durationField.setY(popupY + 270);
		}
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Darken background
		context.fill(0, 0, width, height, 0xAA000000);
		
		// Popup background
		context.fill(popupX, popupY, popupX + popupWidth, popupY + popupHeight, 0xFF2D2D2D);
		context.fill(popupX, popupY, popupX + popupWidth, popupY + 2, 0xFF5ABAFF);
		
		// Title
		context.drawCenteredTextWithShadow(textRenderer, "§l§6Cấu hình thuê: §f" + toolName, 
			popupX + popupWidth / 2, popupY + 10, 0xFFFFFFFF);
		
		// Close button (X)
		int closeX = popupX + popupWidth - 30;
		int closeY = popupY + 5;
		boolean closeHovered = mouseX >= closeX && mouseX <= closeX + 20 && 
		                        mouseY >= closeY && mouseY <= closeY + 20;
		context.drawText(textRenderer, closeHovered ? "§c§lX" : "§7X", closeX + 6, closeY + 6, 0xFFFFFFFF, false);
		
		// Stat sliders (4 rows)
		int sliderY = popupY + 40;
		int sliderSpacing = 45;
		
		renderStatRow(context, popupX + 20, sliderY, "Row 1", 0, mouseX, mouseY, useFortuneMode, fortuneLevel);
		renderStatRow(context, popupX + 20, sliderY + sliderSpacing, "Hiệu Suất", 1, mouseX, mouseY, true, efficiencyLevel);
		renderStatRow(context, popupX + 20, sliderY + 2 * sliderSpacing, "Chậm Hỏng", 2, mouseX, mouseY, true, unbreakingLevel);
		renderStatRow(context, popupX + 20, sliderY + 3 * sliderSpacing, "Sửa Chữa", 3, mouseX, mouseY, true, mendingLevel);
		
		// Duration input
		int durationY = popupY + 270;
		context.drawText(textRenderer, "§7Thời gian:", popupX + 20, durationY + 5, 0xFFAAAAAA, false);
		durationField.render(context, mouseX, mouseY, delta);
		
		// Time unit selector (3 buttons: Minutes, Hours, Days)
		int unitX = popupX + 190;
		int unitWidth = 60;
		int unitHeight = 20;
		int unitSpacing = 5;
		
		TimeUnit[] units = TimeUnit.values();
		for (int i = 0; i < units.length; i++) {
			TimeUnit unit = units[i];
			int btnX = unitX + i * (unitWidth + unitSpacing);
			boolean selected = (timeUnit == unit);
			boolean hovered = mouseX >= btnX && mouseX <= btnX + unitWidth &&
			                  mouseY >= durationY && mouseY <= durationY + unitHeight;
			
			int color = selected ? 0xFF4A9EFF : (hovered ? 0xFF3A3A3A : 0xFF2A2A2A);
			context.fill(btnX, durationY, btnX + unitWidth, durationY + unitHeight, color);
			if (selected) {
				context.fill(btnX, durationY, btnX + unitWidth, durationY + 2, 0xFFFFFFFF);
			}
			context.drawCenteredTextWithShadow(textRenderer, unit.getDisplayName(), 
				btnX + unitWidth / 2, durationY + 6, 0xFFFFFFFF);
		}
		
		// Display total minutes
		int totalMinutes = timeUnit.toMinutes(durationValue);
		context.drawText(textRenderer, "§7= " + totalMinutes + " phút", popupX + 420, durationY + 5, 0xFF888888, false);
		
		// Payment method selector
		renderPaymentSelector(context, popupX + 20, popupY + 305, popupWidth - 40, mouseX, mouseY);
		
		// Live cost display
		renderCostDisplay(context, popupX + 20, popupY + 355, popupWidth - 40);
		
		// Confirm button
		renderConfirmButton(context, popupX + 20, popupY + 390, popupWidth - 40, mouseX, mouseY);
		
		super.render(context, mouseX, mouseY, delta);
	}
	
	private void renderStatRow(DrawContext context, int x, int y, String label, int sliderIndex, 
	                            int mouseX, int mouseY, boolean enabled, int level) {
		// Special handling for Row 1 (Fortune/Silk Touch toggle)
		if (sliderIndex == 0) {
			// Toggle buttons
			int toggleWidth = 120;
			int fortuneX = x;
			int silkX = x + toggleWidth + 5;
			int toggleY = y;
			int toggleHeight = 20;
			
			boolean fortuneHovered = mouseX >= fortuneX && mouseX <= fortuneX + toggleWidth &&
			                          mouseY >= toggleY && mouseY <= toggleY + toggleHeight;
			boolean silkHovered = mouseX >= silkX && mouseX <= silkX + toggleWidth &&
			                       mouseY >= toggleY && mouseY <= toggleY + toggleHeight;
			
			int fortuneColor = useFortuneMode ? 0xFF4A9EFF : (fortuneHovered ? 0xFF3A3A3A : 0xFF2A2A2A);
			int silkColor = !useFortuneMode ? 0xFF4A9EFF : (silkHovered ? 0xFF3A3A3A : 0xFF2A2A2A);
			
			context.fill(fortuneX, toggleY, fortuneX + toggleWidth, toggleY + toggleHeight, fortuneColor);
			context.fill(silkX, toggleY, silkX + toggleWidth, toggleY + toggleHeight, silkColor);
			
			if (useFortuneMode) {
				context.fill(fortuneX, toggleY + toggleHeight - 2, fortuneX + toggleWidth, toggleY + toggleHeight, 0xFFFFFFFF);
			} else {
				context.fill(silkX, toggleY + toggleHeight - 2, silkX + toggleWidth, toggleY + toggleHeight, 0xFFFFFFFF);
			}
			
			context.drawCenteredTextWithShadow(textRenderer, "§fGia Tài", fortuneX + toggleWidth / 2, toggleY + 6, 0xFFFFFFFF);
			context.drawCenteredTextWithShadow(textRenderer, "§fĐộ Mềm Mại", silkX + toggleWidth / 2, toggleY + 6, 0xFFFFFFFF);
			
			y += 25; // Move slider below toggle
		}
		
		// Label
		String displayLabel = sliderIndex == 0 ? (useFortuneMode ? "Gia Tài" : "Độ Mềm Mại") : label;
		context.drawText(textRenderer, "§7" + displayLabel + ":", x, y, 0xFFAAAAAA, false);
		
		// Disable slider if Silk Touch mode (Row 1)
		boolean sliderEnabled = enabled && !(sliderIndex == 0 && !useFortuneMode);
		
		if (!sliderEnabled) {
			context.drawText(textRenderer, "§7(Không có cấp độ)", x + 100, y, 0xFF666666, false);
			return;
		}
		
		// Slider bar
		int sliderX = x + 100;
		int sliderWidth = popupWidth - 200;
		int sliderY = y + 2;
		
		// FIX: Check if mouse is hovering over slider area for highlight
		boolean sliderHovered = mouseX >= sliderX && mouseX <= sliderX + sliderWidth &&
		                        mouseY >= sliderY - 8 && mouseY <= sliderY + 12;
		
		// Background bar (darker when not hovered, lighter when hovered)
		int bgColor = sliderHovered ? 0xFF555555 : 0xFF444444;
		context.fill(sliderX, sliderY, sliderX + sliderWidth, sliderY + 4, bgColor);
		
		// Slider filled part (brighter when hovered)
		float progress = level / 10.0f;
		int fillColor = sliderHovered ? 0xFF6ABAFF : 0xFF5ABAFF;
		context.fill(sliderX, sliderY, sliderX + (int)(sliderWidth * progress), sliderY + 4, fillColor);
		
		// Slider handle (larger and more prominent when hovered)
		int handleX = sliderX + (int)(sliderWidth * progress) - 3;
		int handleColor = sliderHovered ? 0xFF8ACAFF : 0xFF6ABAFF;
		int handleSize = sliderHovered ? 10 : 6; // Bigger when hovered
		context.fill(handleX - (handleSize - 6) / 2, sliderY - 3, handleX + handleSize - (handleSize - 6) / 2, sliderY + 7, handleColor);
		
		// Glow effect when hovered
		if (sliderHovered) {
			// Draw a subtle glow around the slider
			context.fill(sliderX - 2, sliderY - 2, sliderX + sliderWidth + 2, sliderY - 1, 0x40FFFFFF);
			context.fill(sliderX - 2, sliderY + 5, sliderX + sliderWidth + 2, sliderY + 6, 0x40FFFFFF);
		}
		
		// Level display
		context.drawText(textRenderer, "§f" + level, x + 80, y, 0xFFFFFFFF, false);
		
		// Numeric input box (right side)
		int inputX = sliderX + sliderWidth + 10;
		int inputWidth = 30;
		context.fill(inputX, y - 2, inputX + inputWidth, y + 12, 0xFF3A3A3A);
		context.drawCenteredTextWithShadow(textRenderer, String.valueOf(level), inputX + inputWidth / 2, y + 2, 0xFFFFFFFF);
	}
	
	private void renderPaymentSelector(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		context.drawText(textRenderer, "§7Thanh toán bằng:", x, y, 0xFFAAAAAA, false);
		
		int btnWidth = (width - 10) / 2;
		int btnHeight = 25;
		int btnY = y + 15;
		
		// Silver button
		int silverX = x;
		boolean selectedSilver = useSilverPayment;
		boolean hoverSilver = mouseX >= silverX && mouseX <= silverX + btnWidth &&
		                      mouseY >= btnY && mouseY <= btnY + btnHeight;
		int silverColor = selectedSilver ? 0xFF4A9EFF : (hoverSilver ? 0xFF3A3A3A : 0xFF2A2A2A);
		context.fill(silverX, btnY, silverX + btnWidth, btnY + btnHeight, silverColor);
		if (selectedSilver) {
			context.fill(silverX, btnY, silverX + btnWidth, btnY + 2, 0xFFFFFFFF);
		}
		context.drawCenteredTextWithShadow(textRenderer, "Silver", silverX + btnWidth / 2, btnY + 8, 0xFFC0C0C0);
		
		// Gold button
		int goldX = x + btnWidth + 10;
		boolean selectedGold = !useSilverPayment;
		boolean hoverGold = mouseX >= goldX && mouseX <= goldX + btnWidth &&
		                    mouseY >= btnY && mouseY <= btnY + btnHeight;
		int goldColor = selectedGold ? 0xFF4A9EFF : (hoverGold ? 0xFF3A3A3A : 0xFF2A2A2A);
		context.fill(goldX, btnY, goldX + btnWidth, btnY + btnHeight, goldColor);
		if (selectedGold) {
			context.fill(goldX, btnY, goldX + btnWidth, btnY + 2, 0xFFFFFFFF);
		}
		context.drawCenteredTextWithShadow(textRenderer, "Gold", goldX + btnWidth / 2, btnY + 8, 0xFFFFD700);
	}
	
	private void renderCostDisplay(DrawContext context, int x, int y, int width) {
		context.fill(x, y, x + width, y + 1, 0xFF4A4A4A);
		context.drawText(textRenderer, "§7Tổng chi phí:", x, y + 5, 0xFFAAAAAA, false);
		
		// Calculate cost
		int[] cost = calculateCost();
		int silverCost = cost[0];
		int goldCost = cost[1];
		
		String costText;
		int color;
		if (useSilverPayment) {
			costText = silverCost + " Silver";
			color = 0xFFC0C0C0;
		} else {
			costText = goldCost + " Gold";
			color = 0xFFFFD700;
		}
		
		int textWidth = textRenderer.getWidth(costText);
		context.drawText(textRenderer, costText, x + width - textWidth, y + 5, color, true);
	}
	
	private void renderConfirmButton(DrawContext context, int x, int y, int width, int mouseX, int mouseY) {
		int btnHeight = 35;
		
		// Check if can afford
		int[] cost = calculateCost();
		int silverCost = cost[0];
		int goldCost = cost[1];
		
		boolean canAfford;
		if (useSilverPayment) {
			canAfford = ClientDataCache.getSilverCoins() >= silverCost;
		} else {
			canAfford = ClientDataCache.getGoldCoins() >= goldCost;
		}
		
		boolean hovered = mouseX >= x && mouseX <= x + width &&
		                  mouseY >= y && mouseY <= y + btnHeight;
		
		int bgColor = canAfford ? (hovered ? 0xFF5ABAFF : 0xFF4A9EFF) : 0xFF444444;
		context.fill(x, y, x + width, y + btnHeight, bgColor);
		
		if (canAfford) {
			context.fill(x, y, x + width, y + 2, 0xFF6ABAFF);
			context.fill(x, y + btnHeight - 2, x + width, y + btnHeight, 0xFF6ABAFF);
		}
		
		String buttonText = canAfford ? "§l§fXÁC NHẬN THUÊ" : "§7Không đủ tiền";
		context.drawCenteredTextWithShadow(textRenderer, buttonText, 
			x + width / 2, y + 12, 0xFFFFFFFF);
	}
	
	/**
	 * Calculate rental cost using EXACT formula from spec
	 * cost_silver = 30 + (level² sum of 4 stats)
	 * - If Độ Mềm Mại mode: treat as fixed 100 contribution
	 * - Multiply by ceil(duration / 30)
	 */
	private int[] calculateCost() {
		// Base cost
		int baseCost = 30;
		
		// Stat contributions (level²)
		int fortuneContribution = useFortuneMode ? (fortuneLevel * fortuneLevel) : 100; // Silk Touch = flat 100
		int efficiencyContribution = efficiencyLevel * efficiencyLevel;
		int unbreakingContribution = unbreakingLevel * unbreakingLevel;
		int mendingContribution = mendingLevel * mendingLevel;
		
		int statSum = fortuneContribution + efficiencyContribution + unbreakingContribution + mendingContribution;
		
		// Per-block cost (30 minutes)
		int perBlockCost = baseCost + statSum;
		
		// Convert duration to minutes
		int totalMinutes = timeUnit.toMinutes(durationValue);
		
		// Number of 30-minute blocks (rounded UP)
		int blocks = (int) Math.ceil(totalMinutes / 30.0);
		
		// Total silver cost
		int totalSilver = perBlockCost * blocks;
		
		// Gold conversion (100 silver = 1 gold, rounded UP)
		int totalGold = (int) Math.ceil(totalSilver / 100.0);
		
		return new int[]{totalSilver, totalGold};
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
		
		// Duration field
		if (durationField != null && durationField.mouseClicked(mouseX, mouseY, button)) {
			durationField.setFocused(true);
			return true;
		}
		
		// Time unit selector
		int durationY = popupY + 270;
		int unitX = popupX + 190;
		int unitWidth = 60;
		int unitHeight = 20;
		int unitSpacing = 5;
		
		TimeUnit[] units = TimeUnit.values();
		for (int i = 0; i < units.length; i++) {
			int btnX = unitX + i * (unitWidth + unitSpacing);
			if (mx >= btnX && mx <= btnX + unitWidth &&
			    my >= durationY && my <= durationY + unitHeight) {
				timeUnit = units[i];
				return true;
			}
		}
		
		// Fortune/Silk Touch toggle (Row 1)
		int toggleY = popupY + 40;
		int toggleWidth = 120;
		int fortuneX = popupX + 20;
		int silkX = fortuneX + toggleWidth + 5;
		int toggleHeight = 20;
		
		if (my >= toggleY && my <= toggleY + toggleHeight) {
			if (mx >= fortuneX && mx <= fortuneX + toggleWidth) {
				useFortuneMode = true;
				return true;
			}
			if (mx >= silkX && mx <= silkX + toggleWidth) {
				useFortuneMode = false;
				fortuneLevel = 0; // Reset level when switching to Silk Touch
				return true;
			}
		}
		
		// Slider clicks (start dragging)
		int sliderY = popupY + 40;
		int sliderSpacing = 45;
		int sliderX = popupX + 120;
		int sliderWidth = popupWidth - 200;
		
		// FIX: Match exact Y coordinates from render + EXPANDED hitbox
		int[] rowYPositions = new int[4];
		rowYPositions[0] = sliderY + 25 + 2;
		rowYPositions[1] = sliderY + sliderSpacing + 2;
		rowYPositions[2] = sliderY + 2 * sliderSpacing + 2;
		rowYPositions[3] = sliderY + 3 * sliderSpacing + 2;
		
		for (int i = 0; i < 4; i++) {
			if (i == 0 && !useFortuneMode) continue;
			
			int sliderBarY = rowYPositions[i];
			// EXPANDED hitbox: from y-8 to y+12 (20 pixels tall)
			if (mx >= sliderX && mx <= sliderX + sliderWidth && 
			    my >= sliderBarY - 8 && my <= sliderBarY + 12) {
				draggingSlider = i;
				updateSliderValue(mx, sliderX, sliderWidth, i);
				return true;
			}
		}
		
		// Payment method buttons
		int paymentY = popupY + 305;
		int btnWidth = (popupWidth - 50) / 2;
		int btnHeight = 25;
		int btnY = paymentY + 15;
		int silverX = popupX + 20;
		int goldX = silverX + btnWidth + 10;
		
		if (my >= btnY && my <= btnY + btnHeight) {
			if (mx >= silverX && mx <= silverX + btnWidth) {
				useSilverPayment = true;
				return true;
			}
			if (mx >= goldX && mx <= goldX + btnWidth) {
				useSilverPayment = false;
				return true;
			}
		}
		
		// Confirm button
		int confirmY = popupY + 390;
		int confirmHeight = 35;
		if (mx >= popupX + 20 && mx <= popupX + popupWidth - 20 &&
		    my >= confirmY && my <= confirmY + confirmHeight) {
			int[] cost = calculateCost();
			int silverCost = cost[0];
			int goldCost = cost[1];
			
			boolean canAfford = useSilverPayment ? 
				(ClientDataCache.getSilverCoins() >= silverCost) :
				(ClientDataCache.getGoldCoins() >= goldCost);
			
			if (canAfford) {
				// Convert to total minutes
				int totalMinutes = timeUnit.toMinutes(durationValue);
				
				// Send rental request to server
				com.focustimershop.network.ModNetworking.sendRentalRequest(
					toolIndex, useFortuneMode, fortuneLevel, efficiencyLevel,
					unbreakingLevel, mendingLevel, totalMinutes, useSilverPayment
				);
				
				com.focustimershop.FocusTimerShop.LOGGER.info("Sent rental request: {} - {}min - Cost: {}s/{}g", 
					toolName, totalMinutes, silverCost, goldCost);
				close();
				return true;
			}
		}
		
		return super.mouseClicked(mouseX, mouseY, button);
	}
	
	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (draggingSlider >= 0) {
			int sliderX = popupX + 120;
			int sliderWidth = popupWidth - 200;
			updateSliderValue((int)mouseX, sliderX, sliderWidth, draggingSlider);
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}
	
	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		draggingSlider = -1;
		return super.mouseReleased(mouseX, mouseY, button);
	}
	
	private void updateSliderValue(int mouseX, int sliderX, int sliderWidth, int sliderIndex) {
		float ratio = Math.max(0, Math.min(1, (mouseX - sliderX) / (float)sliderWidth));
		int newValue = (int)(ratio * 10);
		
		switch (sliderIndex) {
			case 0: fortuneLevel = newValue; break;
			case 1: efficiencyLevel = newValue; break;
			case 2: unbreakingLevel = newValue; break;
			case 3: mendingLevel = newValue; break;
		}
	}
	
	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}
	
	@Override
	public boolean shouldPause() {
		return false;
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		if (durationField != null && durationField.isFocused()) {
			return durationField.keyPressed(keyCode, scanCode, modifiers);
		}
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public boolean charTyped(char chr, int modifiers) {
		if (durationField != null && durationField.isFocused()) {
			return durationField.charTyped(chr, modifiers);
		}
		return super.charTyped(chr, modifiers);
	}
}
