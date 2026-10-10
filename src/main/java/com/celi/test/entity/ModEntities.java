package com.celi.test.entity;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final EntityType<RubyAxeProjectileEntity> RUBY_AXE_PROJECTILE = register(
            ModEntityTypeIds.RUBY_AXE_PROJECTILE,
            EntityType.Builder.of(RubyAxeProjectileEntity::new, MobCategory.MISC)
                    .sized(0.5F, 0.5F)
                    .clientTrackingRange(8)
                    .updateInterval(1)
                    .build(ModEntityTypeIds.RUBY_AXE_PROJECTILE)
    );

    private ModEntities() {
    }

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(
            net.minecraft.resources.ResourceKey<EntityType<?>> key,
            EntityType<T> type
    ) {
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, key, type);
    }

    public static void initialize() {
    }
}
