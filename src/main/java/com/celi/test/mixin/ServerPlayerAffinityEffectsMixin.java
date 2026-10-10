package com.celi.test.mixin;

import com.celi.test.magic.affinity.AffinityOwnedEffects;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerAffinityEffectsMixin {
    @Inject(method = "restoreFrom(Lnet/minecraft/server/level/ServerPlayer;Z)V", at = @At("HEAD"))
    private void celitest$clearAffinityEffectsBeforeCopy(ServerPlayer oldPlayer, boolean alive, CallbackInfo callback) {
        AffinityOwnedEffects.clear(oldPlayer);
    }
}
