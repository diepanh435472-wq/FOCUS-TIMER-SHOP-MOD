package com.focustimershop.shop;

public class ShopItem {
	private final String itemId;
	private final ShopCategory category;
	private final int silverPrice;
	private final int goldPrice;
	private final String displayName;

	public ShopItem(String itemId, ShopCategory category, int silverPrice, int goldPrice, String displayName) {
		this.itemId = itemId;
		this.category = category;
		this.silverPrice = silverPrice;
		this.goldPrice = goldPrice;
		this.displayName = displayName;
	}

	public String getItemId() {
		return itemId;
	}

	public ShopCategory getCategory() {
		return category;
	}

	public int getSilverPrice() {
		return silverPrice;
	}

	public int getGoldPrice() {
		return goldPrice;
	}

	public String getDisplayName() {
		return displayName;
	}

	/**
	 * Check if item can be purchased with gold (conversion: 100 silver = 1 gold)
	 */
	public boolean canPayWithGold(int playerGold) {
		int goldCost = (int) Math.ceil(silverPrice / 100.0);
		return playerGold >= goldCost;
	}

	public int getGoldCost() {
		return (int) Math.ceil(silverPrice / 100.0);
	}
}
