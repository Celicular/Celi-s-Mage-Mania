package com.celi.test.mixin;

import com.celi.test.magic.mana.ManaSystem;
import java.util.Map;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityManaEquipmentMixin {
    @Inject(method = "onEquipItem(Lnet/minecraft/world/entity/EquipmentSlot;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)V", at = @At("TAIL"))
    private void celitest$refreshArmorMana(EquipmentSlot slot, ItemStack previous, ItemStack equipped, CallbackInfo callback) {
        if ((Object) this instanceof ServerPlayer player && player.connection != null
                && (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST
                || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET)) {
            ManaSystem.refreshEquipment(player);
        }
    }

    @Inject(method = "collectEquipmentChanges(Ljava/util/Map;)Ljava/util/Map;", at = @At("TAIL"))
    private void celitest$refreshChangedArmorMana(Map<EquipmentSlot, ItemStack> previous,
            CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> callback) {
        Map<EquipmentSlot, ItemStack> changed = callback.getReturnValue();
        if ((Object) this instanceof ServerPlayer player && player.connection != null && changed != null
                && (changed.containsKey(EquipmentSlot.HEAD) || changed.containsKey(EquipmentSlot.CHEST)
                || changed.containsKey(EquipmentSlot.LEGS) || changed.containsKey(EquipmentSlot.FEET))) {
            ManaSystem.refreshEquipment(player);
        }
    }
}
