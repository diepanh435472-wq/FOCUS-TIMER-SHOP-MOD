package com.focustimershop.shop;

public enum ShopCategory {
	ALL("Tất cả"),
	BUILDING_BLOCKS("Xây dựng"),
	COLORED_BLOCKS("Màu sắc"),
	NATURAL_BLOCKS("Tự nhiên"),
	FUNCTIONAL_BLOCKS("Chức năng"),
	REDSTONE("Redstone"),
	TOOLS_UTILITIES("Công cụ & Tiện ích"),
	FOOD_DRINKS("Đồ ăn & Thức uống"),
	INGREDIENTS("Nguyên liệu"),
	// Future categories
	WEAPONS("Vũ khí");

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
			case NATURAL_BLOCKS: return "Tự nhiên";
			case FUNCTIONAL_BLOCKS: return "Chức năng";
			case REDSTONE: return "Red";
			case TOOLS_UTILITIES: return "Công cụ";
			case FOOD_DRINKS: return "Đồ ăn";
			case INGREDIENTS: return "Nguyên liệu";
			case WEAPONS: return "Weapon";
			default: return "?";
		}
	}
}
