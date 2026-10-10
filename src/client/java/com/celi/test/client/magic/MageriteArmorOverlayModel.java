package com.celi.test.client.magic;

import java.util.List;
import java.util.Map;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.ModelPart;

final class MageriteArmorOverlayModel<S> extends Model<S> {
	private final Model<? super S> original;

	MageriteArmorOverlayModel(Model<? super S> original, String limb) {
		super(new ModelPart(List.of(), Map.of(limb, original.root().getChild(limb))), original.renderType());
		this.original = original;
	}

	@Override
	public void setupAnim(S state) {
		original.setupAnim(state);
		ModelPart originalRoot = original.root();
		root.loadPose(originalRoot.storePose());
		root.xScale = originalRoot.xScale;
		root.yScale = originalRoot.yScale;
		root.zScale = originalRoot.zScale;
		root.visible = originalRoot.visible;
	}
}
