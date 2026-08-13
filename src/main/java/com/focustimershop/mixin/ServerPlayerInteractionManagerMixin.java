package com.focustimershop.mixin;

import com.focustimershop.rental.AreaMiningHandler;
import com.focustimershop.rental.RentalManager;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.network.ServerPlayerInteractionManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Intercept block breaking to handle rental tool 3x3 mining
 */
@Mixin(ServerPlayerInteractionManager.class)
public class ServerPlayerInteractionManagerMixin {
	
	@Shadow
	public ServerPlayerEntity player;
	
	@Shadow
	public ServerWorld world;
	
	/**
	 * Intercept block breaking to check for rental tool area mining
	 */
	@Inject(method = "tryBreakBlock", at = @At("HEAD"), cancellable = true)
	private void onTryBreakBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
		if (player == null || world == null) {
			return;
		}
		
		// Check if player is sneaking (shift) and has rental tool
		if (!player.isSneaking()) {
			return;
		}
		
		ItemStack mainHand = player.getMainHandStack();
		if (!RentalManager.isValidRentalTool(player, mainHand)) {
			return;
		}
		
		// Handle 3x3 area mining
		boolean handled = AreaMiningHandler.handleAreaMining(player, mainHand, pos, world);
		
		if (handled) {
			// Cancel normal block breaking (we already handled it)
			cir.setReturnValue(true);
		}
	}
}
