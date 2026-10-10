package com.celi.test.magic.spell;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public final class StaffCombat {
    private StaffCombat() {
    }

    public static boolean isValidTarget(ServerPlayer player, LivingEntity target) {
        if (target == player || player.hasDisconnected() || player.isRemoved() || !player.isAlive()
                || player.isSpectator() || target.level() != player.level() || target.isRemoved()
                || !target.isAlive() || target.isSpectator() || target.isInvulnerable()
                || !target.isAttackable() || player.isAlliedTo(target)) {
            return false;
        }

        if (target instanceof OwnableEntity owned && owned.getOwnerReference() != null
                && owned.getOwnerReference().matches(player)) {
            return false;
        }

        if (target instanceof ServerPlayer other && other.hasDisconnected()) {
            return false;
        }

        if (target instanceof Player other) {
            return player.level().isPvpAllowed() && player.canHarmPlayer(other) && other.canHarmPlayer(player)
                    && !other.isInvulnerableTo(player.level(), damageSource(player));
        }
        return true;
    }

    public static boolean damage(ServerPlayer player, LivingEntity target, float amount) {
        if (!Float.isFinite(amount) || amount <= 0.0F || !isValidTarget(player, target)) {
            return false;
        }

        boolean hit = target.hurtServer(player.level(), damageSource(player), amount);
        if (hit) {
            player.setLastHurtMob(target);
        }
        return hit;
    }

    public static void applyImpulse(LivingEntity target, Vec3 impulse) {
        if (!(target.level() instanceof ServerLevel) || target.isRemoved() || !impulse.isFinite()) {
            return;
        }

        double resistance = Math.clamp(1.0D - target.getAttributeValue(Attributes.KNOCKBACK_RESISTANCE), 0.0D, 1.0D);
        if (resistance <= 0.0D) {
            return;
        }

        target.push(impulse.scale(resistance));
        target.syncVelocity = true;
        if (target instanceof ServerPlayer player && !player.hasDisconnected()) {
            player.connection.send(new ClientboundSetEntityMotionPacket(player));
        }
    }

    private static DamageSource damageSource(ServerPlayer player) {
        return player.level().damageSources().indirectMagic(null, player);
    }
}
