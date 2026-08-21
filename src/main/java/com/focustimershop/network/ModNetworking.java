package com.focustimershop.network;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.client.ClientDataCache;
import com.focustimershop.economy.EconomyManager;
import com.focustimershop.economy.PlayerEconomyData;
import com.focustimershop.shop.ShopManager;
import com.focustimershop.timer.TimerManager;
import com.focustimershop.timer.TimerSession;
import com.focustimershop.timer.TimerState;
import com.focustimershop.timer.TimerType;
import com.focustimershop.luckychest.LuckyChestManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;

public class ModNetworking {
	
	// Packet identifiers
	public static final Identifier ECONOMY_SYNC = new Identifier(FocusTimerShop.MOD_ID, "economy_sync");
	public static final Identifier TIMER_START = new Identifier(FocusTimerShop.MOD_ID, "timer_start");
	public static final Identifier TIMER_PAUSE = new Identifier(FocusTimerShop.MOD_ID, "timer_pause");
	public static final Identifier TIMER_RESUME = new Identifier(FocusTimerShop.MOD_ID, "timer_resume");
	public static final Identifier TIMER_STOP = new Identifier(FocusTimerShop.MOD_ID, "timer_stop");
	public static final Identifier TIMER_STATE_UPDATE = new Identifier(FocusTimerShop.MOD_ID, "timer_state_update");
	public static final Identifier SHOP_PURCHASE = new Identifier(FocusTimerShop.MOD_ID, "shop_purchase");
	public static final Identifier SHOP_CHECKOUT = new Identifier(FocusTimerShop.MOD_ID, "shop_checkout");
	public static final Identifier CURRENCY_CONVERT = new Identifier(FocusTimerShop.MOD_ID, "currency_convert");
	public static final Identifier CHEST_OPEN = new Identifier(FocusTimerShop.MOD_ID, "chest_open");
	public static final Identifier CHEST_OPEN_BULK = new Identifier(FocusTimerShop.MOD_ID, "chest_open_bulk"); // x10+1 package
	public static final Identifier CHEST_BULK_RESULT = new Identifier(FocusTimerShop.MOD_ID, "chest_bulk_result"); // Server sends 11 rewards to client
	public static final Identifier SHOP_DATA_SYNC = new Identifier(FocusTimerShop.MOD_ID, "shop_data_sync"); // Server sends shop items to client on join
	public static final Identifier RENTAL_REQUEST = new Identifier(FocusTimerShop.MOD_ID, "rental_request"); // Client requests tool rental
	public static final Identifier RENTAL_EXPIRED = new Identifier(FocusTimerShop.MOD_ID, "rental_expired"); // PHASE 4: Server notifies client when rental expires (BUG #21 fix)
	public static final Identifier PROFILE_SYNC = new Identifier(FocusTimerShop.MOD_ID, "profile_sync"); // v1.0.6 - Server sends profile data to client
	public static final Identifier CUSTOM_NAME_UPDATE = new Identifier(FocusTimerShop.MOD_ID, "custom_name_update"); // v1.0.6 - Client updates custom name
	public static final Identifier BULK_ORDER_PURCHASE = new Identifier(FocusTimerShop.MOD_ID, "bulk_order_purchase"); // v1.0.6-beta - Bulk order purchase

	// Server-side packet handlers
	public static void registerServerPackets() {
		// Timer control
		ServerPlayNetworking.registerGlobalReceiver(TIMER_START, (server, player, handler, buf, responseSender) -> {
			try {
				TimerType type = buf.readEnumConstant(TimerType.class);
				int targetSeconds = buf.readInt();
				
				// SECURITY: Validate timer duration (same as TimerManager validation)
				if (type != TimerType.STOPWATCH && (targetSeconds <= 0 || targetSeconds > 7200)) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid timer duration: {} (max: 7200)", 
						player.getName().getString(), targetSeconds);
					return;
				}
				
				// SECURITY: Stopwatch can't have target
				if (type == TimerType.STOPWATCH && targetSeconds != 0) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent non-zero target for STOPWATCH: {}", 
						player.getName().getString(), targetSeconds);
					return;
				}
				
