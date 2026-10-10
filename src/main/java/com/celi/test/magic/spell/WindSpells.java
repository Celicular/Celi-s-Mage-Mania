package com.celi.test.magic.spell;

import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinityPassives;
import com.celi.test.magic.affinity.AffinitySystem;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityLevelChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class WindSpells {
    private static final double LAUNCH_VELOCITY = 2.2D;
    private static final double MAX_CLEARANCE = 128.0D;
    private static final int SLOW_FALLING_TICKS = 200;
    private static final int PARTICLE_TICKS = 40;
    private static final int PARTICLE_INTERVAL = 4;
    private static final int RING_SPECKS = 8;
    private static final DustParticleOptions WHITE_WIND = new DustParticleOptions(0xF1FCFF, 0.65F);
    private static final DustParticleOptions CYAN_WIND = new DustParticleOptions(0x9DEFFF, 0.65F);
    private static final Map<UUID, Ascent> ASCENTS = new HashMap<>();

    private static boolean initialized;

    private WindSpells() {
    }

    public static void initialize() {
        if (initialized) {
            return;
        }
        ServerTickEvents.END_SERVER_TICK.register(WindSpells::tick);
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> ASCENTS.remove(handler.player.getUUID()));
        ServerEntityLevelChangeEvents.AFTER_PLAYER_CHANGE_LEVEL.register(
                (player, origin, destination) -> ASCENTS.remove(player.getUUID()));
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity instanceof ServerPlayer player) {
                ASCENTS.remove(player.getUUID());
            }
        });
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> ASCENTS.clear());
        initialized = true;
    }

    public static boolean castSkywardAscent(ServerPlayer player) {
        Objects.requireNonNull(player, "player");
        ServerLevel level = player.level();
        if (!level.getServer().isSameThread()) {
            throw new IllegalStateException("Skyward Ascent must be cast on the server thread");
        }
        if (!canAscend(player) || player.hasEffect(MobEffects.LEVITATION)) {
            return rejectUnsafe(player);
        }
        Vec3 pending = player.getDeltaMovement();
        Vec3 horizontal = player.needsSync || player.syncVelocity ? pending : player.getKnownMovement();
        double gravity = player.getGravity();
        double clearance = Math.ceil(LAUNCH_VELOCITY * LAUNCH_VELOCITY / (2.0D * gravity)
                + LAUNCH_VELOCITY + 1.0D);
        if (!Double.isFinite(gravity) || gravity <= 0.0D || !Double.isFinite(clearance)
                || clearance > MAX_CLEARANCE || !Double.isFinite(horizontal.x)
                || !Double.isFinite(horizontal.z) || Math.abs(horizontal.x) > 4.0D
                || Math.abs(horizontal.z) > 4.0D || !hasLaunchClearance(player, clearance, horizontal)) {
            return rejectUnsafe(player);
        }
        AffinityPassives.grantSlowFalling(player, SLOW_FALLING_TICKS);
        player.resetFallDistance();
        player.setOnGround(false);
        player.setDeltaMovement(horizontal.x, LAUNCH_VELOCITY, horizontal.z);
        level.getChunkSource().sendToTrackingPlayersAndSelf(player, new ClientboundSetEntityMotionPacket(player));
        Ascent ascent = new Ascent(player);
        ASCENTS.put(player.getUUID(), ascent);
        spawnSpiral(ascent);
        return true;
    }

    private static boolean canAscend(ServerPlayer player) {
        return player.connection != null && !player.hasDisconnected() && !player.isRemoved()
                && player.isAlive() && !player.isSpectator() && !player.isPassenger()
                && !player.isSleeping() && !player.getAbilities().flying && !player.isFallFlying()
                && !player.isSwimming() && !player.isInLiquid()
                && AffinitySystem.getAffinity(player) == Affinity.WIND;
    }

    private static boolean hasLaunchClearance(ServerPlayer player, double clearance, Vec3 horizontal) {
        ServerLevel level = player.level();
        AABB standing = player.getDimensions(Pose.STANDING).makeBoundingBox(player.position())
                .minmax(player.getBoundingBox());
        AABB swept = standing.expandTowards(horizontal.x, clearance, horizontal.z).inflate(0.1D, 0.0D, 0.1D);
        WorldBorder border = level.getWorldBorder();
        if (swept.hasNaN() || swept.minY < level.getMinY() || swept.maxY >= level.getMaxY() + 1.0D
                || swept.minX <= border.getMinX() || swept.maxX >= border.getMaxX()
                || swept.minZ <= border.getMinZ() || swept.maxZ >= border.getMaxZ()
                || !hasLoadedChunks(level, swept.inflate(1.0D, 0.0D, 1.0D))) {
            return false;
        }
        if (!level.noCollision(player, swept) || level.collidesWithSuffocatingBlock(player, swept)) {
            return false;
        }
        AABB hazardArea = swept.setMinY(Math.max(level.getMinY(), swept.minY - 0.05D));
        for (BlockPos position : BlockPos.betweenClosed(hazardArea)) {
            LevelChunk chunk = level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
            if (chunk == null || isHazard(chunk.getBlockState(position))) {
                return false;
            }
        }
        return true;
    }

    private static boolean hasLoadedChunks(ServerLevel level, AABB area) {
        int minChunkX = Mth.floor(area.minX) >> 4;
        int maxChunkX = Mth.floor(area.maxX) >> 4;
        int minChunkZ = Mth.floor(area.minZ) >> 4;
        int maxChunkZ = Mth.floor(area.maxZ) >> 4;
        for (int chunkX = minChunkX; chunkX <= maxChunkX; chunkX++) {
            for (int chunkZ = minChunkZ; chunkZ <= maxChunkZ; chunkZ++) {
                if (level.getChunkSource().getChunkNow(chunkX, chunkZ) == null) {
                    return false;
                }
            }
        }
        return true;
    }

    private static boolean isHazard(BlockState state) {
        return !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE) || state.is(BlockTags.PORTALS)
                || state.is(BlockTags.DANGEROUS_FOR_TELEPORTATION)
                || state.is(Blocks.CACTUS) || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.COBWEB)
                || CampfireBlock.isLitCampfire(state);
    }

    private static boolean rejectUnsafe(ServerPlayer player) {
        if (player.connection != null && !player.hasDisconnected()) {
            player.sendOverlayMessage(Component.translatable("message.celitest.staff.unsafe"));
        }
        return false;
    }

    private static void tick(MinecraftServer server) {
        boolean gameTick = !server.isPaused() && server.tickRateManager().runsNormally();
        Iterator<Ascent> iterator = ASCENTS.values().iterator();
        while (iterator.hasNext()) {
            Ascent ascent = iterator.next();
            ServerPlayer player = ascent.player;
            if (!canAscend(player) || player.level() != ascent.level
                    || server.getPlayerList().getPlayer(player.getUUID()) != player
                    || player.onGround()) {
                iterator.remove();
                continue;
            }
            if (!gameTick) {
                ascent.previousY = player.getY();
                continue;
            }
            ascent.age++;
            if (ascent.age >= PARTICLE_TICKS) {
                iterator.remove();
                continue;
            }
            if (ascent.age % PARTICLE_INTERVAL == 0 && player.getY() > ascent.previousY + 0.001D) {
                spawnSpiral(ascent);
            }
            ascent.previousY = player.getY();
        }
    }

    private static void spawnSpiral(Ascent ascent) {
        ServerPlayer player = ascent.player;
        double phase = ascent.age * 0.32D;
        double rise = ascent.age % 12 / 12.0D;
        for (int ring = 0; ring < 2; ring++) {
            double radius = 1.1D + ring * 0.35D;
            double y = player.getY() + 0.2D + ring * 0.85D + rise;
            for (int speck = 0; speck < RING_SPECKS; speck++) {
                double angle = phase + ring * Math.PI / RING_SPECKS + speck * Math.PI * 2.0D / RING_SPECKS;
                double cos = Math.cos(angle);
                double sin = Math.sin(angle);
                DustParticleOptions particle = (speck + ring) % 2 == 0 ? WHITE_WIND : CYAN_WIND;
                ascent.level.sendParticles(particle, player.getX() + cos * radius, y, player.getZ() + sin * radius,
                        0, -sin * 0.025D, 0.06D, cos * 0.025D, 1.0D);
            }
        }
        if (ascent.age % 8 == 0) {
            ascent.level.sendParticles(ParticleTypes.SMALL_GUST, player.getX(), player.getY() + 0.7D, player.getZ(),
                    0, 0.0D, 0.04D, 0.0D, 1.0D);
        }
    }

    private static final class Ascent {
        private final ServerPlayer player;
        private final ServerLevel level;
        private int age;
        private double previousY;

        private Ascent(ServerPlayer player) {
            this.player = player;
            this.level = player.level();
            this.previousY = player.getY();
        }
    }
}
