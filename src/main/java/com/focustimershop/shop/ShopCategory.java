package com.focustimershop.shop;

public enum ShopCategory {
	ALL("Tất cả"),
	BUILDING_BLOCKS("Xây dựng"),
	COLORED_BLOCKS("Màu sắc"),
	// Future categories for v1.0.2+
	TOOLS("Công cụ"),
	WEAPONS("Vũ khí"),
	FOOD("Thức ăn"),
	REDSTONE("Redstone");

	private final String displayName;

	ShopCategory(String displayName) {
		this.displayName = displayName;
	}

	public String getDisplayName() {
		return displayName;
	}
	
	public String getShortName() {
		switch (this) {
			case ALL: return "All";
			case BUILDING_BLOCKS: return "Xây dựng";
			case COLORED_BLOCKS: return "Màu";
			case TOOLS: return "Tool";
			case WEAPONS: return "Weapon";
			case FOOD: return "Food";
			case REDSTONE: return "Red";
			default: return "?";
		}
	}
}
