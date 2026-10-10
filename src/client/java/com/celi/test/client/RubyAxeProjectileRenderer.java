package com.celi.test.client;

import com.celi.test.entity.RubyAxeProjectileEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.util.List;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomModelData;

public final class RubyAxeProjectileRenderer extends EntityRenderer<RubyAxeProjectileEntity, RubyAxeProjectileRenderState> {
    private static final CustomModelData CHARGED_MODEL_DATA = new CustomModelData(List.of(), List.of(true), List.of(), List.of());
    private final ItemModelResolver itemModelResolver;

    public RubyAxeProjectileRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.itemModelResolver = context.getItemModelResolver();
    }

    @Override
    public RubyAxeProjectileRenderState createRenderState() {
        return new RubyAxeProjectileRenderState();
    }

    @Override
    public void extractRenderState(RubyAxeProjectileEntity entity, RubyAxeProjectileRenderState state, float partialTick) {
        super.extractRenderState(entity, state, partialTick);
        ItemStack stack = entity.getItem().copy();
        stack.set(DataComponents.CUSTOM_MODEL_DATA, CHARGED_MODEL_DATA);
        itemModelResolver.updateForNonLiving(state.item, stack, ItemDisplayContext.GROUND, entity);
        state.spin = entity.getYRot(partialTick);
    }

    @Override
    public void submit(RubyAxeProjectileRenderState state, PoseStack poseStack, SubmitNodeCollector submitNodeCollector,
                       CameraRenderState cameraRenderState) {
        poseStack.pushPose();
        poseStack.scale(1.35F, 1.35F, 1.35F);
        poseStack.rotate(cameraRenderState.orientation);
        poseStack.rotateDegrees(Axis.ZP, state.spin);
        state.item.submit(poseStack, submitNodeCollector, LightCoordsUtil.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, state.outlineColor);
        poseStack.popPose();
        super.submit(state, poseStack, submitNodeCollector, cameraRenderState);
    }
}
