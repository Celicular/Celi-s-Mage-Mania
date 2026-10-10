package com.celi.test.magic.spell;

import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class SpellTargeting {
    private SpellTargeting() {
    }

    public static Vec3 targetArea(ServerPlayer player, double range) {
        Vec3 start = player.getEyePosition();
        if (!Double.isFinite(range) || range <= 0.0D) {
            return start;
        }

        Vec3 end = start.add(player.getLookAngle().normalize().scale(range));
        return traceSegment(player, start, end, 0.0D,
                entity -> entity.isAlive() && !entity.isSpectator() && entity.isPickable()).getLocation();
    }

    public static HitResult traceSegment(ServerPlayer player, Vec3 start, Vec3 end, double entityPadding,
                                         Predicate<Entity> targetFilter) {
        ServerLevel level = player.level();
        if (!start.isFinite() || !end.isFinite() || !isLoadedPosition(level, start)) {
            Vec3 fallback = start.isFinite() ? start : player.getEyePosition();
            return new BlockHitResult(fallback, Direction.UP, BlockPos.containing(fallback), false);
        }

        Vec3 boundedEnd = worldBoundedEnd(level, start, end);
        Direction direction = Direction.getApproximateNearest(start.subtract(end));
        BlockHitResult blockHit = new LoadedBlockView(level).clip(new ClipContext(start, boundedEnd,
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        if (blockHit.getType() == HitResult.Type.MISS && !isWithinWorld(level, end)) {
            blockHit = new BlockHitResult(boundedEnd, direction, BlockPos.containing(boundedEnd), false).hitBorder();
        }

        HitResult nearest = blockHit;
        double nearestDistance = start.distanceToSqr(blockHit.getLocation());
        double padding = Double.isFinite(entityPadding) ? Math.max(0.0D, entityPadding) : 0.0D;
        AABB search = new AABB(start, blockHit.getLocation()).inflate(padding + 0.000001D);
        for (Entity entity : level.getEntities(player, search, candidate -> candidate != player
                && isLoadedPosition(level, candidate.position()) && targetFilter.test(candidate))) {
            AABB bounds = entity.getBoundingBox().inflate(padding);
            Vec3 point = bounds.contains(start) ? start : bounds.clip(start, blockHit.getLocation()).orElse(null);
            if (point == null) {
                continue;
            }

            double distance = start.distanceToSqr(point);
            if (distance < nearestDistance || (distance == nearestDistance && nearest.getType() == HitResult.Type.MISS)) {
                nearest = new EntityHitResult(entity, point);
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    public static boolean isLoadedPosition(ServerLevel level, Vec3 position) {
        if (!position.isFinite() || !isWithinWorld(level, position)) {
            return false;
        }
        BlockPos block = BlockPos.containing(position);
        return level.getChunkSource().getChunkNow(block.getX() >> 4, block.getZ() >> 4) != null;
    }

    private static boolean isWithinWorld(ServerLevel level, Vec3 position) {
        return position.isFinite() && level.isInWorldBounds(BlockPos.containing(position))
                && level.getWorldBorder().isWithinBounds(position);
    }

    private static Vec3 worldBoundedEnd(ServerLevel level, Vec3 start, Vec3 end) {
        WorldBorder border = level.getWorldBorder();
        Vec3 movement = end.subtract(start);
        double fraction = Math.min(1.0D, axisFraction(start.x, movement.x,
                Math.max(-Level.MAX_LEVEL_SIZE, border.getMinX()), Math.min(Level.MAX_LEVEL_SIZE, border.getMaxX())));
        fraction = Math.min(fraction, axisFraction(start.y, movement.y, level.getMinY(), level.getMaxY() + 1.0D));
        fraction = Math.min(fraction, axisFraction(start.z, movement.z,
                Math.max(-Level.MAX_LEVEL_SIZE, border.getMinZ()), Math.min(Level.MAX_LEVEL_SIZE, border.getMaxZ())));
        return start.lerp(end, Math.clamp(fraction, 0.0D, 1.0D));
    }

    private static double axisFraction(double start, double movement, double minimum, double maximum) {
        if (movement > 0.0D) {
            return (maximum - start) / movement;
        }
        if (movement < 0.0D) {
            return (minimum - start) / movement;
        }
        return 1.0D;
    }

    private record LoadedBlockView(ServerLevel level) implements BlockGetter {
        private LevelChunk chunkAt(BlockPos position) {
            if (!level.isInWorldBounds(position)) {
                return null;
            }
            return level.getChunkSource().getChunkNow(position.getX() >> 4, position.getZ() >> 4);
        }

        @Override
        public BlockEntity getBlockEntity(BlockPos position) {
            LevelChunk chunk = chunkAt(position);
            return chunk == null ? null : chunk.getBlockEntity(position, LevelChunk.EntityCreationType.CHECK);
        }

        @Override
        public BlockState getBlockState(BlockPos position) {
            LevelChunk chunk = chunkAt(position);
            return chunk == null ? Blocks.BARRIER.defaultBlockState() : chunk.getBlockState(position);
        }

        @Override
        public FluidState getFluidState(BlockPos position) {
            LevelChunk chunk = chunkAt(position);
            return chunk == null ? Fluids.EMPTY.defaultFluidState() : chunk.getFluidState(position);
        }

        @Override
        public int getHeight() {
            return level.getHeight();
        }

        @Override
        public int getMinY() {
            return level.getMinY();
        }
    }
}
