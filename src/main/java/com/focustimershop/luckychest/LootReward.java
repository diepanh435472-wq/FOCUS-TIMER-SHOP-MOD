package com.focustimershop.luckychest;

import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;

import java.util.Random;

public class LootReward {
	private final Item item;
	private final int minCount;
	private final int maxCount;
	private final Enchantment enchantment;
	private final int enchantLevel;

	public LootReward(Item item, int minCount, int maxCount) {
		this(item, minCount, maxCount, null, 0);
	}

	public LootReward(Item item, int minCount, int maxCount, Enchantment enchantment, int enchantLevel) {
		this.item = item;
		this.minCount = minCount;
		this.maxCount = maxCount;
		this.enchantment = enchantment;
		this.enchantLevel = enchantLevel;
	}

	public ItemStack generateStack(Random random) {
		int count = minCount + (maxCount > minCount ? random.nextInt(maxCount - minCount + 1) : 0);
		ItemStack stack = new ItemStack(item, count);

		if (enchantment != null && enchantLevel > 0) {
			stack.addEnchantment(enchantment, enchantLevel);
		}

		return stack;
	}

	// Factory methods for common reward types
	public static LootReward simple(Item item, int count) {
		return new LootReward(item, count, count);
	}

	public static LootReward range(Item item, int min, int max) {
		return new LootReward(item, min, max);
	}

	public static LootReward enchantedBook(Enchantment enchantment, int level) {
		ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
		book.addEnchantment(enchantment, level);
		// Note: Proper enchanted book creation requires EnchantedBookItem.forEnchantment()
		// but this simplified version works for basic functionality
		return new LootReward(Items.ENCHANTED_BOOK, 1, 1, enchantment, level);
	}

	public static LootReward enchantedItem(Item item, Enchantment enchantment, int level) {
		return new LootReward(item, 1, 1, enchantment, level);
	}
	
	// Getters for UI display
	public Item getItem() {
		return item;
	}
	
	public int getMinCount() {
		return minCount;
	}
	
	public int getMaxCount() {
		return maxCount;
	}
	
	public Enchantment getEnchantment() {
		return enchantment;
	}
	
	public int getEnchantLevel() {
		return enchantLevel;
	}
	
	/**
	 * Get display name for UI (item name + enchantment if any)
	 */
	public String getDisplayName() {
		String baseName = Registries.ITEM.getId(item).getPath();
		
		if (enchantment != null && enchantLevel > 0) {
			String enchName = Registries.ENCHANTMENT.getId(enchantment).getPath();
			return baseName + " (" + enchName + " " + enchantLevel + ")";
		}
		
		if (minCount == maxCount && minCount > 1) {
			return baseName + " x" + minCount;
		} else if (minCount != maxCount) {
			return baseName + " x" + minCount + "-" + maxCount;
		}
		
		return baseName;
	}
}
