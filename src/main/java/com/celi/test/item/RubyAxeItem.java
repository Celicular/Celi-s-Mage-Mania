package com.celi.test.item;

import com.celi.test.gameplay.RubyGameplay;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;

import java.util.List;

public final class RubyAxeItem extends Item {
    public static final int CHARGE_DURATION_TICKS = 20;
    private static final DustParticleOptions CHARGE_DUST = new DustParticleOptions(0xE51E3A, 1.0F);

    public RubyAxeItem(Properties properties) {
        super(properties);
    }

    public static boolean isFullyCharged(LivingEntity user) {
        if (!user.isUsingItem()) {
            return false;
        }

        ItemStack stack = user.getUseItem();
        if (!(stack.getItem() instanceof RubyAxeItem axe)) {
            return false;
        }

        int useDuration = axe.getUseDuration(stack, user);
        return user.getUseItemRemainingTicks() <= useDuration - CHARGE_DURATION_TICKS
                || (isMarkedCharged(stack) && user.getUseItemRemainingTicks() == useDuration);
    }

    public static boolean isMarkedCharged(ItemStack stack) {
        CustomModelData data = stack.get(DataComponents.CUSTOM_MODEL_DATA);
        return data != null && Boolean.TRUE.equals(data.getBoolean(0));
    }

    private static void markCharged(ItemStack stack) {
        if (!isMarkedCharged(stack)) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(), List.of(true), List.of(), List.of()));
        }
    }

    private static void clearCharged(ItemStack stack) {
        if (isMarkedCharged(stack)) {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
    }

    public static void consumeCharge(LivingEntity user) {
        if (isFullyCharged(user)) {
            clearCharged(user.getUseItem());
            user.stopUsingItem();
        }
    }

    @Override
    public void onUseTick(Level level, LivingEntity user, ItemStack stack, int remainingUseTicks) {
        int chargeTicks = this.getUseDuration(stack, user) - remainingUseTicks;
        if (chargeTicks >= CHARGE_DURATION_TICKS - 1) {
            markCharged(stack);
        }
        if (level instanceof ServerLevel serverLevel && user instanceof ServerPlayer player && serverLevel.getGameTime() % 2L == 0L) {
            double progress = Math.min(1.0D, chargeTicks / (double) CHARGE_DURATION_TICKS);
            double radius = 1.8D - progress * 0.9D;
            double angle = serverLevel.getGameTime() * 0.32D;
            for (int i = 0; i < 4; i++) {
                double particleAngle = angle + i * Math.PI * 0.5D;
                double x = player.getX() + Math.cos(particleAngle) * radius;
                double y = player.getY() + 1.0D + Math.sin(particleAngle * 1.7D) * 0.35D;
                double z = player.getZ() + Math.sin(particleAngle) * radius;
                serverLevel.sendParticles(CHARGE_DUST, x, y, z, 1, 0.0D, 0.0D, 0.0D, -0.03D);
            }
            if (progress >= 1.0D) {
                serverLevel.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(), 2,
                        0.25D, 0.35D, 0.25D, 0.01D);
            }
        }
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isMarkedCharged(stack)) {
            if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
                player.startUsingItem(hand);
                RubyGameplay.requestRubyAxeThrow(serverPlayer, stack);
            }
            return InteractionResult.CONSUME;
        }
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity user) {
        return CHARGE_DURATION_TICKS;
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
        if (this.getUseDuration(stack, user) - remainingUseTicks < CHARGE_DURATION_TICKS) {
            clearCharged(stack);
            return false;
        }

        return true;
    }
}
