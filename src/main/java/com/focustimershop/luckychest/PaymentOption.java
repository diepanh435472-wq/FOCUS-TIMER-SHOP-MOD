package com.focustimershop.luckychest;

/**
 * Represents a payment option for opening chests
 * Can be pure silver, pure gold, or mixed
 */
public class PaymentOption {
	private final int silverCoins;
	private final int goldCoins;

	public PaymentOption(int silverCoins, int goldCoins) {
		this.silverCoins = silverCoins;
		this.goldCoins = goldCoins;
	}

	public static PaymentOption silver(int amount) {
		return new PaymentOption(amount, 0);
	}

	public static PaymentOption gold(int amount) {
		return new PaymentOption(0, amount);
	}

	public static PaymentOption mixed(int goldCoins, int silverCoins) {
		return new PaymentOption(silverCoins, goldCoins);
	}

	public int getSilverCoins() {
		return silverCoins;
	}

	public int getGoldCoins() {
		return goldCoins;
	}

	public boolean canAfford(int playerSilver, int playerGold) {
		return playerSilver >= silverCoins && playerGold >= goldCoins;
	}

	/**
	 * Format for display (e.g., "40s", "3g", "1g+50s")
	 */
	public String getDisplayText() {
		if (goldCoins > 0 && silverCoins > 0) {
			return goldCoins + "g+" + silverCoins + "s";
		} else if (goldCoins > 0) {
			return goldCoins + "g";
		} else {
			return silverCoins + "s";
		}
	}

	/**
	 * Short format for buttons
	 */
	public String getShortDisplay() {
		if (goldCoins > 0 && silverCoins > 0) {
			return goldCoins + "g" + silverCoins + "s";
		} else if (goldCoins > 0) {
			return goldCoins + "g";
		} else {
			return silverCoins + "s";
		}
	}
}
