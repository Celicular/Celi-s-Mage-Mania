package com.celi.test.client.mixin;

import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(MultiPlayerGameMode.class)
public interface MultiPlayerGameModeStaffAccessor {
	@Invoker("ensureHasSentCarriedItem")
	void celitest$ensureStaffCarriedItem();
}
