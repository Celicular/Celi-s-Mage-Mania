package com.celi.test.magic;

import com.celi.test.magic.mana.ManaMath;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;

public final class MageriteUpgrades {
	public static final int MAX_CLUSTERS = 10;
	public static final int MANA_PER_CLUSTER = 25;

	private MageriteUpgrades() {
	}

	public static boolean isUpgradeable(ItemStack stack) {
		if (stack == null || stack.isEmpty() || stack.getCount() != 1 || stack.has(DataComponents.GLIDER) || storedClusters(stack) < 0) {
			return false;
		}

		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (equippable == null || !equippable.canBeEquippedBy(EntityTypes.PLAYER.builtInRegistryHolder())) {
			return false;
		}

		EquipmentSlot slot = equippable.slot();
		TagKey<Item> armorTag = switch (slot) {
			case FEET -> ItemTags.FOOT_ARMOR;
			case LEGS -> ItemTags.LEG_ARMOR;
			case CHEST -> ItemTags.CHEST_ARMOR;
			case HEAD -> ItemTags.HEAD_ARMOR;
			default -> null;
		};
		if (armorTag == null) {
			return false;
		}
		if (stack.is(armorTag)) {
			return true;
		}

		ItemAttributeModifiers modifiers = stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);
		return modifiers.modifiers().stream().anyMatch(entry -> entry.attribute().equals(Attributes.ARMOR) && entry.slot().test(slot));
	}

	public static int getClusters(ItemStack stack) {
		return isUpgradeable(stack) ? storedClusters(stack) : 0;
	}

	public static int getBaseManaBonus(ItemStack stack) {
		return getClusters(stack) * MANA_PER_CLUSTER;
	}

	public static double getManaBonus(ItemStack stack) {
		int clusters = getClusters(stack);
		return clusters == 0 ? 0.0D : ManaMath.armorMana(clusters, MagicEnchantments.getManaStoreLevel(stack));
	}

	public static boolean tryUpgrade(ItemStack stack) {
		if (!isUpgradeable(stack)) {
			return false;
		}

		int clusters = storedClusters(stack);
		if (clusters < 0 || clusters >= MAX_CLUSTERS) {
			return false;
		}

		stack.set(MagicComponents.CLUSTERS, clusters + 1);
		return true;
	}

	private static int storedClusters(ItemStack stack) {
		Object value = stack.get(MagicComponents.CLUSTERS);
		if (value == null) {
			return 0;
		}
		return value instanceof Integer clusters && clusters >= 0 && clusters <= MAX_CLUSTERS ? clusters : -1;
	}
}