				// SECURITY: Rate limiting for timer start (prevent spam)
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"TIMER_START", com.focustimershop.util.RateLimiter.Limits.TIMER_START)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "TIMER_START", 
						com.focustimershop.util.RateLimiter.Limits.TIMER_START);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cBật timer quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				server.execute(() -> {
					TimerManager.startTimer(player, type, targetSeconds);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid TIMER_START packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});

		ServerPlayNetworking.registerGlobalReceiver(TIMER_PAUSE, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				TimerManager.pauseTimer(player);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(TIMER_RESUME, (server, player, handler, buf, responseSender) -> {
			server.execute(() -> {
				TimerManager.resumeTimer(player);
			});
		});

		ServerPlayNetworking.registerGlobalReceiver(TIMER_STOP, (server, player, handler, buf, responseSender) -> {
			try {
				boolean abandoned = buf.readBoolean();
				
				server.execute(() -> {
					TimerManager.stopTimer(player, abandoned);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid TIMER_STOP packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});

		// Shop purchase (old single-item method)
		ServerPlayNetworking.registerGlobalReceiver(SHOP_PURCHASE, (server, player, handler, buf, responseSender) -> {
			try {
				// SECURITY VULN-002: Validate string length
				String itemId = buf.readString(ShopManager.MAX_ITEM_ID_LENGTH);
				boolean useGold = buf.readBoolean();
				
				server.execute(() -> {
					ShopManager.handlePurchase(player, itemId, useGold);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid SHOP_PURCHASE packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});

		// Shop checkout (new cart-based method)
		ServerPlayNetworking.registerGlobalReceiver(SHOP_CHECKOUT, (server, player, handler, buf, responseSender) -> {
			try {
				boolean useSilver = buf.readBoolean();
				int itemCount = buf.readInt();
				
				// SECURITY VULN-002: Validate item count bounds
				if (itemCount <= 0 || itemCount > ShopManager.MAX_CART_ITEMS) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid cart size: {} (max: {})", 
						player.getName().getString(), itemCount, ShopManager.MAX_CART_ITEMS);
					return;
				}
				
				// SECURITY: Rate limiting for shop checkout
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"SHOP_CHECKOUT", com.focustimershop.util.RateLimiter.Limits.SHOP_CHECKOUT)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "SHOP_CHECKOUT", 
						com.focustimershop.util.RateLimiter.Limits.SHOP_CHECKOUT);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cMua hàng quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				// Read cart items with validation
				java.util.Map<String, Integer> items = new java.util.HashMap<>();
				for (int i = 0; i < itemCount; i++) {
					// SECURITY: Limit string length
					String itemId = buf.readString(ShopManager.MAX_ITEM_ID_LENGTH);
					int quantity = buf.readInt();
					
					// SECURITY: Basic validation (detailed validation in ShopManager)
					if (quantity <= 0 || quantity > ShopManager.MAX_PURCHASE_QUANTITY) {
						FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid quantity {} in packet", 
							player.getName().getString(), quantity);
						return; // Reject entire packet
					}
					
					items.put(itemId, quantity);
				}
				
				server.execute(() -> {
					ShopManager.handleCheckout(player, items, useSilver);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid SHOP_CHECKOUT packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});

		// Currency conversion
		ServerPlayNetworking.registerGlobalReceiver(CURRENCY_CONVERT, (server, player, handler, buf, responseSender) -> {
			try {
				boolean silverToGold = buf.readBoolean();
				int amount = buf.readInt();
				
				// SECURITY: Validate conversion amount
				if (amount <= 0) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent non-positive conversion amount: {}", 
						player.getName().getString(), amount);
					return;
				}
				
				// SECURITY: Reasonable max to prevent overflow (economy uses long but should still limit)
				// Max 1 billion units per conversion
				if (amount > 1_000_000_000) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent excessive conversion amount: {}", 
						player.getName().getString(), amount);
					return;
				}
				
				// SECURITY: Rate limiting for currency conversion
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"CURRENCY_CONVERT", com.focustimershop.util.RateLimiter.Limits.CURRENCY_CONVERT)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "CURRENCY_CONVERT", 
						com.focustimershop.util.RateLimiter.Limits.CURRENCY_CONVERT);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cĐổi tiền quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				server.execute(() -> {
				PlayerEconomyData data = EconomyManager.getPlayerData(player);
				boolean success;
				
				if (silverToGold) {
					success = data.convertSilverToGold(amount);
					
					// Track stats (v1.0.6 - Phase A)
					if (success) {
						com.focustimershop.database.PlayerStatsData stats = 
							com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
						stats.setTotalSilverConvertedToGold(stats.getTotalSilverConvertedToGold() + amount);
						com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
					}
				} else {
					success = data.convertGoldToSilver(amount);
				}
				
				if (success) {
					EconomyManager.savePlayerData(player);
					EconomyManager.syncToClient(player);
				}
			});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid CURRENCY_CONVERT packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});

		// Lucky chest opening (single)
		ServerPlayNetworking.registerGlobalReceiver(CHEST_OPEN, (server, player, handler, buf, responseSender) -> {
			try {
				// SECURITY: Limit string length for chest type
				String chestType = buf.readString(32); // Reasonable max for chest type identifier
				
				// SECURITY: Basic validation - not empty
				if (chestType.trim().isEmpty()) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent empty chest type", 
						player.getName().getString());
					return;
				}
				
				server.execute(() -> {
					LuckyChestManager.openChest(player, chestType);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid CHEST_OPEN packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});
		
		// Lucky chest bulk opening (x10+1)
		ServerPlayNetworking.registerGlobalReceiver(CHEST_OPEN_BULK, (server, player, handler, buf, responseSender) -> {
			try {
				// SECURITY: Limit string length for chest type
				String chestType = buf.readString(32);
				long requestId = buf.readLong(); // For idempotency check
				
				// SECURITY: Basic validation
				if (chestType.trim().isEmpty()) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent empty chest type in bulk open", 
						player.getName().getString());
					return;
				}
				
				// SECURITY: Validate requestId is reasonable (prevent negative/zero which might bypass idempotency)
				if (requestId <= 0) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid requestId: {}", 
						player.getName().getString(), requestId);
					return;
				}
				
				// SECURITY: Rate limiting for bulk operations (most expensive)
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"CHEST_OPEN_BULK", com.focustimershop.util.RateLimiter.Limits.CHEST_BULK_OPEN)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "CHEST_OPEN_BULK", 
						com.focustimershop.util.RateLimiter.Limits.CHEST_BULK_OPEN);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cThao tác quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				server.execute(() -> {
					LuckyChestManager.openChestBulk(player, chestType, requestId);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid CHEST_OPEN_BULK packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});
		
		// Tool rental request
		ServerPlayNetworking.registerGlobalReceiver(RENTAL_REQUEST, (server, player, handler, buf, responseSender) -> {
			try {
				int toolIndex = buf.readInt();
				boolean useFortuneMode = buf.readBoolean();
				int fortuneLevel = buf.readInt();
				int efficiencyLevel = buf.readInt();
				int unbreakingLevel = buf.readInt();
				int mendingLevel = buf.readInt();
				int durationMinutes = buf.readInt();
				boolean useSilverPayment = buf.readBoolean();
				
				// SECURITY VULN-003 & VULN-004: Validate in packet handler for early rejection
				if (toolIndex < 0 || toolIndex >= com.focustimershop.rental.RentalManager.MAX_TOOL_TYPES) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid tool index in packet: {}", 
						player.getName().getString(), toolIndex);
					return;
				}
				
				if (fortuneLevel < 0 || fortuneLevel > com.focustimershop.rental.RentalManager.MAX_ENCHANT_LEVEL ||
				    efficiencyLevel < 0 || efficiencyLevel > com.focustimershop.rental.RentalManager.MAX_ENCHANT_LEVEL ||
				    unbreakingLevel < 0 || unbreakingLevel > com.focustimershop.rental.RentalManager.MAX_ENCHANT_LEVEL ||
				    mendingLevel < 0 || mendingLevel > com.focustimershop.rental.RentalManager.MAX_ENCHANT_LEVEL) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid enchant levels in packet", 
						player.getName().getString());
					return;
				}
				
				if (durationMinutes < com.focustimershop.rental.RentalManager.MIN_DURATION_MINUTES || 
				    durationMinutes > com.focustimershop.rental.RentalManager.MAX_DURATION_MINUTES) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid duration in packet: {}", 
						player.getName().getString(), durationMinutes);
					return;
				}
				
				// SECURITY: Rate limiting for rental requests (expensive calculations)
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"RENTAL_REQUEST", com.focustimershop.util.RateLimiter.Limits.RENTAL_REQUEST)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "RENTAL_REQUEST", 
						com.focustimershop.util.RateLimiter.Limits.RENTAL_REQUEST);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cThuê tool quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				server.execute(() -> {
					com.focustimershop.rental.RentalManager.handleRentalRequest(
						player, toolIndex, useFortuneMode, fortuneLevel, efficiencyLevel,
						unbreakingLevel, mendingLevel, durationMinutes, useSilverPayment
					);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid RENTAL_REQUEST packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});
		
		// v1.0.6 - Custom name update
		ServerPlayNetworking.registerGlobalReceiver(CUSTOM_NAME_UPDATE, (server, player, handler, buf, responseSender) -> {
			try {
				// SECURITY: Limit custom name length (ProfileManager validates max 24, but prevent packet abuse)
				String newName = buf.readString(64); // Allow some buffer beyond 24 for validation message
				
				// SECURITY: Reject empty or whitespace-only names early
				if (newName.trim().isEmpty()) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent empty custom name", 
						player.getName().getString());
					return;
				}
				
				// SECURITY: Reject excessively long names (double-check beyond readString limit)
				if (newName.length() > 32) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent excessively long custom name: {} chars", 
						player.getName().getString(), newName.length());
					return;
				}
				
				// SECURITY: Rate limiting for profile updates
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"CUSTOM_NAME_UPDATE", com.focustimershop.util.RateLimiter.Limits.CUSTOM_NAME_UPDATE)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "CUSTOM_NAME_UPDATE", 
						com.focustimershop.util.RateLimiter.Limits.CUSTOM_NAME_UPDATE);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cCập nhật tên quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				server.execute(() -> {
					com.focustimershop.profile.ProfileManager.updateCustomName(player, newName);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid CUSTOM_NAME_UPDATE packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});
		
		// v1.0.6-beta - Bulk order purchase
		ServerPlayNetworking.registerGlobalReceiver(BULK_ORDER_PURCHASE, (server, player, handler, buf, responseSender) -> {
			try {
				// SECURITY: Validate string length for item ID
				String itemId = buf.readString(256);
				int chestCount = buf.readInt();
				boolean useSilverOnly = buf.readBoolean();
				
				// SECURITY: Validate chest count (positive, reasonable limit)
				if (chestCount < 1 || chestCount > 1000) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid chest count: {}", 
						player.getName().getString(), chestCount);
					return;
				}
				
				// SECURITY: Validate item ID not empty
				if (itemId.trim().isEmpty()) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent empty item ID", 
						player.getName().getString());
					return;
				}
				
				// Rate limiting for bulk purchases
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"BULK_ORDER_PURCHASE", com.focustimershop.util.RateLimiter.Limits.SHOP_CHECKOUT)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "BULK_ORDER_PURCHASE", 
						com.focustimershop.util.RateLimiter.Limits.SHOP_CHECKOUT);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cMua hàng quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				server.execute(() -> {
					com.focustimershop.bulkorder.BulkOrderManager.handleBulkPurchase(
						player, itemId, chestCount, useSilverOnly
					);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid BULK_ORDER_PURCHASE packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});
		
		// v1.0.6 Phase 5 - Equip title
		ServerPlayNetworking.registerGlobalReceiver(NetworkHandler.EQUIP_TITLE_C2S, (server, player, handler, buf, responseSender) -> {
			try {
				// SECURITY: Limit title ID length
				String titleId = buf.readString(64); // Reasonable max for title identifier
				
				// SECURITY: Validate not empty
				if (titleId.trim().isEmpty()) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent empty title ID", 
						player.getName().getString());
					return;
				}
				
				// SECURITY: Basic alphanumeric validation (title IDs should be simple identifiers)
				if (!titleId.matches("^[a-zA-Z0-9_\\-]+$")) {
					FocusTimerShop.LOGGER.warn("SECURITY: Player {} sent invalid title ID format: {}", 
						player.getName().getString(), titleId);
					return;
				}
				
				// SECURITY: Rate limiting for title equip (less strict than name update)
				if (!com.focustimershop.util.RateLimiter.tryRequest(
						player.getUuid(), player.getName().getString(), 
						"EQUIP_TITLE", com.focustimershop.util.RateLimiter.Limits.EQUIP_TITLE)) {
					long resetMs = com.focustimershop.util.RateLimiter.getResetTimeMs(
						player.getUuid(), "EQUIP_TITLE", 
						com.focustimershop.util.RateLimiter.Limits.EQUIP_TITLE);
					player.sendMessage(
						net.minecraft.text.Text.literal("§cĐổi danh hiệu quá nhanh! Vui lòng chờ " + 
							com.focustimershop.util.RateLimiter.formatResetTime(resetMs)),
						false
					);
					return;
				}
				
				server.execute(() -> {
					com.focustimershop.profile.ProfileManager.equipTitle(player, titleId);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.warn("Invalid EQUIP_TITLE packet from {}: {}", 
					player.getName().getString(), e.getMessage());
			}
		});
	}

	// Client-side packet handlers
	public static void registerClientPackets() {
		ClientPlayNetworking.registerGlobalReceiver(ECONOMY_SYNC, (client, handler, buf, responseSender) -> {
			long silver = buf.readLong(); // Phase 0 - changed to long
			long gold = buf.readLong();
			long xp = buf.readLong();
			
			client.execute(() -> {
				ClientDataCache.updateEconomy(silver, gold, xp);
			});
		});

		ClientPlayNetworking.registerGlobalReceiver(TIMER_STATE_UPDATE, (client, handler, buf, responseSender) -> {
			boolean hasTimer = buf.readBoolean();
			
			if (hasTimer) {
				TimerType type = buf.readEnumConstant(TimerType.class);
				TimerState state = buf.readEnumConstant(TimerState.class);
				int elapsed = buf.readInt();
				int target = buf.readInt();
				int pomodoroRounds = buf.readInt();
				
				client.execute(() -> {
					ClientDataCache.updateTimerState(type, state, elapsed, target, pomodoroRounds);
					
					// v1.0.7-beta: Auto-open ActiveSessionScreen if timer is running
					if (state == TimerState.RUNNING && client.currentScreen == null) {
						client.setScreen(new com.focustimershop.client.gui.ActiveSessionScreen());
						FocusTimerShop.LOGGER.info("[CLIENT] Opened ActiveSessionScreen (timer restored)");
					}
				});
			} else {
				client.execute(() -> {
					ClientDataCache.clearTimerState();
					
					// Close ActiveSessionScreen if it's open
					if (client.currentScreen instanceof com.focustimershop.client.gui.ActiveSessionScreen) {
						client.setScreen(null);
					}
				});
			}
		});
		
		ClientPlayNetworking.registerGlobalReceiver(CHEST_BULK_RESULT, (client, handler, buf, responseSender) -> {
			try {
				// PHASE 4: BUG #20 FIX - Validate packet data to prevent client crash
				String chestName = buf.readString(64); // Limit length
				int rewardCount = buf.readInt();
				
				// PHASE 4: Validate reward count (bulk is 11, allow buffer up to 20)
				if (rewardCount < 0 || rewardCount > 20) {
					FocusTimerShop.LOGGER.warn("PHASE4_NETWORK: Invalid CHEST_BULK_RESULT count: {}", rewardCount);
					return;
				}
				
				// Read all rewards
				java.util.List<net.minecraft.item.ItemStack> rewards = new java.util.ArrayList<>(rewardCount);
				for (int i = 0; i < rewardCount; i++) {
					try {
						net.minecraft.item.ItemStack stack = buf.readItemStack();
						rewards.add(stack);
					} catch (Exception e) {
						FocusTimerShop.LOGGER.error("Failed to read ItemStack from CHEST_BULK_RESULT packet", e);
					}
				}
				
				client.execute(() -> {
					// Only open screen if player and world are ready
					if (client.player != null && client.world != null && !rewards.isEmpty()) {
						try {
							com.focustimershop.client.gui.ChestBulkResultScreen resultScreen = 
								new com.focustimershop.client.gui.ChestBulkResultScreen(chestName, rewards);
							client.setScreen(resultScreen);
						} catch (Exception e) {
							FocusTimerShop.LOGGER.error("Failed to open ChestBulkResultScreen", e);
						}
					}
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("PHASE4_NETWORK: Failed to process CHEST_BULK_RESULT packet", e);
			}
		});
		
		// PHASE 4: BUG #21 FIX - Handle rental expiry notification from server
		ClientPlayNetworking.registerGlobalReceiver(RENTAL_EXPIRED, (client, handler, buf, responseSender) -> {
			try {
				String rentalType = buf.readString(32); // Rental type identifier
				
				client.execute(() -> {
					// Clear client cache - rental has expired on server
					com.focustimershop.client.ClientRentalCache.clearRental();
					FocusTimerShop.LOGGER.info("PHASE4_RENTAL: Rental expired notification received: {}", rentalType);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("PHASE4_RENTAL: Failed to process RENTAL_EXPIRED packet", e);
			}
		});
		
		ClientPlayNetworking.registerGlobalReceiver(SHOP_DATA_SYNC, (client, handler, buf, responseSender) -> {
			int itemCount = buf.readInt();
			
			java.util.List<com.focustimershop.shop.ShopItem> items = new java.util.ArrayList<>();
			for (int i = 0; i < itemCount; i++) {
				try {
					String itemId = buf.readString();
					String categoryName = buf.readString();
					int silverPrice = buf.readInt();
					int goldCost = buf.readInt();
					String displayName = buf.readString();
					
					com.focustimershop.shop.ShopCategory category = com.focustimershop.shop.ShopCategory.valueOf(categoryName);
					com.focustimershop.shop.ShopItem item = new com.focustimershop.shop.ShopItem(itemId, category, silverPrice, goldCost, displayName);
					items.add(item);
				} catch (Exception e) {
					FocusTimerShop.LOGGER.error("Failed to read ShopItem from SHOP_DATA_SYNC packet", e);
				}
			}
			
			client.execute(() -> {
				ClientDataCache.setShopItems(items);
				FocusTimerShop.LOGGER.info("Received {} shop items from server", items.size());
			});
		});
		
		// v1.0.6 - Profile data sync (Phase 0 - long, Phase 0.2 - added stats)
		ClientPlayNetworking.registerGlobalReceiver(PROFILE_SYNC, (client, handler, buf, responseSender) -> {
			try {
				String inGameName = buf.readString();
				String customName = buf.readString();
				long totalFocusXpEarned = buf.readLong(); // Lifetime
				long seasonRankXp = buf.readLong(); // v1.0.6-beta Season System
				int currentSeasonNumber = buf.readInt(); // v1.0.6-beta Season System
				int currentStreakDays = buf.readInt();
				int longestStreakDays = buf.readInt();
				int longestSingleSessionSeconds = buf.readInt();
				long totalSessionsCompleted = buf.readLong(); // Phase 0 - long
				long totalFocusTimeSeconds = buf.readLong();
				String favoriteTimerType = buf.readString();
				long profileCreatedAtEpochSeconds = buf.readLong();
				long lastFocusDate = buf.readLong(); // Phase 0.2
				
				// Phase 0.2 - Read missing stats
				long totalSilverEarned = buf.readLong();
				long totalSilverConvertedToGold = buf.readLong();
				long totalItemsPurchased = buf.readLong();
				long totalChestsOpened = buf.readLong();
				long totalBlocksMined = buf.readLong();
				
				client.execute(() -> {
					com.focustimershop.client.ClientProfileCache.updateProfile(
						inGameName, customName, totalFocusXpEarned, seasonRankXp, currentSeasonNumber,
						currentStreakDays, longestStreakDays, longestSingleSessionSeconds, 
						totalSessionsCompleted, totalFocusTimeSeconds, favoriteTimerType, profileCreatedAtEpochSeconds
					);
					com.focustimershop.client.ClientProfileCache.setLastFocusDate(lastFocusDate);
					
					// Phase 0.2 - Update stats in client cache
					com.focustimershop.client.ClientDataCache.setTotalSilverEarned(totalSilverEarned);
					com.focustimershop.client.ClientDataCache.setTotalSilverConvertedToGold(totalSilverConvertedToGold);
					com.focustimershop.client.ClientDataCache.setTotalItemsPurchased(totalItemsPurchased);
					com.focustimershop.client.ClientDataCache.setTotalChestsOpened(totalChestsOpened);
					com.focustimershop.client.ClientDataCache.setTotalBlocksMined(totalBlocksMined);
				});
			} catch (Exception e) {
				FocusTimerShop.LOGGER.error("Failed to read PROFILE_SYNC packet", e);
			}
		});
	}

	// Helper methods to send packets

	/**
	 * Send economy data to client (Phase 0 - long)
	 */
	public static void sendEconomySync(ServerPlayerEntity player, PlayerEconomyData data) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeLong(data.getSilverCoins());
		buf.writeLong(data.getGoldCoins());
		buf.writeLong(data.getFocusXp());
		
		ServerPlayNetworking.send(player, ECONOMY_SYNC, buf);
	}

	/**
	 * Send timer state update to client
	 */
	public static void sendTimerStateUpdate(ServerPlayerEntity player, TimerSession session) {
		PacketByteBuf buf = PacketByteBufs.create();
		
		if (session != null) {
			buf.writeBoolean(true);
			buf.writeEnumConstant(session.getType());
			buf.writeEnumConstant(session.getState());
			buf.writeInt(session.getElapsedTime());
			buf.writeInt(session.getTargetTime());
			buf.writeInt(session.getPomodoroRounds());
		} else {
			buf.writeBoolean(false);
		}
		
		ServerPlayNetworking.send(player, TIMER_STATE_UPDATE, buf);
	}

	// Client-to-server helpers

	public static void sendTimerStart(TimerType type, int targetSeconds) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeEnumConstant(type);
		buf.writeInt(targetSeconds);
		ClientPlayNetworking.send(TIMER_START, buf);
	}

	public static void sendTimerPause() {
		ClientPlayNetworking.send(TIMER_PAUSE, PacketByteBufs.create());
	}

	public static void sendTimerResume() {
		ClientPlayNetworking.send(TIMER_RESUME, PacketByteBufs.create());
	}

	public static void sendTimerStop(boolean abandoned) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(abandoned);
		ClientPlayNetworking.send(TIMER_STOP, buf);
	}

	public static void sendShopPurchase(String itemId, boolean useGold) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(itemId);
		buf.writeBoolean(useGold);
		ClientPlayNetworking.send(SHOP_PURCHASE, buf);
	}

	public static void sendShopCheckout(java.util.Map<String, Integer> items, boolean useSilver) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(useSilver);
		buf.writeInt(items.size());
		
		for (java.util.Map.Entry<String, Integer> entry : items.entrySet()) {
			buf.writeString(entry.getKey());
			buf.writeInt(entry.getValue());
		}
		
		ClientPlayNetworking.send(SHOP_CHECKOUT, buf);
	}

	public static void sendCurrencyConvert(boolean silverToGold, int amount) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeBoolean(silverToGold);
		buf.writeInt(amount);
		ClientPlayNetworking.send(CURRENCY_CONVERT, buf);
	}

	public static void sendChestOpen(String chestType) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(chestType);
		ClientPlayNetworking.send(CHEST_OPEN, buf);
	}
	
	public static void sendChestOpenBulk(String chestType) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(chestType);
		buf.writeLong(System.currentTimeMillis()); // Unique request ID for idempotency
		ClientPlayNetworking.send(CHEST_OPEN_BULK, buf);
	}
	
	/**
	 * Send bulk order purchase request (v1.0.6-beta)
	 */
	public static void sendBulkOrderPurchase(String itemId, int chestCount, boolean useSilverOnly) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeString(itemId);
		buf.writeInt(chestCount);
		buf.writeBoolean(useSilverOnly);
		ClientPlayNetworking.send(BULK_ORDER_PURCHASE, buf);
	}
	
	/**
	 * Send bulk chest result to client (11 rewards)
	 * Only send if player is fully connected and world is ready
	 */
	public static void sendChestBulkResult(ServerPlayerEntity player, String chestName, java.util.List<net.minecraft.item.ItemStack> rewards) {
		if (player == null || player.getWorld() == null || !player.networkHandler.isConnectionOpen()) {
			FocusTimerShop.LOGGER.warn("Cannot send CHEST_BULK_RESULT - player not ready");
			return;
		}
		
		try {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeString(chestName);
			buf.writeInt(rewards.size());
			
			for (net.minecraft.item.ItemStack stack : rewards) {
				buf.writeItemStack(stack);
			}
			
			ServerPlayNetworking.send(player, CHEST_BULK_RESULT, buf);
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to send CHEST_BULK_RESULT packet", e);
		}
	}
	
	/**
	 * Send shop data to client on join
	 */
	public static void sendRentalRequest(int toolIndex, boolean useFortuneMode, int fortuneLevel,
	                                     int efficiencyLevel, int unbreakingLevel, int mendingLevel,
	                                     int durationMinutes, boolean useSilverPayment) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(toolIndex);
		buf.writeBoolean(useFortuneMode);
		buf.writeInt(fortuneLevel);
		buf.writeInt(efficiencyLevel);
		buf.writeInt(unbreakingLevel);
		buf.writeInt(mendingLevel);
		buf.writeInt(durationMinutes);
		buf.writeBoolean(useSilverPayment);
		ClientPlayNetworking.send(RENTAL_REQUEST, buf);
	}
	
	/**
	 * Send shop data to client on join
	 */
	public static void sendShopData(ServerPlayerEntity player) {
		java.util.Collection<com.focustimershop.shop.ShopItem> items = ShopManager.getAllItems();
		
		try {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeInt(items.size());
			
			for (com.focustimershop.shop.ShopItem item : items) {
				buf.writeString(item.getItemId());
				buf.writeString(item.getCategory().name());
				buf.writeInt(item.getSilverPrice());
				buf.writeInt(item.getGoldCost());
				buf.writeString(item.getDisplayName());
			}
			
			ServerPlayNetworking.send(player, SHOP_DATA_SYNC, buf);
			FocusTimerShop.LOGGER.info("Sent {} shop items to {}", items.size(), player.getName().getString());
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to send SHOP_DATA_SYNC packet to {}", player.getName().getString(), e);
		}
	}
	
	/**
	 * Send profile data to client (v1.0.6 Phase A - reads from both Profile + Stats)
	 * v1.0.6 Phase 0.2 - Added missing stats sync
	 */
	public static void sendProfileSync(ServerPlayerEntity player, com.focustimershop.profile.PlayerProfile profile) {
		try {
			// Get stats from PlayerStatsData
			com.focustimershop.database.PlayerStatsData stats = 
				com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
			
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeString(profile.getInGameName());
			buf.writeString(profile.getCustomName() != null ? profile.getCustomName() : "");
			buf.writeLong(stats.getTotalXpEarned()); // Lifetime XP (Phase 0 - long)
			buf.writeLong(stats.getSeasonRankXp()); // v1.0.6-beta Season System - Seasonal XP
			buf.writeInt(stats.getCurrentSeasonNumber()); // v1.0.6-beta Season System - Season number
			buf.writeInt(profile.getCurrentStreakDays());
			buf.writeInt(profile.getLongestStreakDays());
			buf.writeInt(profile.getLongestSingleSessionSeconds());
			buf.writeLong(stats.getTotalTimerSessionsCompleted()); // From stats (Phase 0 - long)
			buf.writeLong(stats.getTotalFocusTimeSeconds()); // From stats (Phase 0 - long)
			buf.writeString(profile.getFavoriteTimerType() != null ? profile.getFavoriteTimerType() : "POMODORO");
			buf.writeLong(profile.getProfileCreatedAtEpochSeconds());
			buf.writeLong(profile.getLastFocusSessionEpochSeconds()); // v1.0.6 Phase 0.2
			
			// v1.0.6 Phase 0.2 - Add missing stats for DetailedStatsTab
			buf.writeLong(stats.getTotalSilverEarned());
			buf.writeLong(stats.getTotalSilverConvertedToGold());
			buf.writeLong(stats.getTotalItemsPurchased());
			buf.writeLong(stats.getTotalChestsOpened());
			buf.writeLong(stats.getTotalBlocksMined());
			
			ServerPlayNetworking.send(player, PROFILE_SYNC, buf);
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to send PROFILE_SYNC packet to {}", player.getName().getString(), e);
		}
	}
	
	/**
	 * PHASE 4: BUG #21 FIX - Send rental expired notification to client
	 * Called by server when rental expires to invalidate client cache
	 */
	public static void sendRentalExpired(ServerPlayerEntity player, String rentalType) {
		if (player == null || !player.networkHandler.isConnectionOpen()) {
			return;
		}
		
		try {
			PacketByteBuf buf = PacketByteBufs.create();
			buf.writeString(rentalType);
			ServerPlayNetworking.send(player, RENTAL_EXPIRED, buf);
			FocusTimerShop.LOGGER.debug("PHASE4_RENTAL: Sent rental expired notification to {}: {}", 
				player.getName().getString(), rentalType);
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("PHASE4_RENTAL: Failed to send RENTAL_EXPIRED packet", e);
		}
	}
}
