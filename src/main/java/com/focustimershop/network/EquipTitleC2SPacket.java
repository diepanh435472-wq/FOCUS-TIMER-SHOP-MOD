package com.focustimershop.network;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;

/**
 * Client-to-server packet for equipping a title (v1.0.6 Phase 5)
 */
public class EquipTitleC2SPacket {
	
	private final String titleId;
	
	public EquipTitleC2SPacket(String titleId) {
		this.titleId = titleId;
	}
	
	public PacketByteBuf toPacket() {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(titleId);
		return buf;
	}
	
	public static EquipTitleC2SPacket fromPacket(PacketByteBuf buf) {
		String titleId = buf.readString();
		return new EquipTitleC2SPacket(titleId);
	}
	
	public String getTitleId() {
		return titleId;
	}
}
