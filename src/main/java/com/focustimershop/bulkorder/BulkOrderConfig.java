package com.focustimershop.bulkorder;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Bulk Order configuration - discount table and items per chest
 * v1.0.6-beta - Simplified chest-based bulk purchasing
 */
public class BulkOrderConfig {
	
	private String version = "1.0.6";
	
	// Items per chest (full stack × 27 slots)
	// Default: 64 items/slot × 27 slots = 1728 items per chest
	private int itemsPerChest = 1728;
	
	// Discount table: chest count ranges → discount percentage
	// Format: "min-max" → discount (e.g., "10-24" → 0.01 means 1% off)
	// Special: "100+" means 100 and above
	private Map<String, Double> discountTable = new LinkedHashMap<>();
	
	public BulkOrderConfig() {
		// Initialize default discount table
		discountTable.put("1-9", 0.00);      // 0% discount for 1-9 chests
		discountTable.put("10-24", 0.01);    // 1% discount for 10-24 chests
		discountTable.put("25-49", 0.02);    // 2% discount for 25-49 chests
		discountTable.put("50-99", 0.03);    // 3% discount for 50-99 chests
		discountTable.put("100+", 0.05);     // 5% discount for 100+ chests
	}
	
	public static BulkOrderConfig createDefault() {
		return new BulkOrderConfig();
	}
	
	// Getters
	public String getVersion() { 
		return version; 
	}
	
	public int getItemsPerChest() { 
		return itemsPerChest; 
	}
	
	public Map<String, Double> getDiscountTable() { 
		return discountTable; 
	}
	
	// Setters
	public void setVersion(String version) { 
		this.version = version; 
	}
	
	public void setItemsPerChest(int itemsPerChest) { 
		this.itemsPerChest = itemsPerChest; 
	}
	
	public void setDiscountTable(Map<String, Double> discountTable) { 
		this.discountTable = discountTable; 
	}
	
	/**
	 * Get discount multiplier for a given chest count
	 * Returns 0.0-1.0 (e.g., 0.05 = 5% discount)
	 * 
	 * @param chestCount number of chests being purchased
	 * @return discount as decimal (0.05 = 5% off)
	 */
	public double getDiscountForChestCount(int chestCount) {
		if (chestCount < 1) return 0.0;
		
		// Check each range in order
		for (Map.Entry<String, Double> entry : discountTable.entrySet()) {
			String range = entry.getKey();
			double discount = entry.getValue();
			
			if (range.endsWith("+")) {
				// Handle "100+" format
				int minCount = Integer.parseInt(range.replace("+", ""));
				if (chestCount >= minCount) {
					return discount;
				}
			} else if (range.contains("-")) {
				// Handle "10-24" format
				String[] parts = range.split("-");
				int min = Integer.parseInt(parts[0]);
				int max = Integer.parseInt(parts[1]);
				if (chestCount >= min && chestCount <= max) {
					return discount;
				}
			}
		}
		
		// Default: no discount
		return 0.0;
	}
	
	/**
	 * Calculate total price for bulk order with discount applied
	 * Formula: normalUnitPrice × itemsPerChest × chestCount × (1 - discount)
	 * 
	 * @param normalUnitPrice base price for single item (silver)
	 * @param chestCount number of chests
	 * @return total price in silver (rounded up)
	 */
	public long calculateTotalPrice(long normalUnitPrice, int chestCount) {
		if (chestCount < 1 || normalUnitPrice < 0) return 0;
		
		double discount = getDiscountForChestCount(chestCount);
		long basePrice = normalUnitPrice * itemsPerChest * chestCount;
		long discountedPrice = (long) Math.ceil(basePrice * (1.0 - discount));
		
		return discountedPrice;
	}
	
	/**
	 * Get total item count for chest count
	 * @param chestCount number of chests
	 * @return total items (chestCount × itemsPerChest)
	 */
	public long getTotalItemCount(int chestCount) {
		return (long) chestCount * itemsPerChest;
	}
}
