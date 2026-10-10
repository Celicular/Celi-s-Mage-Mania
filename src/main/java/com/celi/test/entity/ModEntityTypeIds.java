package com.celi.test.entity;

import com.celi.test.CeliTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;

public final class ModEntityTypeIds {
    public static final ResourceKey<EntityType<?>> RUBY_AXE_PROJECTILE = create("ruby_axe_projectile");

    private ModEntityTypeIds() {
    }

    private static ResourceKey<EntityType<?>> create(String name) {
        return ResourceKey.create(Registries.ENTITY_TYPE, CeliTest.id(name));
    }
}
