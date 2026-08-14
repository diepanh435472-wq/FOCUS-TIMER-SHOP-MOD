package com.focustimershop.rental;

import com.focustimershop.FocusTimerShop;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles 3x3 area mining for rental pickaxes and shovels
 * Only activates when player is sneaking (shift)
 */
public class AreaMiningHandler {
	
	public static void register() {
		PlayerBlockBreakEvents.AFTER.register(AreaMiningHandler::onBlockBreak);
	}
	
	private static void onBlockBreak(World world, PlayerEntity player, BlockPos pos, BlockState state, net.minecraft.block.entity.BlockEntity blockEntity) {
		// Only handle server-side
		if (world.isClient) {
			return;
		}
		
		// Only handle if player is sneaking
		if (!player.isSneaking()) {
			return;
		}
		
		// Get the tool used
		ItemStack tool = player.getMainHandStack();
		if (tool.isEmpty() || !tool.hasNbt()) {
			return;
		}
		
		// Check if it's a rental tool
		NbtCompound nbt = tool.getNbt();
		if (!nbt.contains("RentalType")) {
			return;
		}
		
		String rentalType = nbt.getString("RentalType");
		
		// Handle different tool types
		ServerWorld serverWorld = (ServerWorld) world;
		ServerPlayerEntity serverPlayer = (ServerPlayerEntity) player;
		int brokenCount = 0;
		
		if (rentalType.equals("AXE")) {
			// Tree chopper - chain reaction for wood blocks
			brokenCount = handleTreeChopping(serverWorld, serverPlayer, pos, state, tool);
		} else if (rentalType.equals("PICKAXE") || rentalType.equals("SHOVEL")) {
			// 3x3 area mining
			brokenCount = handle3x3Mining(serverWorld, serverPlayer, pos, state, tool, rentalType);
		}
		
		// Track stats if any blocks were broken
		if (brokenCount > 0) {
			trackBlocksMinedStats(serverPlayer, brokenCount);
			FocusTimerShop.LOGGER.debug("Player {} mined: {} blocks broken", 
				player.getName().getString(), brokenCount);
		}
	}
	
	/**
	 * Handle tree chopping - chain reaction breaking adjacent wood blocks
	 */
	private static int handleTreeChopping(ServerWorld world, ServerPlayerEntity player, 
	                                       BlockPos startPos, BlockState startState, ItemStack tool) {
		// Only chop wood/log blocks
		if (!isWoodBlock(startState)) {
			return 0;
		}
		
		List<BlockPos> toBreak = new ArrayList<>();
		List<BlockPos> visited = new ArrayList<>();
		toBreak.add(startPos);
		
		int brokenCount = 0;
		
		// BFS to find all connected wood blocks
		while (!toBreak.isEmpty() && brokenCount < 200) { // Limit to 200 blocks
			BlockPos pos = toBreak.remove(0);
			
			if (visited.contains(pos)) {
				continue;
			}
			visited.add(pos);
			
			// Skip if already broken or not wood
			BlockState state = world.getBlockState(pos);
			if (state.isAir() || !isWoodBlock(state)) {
				continue;
			}
			
			// Check tool durability
			if (tool.getDamage() >= tool.getMaxDamage() - 1) {
				break;
			}
			
			// Break the block (skip first block as it's already broken)
			if (!pos.equals(startPos)) {
				boolean broken = world.breakBlock(pos, true, player);
				if (broken) {
					brokenCount++;
					tool.damage(1, player, (p) -> {
						p.sendToolBreakStatus(p.getActiveHand());
					});
				}
			}
			
			// Add adjacent blocks to check
			for (Direction dir : Direction.values()) {
				BlockPos adjacent = pos.offset(dir);
				if (!visited.contains(adjacent)) {
					toBreak.add(adjacent);
				}
			}
		}
		
		return brokenCount;
	}
	
	/**
	 * Check if block is a wood/log block
	 */
	private static boolean isWoodBlock(BlockState state) {
		Block block = state.getBlock();
		return state.isIn(net.minecraft.registry.tag.BlockTags.LOGS) || 
		       state.isIn(net.minecraft.registry.tag.BlockTags.LOGS_THAT_BURN);
	}
	
