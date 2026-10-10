package com.celi.test.client.magic;

import com.celi.test.magic.MageriteUpgrades;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.Equippable;

public final class MageriteArmorRendering {
	private MageriteArmorRendering() {
	}

	public static void initialize() {
	}

	public static <S> void submitOverlay(EquipmentClientInfo.LayerType layerType, Model<? super S> model,
			S state, ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector, int light,
			int outlineColor, int order) {
		if (!(state instanceof HumanoidRenderState) || !MageriteUpgrades.isUpgradeable(stack)) {
			return;
		}

		int clusters = MageriteUpgrades.getClusters(stack);
		Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
		if (clusters == 0 || equippable == null || !HumanoidArmorLayer.shouldRender(stack, equippable.slot())) {
			return;
		}

		EquipmentSlot slot = equippable.slot();
		boolean baby = layerType == EquipmentClientInfo.LayerType.HUMANOID_BABY;
		if (!baby && layerType != (slot == EquipmentSlot.LEGS
				? EquipmentClientInfo.LayerType.HUMANOID_LEGGINGS : EquipmentClientInfo.LayerType.HUMANOID)) {
			return;
		}

		String slotName = switch (slot) {
			case HEAD -> "head";
			case CHEST -> "chest";
			case LEGS -> "legs";
			case FEET -> "feet";
			default -> null;
		};
		if (slotName == null) {
			return;
		}

		if (slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
			if (!model.root().hasChild("right_leg") || !model.root().hasChild("left_leg")) {
				return;
			}
			submitPart(layerType, new MageriteArmorOverlayModel<>(model, "right_leg"), state,
					stack, poseStack, collector, light, outlineColor, order, slotName + "_right", clusters);
			submitPart(layerType, new MageriteArmorOverlayModel<>(model, "left_leg"), state,
					stack, poseStack, collector, light, outlineColor, order, slotName + "_left", clusters);
		} else {
			submitPart(layerType, model, state, stack, poseStack, collector, light, outlineColor,
					order, slotName, clusters);
		}
	}

	private static <S> void submitPart(EquipmentClientInfo.LayerType layerType, Model<? super S> model,
			S state, ItemStack stack, PoseStack poseStack, SubmitNodeCollector collector, int light,
			int outlineColor, int order, String part, int clusters) {
		String path = "textures/entity/equipment/" + layerType.getSerializedName() + "/magerite/"
				+ part + "_" + clusters;
		Identifier texture = Identifier.fromNamespaceAndPath("celitest", path + ".png");
		RenderType renderType = stack.hasFoil()
				? RenderTypes.armorCutoutNoCullGlint(texture) : RenderTypes.armorCutoutNoCull(texture);
		collector.order(order).submitModel(model, state, poseStack, renderType, light,
				OverlayTexture.NO_OVERLAY, -1, null, outlineColor);
		if (clusters >= 5) {
			Identifier glow = Identifier.fromNamespaceAndPath("celitest", path + "_glow.png");
			collector.order(order + 1).submitModel(model, state, poseStack, RenderTypes.eyes(glow),
					light, OverlayTexture.NO_OVERLAY, -1, null, 0);
		}
	}
}
