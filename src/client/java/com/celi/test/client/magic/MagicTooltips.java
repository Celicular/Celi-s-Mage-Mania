package com.celi.test.client.magic;

import java.util.List;

import com.celi.test.magic.MageriteUpgrades;
import com.celi.test.magic.MagicBlocks;
import com.celi.test.magic.MagicEnchantments;
import com.celi.test.magic.MagicItems;
import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.StaffAbility;
import com.celi.test.magic.mana.ManaMath;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class MagicTooltips {
	private static final EquipmentSlot[] ARMOR_SLOTS = {
			EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
	};

	private MagicTooltips() {
	}

	public static void initialize() {
		ItemTooltipCallback.EVENT.register(MagicTooltips::appendTooltip);
	}

	private static void appendTooltip(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines) {
		if (stack.getItem() == MagicBlocks.MAGERALD_ORE.asItem()) {
			add(lines, "tooltip.celitest.magerald_ore.source", ChatFormatting.AQUA);
			add(lines, "tooltip.celitest.magerald_ore.drops", ChatFormatting.GRAY);
			add(lines, "tooltip.celitest.magerald_ore.silk_touch", ChatFormatting.GRAY);
		} else if (stack.getItem() == MagicItems.MAGERALD_SHARD) {
			add(lines, "tooltip.celitest.magerald_shard.crafting", ChatFormatting.AQUA);
			add(lines, "tooltip.celitest.magerald_shard.altar", ChatFormatting.GRAY);
		} else if (stack.getItem() == MagicItems.MAGERITE_CLUSTER) {
			add(lines, "tooltip.celitest.magerite_cluster.purpose", ChatFormatting.AQUA);
			add(lines, "tooltip.celitest.magerite_cluster.mana", ChatFormatting.GOLD,
					ManaMath.format(MageriteUpgrades.MANA_PER_CLUSTER));
			add(lines, "tooltip.celitest.magerite_cluster.recipe", ChatFormatting.GRAY);
			add(lines, "tooltip.celitest.magerite.max_clusters", ChatFormatting.GRAY, MageriteUpgrades.MAX_CLUSTERS);
		} else if (stack.getItem() == MagicBlocks.MAGES_ALTAR.asItem()) {
			add(lines, "tooltip.celitest.mages_altar.purpose", ChatFormatting.AQUA);
			add(lines, "tooltip.celitest.mages_altar.instructions", ChatFormatting.GRAY);
			add(lines, "tooltip.celitest.magerite.max_clusters", ChatFormatting.GRAY, MageriteUpgrades.MAX_CLUSTERS);
		} else if (stack.getItem() == MagicItems.MAGES_STAFF) {
			add(lines, "tooltip.celitest.mages_staff.description", ChatFormatting.GOLD);
			Affinity affinity = StaffClient.getAffinity();
			add(lines, "tooltip.celitest.mages_staff.affinity", affinityColor(affinity),
					Component.translatable(affinity.translationKey()));
			if (affinity == Affinity.NONE) {
				add(lines, "tooltip.celitest.mages_staff.attune", ChatFormatting.GRAY);
			} else {
				appendAffinity(lines, affinity);
				add(lines, "tooltip.celitest.mages_staff.channel", ChatFormatting.DARK_GRAY);
			}
		} else if (stack.getItem() == MagicItems.FIRE_AFFINITY_STONE) {
			appendStone(lines, Affinity.FIRE, "item.minecraft.lava_bucket");
		} else if (stack.getItem() == MagicItems.WATER_AFFINITY_STONE) {
			appendStone(lines, Affinity.WATER, "item.minecraft.water_bucket");
		} else if (stack.getItem() == MagicItems.WIND_AFFINITY_STONE) {
			appendStone(lines, Affinity.WIND, "item.minecraft.wind_charge");
		} else if (stack.getItem() == MagicItems.EARTH_AFFINITY_STONE) {
			appendStone(lines, Affinity.EARTH, "item.minecraft.dirt");
		}
		int clusters = MageriteUpgrades.getClusters(stack);
		if (clusters > 0) {
			add(lines, "tooltip.celitest.magerite.upgrades", ChatFormatting.AQUA,
					clusters, MageriteUpgrades.MAX_CLUSTERS);
			add(lines, "tooltip.celitest.magerite.base_bonus", ChatFormatting.GRAY,
					ManaMath.format(MageriteUpgrades.getBaseManaBonus(stack)));
			int storeLevel = isCompatibleArmor(stack) ? MagicEnchantments.getManaStoreLevel(stack) : 0;
			if (storeLevel > 0) {
				add(lines, "tooltip.celitest.mana_store.bonus", ChatFormatting.LIGHT_PURPLE,
						Component.translatable("enchantment.level." + storeLevel),
						ManaMath.format(ManaMath.storeBonusPercent(storeLevel)));
			}
			add(lines, "tooltip.celitest.magerite.bonus", ChatFormatting.GOLD,
					ManaMath.format(MageriteUpgrades.getManaBonus(stack)));
			add(lines, "tooltip.celitest.magerite.per_cluster", ChatFormatting.GRAY,
					ManaMath.format(MageriteUpgrades.MANA_PER_CLUSTER));
			add(lines, "tooltip.celitest.magerite.equipped", ChatFormatting.GRAY);
		}
		appendManaRegeneration(stack, lines);
	}

	private static void appendStone(List<Component> lines, Affinity affinity, String centerItemKey) {
		add(lines, "tooltip.celitest.affinity_stone.attune", affinityColor(affinity),
				Component.translatable(affinity.translationKey()));
		appendAffinity(lines, affinity);
		add(lines, "tooltip.celitest.affinity_stone.consume", ChatFormatting.GRAY);
		add(lines, "tooltip.celitest.affinity_stone.recipe", ChatFormatting.DARK_GRAY,
				Component.translatable(centerItemKey));
	}

	private static void appendAffinity(List<Component> lines, Affinity affinity) {
		StaffAbility primary = StaffAbility.forAffinity(affinity, false);
		StaffAbility secondary = StaffAbility.forAffinity(affinity, true);
		add(lines, "tooltip.celitest.affinity.active", affinityColor(affinity),
				Component.translatable(primary.translationKey()), Component.translatable(secondary.translationKey()));
		add(lines, "tooltip.celitest.affinity.passive", ChatFormatting.GRAY,
				Component.translatable("tooltip.celitest.affinity." + affinity.id() + ".passive"));
		if (affinity == Affinity.FIRE) {
			add(lines, "tooltip.celitest.affinity.fire.lava", ChatFormatting.DARK_GRAY);
		}
		add(lines, "tooltip.celitest.mages_staff.primary", ChatFormatting.GRAY,
				ManaMath.format(primary.cooldownTicks() / 20.0), primary.manaCost());
		add(lines, "tooltip.celitest.mages_staff.secondary", ChatFormatting.GRAY,
				ManaMath.format(secondary.castTicks() / 20.0), ManaMath.format(secondary.cooldownTicks() / 20.0),
				secondary.manaCost());
		add(lines, "tooltip.celitest.ability." + primary.id(), ChatFormatting.DARK_GRAY);
		add(lines, "tooltip.celitest.ability." + secondary.id(), ChatFormatting.DARK_GRAY);
	}

	private static ChatFormatting affinityColor(Affinity affinity) {
		return switch (affinity) {
			case FIRE -> ChatFormatting.GOLD;
			case WATER -> ChatFormatting.BLUE;
			case WIND -> ChatFormatting.AQUA;
			case EARTH -> ChatFormatting.GREEN;
			case NONE -> ChatFormatting.GRAY;
		};
	}

	private static void appendManaRegeneration(ItemStack stack, List<Component> lines) {
		if (!isCompatibleArmor(stack)) {
			return;
		}
		int level = MagicEnchantments.getManaRegenLevel(stack);
		if (level <= 0) {
			return;
		}
		add(lines, "tooltip.celitest.mana_regen.bonus", ChatFormatting.LIGHT_PURPLE,
				Component.translatable("enchantment.level." + level), ManaMath.format(ManaMath.regenBonusPercent(level)));
		add(lines, "tooltip.celitest.mana_regen.equipped", ChatFormatting.GRAY);
		Player player = Minecraft.getInstance().player;
		if (player == null) {
			return;
		}
		int totalLevel = 0;
		double totalBonusPercent = 0;
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			ItemStack equipped = player.getItemBySlot(slot);
			if (MagicEnchantments.isCompatibleArmor(equipped, slot)) {
				int equippedLevel = MagicEnchantments.getManaRegenLevel(equipped);
				totalLevel += equippedLevel;
				totalBonusPercent += ManaMath.regenBonusPercent(equippedLevel);
			}
		}
		add(lines, "tooltip.celitest.mana_regen.total", ChatFormatting.AQUA,
				totalLevel, ManaMath.format(totalBonusPercent));
		add(lines, "tooltip.celitest.mana_regen.rate", ChatFormatting.GOLD,
				ManaMath.format(ManaMath.regeneration(totalLevel)), ManaMath.REGENERATION_INTERVAL_TICKS);
	}

	private static boolean isCompatibleArmor(ItemStack stack) {
		for (EquipmentSlot slot : ARMOR_SLOTS) {
			if (MagicEnchantments.isCompatibleArmor(stack, slot)) {
				return true;
			}
		}
		return false;
	}

	private static void add(List<Component> lines, String key, ChatFormatting color, Object... arguments) {
		lines.add(Component.translatable(key, arguments).withStyle(color));
	}
}
