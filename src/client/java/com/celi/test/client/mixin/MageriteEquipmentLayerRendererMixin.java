package com.celi.test.client.mixin;

import com.celi.test.client.magic.MageriteArmorRendering;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EquipmentLayerRenderer;
import net.minecraft.client.resources.model.EquipmentAssetManager;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.equipment.EquipmentAsset;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EquipmentLayerRenderer.class)
public abstract class MageriteEquipmentLayerRendererMixin {
	@Shadow
	@Final
	private EquipmentAssetManager equipmentAssets;

	@Inject(method = "renderLayers(Lnet/minecraft/client/resources/model/EquipmentClientInfo$LayerType;Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/client/model/Model;Ljava/lang/Object;Lnet/minecraft/world/item/ItemStack;Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/resources/Identifier;II)V", at = @At("TAIL"))
	private <S> void celitest$submitMageriteOverlay(EquipmentClientInfo.LayerType layerType,
			ResourceKey<EquipmentAsset> asset, Model<? super S> model, S state, ItemStack stack,
			PoseStack poseStack, SubmitNodeCollector collector, int light, Identifier playerTexture,
			int outlineColor, int firstOrder, CallbackInfo info) {
		int layerCount = equipmentAssets.get(asset).getLayers(layerType).size();
		if (layerCount != 0) {
			MageriteArmorRendering.submitOverlay(layerType, model, state, stack, poseStack,
					collector, light, outlineColor, firstOrder + layerCount + 2);
		}
	}
}
