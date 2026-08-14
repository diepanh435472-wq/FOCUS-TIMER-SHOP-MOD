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

	// Server-side packet handlers
	public static void registerServerPackets() {
		// Timer control
		ServerPlayNetworking.registerGlobalReceiver(TIMER_START, (server, player, handler, buf, responseSender) -> {
			TimerType type = buf.readEnumConstant(TimerType.class);
			int targetSeconds = buf.readInt();
			
			server.execute(() -> {
				TimerManager.startTimer(player, type, targetSeconds);
			});
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
			boolean abandoned = buf.readBoolean();
			
			server.execute(() -> {
				TimerManager.stopTimer(player, abandoned);
			});
		});

		// Shop purchase (old single-item method)
		ServerPlayNetworking.registerGlobalReceiver(SHOP_PURCHASE, (server, player, handler, buf, responseSender) -> {
			String itemId = buf.readString();
			boolean useGold = buf.readBoolean();
			
			server.execute(() -> {
				ShopManager.handlePurchase(player, itemId, useGold);
			});
		});

		// Shop checkout (new cart-based method)
		ServerPlayNetworking.registerGlobalReceiver(SHOP_CHECKOUT, (server, player, handler, buf, responseSender) -> {
			boolean useSilver = buf.readBoolean();
			int itemCount = buf.readInt();
			
			// Read cart items
			java.util.Map<String, Integer> items = new java.util.HashMap<>();
			for (int i = 0; i < itemCount; i++) {
				String itemId = buf.readString();
				int quantity = buf.readInt();
				items.put(itemId, quantity);
			}
			
			server.execute(() -> {
				ShopManager.handleCheckout(player, items, useSilver);
			});
		});

		// Currency conversion
		ServerPlayNetworking.registerGlobalReceiver(CURRENCY_CONVERT, (server, player, handler, buf, responseSender) -> {
			boolean silverToGold = buf.readBoolean();
			int amount = buf.readInt();
			
			server.execute(() -> {
				PlayerEconomyData data = EconomyManager.getPlayerData(player);
				boolean success;
				
				if (silverToGold) {
					success = data.convertSilverToGold(amount);
				} else {
					success = data.convertGoldToSilver(amount);
				}
				
				if (success) {
					EconomyManager.savePlayerData(player);
					EconomyManager.syncToClient(player);
				}
			});
		});

		// Lucky chest opening (single)
		ServerPlayNetworking.registerGlobalReceiver(CHEST_OPEN, (server, player, handler, buf, responseSender) -> {
			String chestType = buf.readString();
			
			server.execute(() -> {
				LuckyChestManager.openChest(player, chestType);
			});
		});
		
		// Lucky chest bulk opening (x10+1)
		ServerPlayNetworking.registerGlobalReceiver(CHEST_OPEN_BULK, (server, player, handler, buf, responseSender) -> {
			String chestType = buf.readString();
			long requestId = buf.readLong(); // For idempotency check
			
			server.execute(() -> {
				LuckyChestManager.openChestBulk(player, chestType, requestId);
			});
		});
		
		// Tool rental request
		ServerPlayNetworking.registerGlobalReceiver(RENTAL_REQUEST, (server, player, handler, buf, responseSender) -> {
			int toolIndex = buf.readInt(); // 0=Pickaxe, 1=Axe, 2=Shovel
			boolean useFortuneMode = buf.readBoolean();
			int fortuneLevel = buf.readInt();
			int efficiencyLevel = buf.readInt();
			int unbreakingLevel = buf.readInt();
			int mendingLevel = buf.readInt();
			int durationMinutes = buf.readInt();
			boolean useSilverPayment = buf.readBoolean();
			
			server.execute(() -> {
				com.focustimershop.rental.RentalManager.handleRentalRequest(
					player, toolIndex, useFortuneMode, fortuneLevel, efficiencyLevel,
					unbreakingLevel, mendingLevel, durationMinutes, useSilverPayment
				);
			});
		});
	}

	// Client-side packet handlers
	public static void registerClientPackets() {
		ClientPlayNetworking.registerGlobalReceiver(ECONOMY_SYNC, (client, handler, buf, responseSender) -> {
			int silver = buf.readInt();
			int gold = buf.readInt();
			int xp = buf.readInt();
			
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
				});
			} else {
				client.execute(() -> {
					ClientDataCache.clearTimerState();
				});
			}
		});
		
		ClientPlayNetworking.registerGlobalReceiver(CHEST_BULK_RESULT, (client, handler, buf, responseSender) -> {
			String chestName = buf.readString();
			int rewardCount = buf.readInt();
			
			// Read all rewards
			java.util.List<net.minecraft.item.ItemStack> rewards = new java.util.ArrayList<>();
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
	}

	// Helper methods to send packets

	/**
	 * Send economy data to client
	 */
	public static void sendEconomySync(ServerPlayerEntity player, PlayerEconomyData data) {
		PacketByteBuf buf = PacketByteBufs.create();
		buf.writeInt(data.getSilverCoins());
		buf.writeInt(data.getGoldCoins());
		buf.writeInt(data.getFocusXp());
		
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
}
