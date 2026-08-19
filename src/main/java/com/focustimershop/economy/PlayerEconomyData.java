package com.focustimershop.economy;

import net.minecraft.nbt.NbtCompound;

/**
 * Stores per-player economy data: Silver Coin, Gold Coin, Focus XP
 * Server-authoritative - never trust client-sent values
 * v1.0.6 Phase 0 - Changed to long to prevent overflow
 */
public class PlayerEconomyData {
	private long silverCoins;
	private long goldCoins;
	private long focusXp;

	public PlayerEconomyData() {
		this.silverCoins = 0;
		this.goldCoins = 0;
		this.focusXp = 0;
	}

	public PlayerEconomyData(long silverCoins, long goldCoins, long focusXp) {
		this.silverCoins = silverCoins;
		this.goldCoins = goldCoins;
		this.focusXp = focusXp;
	}

	// Getters
	public long getSilverCoins() {
		return silverCoins;
	}

	public long getGoldCoins() {
		return goldCoins;
	}

	public long getFocusXp() {
		return focusXp;
	}

	// Setters (admin commands only)
	public void setSilverCoins(long amount) {
		this.silverCoins = Math.max(0, amount);
	}

	public void setGoldCoins(long amount) {
		this.goldCoins = Math.max(0, amount);
	}

	public void setFocusXp(long amount) {
		this.focusXp = Math.max(0, amount);
	}

	// Add currency (server-side only, validates non-negative)
	// PHASE 3 FIX (BUG #17): Overflow protection with Math.addExact
	public void addSilverCoins(long amount) {
		if (amount > 0) {
			try {
				this.silverCoins = Math.addExact(this.silverCoins, amount);
			} catch (ArithmeticException e) {
				// Overflow would occur - cap at Long.MAX_VALUE
				this.silverCoins = Long.MAX_VALUE;
				com.focustimershop.FocusTimerShop.LOGGER.warn("PHASE3_ECONOMY: Silver overflow capped at MAX_VALUE");
			}
		}
	}

	public void addGoldCoins(long amount) {
		if (amount > 0) {
			try {
				this.goldCoins = Math.addExact(this.goldCoins, amount);
			} catch (ArithmeticException e) {
				// Overflow would occur - cap at Long.MAX_VALUE
				this.goldCoins = Long.MAX_VALUE;
				com.focustimershop.FocusTimerShop.LOGGER.warn("PHASE3_ECONOMY: Gold overflow capped at MAX_VALUE");
			}
		}
	}

	public void addFocusXp(long amount) {
		if (amount > 0) {
			try {
				this.focusXp = Math.addExact(this.focusXp, amount);
			} catch (ArithmeticException e) {
				// Overflow would occur - cap at Long.MAX_VALUE
				this.focusXp = Long.MAX_VALUE;
				com.focustimershop.FocusTimerShop.LOGGER.warn("PHASE3_ECONOMY: XP overflow capped at MAX_VALUE");
			}
		}
	}

	// Remove currency with validation (returns true if successful)
	public boolean removeSilverCoins(long amount) {
		if (amount > 0 && this.silverCoins >= amount) {
			this.silverCoins -= amount;
			return true;
		}
		return false;
	}

	public boolean removeGoldCoins(long amount) {
		if (amount > 0 && this.goldCoins >= amount) {
			this.goldCoins -= amount;
			return true;
		}
		return false;
	}

	// Currency conversion: 100 Silver = 1 Gold (bidirectional)
	// PHASE 3 FIX (BUG #14, #15): Safe arithmetic with overflow detection and atomicity
	public boolean convertSilverToGold(long silverAmount) {
		// Validation
		if (silverAmount < 100 || silverAmount % 100 != 0) {
			return false; // Must be multiple of 100
		}
		if (this.silverCoins < silverAmount) {
			return false; // Not enough silver
		}
		
		try {
			long goldToAdd = silverAmount / 100; // Safe division, no overflow possible
			
			// Check if adding gold would overflow
			long newGold = Math.addExact(this.goldCoins, goldToAdd);
			
			// All checks passed - perform atomic conversion
			this.silverCoins -= silverAmount;
			this.goldCoins = newGold;
			
			return true;
			
		} catch (ArithmeticException e) {
			// Overflow would occur
			com.focustimershop.FocusTimerShop.LOGGER.warn("PHASE3_ECONOMY: Silver→Gold conversion overflow prevented");
			return false;
		}
	}

	public boolean convertGoldToSilver(long goldAmount) {
		// Validation
		if (goldAmount <= 0 || this.goldCoins < goldAmount) {
			return false;
		}
		
		try {
			// PHASE 3 FIX (BUG #14): Safe multiplication with overflow detection
			long silverToAdd = Math.multiplyExact(goldAmount, 100);
			
			// Check if adding silver would overflow
			long newSilver = Math.addExact(this.silverCoins, silverToAdd);
			
			// All checks passed - perform atomic conversion
			this.goldCoins -= goldAmount;
			this.silverCoins = newSilver;
			
			return true;
			
		} catch (ArithmeticException e) {
			// Overflow would occur
			com.focustimershop.FocusTimerShop.LOGGER.warn("PHASE3_ECONOMY: Gold→Silver conversion overflow prevented (gold: {})", 
				goldAmount);
			return false;
		}
	}

	// NBT Serialization (Phase 0 - migration: repair negative values)
	public void writeNbt(NbtCompound nbt) {
		nbt.putLong("SilverCoins", this.silverCoins);
		nbt.putLong("GoldCoins", this.goldCoins);
		nbt.putLong("FocusXp", this.focusXp);
	}

	public void readNbt(NbtCompound nbt) {
		// Migration: read old int values if exist, else read long
		if (nbt.contains("SilverCoins", 3)) { // 3 = INT type
			long oldValue = nbt.getInt("SilverCoins");
			this.silverCoins = oldValue < 0 ? 0 : oldValue; // Repair negative overflow
		} else {
			this.silverCoins = nbt.getLong("SilverCoins");
			if (this.silverCoins < 0) this.silverCoins = 0; // Repair
		}
		
		if (nbt.contains("GoldCoins", 3)) {
			long oldValue = nbt.getInt("GoldCoins");
			this.goldCoins = oldValue < 0 ? 0 : oldValue;
		} else {
			this.goldCoins = nbt.getLong("GoldCoins");
			if (this.goldCoins < 0) this.goldCoins = 0;
		}
		
		if (nbt.contains("FocusXp", 3)) {
			long oldValue = nbt.getInt("FocusXp");
			this.focusXp = oldValue < 0 ? 0 : oldValue;
		} else {
			this.focusXp = nbt.getLong("FocusXp");
			if (this.focusXp < 0) this.focusXp = 0;
		}
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
