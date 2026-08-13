package com.focustimershop.rental;

import com.focustimershop.FocusTimerShop;
import com.focustimershop.database.RentalData;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles 3x3 area mining for rental tools
 * SERVER-SIDE ONLY - validates all operations
 */
public class AreaMiningHandler {
	
	// Track recently mined blocks to prevent duplicates
	private static final Set<BlockPos> recentlyMined = new HashSet<>();
	private static long lastCleanup = 0;
	
	/**
	 * Handle area mining attempt
	 * Returns true if handled (cancel normal mining)
	 */
	public static boolean handleAreaMining(ServerPlayerEntity player, ItemStack tool, BlockPos targetPos, ServerWorld world) {
		// Validate rental
		if (!RentalManager.isValidRentalTool(player, tool)) {
			return false;
		}
		
		RentalData rental = RentalManager.getRental(player.getUuid());
		if (rental == null || !rental.hasActiveRental()) {
			player.sendMessage(net.minecraft.text.Text.literal("§cYour rental has expired!"), true);
			RentalManager.expireRental(player);
			return false;
		}
		
		// Get 3x3 area
		List<BlockPos> blocksToMine = getAreaBlocks(player, targetPos, world);
		
		// Cleanup old entries (every 5 seconds)
		long now = System.currentTimeMillis();
		if (now - lastCleanup > 5000) {
			recentlyMined.clear();
			lastCleanup = now;
		}
		
		// Mine each block
		int minedCount = 0;
		for (BlockPos pos : blocksToMine) {
			// Skip if recently mined (prevent double-break)
			if (recentlyMined.contains(pos)) {
				continue;
			}
			
			BlockState state = world.getBlockState(pos);
			
			// Validate block can be mined
			if (!canMineBlock(state, tool, world, pos)) {
				continue;
			}
			
			// Mine block with rental tool stats
			boolean success = mineBlockWithRentalStats(player, world, pos, state, rental, tool);
			
			if (success) {
				recentlyMined.add(pos);
				minedCount++;
			}
		}
		
		FocusTimerShop.LOGGER.debug("Player {} mined {} blocks with rental tool", 
			player.getName().getString(), minedCount);
		
		return minedCount > 0;
	}
	
	/**
	 * Get 3x3 area blocks based on player look direction
	 */
	public static List<BlockPos> getAreaBlocks(ServerPlayerEntity player, BlockPos center, World world) {
		List<BlockPos> blocks = new ArrayList<>();
		
		// Get face being mined
		HitResult hit = player.raycast(5.0, 0, false);
		if (!(hit instanceof BlockHitResult blockHit)) {
			// Fallback: just center
			blocks.add(center);
			return blocks;
		}
		
		Direction face = blockHit.getSide();
		
		// Calculate 3x3 grid perpendicular to hit face
		Direction.Axis axis = face.getAxis();
		
		int[] offsets = {-1, 0, 1};
		
		for (int dx : offsets) {
			for (int dy : offsets) {
				BlockPos pos;
				
				switch (axis) {
					case X -> pos = center.add(0, dy, dx);
					case Y -> pos = center.add(dx, 0, dy);
					case Z -> pos = center.add(dx, dy, 0);
					default -> pos = center;
				}
				
				blocks.add(pos);
			}
		}
		
		return blocks;
	}
	
	/**
	 * Check if block can be mined with this tool
	 */
	private static boolean canMineBlock(BlockState state, ItemStack tool, World world, BlockPos pos) {
		// Skip air
		if (state.isAir()) {
			return false;
		}
		
		// Skip bedrock and other indestructible blocks
		if (state.getHardness(world, pos) < 0) {
			return false;
		}
		
		// Check if tool is effective (pickaxe should mine stone-like blocks)
		if (!tool.isSuitableFor(state)) {
			return false;
		}
		
		return true;
	}
	
	/**
	 * Mine block with rental tool fortune/efficiency applied
	 * Server handles drops with correct fortune level
	 */
	private static boolean mineBlockWithRentalStats(ServerPlayerEntity player, ServerWorld world, 
	                                                 BlockPos pos, BlockState state, 
	                                                 RentalData rental, ItemStack tool) {
		// Create temporary tool with rental enchantments for drop calculation
		ItemStack tempTool = tool.copy();
		
		// Add enchantments for drop calculation
		if (rental.getFortuneLevel() > 0) {
			tempTool.addEnchantment(Enchantments.FORTUNE, rental.getFortuneLevel());
		}
		if (rental.getUnbreakingLevel() > 0) {
			tempTool.addEnchantment(Enchantments.UNBREAKING, rental.getUnbreakingLevel());
		}
		if (rental.getEfficiencyLevel() > 0) {
			tempTool.addEnchantment(Enchantments.EFFICIENCY, rental.getEfficiencyLevel());
		}
		if (rental.hasMending()) {
			tempTool.addEnchantment(Enchantments.MENDING, 1);
		}
		
		// Break block and drop items (vanilla handles fortune)
		boolean broken = world.breakBlock(pos, true, player);
		
		if (broken) {
			// Damage tool slightly (rental tools have high unbreaking)
			tool.damage(1, player, (p) -> {});
		}
		
		return broken;
	}
	
	/**
	 * Clear recently mined cache
	 */
	public static void clearCache() {
		recentlyMined.clear();
	}
}
