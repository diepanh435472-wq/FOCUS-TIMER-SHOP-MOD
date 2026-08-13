package com.focustimershop.mixin;

import com.focustimershop.client.ClientDataCache;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Prevents all player interactions when timer is running
 */
@Mixin(ClientPlayerInteractionManager.class)
public class ClientPlayerInteractionManagerMixin {
	
	/**
	 * Prevent attacking entities
	 */
	@Inject(method = "attackEntity", at = @At("HEAD"), cancellable = true)
	private void onAttackEntity(PlayerEntity player, Entity target, CallbackInfo ci) {
		if (ClientDataCache.isGameFrozen()) {
			ci.cancel();
		}
	}
	
	/**
	 * Prevent breaking blocks
	 */
	@Inject(method = "attackBlock", at = @At("HEAD"), cancellable = true)
	private void onAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (ClientDataCache.isGameFrozen()) {
			cir.setReturnValue(false);
		}
	}
	
	/**
	 * Prevent continuing to break blocks
	 */
	@Inject(method = "updateBlockBreakingProgress", at = @At("HEAD"), cancellable = true)
	private void onUpdateBlockBreaking(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
		if (ClientDataCache.isGameFrozen()) {
			cir.setReturnValue(false);
		}
	}
	
	/**
	 * Prevent using items (right click)
	 */
	@Inject(method = "interactItem", at = @At("HEAD"), cancellable = true)
	private void onInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		if (ClientDataCache.isGameFrozen()) {
			cir.setReturnValue(ActionResult.FAIL);
		}
	}
	
	/**
	 * Prevent interacting with blocks (right click on block)
	 */
	@Inject(method = "interactBlock", at = @At("HEAD"), cancellable = true)
	private void onInteractBlock(PlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
		if (ClientDataCache.isGameFrozen()) {
			cir.setReturnValue(ActionResult.FAIL);
		}
	}
	
	/**
	 * Prevent interacting with entities (right click on entity)
	 */
	@Inject(method = "interactEntity", at = @At("HEAD"), cancellable = true)
	private void onInteractEntity(PlayerEntity player, Entity entity, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
		if (ClientDataCache.isGameFrozen()) {
			cir.setReturnValue(ActionResult.FAIL);
		}
	}
	
	/**
	 * Prevent picking blocks (middle click)
	 */
	@Inject(method = "pickFromInventory", at = @At("HEAD"), cancellable = true)
	private void onPickFromInventory(int slot, CallbackInfo ci) {
		if (ClientDataCache.isGameFrozen()) {
			ci.cancel();
		}
	}
}
