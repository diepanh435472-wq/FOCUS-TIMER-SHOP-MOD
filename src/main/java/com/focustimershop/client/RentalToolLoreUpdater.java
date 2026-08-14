package com.focustimershop.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.text.Text;

/**
 * Updates rental tool lore on client side to show real-time countdown
 */
public class RentalToolLoreUpdater {
	
	private static int tickCounter = 0;
	
	public static void register() {
		ClientTickEvents.END_CLIENT_TICK.register(RentalToolLoreUpdater::onClientTick);
	}
	
	private static void onClientTick(MinecraftClient client) {
		// Only update every 20 ticks (1 second)
		tickCounter++;
		if (tickCounter < 20) {
			return;
		}
		tickCounter = 0;
		
		PlayerEntity player = client.player;
		if (player == null) {
			return;
		}
		
		// Update all rental tools in player inventory
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			updateRentalToolLore(stack);
		}
	}
	
	/**
	 * Update lore on rental tool to show remaining time
	 */
	private static void updateRentalToolLore(ItemStack stack) {
		if (stack.isEmpty() || !stack.hasNbt()) {
			return;
		}
		
		NbtCompound nbt = stack.getNbt();
		if (!nbt.contains("RentalType") || !nbt.contains("RemainingSeconds")) {
			return;
		}
		
		// Get rental info
		int remainingSeconds = nbt.getInt("RemainingSeconds");
		boolean useFortuneMode = nbt.getBoolean("UseFortuneMode");
		int fortuneLevel = nbt.getInt("CustomFortuneLevel");
		int efficiencyLevel = nbt.getInt("CustomEfficiencyLevel");
		int unbreakingLevel = nbt.getInt("CustomUnbreakingLevel");
		int mendingLevel = nbt.getInt("CustomMendingLevel");
		String rentalType = nbt.getString("RentalType");
		
		// Calculate time display
		int hours = remainingSeconds / 3600;
		int minutes = (remainingSeconds % 3600) / 60;
		int secs = remainingSeconds % 60;
		
		String timeDisplay = String.format("%dh %dm %ds", hours, minutes, secs);
		
		// Rebuild lore
		NbtCompound display = nbt.getCompound("display");
		if (!nbt.contains("display")) {
			display = new NbtCompound();
			nbt.put("display", display);
		}
		
		NbtList lore = new NbtList();
		
		// Time remaining
		lore.add(NbtString.of(Text.Serializer.toJson(
			Text.literal("§7⏱ Còn lại: §e" + timeDisplay)
		)));
		
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§7-------------------"))));
		
		// Stats
		if (useFortuneMode && fortuneLevel > 0) {
			lore.add(NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Gia Tài " + fortuneLevel)
			)));
		} else if (!useFortuneMode) {
			lore.add(NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Độ Mềm Mại")
			)));
		}
		
		if (efficiencyLevel > 0) {
			lore.add(NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Hiệu Suất " + efficiencyLevel)
			)));
		}
		
		if (unbreakingLevel > 0) {
			lore.add(NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Chậm Hỏng " + unbreakingLevel)
			)));
		}
		
		if (mendingLevel > 0) {
			lore.add(NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Sửa Chữa " + mendingLevel)
			)));
		}
		
		lore.add(NbtString.of(Text.Serializer.toJson(Text.literal("§7-------------------"))));
		
		// Feature info based on tool type
		if (rentalType.equals("PICKAXE") || rentalType.equals("SHOVEL")) {
			lore.add(NbtString.of(Text.Serializer.toJson(
				Text.literal("§7Đào 3x3: §aĐè Shift")
			)));
		} else if (rentalType.equals("AXE")) {
			lore.add(NbtString.of(Text.Serializer.toJson(
				Text.literal("§7Chặt cây nhanh: §aĐè Shift")
			)));
		}
		
		display.put("Lore", lore);
	}
}
