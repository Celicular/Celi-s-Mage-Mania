package com.celi.test.magic.spell;

import com.celi.test.CeliTest;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class FireSpells {
    private static final double TARGET_RANGE = 45.0;
    private static final double RADIUS = 6.0;
    private static final float IGNITION_SECONDS = 8.0F;
    private static final DustParticleOptions ORANGE = new DustParticleOptions(0xFF761A, 1.4F);
    private static final DustParticleOptions RED = new DustParticleOptions(0xEF3421, 1.2F);
    private static final DustParticleOptions YELLOW = new DustParticleOptions(0xFFD347, 1.1F);
    private static MagicFlameBlock magicalFlame;

    private FireSpells() {
    }

    public static void initialize() {
        if (magicalFlame != null) {
            return;
        }
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, CeliTest.id("magical_flame"));
        magicalFlame = Registry.register(BuiltInRegistries.BLOCK, key, new MagicFlameBlock(
                BlockBehaviour.Properties.of()
                        .setId(key)
                        .mapColor(MapColor.FIRE)
                        .noCollision()
                        .noOcclusion()
                        .instabreak()
                        .lightLevel(state -> 15)
                        .sound(SoundType.WOOL)
                        .replaceable()
                        .pushReaction(PushReaction.POPPED)
                        .noLootTable()
                        .noTerrainParticles()
        ));
    }

    public static boolean castInferno(ServerPlayer player) {
        if (magicalFlame == null || !player.isAlive() || player.isRemoved() || player.isSpectator()) {
            return false;
        }
        ServerLevel level = player.level();
        Vec3 center = SpellTargeting.targetArea(player, TARGET_RANGE);
        if (center == null || !center.isFinite()) {
            return false;
        }
        BlockPos centerPos = BlockPos.containing(center);
        if (!level.isInWorldBounds(centerPos) || !hasLoadedChunk(level, centerPos)
                || !level.getWorldBorder().isWithinBounds(centerPos)) {
            return false;
        }
        AABB bounds = AABB.ofSize(center, RADIUS * 2.0, RADIUS * 2.0, RADIUS * 2.0);
        Set<BlockPos> placed = new HashSet<>();
        for (LivingEntity target : level.getEntitiesOfClass(LivingEntity.class, bounds,
                candidate -> candidate.distanceToSqr(center) <= RADIUS * RADIUS && StaffCombat.isValidTarget(player, candidate))) {
            if (!target.fireImmune()) {
                target.igniteForSeconds(IGNITION_SECONDS);
            }
            BlockPos feet = BlockPos.containing(target.getX(), target.getY() + 0.05, target.getZ());
            if (placed.add(feet)) {
                placeFlame(player, level, feet);
            }
            level.sendParticles(ParticleTypes.FLAME, target.getX(), target.getY() + 0.4, target.getZ(),
                    16, 0.4, 0.5, 0.4, 0.035);
        }
        showBurst(level, center);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.FIRECHARGE_USE, SoundSource.PLAYERS, 1.2F, 0.75F);
        return true;
    }

    private static void placeFlame(ServerPlayer player, ServerLevel level, BlockPos pos) {
        if (!level.isInWorldBounds(pos) || !level.getWorldBorder().isWithinBounds(pos)
                || !hasLoadedChunk(level, pos)
                || !level.mayInteract(player, pos)) {
            return;
        }
        BlockState state = level.getBlockState(pos);
        if (!state.isAir() || state.hasBlockEntity() || !state.getFluidState().isEmpty()) {
            return;
        }
        BlockState flame = magicalFlame.defaultBlockState();
        if (flame.canSurvive(level, pos)) {
            level.setBlock(pos, flame, Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }

    private static boolean hasLoadedChunk(ServerLevel level, BlockPos pos) {
        return level.getChunkSource().getChunkNow(pos.getX() >> 4, pos.getZ() >> 4) != null;
    }

    private static void showBurst(ServerLevel level, Vec3 center) {
        for (int i = 0; i < 72; i++) {
            double angle = Math.PI * 2.0 * i / 72.0;
            double x = center.x + Math.cos(angle) * RADIUS;
            double z = center.z + Math.sin(angle) * RADIUS;
            DustParticleOptions color = switch (i % 3) {
                case 0 -> ORANGE;
                case 1 -> RED;
                default -> YELLOW;
            };
            level.sendParticles(color, x, center.y + 0.15, z, 2, 0.08, 0.12, 0.08, 0.0);
            if (i % 3 == 0) {
                level.sendParticles(ParticleTypes.FLAME, x, center.y + 0.25, z, 1, 0.05, 0.15, 0.05, 0.025);
            }
        }
        level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.5, center.z, 70, 2.0, 0.8, 2.0, 0.06);
        level.sendParticles(ORANGE, center.x, center.y + 0.5, center.z, 32, 2.3, 0.9, 2.3, 0.0);
        level.sendParticles(RED, center.x, center.y + 0.4, center.z, 24, 1.8, 0.7, 1.8, 0.0);
        level.sendParticles(YELLOW, center.x, center.y + 0.65, center.z, 24, 1.4, 1.0, 1.4, 0.0);
    }
}
