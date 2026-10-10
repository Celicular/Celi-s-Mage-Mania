package com.celi.test.mixin;

import com.celi.test.magic.affinity.AffinityData;
import com.celi.test.magic.affinity.AffinityPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerAffinityMixin implements AffinityPlayer {
    @Unique
    private final AffinityData celitest$affinityData = new AffinityData();

    @Override
    public AffinityData celitest$getAffinityData() {
        return celitest$affinityData;
    }

    @Inject(method = "readAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueInput;)V", at = @At("TAIL"))
    private void celitest$readAffinity(ValueInput input, CallbackInfo callback) {
        celitest$affinityData.read(input);
    }

    @Inject(method = "addAdditionalSaveData(Lnet/minecraft/world/level/storage/ValueOutput;)V", at = @At("TAIL"))
    private void celitest$writeAffinity(ValueOutput output, CallbackInfo callback) {
        celitest$affinityData.write(output);
    }
}
