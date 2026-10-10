package com.celi.test.mixin;

import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Level.class)
public interface LevelWeatherAccessor {
    @Accessor("oRainLevel")
    float celitest$getPreviousRainLevel();

    @Accessor("oRainLevel")
    void celitest$setPreviousRainLevel(float level);

    @Accessor("rainLevel")
    float celitest$getRainLevel();

    @Accessor("rainLevel")
    void celitest$setRainLevel(float level);

    @Accessor("oThunderLevel")
    float celitest$getPreviousThunderLevel();

    @Accessor("oThunderLevel")
    void celitest$setPreviousThunderLevel(float level);

    @Accessor("thunderLevel")
    float celitest$getThunderLevel();

    @Accessor("thunderLevel")
    void celitest$setThunderLevel(float level);
}
