package com.focustimershop.client.gui;

import com.focustimershop.client.ClientProfileCache;
import com.focustimershop.network.EquipTitleC2SPacket;
import com.focustimershop.network.NetworkHandler;
import com.focustimershop.title.TitleDefinition;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.DiffuseLighting;
import net.minecraft.client.render.entity.EntityRenderDispatcher;
import net.minecraft.entity.LivingEntity;
import net.minecraft.text.Text;
import org.joml.Quaternionf;

/**
 * Confirmation dialog for equipping title (v1.0.6 Phase 2)
 * Shows 2 preview lines with player model/name + title
 */
public class TitleEquipConfirmScreen extends Screen {
	
	private final Screen parent;
	private final TitleDefinition title;
	
	public TitleEquipConfirmScreen(Screen parent, TitleDefinition title) {
		super(Text.literal("Trang bị danh hiệu"));
		this.parent = parent;
		this.title = title;
	}
	
	@Override
	protected void init() {
		super.init();
		
		int dialogWidth = 400;
		int dialogHeight = 200;
		int dialogX = (width - dialogWidth) / 2;
		int dialogY = (height - dialogHeight) / 2;
		
		// Confirm button
		this.addDrawableChild(ButtonWidget.builder(
			Text.literal("§aXác nhận"),
			button -> {
				// Send equip packet
				try {
					ClientPlayNetworking.send(NetworkHandler.EQUIP_TITLE_C2S,
						new EquipTitleC2SPacket(title.getId()).toPacket());
					
					// Update local cache
					ClientProfileCache.setEquippedTitleId(title.getId());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				// Close dialog
				this.close();
			}
		).dimensions(dialogX + dialogWidth / 2 - 110, dialogY + dialogHeight - 40, 100, 20).build());
		
		// Cancel button
		this.addDrawableChild(ButtonWidget.builder(
			Text.literal("§cHuỷ"),
			button -> this.close()
		).dimensions(dialogX + dialogWidth / 2 + 10, dialogY + dialogHeight - 40, 100, 20).build());
	}
	
	@Override
	public void render(DrawContext context, int mouseX, int mouseY, float delta) {
		// Dim background (v1.21 API)
		renderBackgroundTexture(context);
		
		int dialogWidth = 400;
		int dialogHeight = 200;
		int dialogX = (width - dialogWidth) / 2;
		int dialogY = (height - dialogHeight) / 2;
		
		// Dialog background
		context.fill(dialogX, dialogY, dialogX + dialogWidth, dialogY + dialogHeight, 0xE0000000);
		context.drawBorder(dialogX, dialogY, dialogWidth, dialogHeight, 0xFF4A9EFF);
		
		// Title
		context.drawCenteredTextWithShadow(textRenderer, "§l§dBạn có muốn gắn danh hiệu này không?",
			dialogX + dialogWidth / 2, dialogY + 15, 0xFFFFFFFF);
		
		// Preview section label
		context.drawText(textRenderer, "§7Xem trước:", 
			dialogX + 20, dialogY + 45, 0xFFAAAAAA, false);
		
		// Preview line 1: [model] <title> | <name>
		int previewY = dialogY + 65;
		ClientPlayerEntity player = client.player;
		if (player != null) {
			// Render player model (small)
			int modelX = dialogX + 40;
			int modelY = previewY + 25;
			renderPlayerModel(context, modelX, modelY, 20, player);
			
			// Title prefix
			String titleText = "§d[" + title.getDisplayPrefix() + "]";
			context.drawText(textRenderer, titleText, modelX + 30, previewY, 0xFFFFFFFF, true);
			
			// Player name
			String playerName = ClientProfileCache.getDisplayName();
			context.drawText(textRenderer, " §7| §f" + playerName, 
				modelX + 30 + textRenderer.getWidth(titleText), previewY, 0xFFFFFFFF, false);
		}
		
		// Preview line 2: <title> | <name> (without model)
		previewY += 40;
		String titleText2 = "§d[" + title.getDisplayPrefix() + "]";
		context.drawText(textRenderer, titleText2, dialogX + 40, previewY, 0xFFFFFFFF, true);
		
		String playerName2 = ClientProfileCache.getDisplayName();
		context.drawText(textRenderer, " §7| §f" + playerName2, 
			dialogX + 40 + textRenderer.getWidth(titleText2), previewY, 0xFFFFFFFF, false);
		
		// Render buttons
		super.render(context, mouseX, mouseY, delta);
	}
	
	/**
	 * Render player model (simplified)
	 */
	private void renderPlayerModel(DrawContext context, int x, int y, int size, LivingEntity entity) {
		try {
			DiffuseLighting.method_34742();
			context.getMatrices().push();
			context.getMatrices().translate(x, y, 50);
			context.getMatrices().scale(size, size, -size);
			Quaternionf rotation = new Quaternionf().rotateZ((float)Math.PI);
			Quaternionf rotation2 = new Quaternionf().rotateX(0.2f);
			rotation.mul(rotation2);
			context.getMatrices().multiply(rotation);
			
			EntityRenderDispatcher dispatcher = MinecraftClient.getInstance().getEntityRenderDispatcher();
			rotation2.conjugate();
			dispatcher.setRotation(rotation2);
			dispatcher.setRenderShadows(false);
			
			dispatcher.render(entity, 0, 0, 0, 0, 1, context.getMatrices(), 
				context.getVertexConsumers(), 15728880);
			
			dispatcher.setRenderShadows(true);
			context.getMatrices().pop();
			DiffuseLighting.enableGuiDepthLighting();
		} catch (Exception e) {
			// Fallback - just skip model rendering
		}
	}
	
	@Override
	public void close() {
		client.setScreen(parent);
	}
	
	@Override
	public boolean shouldPause() {
		return false; // Don't pause game
	}
}
