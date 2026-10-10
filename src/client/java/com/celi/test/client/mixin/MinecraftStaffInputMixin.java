package com.celi.test.client.mixin;

import com.celi.test.client.magic.StaffClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Minecraft.class)
public abstract class MinecraftStaffInputMixin {
	@Shadow
	protected int missTime;

	@Inject(method = "startAttack()Z", at = @At("HEAD"), cancellable = true)
	private void celitest$castStaffOnAttack(CallbackInfoReturnable<Boolean> info) {
		if (StaffClient.handleAttack((Minecraft) (Object) this, true)) {
			missTime = 0;
			info.setReturnValue(false);
		}
	}

	@Inject(method = "continueAttack(Z)V", at = @At("HEAD"), cancellable = true)
	private void celitest$castStaffWhileHeld(boolean attacking, CallbackInfo info) {
		if (StaffClient.handleAttack((Minecraft) (Object) this, attacking)) {
			missTime = 0;
			info.cancel();
		}
	}

	@Redirect(method = "handleKeybinds()V", at = @At(value = "INVOKE",
			target = "Lnet/minecraft/client/player/LocalPlayer;isUsingItem()Z"), require = 2)
	private boolean celitest$separateStaffRaiseFromHeldUse(LocalPlayer player) {
		return StaffClient.isUsingItemForInput(player);
	}

	@Inject(method = "startUseItem()V", at = @At("HEAD"))
	private void celitest$handStaffRaiseToVanillaUse(CallbackInfo info) {
		StaffClient.beginVanillaUse((Minecraft) (Object) this);
	}
}
