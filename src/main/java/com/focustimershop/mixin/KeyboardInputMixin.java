package com.focustimershop.mixin;

import com.focustimershop.client.ClientDataCache;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Prevents keyboard input when timer is running
 */
@Mixin(KeyboardInput.class)
public class KeyboardInputMixin {
	
	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void onTick(boolean slowDown, float slowDownFactor, CallbackInfo ci) {
		if (ClientDataCache.isGameFrozen()) {
			KeyboardInput self = (KeyboardInput) (Object) this;
			self.pressingForward = false;
			self.pressingBack = false;
			self.pressingLeft = false;
			self.pressingRight = false;
			self.jumping = false;
			self.sneaking = false;
			self.movementForward = 0.0f;
			self.movementSideways = 0.0f;
			ci.cancel();
		}
	}
}
