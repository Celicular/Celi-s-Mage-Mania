package com.celi.test.mixin;

import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinitySystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Entity.class)
public abstract class EntityAffinityLavaMixin {
    @Redirect(method = "lavaIgnite()V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;igniteForSeconds(F)V"))
    private void celitest$suppressAffinityLavaIgnition(Entity entity, float seconds) {
        if (!(entity instanceof ServerPlayer player) || AffinitySystem.getAffinity(player) != Affinity.FIRE) {
            entity.igniteForSeconds(seconds);
        }
    }
}
