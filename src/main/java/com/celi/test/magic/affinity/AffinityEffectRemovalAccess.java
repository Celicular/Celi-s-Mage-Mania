package com.celi.test.magic.affinity;

import java.util.Collection;
import net.minecraft.world.effect.MobEffectInstance;

public interface AffinityEffectRemovalAccess {
    void celitest$notifyAffinityEffectRemoval(Collection<MobEffectInstance> effects);
}
