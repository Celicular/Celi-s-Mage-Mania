package com.celi.test.mixin;

import com.celi.test.gameplay.RubyGameplay;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;)Z", at = @At("HEAD"), cancellable = true)
    private void celitest$denyDarkness(MobEffectInstance effect, CallbackInfoReturnable<Boolean> callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (effect.is(MobEffects.DARKNESS) && RubyGameplay.hasRubyHelmet(entity)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "forceAddEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V", at = @At("HEAD"), cancellable = true)
    private void celitest$denyForcedDarkness(MobEffectInstance effect, Entity source, CallbackInfo callback) {
        LivingEntity entity = (LivingEntity) (Object) this;
        if (effect.is(MobEffects.DARKNESS) && RubyGameplay.hasRubyHelmet(entity)) {
            callback.cancel();
        }
    }
}
