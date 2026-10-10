package com.celi.test.magic.spell;

import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinitySystem;
import com.celi.test.magic.affinity.StaffAbility;
import java.util.List;
import java.util.Objects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ThunderstormSpells {
    public static final double RADIUS = 10.0D;
    public static final int DURATION_TICKS = 200;
    public static final float DAMAGE = 16.0F;
    public static final int NAUSEA_TICKS = 100;

    private ThunderstormSpells() {
    }

    public static void initialize() {
        ScopedStormWeather.initialize();
    }

    public static void beginChannel(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        if (!caster.level().getServer().isSameThread()) {
            throw new IllegalStateException("Thunderstorm must be channeled on the server thread");
        }
        initialize();
        ScopedStormWeather.beginChannel(caster, caster.position(), StaffAbility.THUNDERSTORM.castTicks());
    }

    public static boolean cast(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        ServerLevel level = caster.level();
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Thunderstorm must be cast on the server thread");
        }
        if (caster.connection == null || caster.hasDisconnected() || caster.isRemoved() || !caster.isAlive()
                || caster.isSpectator()
                || AffinitySystem.getAffinity(caster) != Affinity.WATER) {
            return false;
        }
        initialize();
        Vec3 center = caster.position();
        ScopedStormWeather.acquire(caster, center, DURATION_TICKS);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
                SoundSource.WEATHER, 2.0F, 0.8F);
        AABB bounds = AABB.ofSize(center, RADIUS * 2.0D, RADIUS * 2.0D, RADIUS * 2.0D);
        for (LivingEntity target : List.copyOf(level.getEntitiesOfClass(LivingEntity.class, bounds,
                candidate -> candidate != caster && candidate.distanceToSqr(center) <= RADIUS * RADIUS
                        && StaffCombat.isValidTarget(caster, candidate)))) {
            LightningBolt lightning = new LightningBolt(EntityTypes.LIGHTNING_BOLT, level) {
                private int remainingTicks = 3;

                @Override
                public void tick() {
                    if (--remainingTicks <= 0) {
                        discard();
                    }
                }
            };
            lightning.setVisualOnly(true);
            lightning.setCause(caster);
            lightning.snapTo(target.position());
            level.addFreshEntity(lightning);
            if (StaffCombat.damage(caster, target, DAMAGE)) {
                target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS), caster);
            }
        }
        return true;
    }

    public static void cancel(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        if (!caster.level().getServer().isSameThread()) {
            throw new IllegalStateException("Thunderstorm must be canceled on the server thread");
        }
        ScopedStormWeather.cancel(caster);
    }
}
