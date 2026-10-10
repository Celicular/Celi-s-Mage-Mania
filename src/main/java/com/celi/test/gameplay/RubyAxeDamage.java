package com.celi.test.gameplay;

import com.celi.test.CeliTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

public final class RubyAxeDamage {
    private static final ResourceKey<DamageType> RUBY_AXE_BURST = ResourceKey.create(Registries.DAMAGE_TYPE,
            CeliTest.id("ruby_axe_burst"));

    private RubyAxeDamage() {
    }

    public static boolean hurt(ServerLevel level, Entity projectile, Entity owner, LivingEntity target, float amount) {
        DamageSource source = new DamageSource(level.registryAccess().lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(RUBY_AXE_BURST), projectile, owner);
        float absorption = target.getAbsorptionAmount();
        try {
            target.setAbsorptionAmount(0.0F);
            return target.hurtServer(level, source, amount);
        } finally {
            if (target.getAbsorptionAmount() == 0.0F) {
                target.setAbsorptionAmount(absorption);
            }
        }
    }
}
