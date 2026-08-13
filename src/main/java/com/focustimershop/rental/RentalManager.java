package com.focustimershop.rental;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.DatabaseManager;
import com.focustimershop.database.RentalData;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-side rental management
 * Handles tool rentals with real-time expiration
 */
public class RentalManager {
	
	private static final Map<UUID, RentalData> activeRentals = new HashMap<>();
	
	// Rental tiers (will be loaded from config later)
	public static final int BASIC_FORTUNE = 2;
	public static final int BASIC_EFFICIENCY = 3;
	public static final int BASIC_UNBREAKING = 2;
	
	/**
	 * Initialize - load all active rentals from disk
	 */
	public static void initialize() {
		// Will load from database in future
		FocusTimerShop.LOGGER.info("RentalManager initialized");
	}
	
	/**
	 * Start a rental for player
	 */
	public static boolean startRental(ServerPlayerEntity player, RentalType type, int durationSeconds) {
		UUID playerId = player.getUuid();
		
		// Check if already has active rental
		if (hasActiveRental(playerId)) {
			player.sendMessage(Text.literal("§cYou already have an active rental!"), false);
			return false;
		}
		
		// Create rental data
		RentalData rental = new RentalData(playerId);
		rental.setRentalType(type.name());
		rental.setRentalStartTime(System.currentTimeMillis());
		rental.setRentalEndTime(System.currentTimeMillis() + (durationSeconds * 1000L));
		rental.setRentalDurationSeconds(durationSeconds);
		
		// Set tool stats based on type
		rental.setFortuneLevel(BASIC_FORTUNE);
		rental.setEfficiencyLevel(BASIC_EFFICIENCY);
		rental.setUnbreakingLevel(BASIC_UNBREAKING);
		rental.setMending(true);
		
		// Save to memory and disk
		activeRentals.put(playerId, rental);
		saveRentalData(playerId, rental);
		
		// Give tool to player
		giveRentalTool(player, rental);
		
		player.sendMessage(Text.literal("§aRented " + type.getDisplayName() + " for " + formatDuration(durationSeconds) + "!"), false);
		FocusTimerShop.LOGGER.info("Player {} started rental: {} for {}s", 
			player.getName().getString(), type, durationSeconds);
		
		return true;
	}
	
	/**
	 * Give rental tool to player
	 */
	private static void giveRentalTool(ServerPlayerEntity player, RentalData rental) {
		RentalType type = RentalType.valueOf(rental.getRentalType());
		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		
		// Set custom name
		tool.setCustomName(Text.literal("§d§lCúp Vạn Năng §7(Rental)"));
		
		// Add NBT tag to identify as rental tool
		NbtCompound nbt = tool.getOrCreateNbt();
		nbt.putString("RentalType", type.name());
		nbt.putString("RentalOwner", player.getUuidAsString());
		nbt.putLong("RentalExpiry", rental.getRentalEndTime());
		
		// Add lore
		NbtCompound display = nbt.getCompound("display");
		if (!nbt.contains("display")) {
			nbt.put("display", display);
		}
		
		// Note: Enchantments will be handled server-side during mining
		// We don't add actual enchantments to avoid conflicts
		
		// Give to player
		player.giveItemStack(tool);
	}
	
	/**
	 * Check if player has active rental
	 */
	public static boolean hasActiveRental(UUID playerId) {
		RentalData rental = activeRentals.get(playerId);
		if (rental == null) {
			// Try load from disk
			rental = loadRentalData(playerId);
			if (rental != null && rental.hasActiveRental()) {
				activeRentals.put(playerId, rental);
				return true;
			}
			return false;
		}
		return rental.hasActiveRental();
	}
	
	/**
	 * Get rental data for player
	 */
	public static RentalData getRental(UUID playerId) {
		RentalData rental = activeRentals.get(playerId);
		if (rental == null) {
			rental = loadRentalData(playerId);
			if (rental != null && rental.hasActiveRental()) {
				activeRentals.put(playerId, rental);
			}
		}
		return rental;
	}
	
	/**
	 * Check if item is a valid rental tool for this player
	 */
	public static boolean isValidRentalTool(ServerPlayerEntity player, ItemStack tool) {
		if (tool.isEmpty() || !tool.hasNbt()) {
			return false;
		}
		
		NbtCompound nbt = tool.getNbt();
		if (!nbt.contains("RentalType") || !nbt.contains("RentalOwner")) {
			return false;
		}
		
		String owner = nbt.getString("RentalOwner");
		if (!owner.equals(player.getUuidAsString())) {
			return false;
		}
		
		long expiry = nbt.getLong("RentalExpiry");
		return System.currentTimeMillis() < expiry;
	}
	
	/**
	 * Expire rental
	 */
	public static void expireRental(ServerPlayerEntity player) {
		UUID playerId = player.getUuid();
		RentalData rental = activeRentals.get(playerId);
		
		if (rental != null) {
			rental.clearRental();
			activeRentals.remove(playerId);
			saveRentalData(playerId, rental);
			
			// Remove rental tool from inventory
			removeRentalTool(player);
			
			player.sendMessage(Text.literal("§cYour rental has expired!"), false);
			FocusTimerShop.LOGGER.info("Player {} rental expired", player.getName().getString());
		}
	}
	
	/**
	 * Remove rental tool from player inventory
	 */
	private static void removeRentalTool(ServerPlayerEntity player) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			if (isRentalTool(stack)) {
				player.getInventory().removeStack(i);
			}
		}
	}
	
	/**
	 * Check if item is any rental tool (not player-specific)
	 */
	private static boolean isRentalTool(ItemStack stack) {
		if (stack.isEmpty() || !stack.hasNbt()) {
			return false;
		}
		return stack.getNbt().contains("RentalType");
	}
	
	/**
	 * Server tick - check for expired rentals
	 */
	public static void tick() {
		long now = System.currentTimeMillis();
		
		activeRentals.entrySet().removeIf(entry -> {
			RentalData rental = entry.getValue();
			if (!rental.hasActiveRental()) {
				// Expired - notify if player online
				// (Player notification handled in expireRental when they try to use)
				saveRentalData(entry.getKey(), rental);
				return true;
			}
			return false;
		});
	}
	
	/**
	 * Save rental data to disk
	 */
	private static void saveRentalData(UUID playerId, RentalData data) {
		File file = DatabaseManager.getRentalsDir().resolve(playerId.toString() + ".json").toFile();
		DatabaseManager.writeJson(file, data);
	}
	
	/**
	 * Load rental data from disk
	 */
	private static RentalData loadRentalData(UUID playerId) {
		File file = DatabaseManager.getRentalsDir().resolve(playerId.toString() + ".json").toFile();
		RentalData data = DatabaseManager.readJson(file, RentalData.class);
		
		if (data == null) {
			data = new RentalData(playerId);
		}
		
		return data;
	}
	
	/**
	 * Format duration for display
	 */
	private static String formatDuration(int seconds) {
		int hours = seconds / 3600;
		int minutes = (seconds % 3600) / 60;
		
		if (hours > 0) {
			return hours + "h " + minutes + "m";
		} else {
			return minutes + "m";
		}
	}
	
	/**
	 * Clear all rentals (server shutdown)
	 */
	public static void clearAll() {
		for (Map.Entry<UUID, RentalData> entry : activeRentals.entrySet()) {
			saveRentalData(entry.getKey(), entry.getValue());
		}
		activeRentals.clear();
	}
}
