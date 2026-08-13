package com.focustimershop.luckychest;

public enum ChestRarity {
	COMMON("§7Common", 0xAAAAAA),
	UNCOMMON("§aUncommon", 0x55FF55),
	RARE("§9Rare", 0x5555FF),
	EPIC("§5Epic", 0xAA00AA),
	LEGENDARY("§6Legendary", 0xFFAA00);

	private final String displayName;
	private final int color;

	ChestRarity(String displayName, int color) {
		this.displayName = displayName;
		this.color = color;
	}

	public String getDisplayName() {
		return displayName;
	}

	public int getColor() {
		return color;
	}
}
