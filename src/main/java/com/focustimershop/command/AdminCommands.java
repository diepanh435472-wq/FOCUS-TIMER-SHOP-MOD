package com.focustimershop.command;

import com.focustimershop.economy.EconomyManager;
import com.focustimershop.economy.PlayerEconomyData;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Admin commands for Focus Timer Shop
 * Usage: /focus admin set <currency> <amount|infinity> <player>
 */
public class AdminCommands {
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("focus")
			.requires(source -> source.hasPermissionLevel(2)) // Require OP level 2
			.then(CommandManager.literal("admin")
				.then(CommandManager.literal("set")
					// /focus admin set <currency> <amount> <player>
					.then(CommandManager.argument("currency", StringArgumentType.word())
						.suggests((context, builder) -> {
							builder.suggest("all");
							builder.suggest("silver");
							builder.suggest("gold");
							builder.suggest("xp");
							return builder.buildFuture();
						})
						.then(CommandManager.argument("amount", StringArgumentType.word())
							.suggests((context, builder) -> {
								builder.suggest("infinity");
								builder.suggest("1000");
								builder.suggest("10000");
								builder.suggest("100000");
								return builder.buildFuture();
							})
							.then(CommandManager.argument("player", EntityArgumentType.player())
								.executes(AdminCommands::executeSetCurrency)
							)
						)
					)
				)
			)
		);
	}
	
	private static int executeSetCurrency(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
		ServerCommandSource source = context.getSource();
		String currency = StringArgumentType.getString(context, "currency");
		String amountStr = StringArgumentType.getString(context, "amount");
		ServerPlayerEntity targetPlayer = EntityArgumentType.getPlayer(context, "player");
		
		// Parse amount (can be number or "infinity")
		boolean isInfinity = amountStr.equalsIgnoreCase("infinity");
		int amount = 0;
		
		if (!isInfinity) {
			try {
				amount = Integer.parseInt(amountStr);
				if (amount < 0) {
					source.sendError(Text.literal("§cSố lượng phải là số dương!"));
					return 0;
				}
			} catch (NumberFormatException e) {
				source.sendError(Text.literal("§cSố lượng không hợp lệ! Sử dụng số hoặc 'infinity'."));
				return 0;
			}
		}
		
		// Make final for lambda
		final int finalAmount = amount;
		final String playerName = targetPlayer.getName().getString();
		
		// Get player data
		PlayerEconomyData economy = EconomyManager.getPlayerData(targetPlayer);
		
		// Apply currency changes
		switch (currency.toLowerCase()) {
			case "all":
				if (isInfinity) {
					economy.setSilverCoins(Integer.MAX_VALUE);
					economy.setGoldCoins(Integer.MAX_VALUE);
					economy.setFocusXp(Integer.MAX_VALUE);
					source.sendFeedback(() -> Text.literal("§aĐã set INFINITY tất cả tiền tệ cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set infinity tất cả tiền tệ cho bạn!"), false);
				} else {
					economy.setSilverCoins(finalAmount);
					economy.setGoldCoins(finalAmount);
					economy.setFocusXp(finalAmount);
					source.sendFeedback(() -> Text.literal("§aĐã set tất cả tiền tệ = " + finalAmount + " cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set tất cả tiền tệ của bạn = " + finalAmount), false);
				}
				break;
				
			case "silver":
				if (isInfinity) {
					economy.setSilverCoins(Integer.MAX_VALUE);
					source.sendFeedback(() -> Text.literal("§aĐã set INFINITY Silver Coins cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set infinity Silver Coins cho bạn!"), false);
				} else {
					economy.setSilverCoins(finalAmount);
					source.sendFeedback(() -> Text.literal("§aĐã set Silver Coins = " + finalAmount + " cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set Silver Coins của bạn = " + finalAmount), false);
				}
				break;
				
			case "gold":
				if (isInfinity) {
					economy.setGoldCoins(Integer.MAX_VALUE);
					source.sendFeedback(() -> Text.literal("§aĐã set INFINITY Gold Coins cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set infinity Gold Coins cho bạn!"), false);
				} else {
					economy.setGoldCoins(finalAmount);
					source.sendFeedback(() -> Text.literal("§aĐã set Gold Coins = " + finalAmount + " cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set Gold Coins của bạn = " + finalAmount), false);
				}
				break;
				
			case "xp":
				if (isInfinity) {
					economy.setFocusXp(Integer.MAX_VALUE);
					source.sendFeedback(() -> Text.literal("§aĐã set INFINITY Focus XP cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set infinity Focus XP cho bạn!"), false);
				} else {
					economy.setFocusXp(finalAmount);
					source.sendFeedback(() -> Text.literal("§aĐã set Focus XP = " + finalAmount + " cho " + playerName), true);
					targetPlayer.sendMessage(Text.literal("§a§lADMIN đã set Focus XP của bạn = " + finalAmount), false);
				}
				break;
				
			default:
				source.sendError(Text.literal("§cLoại tiền tệ không hợp lệ! Sử dụng: all, silver, gold, hoặc xp"));
				return 0;
		}
		
		// Save and sync
		EconomyManager.savePlayerData(targetPlayer);
		EconomyManager.syncToClient(targetPlayer);
		
		return 1;
	}
}
