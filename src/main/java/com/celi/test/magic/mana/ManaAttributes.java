package com.celi.test.magic.mana;

import com.celi.test.CeliTest;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.RangedAttribute;

public final class ManaAttributes {
    public static final double BASE_MANA = 50.0D;
    public static final Holder<Attribute> MAX_MANA = Registry.registerForHolder(
            BuiltInRegistries.ATTRIBUTE,
            CeliTest.id("max_mana"),
            new RangedAttribute("attribute.name.celitest.max_mana", BASE_MANA, 0.0D, Double.MAX_VALUE)
                    .setSyncable(true));
    public static final Holder<Attribute> MANA_REGENERATION = Registry.registerForHolder(
            BuiltInRegistries.ATTRIBUTE,
            CeliTest.id("mana_regeneration"),
            new RangedAttribute("attribute.name.celitest.mana_regeneration", ManaMath.BASE_REGENERATION,
                    0.0D, Double.MAX_VALUE).setSyncable(true));

    private ManaAttributes() {
    }

    public static void initialize() {
    }
}