	/**
	 * Handle 3x3 area mining for pickaxe and shovel
	 */
	private static int handle3x3Mining(ServerWorld world, ServerPlayerEntity player,
	                                    BlockPos pos, BlockState state, ItemStack tool, String rentalType) {
		// Check if tool is valid for this material
		if (!isValidToolForBlock(tool, state)) {
			return 0;
		}
		
		// Get 3x3 area around broken block
		List<BlockPos> blocksToBreak = get3x3Area(pos, player);
		
		// Break all blocks in area
		int brokenCount = 0;
		for (BlockPos targetPos : blocksToBreak) {
			// Don't break the original block again
			if (targetPos.equals(pos)) {
				continue;
			}
			
			BlockState targetState = world.getBlockState(targetPos);
			
			// Skip air and invalid blocks
			if (targetState.isAir() || targetState.getBlock() == Blocks.BEDROCK) {
				continue;
			}
			
			// Check if tool is effective against this block
			if (!isValidToolForBlock(tool, targetState)) {
				continue;
			}
			
			// Check tool durability
			if (tool.getDamage() >= tool.getMaxDamage() - 1) {
				// Tool about to break - stop mining
				break;
			}
			
			// Break the block
			boolean broken = world.breakBlock(targetPos, true, player);
			
			if (broken) {
				brokenCount++;
				
				// Damage the tool (1 durability per block)
				tool.damage(1, player, (p) -> {
					p.sendToolBreakStatus(p.getActiveHand());
				});
			}
		}
		
		return brokenCount;
	}
	
	/**
	 * Track blocks mined in player stats
	 */
	private static void trackBlocksMinedStats(ServerPlayerEntity player, int blocksCount) {
		try {
			com.focustimershop.database.PlayerStatsData stats = 
				com.focustimershop.database.DatabaseManager.getPlayerStats(player.getUuid());
			
			if (stats != null) {
				stats.addBlocksMined(blocksCount);
				com.focustimershop.database.DatabaseManager.savePlayerStats(stats);
			}
		} catch (Exception e) {
			FocusTimerShop.LOGGER.error("Failed to track blocks mined stats", e);
		}
	}
	
	/**
	 * Get 3x3 area of blocks around the broken block
	 * Area is perpendicular to player's facing direction
	 */
	private static List<BlockPos> get3x3Area(BlockPos center, PlayerEntity player) {
		List<BlockPos> blocks = new ArrayList<>();
		
		// Get player's facing direction
		Direction facing = player.getHorizontalFacing();
		
		// Determine the 3x3 plane based on facing direction
		// For vertical mining (looking up/down), use horizontal plane
		float pitch = player.getPitch();
		boolean isVertical = Math.abs(pitch) > 45.0f; // Looking up or down
		
		if (isVertical) {
			// Horizontal plane (XZ)
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					blocks.add(center.add(x, 0, z));
				}
			}
		} else {
			// Vertical plane perpendicular to facing direction
			switch (facing) {
				case NORTH:
				case SOUTH:
					// XY plane
					for (int x = -1; x <= 1; x++) {
						for (int y = -1; y <= 1; y++) {
							blocks.add(center.add(x, y, 0));
						}
					}
					break;
				case EAST:
				case WEST:
					// ZY plane
					for (int z = -1; z <= 1; z++) {
						for (int y = -1; y <= 1; y++) {
							blocks.add(center.add(0, y, z));
						}
					}
					break;
				default:
					break;
			}
		}
		
		return blocks;
	}
	
	/**
	 * Check if the tool is effective against the block
	 */
	private static boolean isValidToolForBlock(ItemStack tool, BlockState state) {
		// Get rental type from NBT
		if (!tool.hasNbt()) {
			return false;
		}
		
		NbtCompound nbt = tool.getNbt();
		if (!nbt.contains("RentalType")) {
			return false;
		}
		
		String rentalType = nbt.getString("RentalType");
		Block block = state.getBlock();
		
		// Check if pickaxe can mine stone/ore blocks
		if (rentalType.equals("PICKAXE")) {
			return state.isIn(net.minecraft.registry.tag.BlockTags.PICKAXE_MINEABLE);
		}
		
		// Check if shovel can mine dirt/sand blocks
		if (rentalType.equals("SHOVEL")) {
			return state.isIn(net.minecraft.registry.tag.BlockTags.SHOVEL_MINEABLE);
		}
		
		// Check if axe can chop wood blocks
		if (rentalType.equals("AXE")) {
			return state.isIn(net.minecraft.registry.tag.BlockTags.LOGS) ||
			       state.isIn(net.minecraft.registry.tag.BlockTags.LOGS_THAT_BURN);
		}
		
		return false;
	}
}
