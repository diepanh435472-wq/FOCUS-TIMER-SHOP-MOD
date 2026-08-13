package com.focustimershop.shop;

import com.focustimershop.client.ClientDataCache;

import java.util.HashMap;
import java.util.Map;

/**
 * Client-side shopping cart (not persisted)
 * Stores items before checkout
 */
public class ShoppingCart {
	
	// itemId -> quantity
	private Map<String, Integer> items = new HashMap<>();
	
	// Payment method: true = Silver, false = Gold
	private boolean useSilver = true;
	
	/**
	 * Add item to cart
	 */
	public void addItem(String itemId, int quantity) {
		items.put(itemId, items.getOrDefault(itemId, 0) + quantity);
	}
	
	/**
	 * Remove one unit of item
	 */
	public void decreaseItem(String itemId) {
		int current = items.getOrDefault(itemId, 0);
		if (current > 1) {
			items.put(itemId, current - 1);
		} else {
			items.remove(itemId);
		}
	}
	
	/**
	 * Remove all units of item
	 */
	public void removeItem(String itemId) {
		items.remove(itemId);
	}
	
	/**
	 * Clear entire cart
	 */
	public void clear() {
		items.clear();
	}
	
	/**
	 * Get quantity of specific item
	 */
	public int getQuantity(String itemId) {
		return items.getOrDefault(itemId, 0);
	}
	
	/**
	 * Get all items in cart
	 */
	public Map<String, Integer> getItems() {
		return new HashMap<>(items);
	}
	
	/**
	 * Check if cart is empty
	 */
	public boolean isEmpty() {
		return items.isEmpty();
	}
	
	/**
	 * Get total cost in the selected currency
	 */
	public int getTotalCost() {
		int total = 0;
		for (Map.Entry<String, Integer> entry : items.entrySet()) {
			String itemId = entry.getKey();
			int quantity = entry.getValue();
			
			// Use ClientDataCache instead of ShopManager (client-side!)
			ShopItem item = ClientDataCache.getShopItem(itemId);
			if (item != null) {
				if (useSilver) {
					total += item.getSilverPrice() * quantity;
				} else {
					total += item.getGoldCost() * quantity;
				}
			}
		}
		return total;
	}
	
	/**
	 * Get payment method
	 */
	public boolean isUsingSilver() {
		return useSilver;
	}
	
	/**
	 * Set payment method
	 */
	public void setPaymentMethod(boolean useSilver) {
		this.useSilver = useSilver;
	}
}
