package com.celi.test.mixin;

import com.celi.test.magic.MagicItems;
import com.celi.test.magic.affinity.AffinityPassives;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(Player.class)
public abstract class PlayerAffinityStaffAttackMixin {
    @ModifyArgs(method = "attack(Lnet/minecraft/world/entity/Entity;)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/Entity;hurtOrSimulate(Lnet/minecraft/world/damagesource/DamageSource;F)Z"))
    private void celitest$boostAirborneStaffMelee(Args args) {
        if (!((Object) this instanceof ServerPlayer player)) {
            return;
        }
        DamageSource source = args.get(0);
        ItemStack weapon = source.getWeaponItem();
        if (source.is(DamageTypes.PLAYER_ATTACK) && source.getEntity() == player && source.getDirectEntity() == player
                && player.getMainHandItem().is(MagicItems.MAGES_STAFF) && weapon != null && weapon.is(MagicItems.MAGES_STAFF)) {
            float damage = args.get(1);
            args.set(1, damage * AffinityPassives.staffDamageMultiplier(player));
        }
    }
}
