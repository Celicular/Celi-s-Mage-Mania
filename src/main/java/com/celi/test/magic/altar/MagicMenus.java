package com.celi.test.magic.altar;

import com.celi.test.CeliTest;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;

public final class MagicMenus {
    public static final MenuType<MagesAltarMenu> MAGES_ALTAR = Registry.register(
            BuiltInRegistries.MENU,
            CeliTest.id("mages_altar"),
            new MenuType<>(MagesAltarMenu::new, FeatureFlags.DEFAULT_FLAGS)
    );

    private MagicMenus() {
    }

    public static void initialize() {
    }
}
