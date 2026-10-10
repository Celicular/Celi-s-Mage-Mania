package com.celi.test.mixin;

import com.celi.test.magic.mana.ManaData;
import com.celi.test.magic.mana.ManaPlayer;
import com.celi.test.magic.mana.ManaSystem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerManaMixin implements ManaPlayer {
    @Unique
    private final ManaData celitest$manaData = new ManaData();

    @Override
    public ManaData celitest$getManaData() {
        return celitest$manaData;
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"))
    private void celitest$readMana(ValueInput input, CallbackInfo callback) {
        celitest$manaData.read(input);
        ManaSystem.getMaximumMana((ServerPlayer) (Object) this);
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("TAIL"))
    private void celitest$writeMana(ValueOutput output, CallbackInfo callback) {
        ManaSystem.getCurrentMana((ServerPlayer) (Object) this);
        celitest$manaData.write(output);
    }
}
