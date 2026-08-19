package com.focustimershop.util;

import com.focustimershop.FocusTimerShop;

/**
 * Centralized validation utilities for security hardening
 * Phase 1 - Provides reusable validation methods with consistent logging
 */
public class ValidationUtils {
	
	// String validation constants
	public static final int MAX_ITEM_ID_LENGTH = 128;
	public static final int MAX_CHEST_TYPE_LENGTH = 32;
	public static final int MAX_CUSTOM_NAME_LENGTH = 32;
	public static final int MAX_TITLE_ID_LENGTH = 64;
	
	// Numeric validation constants
	public static final int MAX_TIMER_SECONDS = 7200; // 2 hours
	public static final int MAX_CONVERSION_AMOUNT = 1_000_000_000; // 1 billion
	
	// Shop validation constants (referenced from ShopManager)
	public static final int MAX_PURCHASE_QUANTITY = 6400; // 100 stacks of 64
	public static final int MAX_CART_ITEMS = 100;
	public static final int MAX_TOTAL_ITEMS_PER_TRANSACTION = 10000;
	
	// Rental validation constants (referenced from RentalManager)
	public static final int MAX_ENCHANT_LEVEL = 10;
	public static final int MIN_DURATION_MINUTES = 1;
	public static final int MAX_DURATION_MINUTES = 1440; // 24 hours
	public static final int MAX_TOOL_TYPES = 3;
	
	/**
	 * Validate string is not null, not empty after trim, and within length limit
	 * @param value String to validate
	 * @param maxLength Maximum allowed length
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if valid, false otherwise
	 */
	public static boolean validateString(String value, int maxLength, String fieldName, String playerName) {
		if (value == null) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent null {}", playerName, fieldName);
			return false;
		}
		
