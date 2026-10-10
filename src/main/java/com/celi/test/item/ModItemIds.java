package com.celi.test.item;

import com.celi.test.CeliTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public final class ModItemIds {
	public static final ResourceKey<Item> RUBY = create("ruby");
	public static final ResourceKey<Item> REFINED_RUBY = create("refined_ruby");
	public static final ResourceKey<Item> RUBY_SWORD = create("ruby_sword");
	public static final ResourceKey<Item> RUBY_SHOVEL = create("ruby_shovel");
	public static final ResourceKey<Item> RUBY_PICKAXE = create("ruby_pickaxe");
	public static final ResourceKey<Item> RUBY_AXE = create("ruby_axe");
	public static final ResourceKey<Item> RUBY_HOE = create("ruby_hoe");
	public static final ResourceKey<Item> RUBY_HELMET = create("ruby_helmet");
	public static final ResourceKey<Item> RUBY_CHESTPLATE = create("ruby_chestplate");
	public static final ResourceKey<Item> RUBY_LEGGINGS = create("ruby_leggings");
	public static final ResourceKey<Item> RUBY_BOOTS = create("ruby_boots");

	private ModItemIds() {
	}

	private static ResourceKey<Item> create(String name) {
		return ResourceKey.create(Registries.ITEM, CeliTest.id(name));
	}
}
