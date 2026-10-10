package com.celi.test.mixin;

import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinitySystem;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAffinityDamageMixin {
    @ModifyVariable(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), argsOnly = true, index = 3)
    private float celitest$reduceAffinityFireDamage(float amount, ServerLevel level, DamageSource source, float originalAmount) {
        return (Object) this instanceof ServerPlayer player
                && AffinitySystem.getAffinity(player) == Affinity.FIRE && source.is(DamageTypeTags.IS_FIRE)
                ? amount * 0.2F : amount;
    }

    @Inject(method = "hurtServer(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/damagesource/DamageSource;F)Z",
            at = @At("HEAD"), cancellable = true)
    private void celitest$cancelAffinityFallDamage(ServerLevel level, DamageSource source, float amount,
                                                 CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player
                && AffinitySystem.getAffinity(player) == Affinity.WIND && source.is(DamageTypeTags.IS_FALL)) {
            callback.setReturnValue(false);
        }
    }
}
