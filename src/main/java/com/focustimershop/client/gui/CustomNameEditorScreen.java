package com.focustimershop.client.gui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;

/**
 * v1.0.6 - Custom name editor sub-screen
 * Max 24 chars, no color codes
 */
public class CustomNameEditorScreen extends Screen {
	
	private final Screen parent;
	private TextFieldWidget nameField;
	private ButtonWidget saveButton;
	private ButtonWidget cancelButton;
	private String errorMessage = "";
	
	private static final int MAX_NAME_LENGTH = 24;
	
	public CustomNameEditorScreen(Screen parent) {
		super(Text.literal("Chỉnh sửa tên custom"));
		this.parent = parent;
	}
	
	@Override
	protected void init() {
		int centerX = this.width / 2;
		int centerY = this.height / 2;
		
		// Text field (centered)
		nameField = new TextFieldWidget(this.textRenderer, centerX - 100, centerY - 30, 200, 20, Text.literal("Custom Name"));
		nameField.setMaxLength(MAX_NAME_LENGTH);
		nameField.setFocusUnlocked(true);
		
		// Get current custom name from cache
		String currentName = com.focustimershop.client.ClientProfileCache.getCustomName();
		if (currentName != null && !currentName.isEmpty()) {
			nameField.setText(currentName);
		}
		
		this.addSelectableChild(nameField);
		this.setInitialFocus(nameField);
		
		// Save button
		saveButton = ButtonWidget.builder(Text.literal("§a✔ Lưu"), button -> {
			String newName = nameField.getText().trim();
			
			// Validate
			if (newName.isEmpty()) {
				errorMessage = "§cTên không được để trống";
				return;
			}
			
			if (containsColorCodes(newName)) {
				errorMessage = "§cKhông được dùng color codes (§)";
				return;
			}
			
			if (newName.length() > MAX_NAME_LENGTH) {
				errorMessage = "§cTên quá dài (tối đa " + MAX_NAME_LENGTH + " ký tự)";
				return;
			}
			
			// Send to server
			sendCustomNameUpdate(newName);
			
			// Close and return to profile screen
			if (client != null) {
				client.setScreen(parent);
			}
		})
		.dimensions(centerX - 100, centerY + 10, 95, 20)
		.build();
		
		// Cancel button
		cancelButton = ButtonWidget.builder(Text.literal("§c✖ Hủy"), button -> {
			if (client != null) {
				client.setScreen(parent);
			}
		})
		.dimensions(centerX + 5, centerY + 10, 95, 20)
		.build();
		
		this.addDrawableChild(saveButton);
		this.addDrawableChild(cancelButton);
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Background
		this.renderBackground(context);
		
		// Dark panel
		int panelWidth = 280;
		int panelHeight = 120;
		int panelX = (this.width - panelWidth) / 2;
		int panelY = (this.height - panelHeight) / 2;
		
		context.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xD0000000);
		context.drawBorder(panelX, panelY, panelWidth, panelHeight, 0xFF4A9EFF);
		
		// Title
		context.drawCenteredTextWithShadow(this.textRenderer, "§b§lChỉnh sửa tên custom", 
			this.width / 2, panelY + 15, 0xFFFFFFFF);
		
		// Instructions
		context.drawCenteredTextWithShadow(this.textRenderer, "§7(Tối đa " + MAX_NAME_LENGTH + " ký tự, không color codes)", 
			this.width / 2, panelY + 30, 0xFFAAAAAA);
		
		// Text field
		nameField.render(context, mouseX, mouseY, delta);
		
		// Error message
		if (!errorMessage.isEmpty()) {
			context.drawCenteredTextWithShadow(this.textRenderer, errorMessage, 
				this.width / 2, panelY + 80, 0xFFFFFFFF);
		}
		
		// Buttons
		super.render(context, mouseX, mouseY, delta);
	}
	
	@Override
	public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
		// Enter key = save
		if (keyCode == 257) { // GLFW_KEY_ENTER
			saveButton.onPress();
			return true;
		}
		
		// Esc key = cancel
		if (keyCode == 256) { // GLFW_KEY_ESCAPE
			if (client != null) {
				client.setScreen(parent);
			}
			return true;
		}
		
		return super.keyPressed(keyCode, scanCode, modifiers);
	}
	
	@Override
	public void close() {
		if (client != null) {
			client.setScreen(parent);
		}
	}
	
	private boolean containsColorCodes(String text) {
		return text.contains("§");
	}
	
	/**
	 * Send custom name update to server
	 */
	private void sendCustomNameUpdate(String newName) {
		// Create packet
		net.minecraft.network.PacketByteBuf buf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
		buf.writeString(newName);
		
		// Send to server
		net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking.send(
			com.focustimershop.network.ModNetworking.CUSTOM_NAME_UPDATE, buf);
		
		com.focustimershop.FocusTimerShop.LOGGER.info("Sent custom name update: {}", newName);
	}
}
