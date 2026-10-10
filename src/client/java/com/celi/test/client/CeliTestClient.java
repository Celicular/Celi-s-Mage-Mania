package com.celi.test.client;

import com.celi.test.client.magic.MageriteArmorRendering;
import com.celi.test.client.magic.MagesAltarScreen;
import com.celi.test.client.magic.MagicClient;
import com.celi.test.client.magic.StaffClient;
import com.celi.test.entity.ModEntities;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

public final class CeliTestClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		EntityRendererRegistry.register(ModEntities.RUBY_AXE_PROJECTILE, RubyAxeProjectileRenderer::new);
		MagicClient.initialize();
		StaffClient.initialize();
		MagesAltarScreen.initialize();
		MageriteArmorRendering.initialize();
	}
}
