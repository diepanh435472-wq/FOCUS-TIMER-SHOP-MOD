package com.focustimershop.economy;

import net.minecraft.nbt.NbtCompound;

/**
 * Stores per-player economy data: Silver Coin, Gold Coin, Focus XP
 * Server-authoritative - never trust client-sent values
 */
public class PlayerEconomyData {
	private int silverCoins;
	private int goldCoins;
	private int focusXp;

	public PlayerEconomyData() {
		this.silverCoins = 0;
		this.goldCoins = 0;
		this.focusXp = 0;
	}

	public PlayerEconomyData(int silverCoins, int goldCoins, int focusXp) {
		this.silverCoins = silverCoins;
		this.goldCoins = goldCoins;
		this.focusXp = focusXp;
	}

	// Getters
	public int getSilverCoins() {
		return silverCoins;
	}

	public int getGoldCoins() {
		return goldCoins;
	}

	public int getFocusXp() {
		return focusXp;
	}

	// Add currency (server-side only, validates non-negative)
	public void addSilverCoins(int amount) {
		if (amount > 0) {
			this.silverCoins += amount;
		}
	}

	public void addGoldCoins(int amount) {
		if (amount > 0) {
			this.goldCoins += amount;
		}
	}

	public void addFocusXp(int amount) {
		if (amount > 0) {
			this.focusXp += amount;
		}
	}

	// Remove currency with validation (returns true if successful)
	public boolean removeSilverCoins(int amount) {
		if (amount > 0 && this.silverCoins >= amount) {
			this.silverCoins -= amount;
			return true;
		}
		return false;
	}

	public boolean removeGoldCoins(int amount) {
		if (amount > 0 && this.goldCoins >= amount) {
			this.goldCoins -= amount;
			return true;
		}
		return false;
	}

	// Currency conversion: 100 Silver = 1 Gold (bidirectional)
	public boolean convertSilverToGold(int silverAmount) {
		if (silverAmount >= 100 && silverAmount % 100 == 0 && this.silverCoins >= silverAmount) {
			int goldToAdd = silverAmount / 100;
			this.silverCoins -= silverAmount;
			this.goldCoins += goldToAdd;
			return true;
		}
		return false;
	}

	public boolean convertGoldToSilver(int goldAmount) {
		if (goldAmount > 0 && this.goldCoins >= goldAmount) {
			int silverToAdd = goldAmount * 100;
			this.goldCoins -= goldAmount;
			this.silverCoins += silverToAdd;
			return true;
		}
		return false;
	}

	// NBT Serialization
	public void writeNbt(NbtCompound nbt) {
		nbt.putInt("SilverCoins", this.silverCoins);
		nbt.putInt("GoldCoins", this.goldCoins);
		nbt.putInt("FocusXp", this.focusXp);
	}

	public void readNbt(NbtCompound nbt) {
		this.silverCoins = nbt.getInt("SilverCoins");
		this.goldCoins = nbt.getInt("GoldCoins");
		this.focusXp = nbt.getInt("FocusXp");
	}

	public static PlayerEconomyData fromNbt(NbtCompound nbt) {
		PlayerEconomyData data = new PlayerEconomyData();
		data.readNbt(nbt);
		return data;
	}

	// Clone for safe client-side display
	public PlayerEconomyData copy() {
		return new PlayerEconomyData(this.silverCoins, this.goldCoins, this.focusXp);
	}
}
