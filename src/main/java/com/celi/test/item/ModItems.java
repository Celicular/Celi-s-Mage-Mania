package com.celi.test.item;

import java.util.function.Function;

import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorType;

public final class ModItems {
	public static final Item RUBY = register(ModItemIds.RUBY, Item::new, new Item.Properties());
	public static final Item REFINED_RUBY = register(ModItemIds.REFINED_RUBY, Item::new, new Item.Properties());
	public static final Item RUBY_SWORD = register(ModItemIds.RUBY_SWORD, Item::new, new Item.Properties().sword(ModToolMaterials.RUBY, 3.0F, -2.4F));
	public static final Item RUBY_SHOVEL = register(ModItemIds.RUBY_SHOVEL, Item::new, new Item.Properties().shovel(ModToolMaterials.RUBY, 1.5F, -3.0F));
	public static final Item RUBY_PICKAXE = register(ModItemIds.RUBY_PICKAXE, Item::new, new Item.Properties().pickaxe(ModToolMaterials.RUBY, 1.0F, -2.8F));
	public static final Item RUBY_AXE = register(ModItemIds.RUBY_AXE, RubyAxeItem::new, new Item.Properties().axe(ModToolMaterials.RUBY, 5.0F, -3.0F));
	public static final Item RUBY_HOE = register(ModItemIds.RUBY_HOE, Item::new, new Item.Properties().hoe(ModToolMaterials.RUBY, -3.0F, 0.0F));
	public static final Item RUBY_HELMET = register(ModItemIds.RUBY_HELMET, Item::new, new Item.Properties().humanoidArmor(ModArmorMaterials.RUBY, ArmorType.HELMET));
	public static final Item RUBY_CHESTPLATE = register(ModItemIds.RUBY_CHESTPLATE, Item::new, new Item.Properties().humanoidArmor(ModArmorMaterials.RUBY, ArmorType.CHESTPLATE));
	public static final Item RUBY_LEGGINGS = register(ModItemIds.RUBY_LEGGINGS, Item::new, new Item.Properties().humanoidArmor(ModArmorMaterials.RUBY, ArmorType.LEGGINGS));
	public static final Item RUBY_BOOTS = register(ModItemIds.RUBY_BOOTS, Item::new, new Item.Properties().humanoidArmor(ModArmorMaterials.RUBY, ArmorType.BOOTS));

	private ModItems() {
	}

	public static Item register(ResourceKey<Item> itemKey, Function<Item.Properties, Item> itemFactory, Item.Properties settings) {
		Item item = itemFactory.apply(settings.setId(itemKey));
		return Registry.register(BuiltInRegistries.ITEM, itemKey, item);
	}

	public static void initialize() {
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.INGREDIENTS)
				.register(creativeTab -> {
					creativeTab.accept(RUBY);
					creativeTab.accept(REFINED_RUBY);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.TOOLS_AND_UTILITIES)
				.register(creativeTab -> {
					creativeTab.accept(RUBY_SHOVEL);
					creativeTab.accept(RUBY_PICKAXE);
					creativeTab.accept(RUBY_AXE);
					creativeTab.accept(RUBY_HOE);
				});
		CreativeModeTabEvents.modifyOutputEvent(CreativeModeTabs.COMBAT)
				.register(creativeTab -> {
					creativeTab.accept(RUBY_SWORD);
					creativeTab.accept(RUBY_HELMET);
					creativeTab.accept(RUBY_CHESTPLATE);
					creativeTab.accept(RUBY_LEGGINGS);
					creativeTab.accept(RUBY_BOOTS);
				});
	}
}
