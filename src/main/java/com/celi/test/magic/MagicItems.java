package com.celi.test.magic;

import com.celi.test.CeliTest;
import java.util.function.Function;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;

public final class MagicItems {
	public static final Item MAGERALD_SHARD = register("magerald_shard", new Item.Properties());
	public static final Item MAGERITE_CLUSTER = register("magerite_cluster", new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item FIRE_AFFINITY_STONE = register("fire_affinity_stone", new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item WATER_AFFINITY_STONE = register("water_affinity_stone", new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item WIND_AFFINITY_STONE = register("wind_affinity_stone", new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item EARTH_AFFINITY_STONE = register("earth_affinity_stone", new Item.Properties().rarity(Rarity.UNCOMMON));
	public static final Item MAGES_STAFF = register("mages_staff", new Item.Properties().stacksTo(1).rarity(Rarity.RARE), MagesStaffItem::new);

	private MagicItems() {
	}

	private static Item register(String path, Item.Properties properties) {
		return register(path, properties, Item::new);
	}

	private static Item register(String path, Item.Properties properties, Function<Item.Properties, Item> factory) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, CeliTest.id(path));
		return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(creativeTab -> {
					creativeTab.accept(MAGERALD_SHARD);
					creativeTab.accept(MAGERITE_CLUSTER);
					creativeTab.accept(FIRE_AFFINITY_STONE);
					creativeTab.accept(WATER_AFFINITY_STONE);
					creativeTab.accept(WIND_AFFINITY_STONE);
					creativeTab.accept(EARTH_AFFINITY_STONE);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(creativeTab -> creativeTab.accept(MAGES_STAFF));
	}
}
