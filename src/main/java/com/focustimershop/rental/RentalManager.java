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
 * 
 * SECURITY: All enchant levels validated server-side (VULN-003 fix)
 */
public class RentalManager {
	
	// SECURITY: Rental validation constants (VULN-003 fix)
	public static final int MAX_ENCHANT_LEVEL = 10; // Reasonable max for custom rentals
	public static final int MIN_DURATION_MINUTES = 1;
	public static final int MAX_DURATION_MINUTES = 1440; // 24 hours
	public static final int MAX_TOOL_TYPES = 3; // PICKAXE, AXE, SHOVEL
	
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
	 * Server tick - check for expired rentals and cleanup
	 */
	public static void tick() {
		long now = System.currentTimeMillis();
		
		activeRentals.entrySet().removeIf(entry -> {
			RentalData rental = entry.getValue();
			// Cleanup expired rentals
			rental.cleanupExpired();
			
			if (!rental.hasActiveRental()) {
				// No active rentals left - save and remove from memory
				saveRentalData(entry.getKey(), rental);
				return true;
			}
			return false;
		});
	}
	
	/**
	 * Tick down rental timers for all online players
	 * Only decrements when player is online and game is NOT frozen (timer not running)
	 * Called every second (20 ticks) from server
	 * 
	 * PHASE 2 FIX: Use isPlayerFrozen() instead of hasActiveTimer()
	 * - hasActiveTimer() returns true even when PAUSED (player can move)
	 * - isPlayerFrozen() only returns true when RUNNING (actually frozen)
	 */
	public static void tickRentalTimers(net.minecraft.server.MinecraftServer server) {
		for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			// PHASE 2 FIX: Check if player's game is actually frozen
			// Only freeze when timer is RUNNING, not when PAUSED
			boolean isFrozen = com.focustimershop.timer.TimerManager.isPlayerFrozen(player.getUuid());
			
			// Only tick down rental timers when game is NOT frozen
			if (!isFrozen) {
				tickPlayerRentalTools(player);
				tickPlayerRentalData(player);
			}
		}
	}
	
	/**
	 * Tick down rental time on player's tools (NBT)
	 */
	private static void tickPlayerRentalTools(net.minecraft.server.network.ServerPlayerEntity player) {
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			
			if (stack.isEmpty() || !stack.hasNbt()) {
				continue;
			}
			
			NbtCompound nbt = stack.getNbt();
			if (!nbt.contains("RentalType") || !nbt.contains("RemainingSeconds")) {
				continue;
			}
			
			int remainingSeconds = nbt.getInt("RemainingSeconds");
			if (remainingSeconds > 0) {
				remainingSeconds--;
				nbt.putInt("RemainingSeconds", remainingSeconds);
				
				// Update lore to reflect new time
				updateToolLore(stack, nbt, remainingSeconds);
			}
		}
	}
	
	/**
	 * Update tool lore with current remaining time
	 */
	private static void updateToolLore(ItemStack tool, NbtCompound nbt, int remainingSeconds) {
		// Get tool config
		boolean useFortuneMode = nbt.getBoolean("UseFortuneMode");
		int fortuneLevel = nbt.getInt("CustomFortuneLevel");
		int efficiencyLevel = nbt.getInt("CustomEfficiencyLevel");
		int unbreakingLevel = nbt.getInt("CustomUnbreakingLevel");
		int mendingLevel = nbt.getInt("CustomMendingLevel");
		String rentalType = nbt.getString("RentalType");
		
		// Calculate time display
		int hours = remainingSeconds / 3600;
		int minutes = (remainingSeconds % 3600) / 60;
		int secs = remainingSeconds % 60;
		String timeDisplay = String.format("%dh %dm %ds", hours, minutes, secs);
		
		// Rebuild lore
		NbtCompound display = nbt.getCompound("display");
		if (!nbt.contains("display")) {
			display = new NbtCompound();
			nbt.put("display", display);
		}
		
		net.minecraft.nbt.NbtList lore = new net.minecraft.nbt.NbtList();
		
		// Time
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
			Text.literal("§7⏱ Còn lại: §e" + timeDisplay)
		)));
		
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal("§7-------------------"))));
		
		// Stats
		if (useFortuneMode && fortuneLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Gia Tài " + fortuneLevel)
			)));
		} else if (!useFortuneMode) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Độ Mềm Mại")
			)));
		}
		
		if (efficiencyLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Hiệu Suất " + efficiencyLevel)
			)));
		}
		
		if (unbreakingLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Chậm Hỏng " + unbreakingLevel)
			)));
		}
		
		if (mendingLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Sửa Chữa " + mendingLevel)
			)));
		}
		
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal("§7-------------------"))));
		
		// Feature based on tool type
		if (rentalType.equals("PICKAXE") || rentalType.equals("SHOVEL")) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§7Đào 3x3: §aĐè Shift")
			)));
		} else if (rentalType.equals("AXE")) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§7Chặt cây nhanh: §aĐè Shift")
			)));
		}
		
		display.put("Lore", lore);
	}
	
	/**
	 * Tick down rental time in player's RentalData
	 */
	private static void tickPlayerRentalData(net.minecraft.server.network.ServerPlayerEntity player) {
		java.util.UUID playerId = player.getUuid();
		RentalData rental = activeRentals.get(playerId);
		
		if (rental == null) {
			rental = loadRentalData(playerId);
			if (rental == null) {
				return;
			}
		}
		
		// Tick down all active rentals
		boolean changed = false;
		for (RentalData.SingleRental singleRental : rental.getActiveRentals()) {
			singleRental.tickDown();
			changed = true;
		}
		
		if (changed) {
			activeRentals.put(playerId, rental);
			saveRentalData(playerId, rental);
		}
	}
	
	/**
	 * Check all online players for expired rental tools and remove them
	 * Called every tick from server
	 */
	public static void checkAndRemoveExpiredTools(net.minecraft.server.MinecraftServer server) {
		for (net.minecraft.server.network.ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
			checkPlayerInventoryForExpiredTools(player);
		}
	}
	
	/**
	 * Check player's inventory for expired rental tools and remove them
	 * PHASE 4: Made public for logout cleanup (BUG #19 fix)
	 */
	public static void checkPlayerInventoryForExpiredTools(net.minecraft.server.network.ServerPlayerEntity player) {
		boolean foundExpired = false;
		String expiredRentalType = null; // PHASE 4: Track for client notification (BUG #21 fix)
		
		// Check main inventory
		for (int i = 0; i < player.getInventory().size(); i++) {
			ItemStack stack = player.getInventory().getStack(i);
			
			if (stack.isEmpty() || !stack.hasNbt()) {
				continue;
			}
			
			NbtCompound nbt = stack.getNbt();
			if (!nbt.contains("RentalType") || !nbt.contains("RemainingSeconds")) {
				continue;
			}
			
			int remainingSeconds = nbt.getInt("RemainingSeconds");
			if (remainingSeconds <= 0) {
				// PHASE 4: BUG #21 FIX - Store rental type before removal
				if (!foundExpired) {
					expiredRentalType = nbt.getString("RentalType");
				}
				
				// Tool has expired - remove it
				player.getInventory().removeStack(i);
				foundExpired = true;
			}
		}
		
		if (foundExpired) {
			player.sendMessage(Text.literal("§c⚠ Công cụ thuê của bạn đã hết hạn!"), false);
			
			// PHASE 4: BUG #21 FIX - Notify client to clear cache
			if (expiredRentalType != null) {
				com.focustimershop.network.ModNetworking.sendRentalExpired(player, expiredRentalType);
			}
		}
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
	
	/**
	 * Handle rental request from client (Phase 3)
	 * SECURITY: VULN-003 fix - comprehensive validation and safe arithmetic
	 */
	public static void handleRentalRequest(net.minecraft.server.network.ServerPlayerEntity player,
	                                       int toolIndex, boolean useFortuneMode, int fortuneLevel,
	                                       int efficiencyLevel, int unbreakingLevel, int mendingLevel,
	                                       int durationMinutes, boolean useSilverPayment) {
		UUID playerId = player.getUuid();
		
		// SECURITY VULN-003: Validate enchant levels
		if (fortuneLevel < 0 || fortuneLevel > MAX_ENCHANT_LEVEL) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid fortune level: {}", 
				player.getName().getString(), fortuneLevel);
			player.sendMessage(Text.literal("§cInvalid enchant configuration!"), false);
			return;
		}
		if (efficiencyLevel < 0 || efficiencyLevel > MAX_ENCHANT_LEVEL) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid efficiency level: {}", 
				player.getName().getString(), efficiencyLevel);
			player.sendMessage(Text.literal("§cInvalid enchant configuration!"), false);
			return;
		}
		if (unbreakingLevel < 0 || unbreakingLevel > MAX_ENCHANT_LEVEL) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid unbreaking level: {}", 
				player.getName().getString(), unbreakingLevel);
			player.sendMessage(Text.literal("§cInvalid enchant configuration!"), false);
			return;
		}
		if (mendingLevel < 0 || mendingLevel > MAX_ENCHANT_LEVEL) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid mending level: {}", 
				player.getName().getString(), mendingLevel);
			player.sendMessage(Text.literal("§cInvalid enchant configuration!"), false);
			return;
		}
		
		// SECURITY VULN-003: Validate duration
		if (durationMinutes < MIN_DURATION_MINUTES || durationMinutes > MAX_DURATION_MINUTES) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid duration: {} (min: {}, max: {})", 
				player.getName().getString(), durationMinutes, MIN_DURATION_MINUTES, MAX_DURATION_MINUTES);
			player.sendMessage(Text.literal("§cInvalid rental duration!"), false);
			return;
		}
		
		// FIX: Allow multiple rentals - only check for same tool type
		String[] toolTypes = {"PICKAXE", "AXE", "SHOVEL"};
		
		// SECURITY VULN-004 (also fixed here): Validate tool index
		if (toolIndex < 0 || toolIndex >= toolTypes.length) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid tool index: {} (max: {})", 
				player.getName().getString(), toolIndex, toolTypes.length - 1);
			player.sendMessage(Text.literal("§cInvalid tool type!"), false);
			return;
		}
		
		String requestedToolType = toolTypes[toolIndex];
		
		RentalData rental = activeRentals.get(playerId);
		if (rental == null) {
			rental = loadRentalData(playerId);
			if (rental == null) {
				rental = new RentalData(playerId);
			}
		}
		
		// Check if already renting THIS specific tool
		RentalData.SingleRental existingRental = rental.getRentalByType(requestedToolType);
		if (existingRental != null && existingRental.isActive()) {
			player.sendMessage(Text.literal("§cBạn đã đang thuê " + requestedToolType + " rồi!"), false);
			return;
		}
		
		// SECURITY VULN-003: Calculate cost using SAFE ARITHMETIC (long with overflow detection)
		try {
			long baseCost = 30;
			
			// Calculate contributions using long to prevent overflow
			long fortuneContribution = useFortuneMode ? 
				Math.multiplyExact((long)fortuneLevel, (long)fortuneLevel) : 100;
			long efficiencyContribution = Math.multiplyExact((long)efficiencyLevel, (long)efficiencyLevel);
			long unbreakingContribution = Math.multiplyExact((long)unbreakingLevel, (long)unbreakingLevel);
			long mendingContribution = Math.multiplyExact((long)mendingLevel, (long)mendingLevel);
			
			// Sum with overflow detection
			long statSum = Math.addExact(fortuneContribution, efficiencyContribution);
			statSum = Math.addExact(statSum, unbreakingContribution);
			statSum = Math.addExact(statSum, mendingContribution);
			
			long perBlockCost = Math.addExact(baseCost, statSum);
			long blocks = (long) Math.ceil(durationMinutes / 30.0);
			long totalSilverLong = Math.multiplyExact(perBlockCost, blocks);
			
			// Validate result fits in int (for compatibility with economy system)
			if (totalSilverLong > Integer.MAX_VALUE || totalSilverLong < 0) {
				FocusTimerShop.LOGGER.warn("SECURITY: Player {} rental cost overflow: {} (fortune:{}, eff:{}, unb:{}, mend:{})", 
					player.getName().getString(), totalSilverLong, fortuneLevel, efficiencyLevel, 
					unbreakingLevel, mendingLevel);
				player.sendMessage(Text.literal("§cRental configuration too expensive!"), false);
				return;
			}
			
			int totalSilver = (int) totalSilverLong;
			int totalGold = (int) Math.ceil(totalSilver / 100.0);
			
				// Check if player can afford
			com.focustimershop.economy.PlayerEconomyData economy = 
				com.focustimershop.economy.EconomyManager.getPlayerData(player);
			
			boolean canAfford = useSilverPayment ? 
				(economy.getSilverCoins() >= totalSilver) :
				(economy.getGoldCoins() >= totalGold);
			
			if (!canAfford) {
				player.sendMessage(Text.literal("§cKhông đủ tiền để thuê!"), false);
				return;
			}
			
			// Deduct currency
			if (useSilverPayment) {
				economy.removeSilverCoins(totalSilver);
			} else {
				economy.removeGoldCoins(totalGold);
			}
			
			com.focustimershop.economy.EconomyManager.savePlayerData(player);
			com.focustimershop.economy.EconomyManager.syncToClient(player);
			
			// Add new rental (v1.0.6+ with remainingSeconds instead of endTime)
			long now = System.currentTimeMillis();
			rental.addRental(requestedToolType, now, durationMinutes * 60,
			                 useFortuneMode ? fortuneLevel : 0, efficiencyLevel, 
			                 unbreakingLevel, mendingLevel, !useFortuneMode);
			
			// Save to memory and disk
			activeRentals.put(playerId, rental);
			saveRentalData(playerId, rental);
			
			// Give tool to player (pass duration instead of endTime)
			giveCustomRentalTool(player, rental, toolIndex, useFortuneMode, fortuneLevel, 
			                     efficiencyLevel, unbreakingLevel, mendingLevel, durationMinutes * 60);
			
			String costMsg = useSilverPayment ? 
				(totalSilver + " Silver") :
				(totalGold + " Gold");
			player.sendMessage(Text.literal("§aThuê thành công! Chi phí: " + costMsg), false);
			
			FocusTimerShop.LOGGER.info("Player {} rented {} for {}min ({})", 
				player.getName().getString(), requestedToolType, durationMinutes, costMsg);
				
		} catch (ArithmeticException e) {
			FocusTimerShop.LOGGER.warn("SECURITY: Player {} caused arithmetic overflow in rental calculation", 
				player.getName().getString());
			player.sendMessage(Text.literal("§cRental calculation error! Configuration too extreme."), false);
		}
	}
	
	/**
	 * Give custom rental tool with NBT (Phase 3)
	 */
	private static void giveCustomRentalTool(net.minecraft.server.network.ServerPlayerEntity player,
	                                          RentalData rental, int toolIndex, boolean useFortuneMode,
	                                          int fortuneLevel, int efficiencyLevel, 
	                                          int unbreakingLevel, int mendingLevel, int durationSeconds) {
		// Tool items
		net.minecraft.item.Item[] tools = {
			Items.NETHERITE_PICKAXE,
			Items.NETHERITE_AXE,
			Items.NETHERITE_SHOVEL
		};
		String[] toolNames = {"Cuốc Amethyst", "Rìu Amethyst", "Xẻng Amethyst"};
		
		ItemStack tool = new ItemStack(tools[toolIndex]);
		
		// Set custom name
		tool.setCustomName(Text.literal("§d§l[NETHERITE - ĐÃ THUÊ] §6" + toolNames[toolIndex]));
		
		// Add NBT data (v1.0.6+ with remainingSeconds instead of expiry timestamp)
		NbtCompound nbt = tool.getOrCreateNbt();
		String[] toolTypes = {"PICKAXE", "AXE", "SHOVEL"};
		nbt.putString("RentalType", toolTypes[toolIndex]); // Use correct tool type
		nbt.putString("RentalOwner", player.getUuidAsString());
		nbt.putInt("RemainingSeconds", durationSeconds); // Store remaining time, not absolute timestamp
		
		// Store custom stats
		nbt.putBoolean("UseFortuneMode", useFortuneMode);
		nbt.putInt("CustomFortuneLevel", fortuneLevel);
		nbt.putInt("CustomEfficiencyLevel", efficiencyLevel);
		nbt.putInt("CustomUnbreakingLevel", unbreakingLevel);
		nbt.putInt("CustomMendingLevel", mendingLevel);
		
		// Default config (Phase 4 will use these)
		nbt.putInt("AreaSize", 1); // 1x1 for Pickaxe/Shovel
		nbt.putBoolean("TreeChopping", false); // OFF for Axe
		
		// Add lore
		NbtCompound display = nbt.getCompound("display");
		if (!nbt.contains("display")) {
			nbt.put("display", display);
		}
		
		// FIX: Add REAL enchantments to the tool
		if (useFortuneMode && fortuneLevel > 0) {
			tool.addEnchantment(net.minecraft.enchantment.Enchantments.FORTUNE, fortuneLevel);
		} else if (!useFortuneMode) {
			tool.addEnchantment(net.minecraft.enchantment.Enchantments.SILK_TOUCH, 1);
		}
		
		if (efficiencyLevel > 0) {
			tool.addEnchantment(net.minecraft.enchantment.Enchantments.EFFICIENCY, efficiencyLevel);
		}
		
		if (unbreakingLevel > 0) {
			tool.addEnchantment(net.minecraft.enchantment.Enchantments.UNBREAKING, unbreakingLevel);
		}
		
		if (mendingLevel > 0) {
			tool.addEnchantment(net.minecraft.enchantment.Enchantments.MENDING, 1);
		}
		
		net.minecraft.nbt.NbtList lore = new net.minecraft.nbt.NbtList();
		
		// Remaining time (detailed: hours:minutes:seconds)
		int hours = durationSeconds / 3600;
		int minutes = (durationSeconds % 3600) / 60;
		int secs = durationSeconds % 60;
		
		String timeDisplay = String.format("%dh %dm %ds", hours, minutes, secs);
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
			Text.literal("§7⏱ Còn lại: §e" + timeDisplay)
		)));
		
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal("§7-------------------"))));
		
		// Stats
		if (useFortuneMode && fortuneLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Gia Tài " + fortuneLevel)
			)));
		} else if (!useFortuneMode) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Độ Mềm Mại")
			)));
		}
		
		if (efficiencyLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Hiệu Suất " + efficiencyLevel)
			)));
		}
		
		if (unbreakingLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Chậm Hỏng " + unbreakingLevel)
			)));
		}
		
		if (mendingLevel > 0) {
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§b✦ Sửa Chữa " + mendingLevel)
			)));
		}
		
		lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(Text.literal("§7-------------------"))));
		
		// Feature info based on tool type
		if (toolIndex == 0 || toolIndex == 2) { // Pickaxe or Shovel
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§7Đào 3x3: §aĐè Shift")
			)));
		} else if (toolIndex == 1) { // Axe
			lore.add(net.minecraft.nbt.NbtString.of(Text.Serializer.toJson(
				Text.literal("§7Chặt cây nhanh: §aĐè Shift")
			)));
		}
		
		display.put("Lore", lore);
		
		// Give to player
		player.giveItemStack(tool);
	}
}
