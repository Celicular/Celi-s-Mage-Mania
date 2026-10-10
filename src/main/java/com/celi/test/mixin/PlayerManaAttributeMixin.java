package com.celi.test.mixin;

import com.celi.test.magic.mana.ManaAttributes;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerManaAttributeMixin {
    @Inject(method = "createAttributes()Lnet/minecraft/world/entity/ai/attributes/AttributeSupplier$Builder;", at = @At("RETURN"))
    private static void celitest$addManaAttributes(CallbackInfoReturnable<AttributeSupplier.Builder> callback) {
        callback.getReturnValue().add(ManaAttributes.MAX_MANA).add(ManaAttributes.MANA_REGENERATION);
    }
}
