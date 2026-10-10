package com.celi.test.magic;

import com.celi.test.CeliTest;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.equipment.Equippable;

public final class MagicEnchantments {
	public static final ResourceKey<Enchantment> MANA_STORE = ResourceKey.create(Registries.ENCHANTMENT, CeliTest.id("mana_store"));
	public static final ResourceKey<Enchantment> MANA_REGEN = ResourceKey.create(Registries.ENCHANTMENT, CeliTest.id("mana_regen"));
	public static final int MAX_MANA_STORE_LEVEL = 3;
	public static final int MAX_MANA_REGEN_LEVEL = 5;

	private MagicEnchantments() {
	}

	public static int getManaStoreLevel(ItemStack stack) {
		return getLevel(stack, MANA_STORE, MAX_MANA_STORE_LEVEL);
	}

	public static int getManaRegenLevel(ItemStack stack) {
		return getLevel(stack, MANA_REGEN, MAX_MANA_REGEN_LEVEL);
	}

	public static boolean isCompatibleArmor(ItemStack stack, EquipmentSlot slot) {
		if (stack == null || stack.isEmpty() || slot == null || stack.has(DataComponents.GLIDER)) {
			return false;
		}

		TagKey<Item> armorTag = switch (slot) {
			case FEET -> ItemTags.FOOT_ARMOR;
			case LEGS -> ItemTags.LEG_ARMOR;
			case CHEST -> ItemTags.CHEST_ARMOR;
			case HEAD -> ItemTags.HEAD_ARMOR;
			default -> null;
		};
		if (armorTag == null || !stack.is(armorTag)) {
			return false;
		}

		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		return equippable != null && equippable.slot() == slot
				&& equippable.canBeEquippedBy(EntityTypes.PLAYER.builtInRegistryHolder());
	}

	private static int getLevel(ItemStack stack, ResourceKey<Enchantment> enchantment, int maximumLevel) {
		if (stack == null || stack.isEmpty()) {
			return 0;
		}

		for (var entry : stack.getEnchantments().entrySet()) {
			if (entry.getKey().is(enchantment)) {
				return Math.max(0, Math.min(maximumLevel, entry.getIntValue()));
			}
		}
		return 0;
	}
}
