package com.celi.test.magic.spell;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public final class MagicFlameBlock extends BaseFireBlock {
    public static final int LIFETIME_TICKS = 100;

    public MagicFlameBlock(Properties properties) {
        super(properties, 1.0F);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState();
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return false;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        if (!level.isInsideBuildHeight(below)) {
            return false;
        }
        if (level instanceof ServerLevel serverLevel) {
            if (serverLevel.getChunkSource().getChunkNow(below.getX() >> 4, below.getZ() >> 4) == null) {
                return false;
            }
        } else if (!level.hasChunkAt(below)) {
            return false;
        }
        BlockState support = level.getBlockState(below);
        return !support.hasBlockEntity()
                && support.getFluidState().isEmpty()
                && support.isFaceSturdy(level, below, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
            Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState previousState, boolean movedByPiston) {
        if (level.isClientSide() || previousState.is(this)) {
            return;
        }
        if (!canSurvive(state, level, pos)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
            return;
        }
        level.scheduleTick(pos, this, LIFETIME_TICKS);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (level.getBlockState(pos).is(this)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        }
    }
}
