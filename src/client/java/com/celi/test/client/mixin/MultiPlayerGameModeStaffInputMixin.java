package com.celi.test.client.mixin;

import com.celi.test.client.magic.StaffClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeStaffInputMixin {
	@Shadow
	@Final
	private Minecraft minecraft;

	@Inject(method = "useItemOn(Lnet/minecraft/client/player/LocalPlayer;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)Lnet/minecraft/world/InteractionResult;",
			at = @At("HEAD"), cancellable = true)
	private void celitest$useStaffInsteadOfBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hit,
			CallbackInfoReturnable<InteractionResult> info) {
		if (player == minecraft.player && hand == InteractionHand.MAIN_HAND && StaffClient.isHoldingStaff(minecraft)) {
			info.setReturnValue(InteractionResult.PASS);
		}
	}

	@Inject(method = "interact(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/entity/Entity;Lnet/minecraft/world/phys/EntityHitResult;Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/InteractionResult;",
			at = @At("HEAD"), cancellable = true)
	private void celitest$useStaffInsteadOfEntity(Player player, Entity entity, EntityHitResult hit,
			InteractionHand hand, CallbackInfoReturnable<InteractionResult> info) {
		if (player == minecraft.player && hand == InteractionHand.MAIN_HAND && StaffClient.isHoldingStaff(minecraft)) {
			info.setReturnValue(InteractionResult.PASS);
		}
	}
}
