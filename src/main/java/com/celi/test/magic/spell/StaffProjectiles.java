package com.celi.test.magic.spell;

import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinityPassives;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class StaffProjectiles {
    private static final double RANGE = 45.0D;
    private static final double SPEED = 1.5D;
    private static final double HIT_PADDING = 0.16D;
    private static final double PARTICLE_RANGE_SQUARED = 4096.0D;
    private static final int MAX_TICKS = 40;
    private static final int MAX_PROJECTILES = 256;
    private static final int MAX_PER_CASTER = 16;
    private static final DustParticleOptions FIRE_ORANGE = new DustParticleOptions(0xFF7A1A, 1.1F);
    private static final DustParticleOptions FIRE_RED = new DustParticleOptions(0xEF3526, 0.9F);
    private static final DustParticleOptions FIRE_YELLOW = new DustParticleOptions(0xFFE15A, 0.9F);
    private static final DustParticleOptions WATER_BLUE = new DustParticleOptions(0x218FEA, 1.1F);
    private static final DustParticleOptions WATER_CYAN = new DustParticleOptions(0x77E5FF, 0.8F);
    private static final DustParticleOptions WIND_WHITE = new DustParticleOptions(0xF4FFFF, 0.9F);
    private static final DustParticleOptions WIND_CYAN = new DustParticleOptions(0xB0EFF7, 0.8F);
    private static final DustParticleOptions EARTH_BROWN = new DustParticleOptions(0x8A5A36, 1.0F);
    private static final DustParticleOptions EARTH_TAN = new DustParticleOptions(0xC49A6C, 0.8F);
    private static final BlockParticleOption EARTH_STONE = new BlockParticleOption(
            ParticleTypes.BLOCK, Blocks.STONE.defaultBlockState());
    private static final List<Projectile> PROJECTILES = new ArrayList<>();
    private static boolean initialized;

    private StaffProjectiles() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.END_SERVER_TICK.register(StaffProjectiles::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.player));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                cancel(player);
            }
        });
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register((player, origin, destination) -> cancel(player));
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> PROJECTILES.clear());
    }

    public static void launch(ServerPlayer player, Affinity affinity) {
        if (affinity == null || affinity == Affinity.NONE || player.hasDisconnected() || player.isRemoved()
                || !player.isAlive() || player.isSpectator() || player.isChangingDimension()) {
            return;
        }

        Vec3 start = player.getEyePosition();
        Vec3 direction = player.getLookAngle().normalize();
        if (!SpellTargeting.isLoadedPosition(player.level(), start) || !direction.isFinite() || direction.lengthSqr() < 0.5D) {
            return;
        }

        long ownedCount = PROJECTILES.stream().filter(projectile -> projectile.caster.getUUID().equals(player.getUUID())).count();
        if (ownedCount >= MAX_PER_CASTER) {
            for (int index = 0; index < PROJECTILES.size(); index++) {
                if (PROJECTILES.get(index).caster.getUUID().equals(player.getUUID())) {
                    PROJECTILES.remove(index).finished = true;
                    break;
                }
            }
        }
        if (PROJECTILES.size() >= MAX_PROJECTILES) {
            PROJECTILES.removeFirst().finished = true;
        }

        float multiplier = AffinityPassives.staffDamageMultiplier(player);
        if (!Float.isFinite(multiplier) || multiplier <= 0.0F) {
            multiplier = 1.0F;
        }
        float damage = affinity == Affinity.EARTH
                ? 6.0F
                : (affinity == Affinity.FIRE ? 3.0F : 2.0F) * multiplier;
        Projectile projectile = new Projectile(player, affinity, start, direction, damage);
        PROJECTILES.add(projectile);
        renderCore(projectile);
    }

    private static void tick(MinecraftServer server) {
        for (Projectile projectile : List.copyOf(PROJECTILES)) {
            if (!projectile.advance(server)) {
                PROJECTILES.remove(projectile);
            }
        }
    }

    private static void cancel(ServerPlayer player) {
        PROJECTILES.removeIf(projectile -> {
            if (projectile.caster != player) {
                return false;
            }
            projectile.finished = true;
            return true;
        });
    }

    private static void renderFlight(Projectile projectile, Vec3 start, Vec3 end) {
        int samples = Math.max(1, (int) Math.ceil(start.distanceTo(end) / 0.5D));
        ParticleOptions trail = switch (projectile.affinity) {
            case FIRE -> FIRE_ORANGE;
            case WATER -> WATER_CYAN;
            case WIND -> WIND_CYAN;
            case EARTH -> EARTH_TAN;
            case NONE -> WIND_WHITE;
        };
        for (int sample = 1; sample <= samples; sample++) {
            Vec3 point = start.lerp(end, (double) sample / samples);
            particles(projectile.level, trail, point, 2, 0.035D, 0.0D);
        }
        renderCore(projectile);
    }

    private static void renderCore(Projectile projectile) {
        switch (projectile.affinity) {
            case FIRE -> {
                particles(projectile.level, FIRE_YELLOW, projectile.position, 6, 0.075D, 0.0D);
                particles(projectile.level, ParticleTypes.FLAME, projectile.position, 3, 0.1D, 0.005D);
            }
            case WATER -> {
                particles(projectile.level, WATER_BLUE, projectile.position, 8, 0.09D, 0.0D);
                particles(projectile.level, ParticleTypes.SPLASH, projectile.position, 3, 0.12D, 0.005D);
                renderOrbit(projectile, WATER_CYAN, WATER_BLUE, 0.18D);
            }
            case WIND -> {
                particles(projectile.level, WIND_WHITE, projectile.position, 4, 0.06D, 0.0D);
                renderOrbit(projectile, WIND_WHITE, WIND_CYAN, 0.27D);
            }
            case EARTH -> {
                particles(projectile.level, EARTH_BROWN, projectile.position, 6, 0.08D, 0.0D);
                particles(projectile.level, EARTH_STONE, projectile.position, 3, 0.1D, 0.015D);
            }
            case NONE -> {
            }
        }
    }

    private static void renderOrbit(Projectile projectile, ParticleOptions first, ParticleOptions second, double radius) {
        Vec3 reference = Math.abs(projectile.direction.y) > 0.9D ? Vec3.X_AXIS : Vec3.Y_AXIS;
        Vec3 side = projectile.direction.cross(reference).normalize();
        Vec3 up = side.cross(projectile.direction).normalize();
        for (int point = 0; point < 4; point++) {
            double angle = projectile.age * 0.9D + point * Math.PI / 2.0D;
            Vec3 orbit = projectile.position.add(side.scale(Math.cos(angle) * radius)).add(up.scale(Math.sin(angle) * radius));
            particles(projectile.level, point % 2 == 0 ? first : second, orbit, 1, 0.0D, 0.0D);
        }
    }

    private static void impact(Projectile projectile, HitResult hit) {
        if (hit instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target
                && StaffCombat.damage(projectile.caster, target, projectile.damage)) {
            switch (projectile.affinity) {
                case FIRE -> target.igniteForTicks(80);
                case WATER -> {
                    double strength = 1.7D - 1.4D * Math.clamp(projectile.travelled / RANGE, 0.0D, 1.0D);
                    Vec3 direction = backwardDirection(projectile, target);
                    StaffCombat.applyImpulse(target, direction.scale(strength).add(0.0D, 0.12D * strength, 0.0D));
                }
                case WIND -> StaffCombat.applyImpulse(target, backwardDirection(projectile, target).scale(0.9D).add(0.0D, 0.65D, 0.0D));
                case NONE -> {
                }
            }
        }

        switch (projectile.affinity) {
            case FIRE -> {
                particles(projectile.level, FIRE_ORANGE, projectile.position, 14, 0.3D, 0.04D);
                particles(projectile.level, FIRE_RED, projectile.position, 12, 0.25D, 0.03D);
                particles(projectile.level, FIRE_YELLOW, projectile.position, 12, 0.2D, 0.04D);
                particles(projectile.level, ParticleTypes.FLAME, projectile.position, 12, 0.25D, 0.06D);
            }
            case WATER -> {
                particles(projectile.level, ParticleTypes.SPLASH, projectile.position, 28, 0.3D, 0.12D);
                particles(projectile.level, WATER_CYAN, projectile.position, 14, 0.25D, 0.03D);
            }
            case WIND -> {
                particles(projectile.level, ParticleTypes.CLOUD, projectile.position, 12, 0.3D, 0.08D);
                particles(projectile.level, WIND_WHITE, projectile.position, 14, 0.35D, 0.04D);
                particles(projectile.level, WIND_CYAN, projectile.position, 12, 0.25D, 0.03D);
            }
            case EARTH -> {
                particles(projectile.level, EARTH_STONE, projectile.position, 18, 0.32D, 0.06D);
                particles(projectile.level, EARTH_BROWN, projectile.position, 18, 0.28D, 0.04D);
                particles(projectile.level, ParticleTypes.POOF, projectile.position, 12, 0.3D, 0.04D);
            }
            case NONE -> {
            }
        }
    }

    private static Vec3 backwardDirection(Projectile projectile, LivingEntity target) {
        Vec3 horizontal = projectile.direction.horizontal();
        if (horizontal.lengthSqr() < 0.000001D) {
            horizontal = target.position().subtract(projectile.caster.position()).horizontal();
        }
        if (horizontal.lengthSqr() < 0.000001D) {
            horizontal = Vec3.directionFromRotation(0.0F, projectile.caster.getYRot());
        }
        return horizontal.normalize();
    }

    private static void particles(ServerLevel level, ParticleOptions particle, Vec3 position, int count, double spread, double speed) {
        for (ServerPlayer viewer : level.players()) {
            if (!viewer.hasDisconnected() && viewer.distanceToSqr(position) <= PARTICLE_RANGE_SQUARED) {
                level.sendParticles(viewer, particle, true, true, position.x, position.y, position.z,
                        count, spread, spread, spread, speed);
            }
        }
    }

    private static final class Projectile {
        private final ServerPlayer caster;
        private final ServerLevel level;
        private final Affinity affinity;
        private final Vec3 direction;
        private final float damage;
        private Vec3 position;
        private double travelled;
        private int age;
        private boolean finished;

        private Projectile(ServerPlayer caster, Affinity affinity, Vec3 position, Vec3 direction, float damage) {
            this.caster = caster;
            this.level = caster.level();
            this.affinity = affinity;
            this.position = position;
            this.direction = direction;
            this.damage = damage;
        }

        private boolean advance(MinecraftServer server) {
            if (finished || level.getServer() != server || server.getPlayerList().getPlayer(caster.getUUID()) != caster
                    || caster.hasDisconnected() || caster.isRemoved() || !caster.isAlive() || caster.isSpectator()
                    || caster.level() != level || caster.isChangingDimension() || age >= MAX_TICKS
                    || travelled >= RANGE || !SpellTargeting.isLoadedPosition(level, position)) {
                finished = true;
                return false;
            }
            if (server.isPaused() || !level.tickRateManager().runsNormally()) {
                return true;
            }

            age++;
            Vec3 start = position;
            double step = Math.min(SPEED, RANGE - travelled);
            Vec3 end = start.add(direction.scale(step));
            HitResult hit = SpellTargeting.traceSegment(caster, start, end, HIT_PADDING,
                    entity -> entity.canBeHitByProjectile() && (!(entity instanceof LivingEntity target)
                            || StaffCombat.isValidTarget(caster, target)));
            position = hit.getLocation();
            travelled = Math.min(RANGE, travelled + (hit.getType() == HitResult.Type.MISS ? step : start.distanceTo(position)));
            renderFlight(this, start, position);
            if (hit.getType() != HitResult.Type.MISS) {
                finished = true;
                PROJECTILES.remove(this);
                impact(this, hit);
                return false;
            }
            if (travelled >= RANGE || age >= MAX_TICKS || !SpellTargeting.isLoadedPosition(level, position)) {
                finished = true;
                return false;
            }
            return true;
        }
    }
}
