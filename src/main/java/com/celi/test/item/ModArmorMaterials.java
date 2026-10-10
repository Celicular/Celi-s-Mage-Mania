package com.celi.test.item;

import com.celi.test.CeliTest;
import java.util.Map;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;

public final class ModArmorMaterials {
	public static final TagKey<Item> REPAIRS_RUBY_ARMOR = TagKey.create(Registries.ITEM, CeliTest.id("repairs_ruby_armor"));
	public static final ResourceKey<EquipmentAsset> RUBY_ASSET = ResourceKey.create(EquipmentAssets.ROOT_ID, CeliTest.id("ruby"));
	public static final ArmorMaterial RUBY = new ArmorMaterial(
			37,
			Map.of(
					ArmorType.BOOTS, 3,
					ArmorType.LEGGINGS, 6,
					ArmorType.CHESTPLATE, 8,
					ArmorType.HELMET, 3,
					ArmorType.BODY, 11
			),
			18,
			SoundEvents.ARMOR_EQUIP_DIAMOND,
			2.0F,
			0.1F,
			REPAIRS_RUBY_ARMOR,
			RUBY_ASSET
	);

	private ModArmorMaterials() {
	}
}
