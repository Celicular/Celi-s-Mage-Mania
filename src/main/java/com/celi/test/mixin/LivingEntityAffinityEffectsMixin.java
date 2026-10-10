package com.celi.test.mixin;

import com.celi.test.magic.affinity.AffinityEffectRemovalAccess;
import com.celi.test.magic.affinity.AffinityOwnedEffects;
import java.util.Collection;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityAffinityEffectsMixin implements AffinityEffectRemovalAccess {
    @Invoker("onEffectsRemoved")
    public abstract void celitest$notifyAffinityEffectRemoval(Collection<MobEffectInstance> effects);

    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;get(Ljava/lang/Object;)Ljava/lang/Object;"), cancellable = true)
    private void celitest$relinquishAffinityEffect(MobEffectInstance effect, Entity source,
                                                CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player && !AffinityOwnedEffects.beforeAdd(player, effect)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "forceAddEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/Map;put(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"),
            cancellable = true)
    private void celitest$relinquishForcedAffinityEffect(MobEffectInstance effect, Entity source, CallbackInfo callback) {
        if ((Object) this instanceof ServerPlayer player && !AffinityOwnedEffects.beforeAdd(player, effect)) {
            callback.cancel();
        }
    }

    @Inject(method = "removeEffectNoUpdate(Lnet/minecraft/core/Holder;)Lnet/minecraft/world/effect/MobEffectInstance;",
            at = @At("RETURN"))
    private void celitest$forgetRemovedAffinityEffect(Holder<MobEffect> effect,
                                                    CallbackInfoReturnable<MobEffectInstance> callback) {
        if ((Object) this instanceof ServerPlayer player) {
            AffinityOwnedEffects.afterRemoval(player, effect, callback.getReturnValue());
        }
    }

    @Inject(method = "removeAllEffects()Z", at = @At("RETURN"))
    private void celitest$cancelClearedAffinityWindow(CallbackInfoReturnable<Boolean> callback) {
        if ((Object) this instanceof ServerPlayer player) {
            AffinityOwnedEffects.afterRemoveAll(player);
        }
    }

    @ModifyArg(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V",
            at = @At(value = "INVOKE", target = "Ljava/util/List;copyOf(Ljava/util/Collection;)Ljava/util/List;"), index = 0)
    private Collection<MobEffectInstance> celitest$omitSessionOwnedAffinityEffects(Collection<MobEffectInstance> effects) {
        return (Object) this instanceof ServerPlayer player ? AffinityOwnedEffects.effectsToSave(player, effects) : effects;
    }
}
