package com.focustimershop.mixin;

import com.focustimershop.client.ClientDataCache;
import net.minecraft.client.network.ClientPlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents player actions that could bypass interaction manager
 */
@Mixin(ClientPlayerEntity.class)
public class ClientPlayerEntityMixin {
	
	/**
	 * Prevent opening inventory/crafting menu
	 */
	@Inject(method = "openRidingInventory", at = @At("HEAD"), cancellable = true)
	private void onOpenRidingInventory(CallbackInfo ci) {
		if (ClientDataCache.isGameFrozen()) {
			ci.cancel();
		}
	}
	
	/**
	 * Prevent dropping items (Q key)
	 */
	@Inject(method = "dropSelectedItem", at = @At("HEAD"), cancellable = true)
	private void onDropSelectedItem(boolean entireStack, org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable<Boolean> cir) {
		if (ClientDataCache.isGameFrozen()) {
			cir.setReturnValue(false);
		}
	}
}