		if (value.trim().isEmpty()) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent empty {}", playerName, fieldName);
			return false;
		}
		
		if (value.length() > maxLength) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent {} exceeding max length: {} (max: {})", 
				playerName, fieldName, value.length(), maxLength);
			return false;
		}
		
		return true;
	}
	
	/**
	 * Validate alphanumeric string (letters, numbers, underscore, dash only)
	 * @param value String to validate
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if valid format, false otherwise
	 */
	public static boolean validateAlphanumeric(String value, String fieldName, String playerName) {
		if (!value.matches("^[a-zA-Z0-9_\\-]+$")) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid {} format: {}", 
				playerName, fieldName, value);
			return false;
		}
		return true;
	}
	
	/**
	 * Validate integer is within range (inclusive)
	 * @param value Value to validate
	 * @param min Minimum allowed value (inclusive)
	 * @param max Maximum allowed value (inclusive)
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if within range, false otherwise
	 */
	public static boolean validateIntRange(int value, int min, int max, String fieldName, String playerName) {
		if (value < min || value > max) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent {} out of range: {} (min: {}, max: {})", 
				playerName, fieldName, value, min, max);
			return false;
		}
		return true;
	}
	
	/**
	 * Validate long is within range (inclusive)
	 * @param value Value to validate
	 * @param min Minimum allowed value (inclusive)
	 * @param max Maximum allowed value (inclusive)
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if within range, false otherwise
	 */
	public static boolean validateLongRange(long value, long min, long max, String fieldName, String playerName) {
		if (value < min || value > max) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent {} out of range: {} (min: {}, max: {})", 
				playerName, fieldName, value, min, max);
			return false;
		}
		return true;
	}
	
	/**
	 * Validate positive integer (> 0)
	 * @param value Value to validate
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if positive, false otherwise
	 */
	public static boolean validatePositive(int value, String fieldName, String playerName) {
		if (value <= 0) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent non-positive {}: {}", 
				playerName, fieldName, value);
			return false;
		}
		return true;
	}
	
	/**
	 * Validate positive long (> 0)
	 * @param value Value to validate
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if positive, false otherwise
	 */
	public static boolean validatePositiveLong(long value, String fieldName, String playerName) {
		if (value <= 0) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent non-positive {}: {}", 
				playerName, fieldName, value);
			return false;
		}
		return true;
	}
	
	/**
	 * Validate array index is within bounds
	 * @param index Index to validate
	 * @param arrayLength Length of array
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if valid index, false otherwise
	 */
	public static boolean validateArrayIndex(int index, int arrayLength, String fieldName, String playerName) {
		if (index < 0 || index >= arrayLength) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid {} index: {} (max: {})", 
				playerName, fieldName, index, arrayLength - 1);
			return false;
		}
		return true;
	}
	
	/**
	 * Safe multiplication with overflow detection
	 * @param a First operand
	 * @param b Second operand
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return Result as long, or null if overflow
	 */
	public static Long safeMultiply(long a, long b, String fieldName, String playerName) {
		try {
			return Math.multiplyExact(a, b);
		} catch (ArithmeticException e) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} caused multiplication overflow in {}: {} * {}", 
				playerName, fieldName, a, b);
			return null;
		}
	}
	
	/**
	 * Safe addition with overflow detection
	 * @param a First operand
	 * @param b Second operand
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return Result as long, or null if overflow
	 */
	public static Long safeAdd(long a, long b, String fieldName, String playerName) {
		try {
			return Math.addExact(a, b);
		} catch (ArithmeticException e) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} caused addition overflow in {}: {} + {}", 
				playerName, fieldName, a, b);
			return null;
		}
	}
	
	/**
	 * Validate long fits in int range
	 * @param value Long value to check
	 * @param fieldName Field name for logging
	 * @param playerName Player name for logging
	 * @return true if fits in int, false otherwise
	 */
	public static boolean validateFitsInInt(long value, String fieldName, String playerName) {
		if (value > Integer.MAX_VALUE || value < Integer.MIN_VALUE) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} caused int overflow in {}: {}", 
				playerName, fieldName, value);
			return false;
		}
		return true;
	}
	
	/**
	 * Validate enchant level for rentals
	 * @param level Enchant level
	 * @param enchantType Enchant type name
	 * @param playerName Player name for logging
	 * @return true if valid, false otherwise
	 */
	public static boolean validateEnchantLevel(int level, String enchantType, String playerName) {
		return validateIntRange(level, 0, MAX_ENCHANT_LEVEL, enchantType + " level", playerName);
	}
	
	/**
	 * Validate rental duration
	 * @param minutes Duration in minutes
	 * @param playerName Player name for logging
	 * @return true if valid, false otherwise
	 */
	public static boolean validateRentalDuration(int minutes, String playerName) {
		return validateIntRange(minutes, MIN_DURATION_MINUTES, MAX_DURATION_MINUTES, 
			"rental duration", playerName);
	}
	
	/**
	 * Validate timer duration
	 * @param seconds Duration in seconds
	 * @param playerName Player name for logging
	 * @return true if valid, false otherwise
	 */
	public static boolean validateTimerDuration(int seconds, String playerName) {
		return validateIntRange(seconds, 1, MAX_TIMER_SECONDS, "timer duration", playerName);
	}
	
	/**
	 * Validate shop quantity
	 * @param quantity Quantity to purchase
	 * @param playerName Player name for logging
	 * @return true if valid, false otherwise
	 */
	public static boolean validateShopQuantity(int quantity, String playerName) {
		return validateIntRange(quantity, 1, MAX_PURCHASE_QUANTITY, "purchase quantity", playerName);
	}
	
	/**
	 * Validate currency conversion amount
	 * @param amount Amount to convert
	 * @param playerName Player name for logging
	 * @return true if valid, false otherwise
	 */
	public static boolean validateConversionAmount(int amount, String playerName) {
		return validateIntRange(amount, 1, MAX_CONVERSION_AMOUNT, "conversion amount", playerName);
	}
	
	/**
	 * Log packet validation failure
	 * @param packetType Type of packet
	 * @param playerName Player name
	 * @param reason Reason for failure
	 */
	public static void logPacketFailure(String packetType, String playerName, String reason) {
		FocusTimerShop.LOGGER.warn("Invalid {} packet from {}: {}", packetType, playerName, reason);
	}
}
