package com.focustimershop.command;

import com.focustimershop.profile.ProfileManager;
import com.focustimershop.profile.RankManager;
import com.focustimershop.profile.RankTier;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

/**
 * Test command for Profile system (Phase 1 verification)
 * /profiletest ranks - Show all 58 ranks
 * /profiletest resolve <xp> - Resolve rank for given XP
 * /profiletest simulateday <dateKey> <seconds> <xp> - Simulate session on specific day (Phase 1)
 * /profiletest showdaily - Show all daily stats (Phase 1)
 */
public class ProfileTestCommand {
	
	public static void register(CommandDispatcher<ServerCommandSource> dispatcher,
	                            CommandRegistryAccess registryAccess,
	                            CommandManager.RegistrationEnvironment environment) {
		dispatcher.register(CommandManager.literal("profiletest")
			.requires(source -> source.hasPermissionLevel(2))
			.then(CommandManager.literal("ranks")
				.executes(ProfileTestCommand::listAllRanks))
			.then(CommandManager.literal("resolve")
				.then(CommandManager.argument("xp", com.mojang.brigadier.arguments.LongArgumentType.longArg(0))
					.executes(ProfileTestCommand::resolveRank)))
			.then(CommandManager.literal("awardxp")
				.then(CommandManager.argument("xp", com.mojang.brigadier.arguments.LongArgumentType.longArg(0))
					.executes(ProfileTestCommand::awardXp)))
			.then(CommandManager.literal("simulateday")
				.then(CommandManager.argument("dateKey", com.mojang.brigadier.arguments.StringArgumentType.string())
					.then(CommandManager.argument("seconds", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
						.then(CommandManager.argument("xp", com.mojang.brigadier.arguments.IntegerArgumentType.integer(0))
							.executes(ProfileTestCommand::simulateDay)))))
			.then(CommandManager.literal("showdaily")
				.executes(ProfileTestCommand::showDailyStats))
		);
	}
	
	private static int listAllRanks(CommandContext<ServerCommandSource> context) {
		var ranks = RankManager.getAllRanks();
		context.getSource().sendMessage(Text.literal("§e=== All Ranks (" + ranks.size() + " total) ==="));
		
		for (var rank : ranks) {
			context.getSource().sendMessage(Text.literal(
				String.format("§7[%d] §f%s §8| §7XP: %d §8| §7Cumulative: %d §8| §7Color: %s",
					ranks.indexOf(rank) + 1,
					rank.getDisplayName(),
					rank.getRequiredXP(),
					rank.getCumulativeXP(),
					rank.getFrameColor())
			));
		}
		
		return 1;
	}
	
	private static int resolveRank(CommandContext<ServerCommandSource> context) {
		long xp = com.mojang.brigadier.arguments.LongArgumentType.getLong(context, "xp");
		RankTier rank = RankManager.resolveRank(xp);
		
		context.getSource().sendMessage(Text.literal(
			String.format("§e=== Rank Resolution for %d XP ===", xp)
		));
		context.getSource().sendMessage(Text.literal(
			String.format("§fRank: §b%s", rank.getDisplayName())
		));
		context.getSource().sendMessage(Text.literal(
			String.format("§fProgress: §a%d §7/ §e%d §7(§6%d%%§7)", 
				rank.getXpIntoLevel(), rank.getXpNeededForLevel(), rank.getPercent())
		));
		context.getSource().sendMessage(Text.literal(
			String.format("§fFrame Color: %s §7| Animated: %s §7| Max Rank: %s",
				rank.getFrameColor(), rank.isAnimated(), rank.isMaxRank())
		));
		
		return 1;
	}
	
	private static int awardXp(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendError(Text.literal("Must be executed by a player"));
			return 0;
		}
		
		long xp = com.mojang.brigadier.arguments.LongArgumentType.getLong(context, "xp");
		ProfileManager.awardFocusXp(player, xp);
		
		// Phase A - read XP from PlayerStatsData (v1.0.6-beta Season System - use seasonRankXp)
		var stats = com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		var rank = RankManager.resolveRank(stats.getSeasonRankXp());
		
		context.getSource().sendMessage(Text.literal(
			String.format("§aAwarded %d XP! Total: %d XP | Rank: %s", 
				xp, stats.getTotalXpEarned(), rank.getDisplayName())
		));
		
		return 1;
	}
	
	/**
	 * Phase 1 test - Simulate session on specific day
	 */
	private static int simulateDay(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendError(Text.literal("Must be executed by a player"));
			return 0;
		}
		
		String dateKey = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "dateKey");
		int seconds = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "seconds");
		int xp = com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(context, "xp");
		
		// Get stats
		var stats = com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		
		// Get or create DailyStat for specified date
		var dailyStats = stats.getDailyStats();
		if (dailyStats == null) {
			dailyStats = new java.util.HashMap<>();
			stats.setDailyStats(dailyStats);
		}
		
		var dayStat = dailyStats.get(dateKey);
		if (dayStat == null) {
			dayStat = new com.focustimershop.database.DailyStat();
			dailyStats.put(dateKey, dayStat);
		}
		
		// Add session
		dayStat.addSession(seconds, xp);
		
		// Save
		com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
		
		context.getSource().sendMessage(Text.literal(
			String.format("§aSimulated session on %s: +%ds, +%d XP", dateKey, seconds, xp)
		));
		context.getSource().sendMessage(Text.literal(
			String.format("§7Day total: %ds focus, %d sessions, %d XP", 
				dayStat.getFocusSeconds(), dayStat.getSessionCount(), dayStat.getXpEarned())
		));
		
		return 1;
	}
	
	/**
	 * Phase 1 test - Show all daily stats
	 */
	private static int showDailyStats(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendError(Text.literal("Must be executed by a player"));
			return 0;
		}
		
		var stats = com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
		var dailyStats = stats.getDailyStats();
		
		if (dailyStats == null || dailyStats.isEmpty()) {
			context.getSource().sendMessage(Text.literal("§eNo daily stats recorded yet"));
			return 0;
		}
		
		context.getSource().sendMessage(Text.literal(
			String.format("§e=== Daily Stats (%d days) ===", dailyStats.size())
		));
		
		// Sort by date (newest first)
		var sortedDays = new java.util.ArrayList<>(dailyStats.keySet());
		sortedDays.sort(java.util.Collections.reverseOrder());
		
		for (String day : sortedDays) {
			var dayStat = dailyStats.get(day);
			int minutes = dayStat.getFocusSeconds() / 60;
			int hours = minutes / 60;
			int remainingMins = minutes % 60;
			
			String timeStr = hours > 0 
				? String.format("%dh %02dm", hours, remainingMins)
				: String.format("%dm", remainingMins);
			
			context.getSource().sendMessage(Text.literal(
				String.format("§f%s §8| §b%s §8| §a%d sessions §8| §e%d XP",
					day, timeStr, dayStat.getSessionCount(), dayStat.getXpEarned())
			));
		}
		
		return 1;
	}
}
