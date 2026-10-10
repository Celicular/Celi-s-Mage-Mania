package com.celi.test.entity;

import com.celi.test.gameplay.RubyAxeDamage;
import com.celi.test.item.ModItems;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoveSimulationType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.entity.EntityTypeTest;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RubyAxeProjectileEntity extends ThrowableItemProjectile {
    public static final float DAMAGE = 40.0F;
    public static final double MAX_TRAVEL_DISTANCE = 20.0D;
    public static final int MAX_LIFETIME_TICKS = 20;
    private static final double BURST_RADIUS = 2.5D;
    private static final DustParticleOptions BURST_DUST = new DustParticleOptions(0xB51A1A, 1.4F);
    private static final float SPIN_PER_TICK = 36.0F;
    private double travelledDistance;
    private boolean impacted;

    public RubyAxeProjectileEntity(EntityType<? extends RubyAxeProjectileEntity> type, Level level) {
        super(type, level);
    }

    private RubyAxeProjectileEntity(Level level, LivingEntity owner, ItemStack stack) {
        this(level, launchPosition(owner), owner, stack);
    }

    private RubyAxeProjectileEntity(Level level, Vec3 position, LivingEntity owner, ItemStack stack) {
        super(ModEntities.RUBY_AXE_PROJECTILE, position.x, position.y, position.z, level, stack);
        setOwner(owner);
    }

    public static RubyAxeProjectileEntity spawn(ServerPlayer thrower, ItemStack stack) {
        return spawn(thrower.level(), thrower, stack);
    }

    public static RubyAxeProjectileEntity spawn(ServerLevel level, ServerPlayer thrower, ItemStack stack) {
        RubyAxeProjectileEntity projectile = new RubyAxeProjectileEntity(level, thrower, stack);
        projectile.shootFromRotation(thrower, thrower.getXRot(), thrower.getYRot(), 0.0F, 1.75F, 0.0F);
        level.addFreshEntity(projectile);
        return projectile;
    }

    private static Vec3 launchPosition(LivingEntity owner) {
        return owner.getEyePosition().add(owner.getViewVector(1.0F).scale(0.75D));
    }

    @Override
    public void tick() {
        if (!level().isClientSide() && (tickCount >= MAX_LIFETIME_TICKS || travelledDistance >= MAX_TRAVEL_DISTANCE)) {
            discard();
            return;
        }

        Vec3 velocity = getDeltaMovement();
        double remainingDistance = MAX_TRAVEL_DISTANCE - travelledDistance;
        double velocityLength = velocity.length();
        if (velocityLength > remainingDistance) {
            setDeltaMovement(velocity.scale(remainingDistance / velocityLength));
        }

        Vec3 previousPosition = position();
        super.tick();
        travelledDistance += previousPosition.distanceTo(position());

        if (!isRemoved()) {
            setYRot(getYRot() + SPIN_PER_TICK);
        }

        if (!level().isClientSide() && travelledDistance >= MAX_TRAVEL_DISTANCE) {
            discard();
        }
    }

    @Override
    protected boolean canHitEntity(Entity entity) {
        return entity != getOwner() && super.canHitEntity(entity);
    }

    @Override
    protected void onHit(HitResult hitResult) {
        if (!(level() instanceof ServerLevel serverLevel) || impacted || isRemoved()
                || hitResult.getType() == HitResult.Type.MISS) {
            return;
        }

        impacted = true;
        Vec3 center = hitResult.getLocation();
        Entity owner = getOwner();
        AABB area = new AABB(center, center).inflate(BURST_RADIUS + 1.0E-7D);
        Set<LivingEntity> targets = new HashSet<>(serverLevel.getEntities(
                EntityTypeTest.<Entity, LivingEntity>forClass(LivingEntity.class), area,
                target -> canBurstHit(owner, target)));
        if (hitResult instanceof EntityHitResult entityHit && entityHit.getEntity() instanceof LivingEntity target
                && canBurstHit(owner, target)) {
            targets.add(target);
        }

        for (LivingEntity target : targets) {
            if (target.getBoundingBox().distanceToSqr(center) <= BURST_RADIUS * BURST_RADIUS) {
                RubyAxeDamage.hurt(serverLevel, this, owner, target, DAMAGE);
            }
        }

        serverLevel.sendParticles(ParticleTypes.GUST_EMITTER_SMALL, center.x, center.y, center.z, 1,
                0.0D, 0.0D, 0.0D, 0.0D);
        serverLevel.sendParticles(BURST_DUST, center.x, center.y, center.z, 64,
                0.85D, 0.85D, 0.85D, 0.08D);
        serverLevel.playSound(null, center.x, center.y, center.z, SoundEvents.WIND_CHARGE_BURST,
                getSoundSource(), 1.2F, 0.85F);
        discard();
    }

    private static boolean canBurstHit(Entity owner, LivingEntity target) {
        if (target == owner || !target.isAlive() || target.isSpectator()
                || target instanceof Player player && player.isCreative()) {
            return false;
        }
        if (owner instanceof LivingEntity livingOwner
                && (!livingOwner.canAttack(target) || livingOwner.isAlliedTo(target))) {
            return false;
        }
        return !(owner instanceof Player player && target instanceof Player other) || player.canHarmPlayer(other);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.RUBY_AXE;
    }

    @Override
    public MoveSimulationType getMoveSimulationType() {
        return MoveSimulationType.AUTHORITATIVE_SIDE;
    }
}
