package com.focustimershop.mixin;

import com.focustimershop.util.IEntityDataSaver;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Entity.class)
public abstract class EntityDataSaverMixin implements IEntityDataSaver {
	@Unique
	private NbtCompound focustimershop$persistentData;

	@Override
	public NbtCompound focustimershop$getPersistentData() {
		if (this.focustimershop$persistentData == null) {
			this.focustimershop$persistentData = new NbtCompound();
		}
		return this.focustimershop$persistentData;
	}

	@Inject(method = "writeNbt", at = @At("HEAD"))
	protected void injectWriteMethod(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> info) {
		if (this.focustimershop$persistentData != null) {
			nbt.put("FocusTimerShopData", this.focustimershop$persistentData);
		}
	}

	@Inject(method = "readNbt", at = @At("HEAD"))
	protected void injectReadMethod(NbtCompound nbt, CallbackInfo info) {
		if (nbt.contains("FocusTimerShopData", 10)) {
			this.focustimershop$persistentData = nbt.getCompound("FocusTimerShopData");
		}
	}
}
