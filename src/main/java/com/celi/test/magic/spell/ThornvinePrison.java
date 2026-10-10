package com.celi.test.magic.spell;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.network.protocol.game.ClientboundTeleportEntityPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class ThornvinePrison {
    private static final double CAPTURE_RADIUS = 6.0D;
    private static final double CAPTURE_RADIUS_SQUARED = CAPTURE_RADIUS * CAPTURE_RADIUS;
    private static final int DURATION_TICKS = 200;
    private static final int DAMAGE_INTERVAL_TICKS = 20;
    private static final float DAMAGE = 2.0F;
    private static final float HEALING_RATIO = 0.25F;
    private static final DustParticleOptions THORN_GREEN = new DustParticleOptions(0x3D6B38, 0.75F);
    private static final DustParticleOptions THORN_BROWN = new DustParticleOptions(0x795331, 0.72F);
    private static final BlockParticleOption VINE_DEBRIS = new BlockParticleOption(
            ParticleTypes.BLOCK_CRUMBLE, Blocks.VINE.defaultBlockState());
    private static final BlockParticleOption WOOD_DEBRIS = new BlockParticleOption(
            ParticleTypes.BLOCK_CRUMBLE, Blocks.OAK_LOG.defaultBlockState());
    private static final Map<UUID, Prison> PRISONS = new HashMap<>();
    private static final Map<LivingEntity, CapturedTarget> CAPTURED_TARGETS = new IdentityHashMap<>();
    private static boolean initialized;

    private ThornvinePrison() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        initialized = true;
        ServerTickEvents.START_SERVER_TICK.register(ThornvinePrison::maintain);
        ServerTickEvents.END_SERVER_TICK.register(ThornvinePrison::tick);
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            removeTarget(entity, false);
            if (entity instanceof ServerPlayer player) {
                cancel(player);
            }
        });
        ServerEntityEvents.ENTITY_UNLOAD.register((entity, level) -> {
            removeTarget(entity, true);
            if (entity instanceof ServerPlayer player) {
                cancel(player);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> cancel(handler.player));
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> cancel(oldPlayer));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> cancel(player));
        ServerLifecycleEvents.SERVER_STOPPING.register(ThornvinePrison::stop);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> clear());
    }

    public static boolean cast(ServerPlayer caster) {
        Objects.requireNonNull(caster, "caster");
        requireServerThread(caster);
        if (!isConnectedAndAlive(caster) || PRISONS.containsKey(caster.getUUID())) {
            return false;
        }

        ServerLevel level = caster.level();
        AABB bounds = caster.getBoundingBox().inflate(CAPTURE_RADIUS);
        List<CapturedTarget> targets = new ArrayList<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                candidate -> candidate != caster
                        && candidate.distanceToSqr(caster) <= CAPTURE_RADIUS_SQUARED
                        && StaffCombat.isValidTarget(caster, candidate)
                        && !CAPTURED_TARGETS.containsKey(candidate))) {
            CapturedTarget captured = capture(target);
            if (captured != null) {
                targets.add(captured);
            }
        }
        if (targets.isEmpty()) {
            return false;
        }

        Prison prison = new Prison(caster, level, targets);
        PRISONS.put(caster.getUUID(), prison);
        for (CapturedTarget target : targets) {
            renderEmergence(target);
        }
        return true;
    }

    private static CapturedTarget capture(LivingEntity target) {
        if (target.isRemoved() || !target.isAlive() || CAPTURED_TARGETS.containsKey(target)) {
            return null;
        }
        Vec3 anchor = target.position();
        if (!anchor.isFinite()) {
            return null;
        }
        AABB box = target.getBoundingBox();
        Vec3 movement = target.getDeltaMovement().isFinite() ? target.getDeltaMovement() : Vec3.ZERO;
        CapturedTarget captured = new CapturedTarget(target, anchor, movement,
                target.isNoGravity(), target.onGround(), Math.clamp(box.getYsize(), 0.5D, 3.5D),
                Math.clamp(Math.max(box.getXsize(), box.getZsize()) * 0.5D, 0.25D, 1.2D));
        CAPTURED_TARGETS.put(target, captured);
        immobilize(captured);
        return captured;
    }

    private static void maintain(MinecraftServer server) {
        for (Prison prison : List.copyOf(PRISONS.values())) {
            if (prison.level.getServer() != server || !isConnectedAndAlive(prison.caster)
                    || prison.caster.level() != prison.level
                    || server.getPlayerList().getPlayer(prison.caster.getUUID()) != prison.caster) {
                cancel(prison.caster);
                continue;
            }
            for (CapturedTarget target : List.copyOf(prison.targets)) {
                if (!isActiveTarget(prison, target)) {
                    prison.remove(target, true);
                } else {
                    immobilize(target);
                }
            }
        }
    }

    private static void tick(MinecraftServer server) {
        boolean gameTick = !server.isPaused() && server.tickRateManager().runsNormally();
        for (Prison prison : List.copyOf(PRISONS.values())) {
            if (prison.level.getServer() != server) {
                continue;
            }
            if (prison.targets.isEmpty()) {
                finish(prison, false);
                continue;
            }
            if (!gameTick) {
                continue;
            }
            prison.age++;
            if (prison.age % 4 == 0) {
                for (CapturedTarget target : List.copyOf(prison.targets)) {
                    if (isActiveTarget(prison, target)) {
                        renderBinding(target, prison.age);
                    }
                }
            }
            if (prison.age % DAMAGE_INTERVAL_TICKS == 0) {
                for (CapturedTarget target : List.copyOf(prison.targets)) {
                    damage(prison, target);
                }
            }
            for (CapturedTarget target : List.copyOf(prison.targets)) {
                if (isActiveTarget(prison, target)) {
                    immobilize(target);
                }
            }
            if (prison.age >= DURATION_TICKS) {
                finish(prison, true);
            }
        }
    }

    private static void damage(Prison prison, CapturedTarget captured) {
        if (!isActiveTarget(prison, captured) || captured.lastDamageAge == prison.age) {
            return;
        }
        captured.lastDamageAge = prison.age;
        float healthBefore = captured.entity.getHealth();
        if (!StaffCombat.damage(prison.caster, captured.entity, DAMAGE)) {
            return;
        }
        float actualDamage = Math.max(0.0F, healthBefore - captured.entity.getHealth());
        if (actualDamage > 0.0F && isConnectedAndAlive(prison.caster)) {
            prison.caster.heal(actualDamage * HEALING_RATIO);
        }
    }

    private static void immobilize(CapturedTarget captured) {
        LivingEntity target = captured.entity;
        if (target.isRemoved()) {
            return;
        }
        target.setNoGravity(true);
        Vec3 movement = target.getDeltaMovement();
        target.setDeltaMovement(Vec3.ZERO);
        target.setOnGround(captured.onGround);
        if (target.position().distanceToSqr(captured.anchor) > 0.000001D) {
            target.setPos(captured.anchor);
            target.syncPosition = true;
            if (target instanceof ServerPlayer player && !player.hasDisconnected()) {
                player.connection.send(new ClientboundTeleportEntityPacket(player.getId(),
                        new PositionMoveRotation(captured.anchor, Vec3.ZERO, player.getYRot(), player.getXRot()),
                        Set.of(), player.onGround()));
            }
        }
        target.syncVelocity = true;
        if (movement.lengthSqr() > 0.000001D && target instanceof ServerPlayer player && !player.hasDisconnected()) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private static boolean isActiveTarget(Prison prison, CapturedTarget captured) {
        LivingEntity target = captured.entity;
        return CAPTURED_TARGETS.get(target) == captured && target.level() == prison.level
                && !target.isRemoved() && target.isAlive();
    }

    private static void removeTarget(net.minecraft.world.entity.Entity entity, boolean restore) {
        if (!(entity instanceof LivingEntity target)) {
            return;
        }
        CapturedTarget captured = CAPTURED_TARGETS.get(target);
        if (captured == null) {
            return;
        }
        Prison prison = captured.prison;
        if (prison != null) {
            prison.remove(captured, restore);
        } else {
            CAPTURED_TARGETS.remove(target);
        }
    }

    public static void cancel(ServerPlayer caster) {
        Prison prison = PRISONS.get(caster.getUUID());
        if (prison != null && prison.caster == caster) {
            finish(prison, false);
        }
    }

    private static void finish(Prison prison, boolean retract) {
        if (PRISONS.get(prison.caster.getUUID()) != prison) {
            return;
        }
        PRISONS.remove(prison.caster.getUUID());
        for (CapturedTarget target : List.copyOf(prison.targets)) {
            if (retract && isActiveTarget(prison, target)) {
                renderRetraction(target);
            }
            prison.remove(target, true);
        }
        prison.targets.clear();
    }

    private static void stop(MinecraftServer server) {
        for (Prison prison : List.copyOf(PRISONS.values())) {
            if (prison.level.getServer() == server) {
                finish(prison, false);
            }
        }
    }

    private static void clear() {
        for (Prison prison : List.copyOf(PRISONS.values())) {
            finish(prison, false);
        }
        PRISONS.clear();
        CAPTURED_TARGETS.clear();
    }

    private static void renderEmergence(CapturedTarget target) {
        ServerLevel level = (ServerLevel) target.entity.level();
        Vec3 center = target.anchor;
        for (int index = 0; index < 8; index++) {
            double angle = index * Math.PI * 2.0D / 8.0D;
            double radius = target.radius * 0.65D;
            level.sendParticles(index % 2 == 0 ? THORN_GREEN : THORN_BROWN,
                    center.x + Math.cos(angle) * radius, center.y + 0.08D, center.z + Math.sin(angle) * radius,
                    1, 0.02D, 0.05D, 0.02D, 0.015D);
        }
        level.sendParticles(VINE_DEBRIS, center.x, center.y + 0.12D, center.z,
                3, target.radius * 0.35D, 0.08D, target.radius * 0.35D, 0.02D);
    }

    private static void renderBinding(CapturedTarget target, int age) {
        if (!(target.entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = target.anchor;
        double phase = age * 0.23D;
        for (int index = 0; index < 4; index++) {
            double angle = phase + index * Math.PI * 2.0D / 4.0D;
            double y = center.y + 0.18D + target.height * ((index + age / 4) % 4) / 4.0D;
            double radius = target.radius * (0.65D + 0.12D * Math.sin(phase + index));
            level.sendParticles(index % 2 == 0 ? THORN_GREEN : THORN_BROWN,
                    center.x + Math.cos(angle) * radius, y, center.z + Math.sin(angle) * radius,
                    1, 0.015D, 0.06D, 0.015D, 0.01D);
        }
        if (age % 8 == 0) {
            level.sendParticles(VINE_DEBRIS, center.x, center.y + target.height * 0.5D, center.z,
                    1, target.radius * 0.45D, target.height * 0.25D, target.radius * 0.45D, 0.01D);
        }
    }

    private static void renderRetraction(CapturedTarget target) {
        if (!(target.entity.level() instanceof ServerLevel level)) {
            return;
        }
        Vec3 center = target.anchor;
        for (int index = 0; index < 8; index++) {
            double angle = index * Math.PI * 2.0D / 8.0D;
            double radius = target.radius * (0.55D + (index % 2) * 0.2D);
            level.sendParticles(index % 2 == 0 ? THORN_BROWN : THORN_GREEN,
                    center.x + Math.cos(angle) * radius, center.y + target.height * 0.8D,
                    center.z + Math.sin(angle) * radius, 1, 0.02D, 0.08D, 0.02D, -0.02D);
        }
        level.sendParticles(WOOD_DEBRIS, center.x, center.y + target.height * 0.75D, center.z,
                3, target.radius * 0.35D, 0.12D, target.radius * 0.35D, -0.02D);
    }

    private static void requireServerThread(ServerPlayer player) {
        if (!player.level().getServer().isSameThread()) {
            throw new IllegalStateException("Thornvine Prison must be cast on the server thread");
        }
    }

    private static boolean isConnectedAndAlive(ServerPlayer player) {
        return player.connection != null && !player.hasDisconnected() && !player.isRemoved()
                && player.isAlive() && !player.isSpectator() && !player.isChangingDimension();
    }

    private static final class Prison {
        private final ServerPlayer caster;
        private final ServerLevel level;
        private final List<CapturedTarget> targets;
        private int age;

        private Prison(ServerPlayer caster, ServerLevel level, List<CapturedTarget> targets) {
            this.caster = caster;
            this.level = level;
            this.targets = targets;
            for (CapturedTarget target : targets) {
                target.prison = this;
            }
        }

        private void remove(CapturedTarget target, boolean restore) {
            if (!targets.remove(target)) {
                return;
            }
            CAPTURED_TARGETS.remove(target.entity, target);
            if (restore && !target.entity.isRemoved() && target.entity.isAlive()) {
                target.entity.setNoGravity(target.noGravity);
                target.entity.setDeltaMovement(target.movement);
                target.entity.setOnGround(target.onGround);
                target.entity.syncVelocity = true;
                if (target.entity instanceof ServerPlayer player && !player.hasDisconnected()) {
                    player.connection.send(new ClientboundSetEntityMotionPacket(player));
                }
            }
        }
    }

    private static final class CapturedTarget {
        private final LivingEntity entity;
        private final Vec3 anchor;
        private final Vec3 movement;
        private final boolean noGravity;
        private final boolean onGround;
        private final double height;
        private final double radius;
        private int lastDamageAge = -1;
        private Prison prison;

        private CapturedTarget(LivingEntity entity, Vec3 anchor, Vec3 movement, boolean noGravity,
                               boolean onGround, double height, double radius) {
            this.entity = entity;
            this.anchor = anchor;
            this.movement = movement;
            this.noGravity = noGravity;
            this.onGround = onGround;
            this.height = height;
            this.radius = radius;
        }
    }
}
