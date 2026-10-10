package com.celi.test.gameplay;

import com.celi.test.CeliTest;
import com.celi.test.entity.RubyAxeProjectileEntity;
import com.celi.test.item.ModItemIds;
import com.celi.test.item.ModItems;
import com.celi.test.item.RubyAxeItem;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

public final class RubyGameplay {
    private static final Identifier LEGGINGS_SPEED_ID = CeliTest.id("ruby_leggings_speed");
    private static final Identifier BOOTS_STEP_HEIGHT_ID = CeliTest.id("ruby_boots_step_height");
    private static final Identifier BOOTS_FALL_DAMAGE_ID = CeliTest.id("ruby_boots_fall_damage");
    private static final DustParticleOptions RUBY_DUST = new DustParticleOptions(0xB51A1A, 1.0F);
    private static final double RUBY_AXE_SMASH_RADIUS = 4.5D;
    private static final double RUBY_AXE_MAX_IMPACT = 32.0D;
    private static final Map<UUID, PlayerState> PLAYER_STATES = new HashMap<>();

    private RubyGameplay() {
    }

    public static void initialize() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(RubyGameplay::afterDamage);
        AttackEntityCallback.EVENT.register(RubyGameplay::onAttackEntity);
        PlayerBlockBreakEvents.AFTER.register(RubyGameplay::afterBlockBreak);
        UseItemCallback.EVENT.register(RubyGameplay::onUseItem);
        ServerTickEvents.END_SERVER_TICK.register(RubyGameplay::onServerTick);
    }

    public static boolean hasRubyHelmet(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.HEAD).is(ModItemIds.RUBY_HELMET);
    }

    public static void requestRubyAxeThrow(ServerPlayer player, ItemStack stack) {
        if (!player.isAlive() || player.isSpectator() || !stack.is(ModItems.RUBY_AXE)
                || player.getUseItem() != stack || !RubyAxeItem.isFullyCharged(player)) {
            return;
        }
    }

    private static InteractionResult onUseItem(Player player, Level level, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (hand != InteractionHand.MAIN_HAND || !stack.is(ModItems.RUBY_AXE)) {
            return InteractionResult.PASS;
        }

        if (level.isClientSide()) {
            return RubyAxeItem.isMarkedCharged(stack) ? InteractionResult.SUCCESS : InteractionResult.PASS;
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.FAIL;
        }

        if (stateFor(serverPlayer).lastRubyAxeActionTick == serverPlayer.level().getServer().getTickCount()) {
            return InteractionResult.FAIL;
        }

        if (!RubyAxeItem.isMarkedCharged(stack)) {
            return InteractionResult.PASS;
        }

        if (player.isSpectator() || !player.isAlive() || player.isUsingItem()) {
            return InteractionResult.FAIL;
        }

        return throwMarkedRubyAxe(serverPlayer, stack)
                ? InteractionResult.SUCCESS
                : InteractionResult.FAIL;
    }

    private static boolean throwMarkedRubyAxe(ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.RUBY_AXE) || !RubyAxeItem.isMarkedCharged(stack)) {
            return false;
        }

        PlayerState state = stateFor(player);
        long tick = player.level().getServer().getTickCount();
        if (state.lastRubyAxeActionTick == tick) {
            return false;
        }

        ItemStack projectileStack = stack.copy();
        stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        state.lastRubyAxeActionTick = tick;
        RubyAxeProjectileEntity.spawn(player, projectileStack);
        player.level().playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.WIND_CHARGE_THROW,
                player.getSoundSource(), 0.8F, 0.85F);
        return true;
    }

    private static void afterDamage(LivingEntity entity, DamageSource source, float baseDamageTaken, float damageTaken, boolean blocked) {
        if (entity instanceof ServerPlayer player && source.is(DamageTypeTags.IS_FIRE) && damageTaken > 0.0F && !blocked && hasRubyChestplate(player)) {
            rubyBurst(player.level(), player);
            PlayerState state = stateFor(player);
            if (state.firePulseCooldown == 0) {
                state.firePulseCooldown = 100;
                grantOwnedEffect(player, state, MobEffects.ABSORPTION, 100, 0);
            }
        }

        Entity attacker = source.getEntity();
        ItemStack weapon = source.getWeaponItem();
        if (attacker instanceof ServerPlayer player && damageTaken > 0.0F && weapon != null && weapon.is(ModItems.RUBY_SWORD)
                && player.level().getRandom().nextFloat() < 0.25F) {
            entity.igniteForTicks(80);
        }
    }

    private static InteractionResult onAttackEntity(Player player, Level level, InteractionHand hand, Entity entity, EntityHitResult hitResult) {
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer) || !(level instanceof ServerLevel serverLevel)
                || hand != InteractionHand.MAIN_HAND || !player.isAlive() || player.isSpectator()
                || !(entity instanceof LivingEntity target) || !target.isAlive() || !player.getItemInHand(hand).is(ModItems.RUBY_AXE)
                || !player.canAttack(target)) {
            return InteractionResult.PASS;
        }

        if (stateFor(serverPlayer).lastRubyAxeActionTick == serverLevel.getServer().getTickCount()) {
            return InteractionResult.FAIL;
        }

        ItemStack stack = player.getItemInHand(hand);
        boolean activelyCharged = RubyAxeItem.isFullyCharged(player) && player.getUsedItemHand() == hand
                && player.getUseItem() == stack;
        boolean releasedCharged = !player.isUsingItem() && RubyAxeItem.isMarkedCharged(stack);
        if (activelyCharged || releasedCharged) {
            if (consumeRubyAxeCharge(serverPlayer, stack)) {
                rubyAxeSmash(serverLevel, serverPlayer, target, stack);
            }
            return InteractionResult.FAIL;
        }

        if (player.getAttackStrengthScale(0.5F) < 0.9F) {
            return InteractionResult.PASS;
        }

        rubyAxeCleave(serverLevel, serverPlayer, target);
        return InteractionResult.PASS;
    }

    private static boolean consumeRubyAxeCharge(ServerPlayer player, ItemStack stack) {
        if (!stack.is(ModItems.RUBY_AXE)) {
            return false;
        }

        boolean activelyCharged = player.getUseItem() == stack && RubyAxeItem.isFullyCharged(player);
        boolean releasedCharged = !player.isUsingItem() && RubyAxeItem.isMarkedCharged(stack);
        if (!activelyCharged && !releasedCharged) {
            return false;
        }

        PlayerState state = stateFor(player);
        long tick = player.level().getServer().getTickCount();
        if (state.lastRubyAxeActionTick == tick) {
            return false;
        }
        state.lastRubyAxeActionTick = tick;
        if (activelyCharged) {
            RubyAxeItem.consumeCharge(player);
        } else {
            stack.remove(DataComponents.CUSTOM_MODEL_DATA);
        }
        return true;
    }

    private static void rubyAxeSmash(ServerLevel level, ServerPlayer player, LivingEntity target, ItemStack stack) {
        double impact = rubyAxeImpact(player);
        Vec3 center = target.position();
        DamageSource source = level.damageSources().mace(player);
        float bonus = rubyAxeSmashBonus(impact);
        float damage = EnchantmentHelper.modifyDamage(level, stack, target, source,
                (float) player.getAttributeValue(Attributes.ATTACK_DAMAGE) + 8.0F + bonus);
        player.resetAttackStrengthTicker();

        boolean hit = target.hurtServer(level, source, damage);
        if (hit) {
            player.setLastHurtMob(target);
            rubyAxeWindKnockback(player, target, center, impact, 1.0D);
            stack.hurtEnemy(target, player);
            EnchantmentHelper.doPostAttackEffectsWithItemSource(level, target, source, stack);
            stack.postHurtEnemy(target, player);
        }

        AABB area = target.getBoundingBox().inflate(RUBY_AXE_SMASH_RADIUS);
        for (LivingEntity nearby : level.getEntities(EntityTypeTest.<Entity, LivingEntity>forClass(LivingEntity.class), area,
                other -> other != target && canRubyAxeSmashHit(player, other))) {
            double distance = nearby.position().distanceTo(center);
            if (distance > RUBY_AXE_SMASH_RADIUS) {
                continue;
            }
            double strength = 1.0D - distance / RUBY_AXE_SMASH_RADIUS * 0.6D;
            if (nearby.hurtServer(level, source, (float) ((6.0F + bonus * 0.4F) * strength))) {
                hit = true;
                rubyAxeWindKnockback(player, nearby, center, impact, strength);
            }
        }

        if (hit && impact > 0.0D) {
            finishRubyAxeImpact(player);
        }
        rubyAxeSmashEffects(level, player, center, impact);
    }

    private static boolean canRubyAxeSmashHit(ServerPlayer player, LivingEntity entity) {
        return entity != player && entity.isAlive() && !entity.isSpectator() && player.canAttack(entity)
                && !player.isAlliedTo(entity) && (!(entity instanceof Player other) || player.canHarmPlayer(other));
    }

    private static double rubyAxeImpact(ServerPlayer player) {
        if (player.isPassenger() || player.isFallFlying() || player.getAbilities().flying
                || player.isInWater() || player.isInLava() || player.onClimbable()) {
            return 0.0D;
        }

        double speed = Math.max(0.0D, -player.getDeltaMovement().y);
        if (!Double.isFinite(speed) || !Double.isFinite(player.fallDistance) || speed == 0.0D) {
            return 0.0D;
        }
        return Math.min(RUBY_AXE_MAX_IMPACT, Math.max(Math.max(0.0D, player.fallDistance), speed * speed / 0.16D));
    }

    private static float rubyAxeSmashBonus(double impact) {
        double maceBonus = Math.min(impact, 3.0D) * 4.0D + Math.min(Math.max(impact - 3.0D, 0.0D), 5.0D) * 2.0D
                + Math.max(impact - 8.0D, 0.0D);
        return (float) (maceBonus + impact * 2.5D);
    }

    private static void rubyAxeWindKnockback(ServerPlayer player, LivingEntity entity, Vec3 center, double impact, double strength) {
        double resistance = Math.max(0.0D, 1.0D - entity.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE));
        if (resistance == 0.0D) {
            return;
        }

        Vec3 direction = entity.position().subtract(center);
        double length = Math.hypot(direction.x, direction.z);
        if (length < 0.0001D) {
            double angle = Math.toRadians(player.getYRot());
            direction = new Vec3(-Math.sin(angle), 0.0D, Math.cos(angle));
            length = 1.0D;
        }
        double horizontal = (1.4D + Math.min(impact * 0.09D, 1.8D)) * strength * resistance;
        double vertical = (0.7D + Math.min(impact * 0.02D, 0.5D)) * strength * resistance;
        entity.push(direction.x / length * horizontal, vertical, direction.z / length * horizontal);
        syncRubyAxeMovement(entity);
    }

    private static void finishRubyAxeImpact(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        Vec3 impactPosition = player.position();
        if (player.isIgnoringFallDamageFromCurrentImpulse() && player.currentImpulseImpactPos.y <= impactPosition.y) {
            impactPosition = player.currentImpulseImpactPos;
        }
        player.setIgnoreFallDamageFromCurrentImpulse(true, impactPosition);
        player.resetFallDistance();
        player.setDeltaMovement(movement.x, 0.01D, movement.z);
        syncRubyAxeMovement(player);
    }

    private static void syncRubyAxeMovement(LivingEntity entity) {
        if (entity instanceof ServerPlayer player) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private static void rubyAxeSmashEffects(ServerLevel level, ServerPlayer player, Vec3 center, double impact) {
        level.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, center.x, center.y + 0.2D, center.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        level.sendParticles(RUBY_DUST, center.x, center.y + 0.3D, center.z, 48, 1.8D, 0.25D, 1.8D, 0.08D);
        level.playSound(null, center.x, center.y, center.z,
                player.onGround() ? (impact > 5.0D ? SoundEvents.MACE_SMASH_GROUND_HEAVY : SoundEvents.MACE_SMASH_GROUND) : SoundEvents.MACE_SMASH_AIR,
                player.getSoundSource(), 1.2F, 0.85F);
        level.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST, player.getSoundSource(), 1.2F, 0.85F);
    }

    private static void rubyAxeCleave(ServerLevel serverLevel, ServerPlayer player, LivingEntity target) {
        DamageSource source = serverLevel.damageSources().playerAttack(player);
        AABB area = player.getBoundingBox().inflate(2.75D);
        for (LivingEntity nearby : serverLevel.getEntities(EntityTypeTest.<Entity, LivingEntity>forClass(LivingEntity.class), area,
                other -> other != player && other != target && other.isAlive() && player.canAttack(other))) {
            Vec3 direction = nearby.position().subtract(player.position());
            double horizontalLength = Math.sqrt(direction.x * direction.x + direction.z * direction.z);
            if (horizontalLength > 0.0001D) {
                nearby.setDeltaMovement(nearby.getDeltaMovement().add(direction.x / horizontalLength * 0.75D, 0.28D, direction.z / horizontalLength * 0.75D));
            }
            nearby.hurtServer(serverLevel, source, 2.0F);
        }
        rubyBurst(serverLevel, player);
    }

    private static void afterBlockBreak(Level level, Player player, BlockPos pos, BlockState state, BlockEntity blockEntity) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }

        ItemStack tool = player.getMainHandItem();
        if (tool.is(ModItems.RUBY_PICKAXE) && state.is(BlockTags.ORES)) {
            grantTransientEffect(serverPlayer, MobEffects.HASTE, 80, 0);
            rubyBurst(serverPlayer.level(), pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D, RUBY_DUST, 8);
        }

        if (tool.is(ModItems.RUBY_SHOVEL) && isNaturalDigBlock(state)) {
            grantTransientEffect(serverPlayer, MobEffects.SPEED, 60, 0);
        }

        if (tool.is(ModItems.RUBY_HOE) && state.getBlock() instanceof CropBlock crop && crop.isMaxAge(state)) {
            player.getFoodData().eat(1, 0.2F);
        }
    }

    private static void onServerTick(MinecraftServer server) {
        Set<UUID> onlinePlayers = new HashSet<>();
        long tick = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            onlinePlayers.add(player.getUUID());
            updatePlayer(player, tick);
        }
        PLAYER_STATES.keySet().removeIf(uuid -> !onlinePlayers.contains(uuid));
    }

    private static void updatePlayer(ServerPlayer player, long tick) {
        PlayerState state = stateFor(player);
        state.effects.entrySet().removeIf(entry -> player.getEffect(entry.getKey()) != entry.getValue());
        if (state.firePulseCooldown > 0) {
            state.firePulseCooldown--;
        }

        boolean helmet = player.getItemBySlot(EquipmentSlot.HEAD).is(ModItems.RUBY_HELMET);
        boolean chestplate = hasRubyChestplate(player);
        boolean leggings = player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.RUBY_LEGGINGS);
        boolean boots = player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.RUBY_BOOTS);
        boolean fullSet = helmet && chestplate && leggings && boots;

        updateManagedEffect(player, state, MobEffects.NIGHT_VISION, helmet, 0, 50);
        updateManagedEffect(player, state, MobEffects.FIRE_RESISTANCE, chestplate, 0, 50);
        updateManagedEffect(player, state, MobEffects.HASTE, fullSet, 0, 50);
        updateManagedEffect(player, state, MobEffects.RESISTANCE, fullSet, 0, 50);

        if (helmet && player.hasEffect(MobEffects.DARKNESS)) {
            player.removeEffect(MobEffects.DARKNESS);
        }

        updateModifier(player.getAttribute(Attributes.MOVEMENT_SPEED), LEGGINGS_SPEED_ID, leggings, 0.12D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);
        updateModifier(player.getAttribute(Attributes.STEP_HEIGHT), BOOTS_STEP_HEIGHT_ID, boots, 0.5D, AttributeModifier.Operation.ADD_VALUE);
        updateModifier(player.getAttribute(Attributes.FALL_DAMAGE_MULTIPLIER), BOOTS_FALL_DAMAGE_ID, boots, -0.35D, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

        if (fullSet) {
            if (!state.fullSet) {
                player.playSound(SoundEvents.AMETHYST_BLOCK_RESONATE, 0.8F, 1.15F);
                resonance(player.level(), player);
            } else if (tick % 40L == 0L) {
                resonance(player.level(), player);
            }
            if (!state.fullSet || tick % 200L == 0L) {
                grantOwnedEffect(player, state, MobEffects.ABSORPTION, 100, 0);
            }
        }
        if (boots && player.isAlive() && player.onGround() && isMoving(player) && tick % 3L == 0L) {
            trail(player.level(), player);
        }
        state.fullSet = fullSet;
    }

    private static boolean isMoving(ServerPlayer player) {
        Vec3 movement = player.getDeltaMovement();
        return movement.x * movement.x + movement.z * movement.z > 0.0004D;
    }

    private static boolean hasRubyChestplate(LivingEntity entity) {
        return entity.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.RUBY_CHESTPLATE);
    }

    private static boolean isNaturalDigBlock(BlockState state) {
        return state.is(BlockTags.DIRT) || state.is(BlockTags.SAND) || state.is(BlockTags.SNOW) || state.is(BlockTags.MUD)
                || state.getBlock() == Blocks.GRAVEL || state.getBlock() == Blocks.CLAY || state.getBlock() == Blocks.SOUL_SAND
                || state.getBlock() == Blocks.SOUL_SOIL;
    }

    private static void updateManagedEffect(ServerPlayer player, PlayerState state, Holder<MobEffect> effect, boolean enabled, int amplifier, int duration) {
        MobEffectInstance current = player.getEffect(effect);
        MobEffectInstance owned = state.effects.get(effect);
        if (!enabled) {
            if (owned != null && current == owned) {
                player.removeEffect(effect);
            }
            state.effects.remove(effect);
            return;
        }

        if (current == null) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier));
            current = player.getEffect(effect);
            if (current != null) {
                state.effects.put(effect, current);
            }
        } else if (current == owned) {
            if (current.getDuration() < 20 || current.getAmplifier() != amplifier) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier));
                current = player.getEffect(effect);
            }
            if (current != null) {
                state.effects.put(effect, current);
            }
        } else {
            state.effects.remove(effect);
            if (current.getAmplifier() < amplifier) {
                player.addEffect(new MobEffectInstance(effect, duration, amplifier));
                current = player.getEffect(effect);
                if (current != null) {
                    state.effects.put(effect, current);
                }
            }
        }
    }

    private static void grantOwnedEffect(ServerPlayer player, PlayerState state, Holder<MobEffect> effect, int duration, int amplifier) {
        MobEffectInstance current = player.getEffect(effect);
        MobEffectInstance owned = state.effects.get(effect);
        if (current != null && current != owned) {
            return;
        }
        player.addEffect(new MobEffectInstance(effect, duration, amplifier));
        current = player.getEffect(effect);
        if (current != null) {
            state.effects.put(effect, current);
        }
    }

    private static void grantTransientEffect(ServerPlayer player, Holder<MobEffect> effect, int duration, int amplifier) {
        if (effect == MobEffects.HASTE && hasRubyArmorSet(player)) {
            return;
        }
        MobEffectInstance current = player.getEffect(effect);
        if (current == null || (current.getAmplifier() <= amplifier && current.getDuration() < duration)) {
            player.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }
    }

    private static boolean hasRubyArmorSet(ServerPlayer player) {
        return hasRubyHelmet(player) && hasRubyChestplate(player)
                && player.getItemBySlot(EquipmentSlot.LEGS).is(ModItems.RUBY_LEGGINGS)
                && player.getItemBySlot(EquipmentSlot.FEET).is(ModItems.RUBY_BOOTS);
    }

    private static void updateModifier(AttributeInstance attribute, Identifier id, boolean enabled, double amount, AttributeModifier.Operation operation) {
        if (attribute == null) {
            return;
        }
        if (enabled) {
            AttributeModifier current = attribute.getModifier(id);
            if (current == null || current.amount() != amount || current.operation() != operation) {
                attribute.addOrUpdateTransientModifier(new AttributeModifier(id, amount, operation));
            }
        } else {
            attribute.removeModifier(id);
        }
    }

    private static PlayerState stateFor(ServerPlayer player) {
        return PLAYER_STATES.computeIfAbsent(player.getUUID(), ignored -> new PlayerState());
    }

    private static void rubyBurst(ServerLevel level, ServerPlayer player) {
        rubyBurst(level, player.getX(), player.getY() + 1.0D, player.getZ(), RUBY_DUST, 16);
    }

    private static void rubyBurst(ServerLevel level, double x, double y, double z, ParticleOptions particle, int count) {
        level.sendParticles(particle, x, y, z, count, 0.35D, 0.45D, 0.35D, 0.02D);
    }

    private static void trail(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.LAVA, player.getX(), player.getY() + 0.08D, player.getZ(), 1, 0.12D, 0.02D, 0.12D, 0.0D);
        level.sendParticles(RUBY_DUST, player.getX(), player.getY() + 0.08D, player.getZ(), 3, 0.18D, 0.04D, 0.18D, 0.01D);
    }

    private static void resonance(ServerLevel level, ServerPlayer player) {
        level.sendParticles(RUBY_DUST, player.getX(), player.getY() + 1.0D, player.getZ(), 20, 0.6D, 0.8D, 0.6D, 0.04D);
    }

    private static final class PlayerState {
        private final Map<Holder<MobEffect>, MobEffectInstance> effects = new HashMap<>();
        private boolean fullSet;
        private int firePulseCooldown;
        private long lastRubyAxeActionTick = -1L;
    }
}
