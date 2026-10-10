package com.celi.test.magic;

import com.celi.test.magic.spell.StaffCasting;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.level.Level;

public final class MagesStaffItem extends Item {
    public static final int USE_DURATION_TICKS = 72000;

    public MagesStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !player.getMainHandItem().is(MagicItems.MAGES_STAFF)) {
            return InteractionResult.FAIL;
        }
        if (level.isClientSide()) {
            player.startUsingItem(hand);
            return InteractionResult.CONSUME;
        }
        return player instanceof ServerPlayer serverPlayer && StaffCasting.request(serverPlayer, true, hand)
                ? InteractionResult.CONSUME : InteractionResult.FAIL;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return USE_DURATION_TICKS;
    }

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.BOW;
    }

    @Override
    public boolean useOnRelease(ItemStack stack) {
        return true;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity user, int remainingUseTicks) {
        if (user instanceof ServerPlayer player) {
            StaffCasting.cancel(player);
        }
        return false;
    }
}
