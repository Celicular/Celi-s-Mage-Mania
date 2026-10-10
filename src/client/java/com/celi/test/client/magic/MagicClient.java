package com.celi.test.client.magic;

import com.celi.test.magic.mana.ManaSyncPayload;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLevelEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

public final class MagicClient {
	private MagicClient() {
	}

	public static void initialize() {
		ClientPlayNetworking.registerGlobalReceiver(ManaSyncPayload.TYPE, (payload, context) -> {
			var client = context.client();
			var player = context.player();
			var level = client.level;
			client.execute(() -> {
				if (client.player == player && client.level == level && level != null) {
					ClientManaState.update(client, payload.current(), payload.maximum());
				}
			});
		});
		ClientPlayConnectionEvents.INIT.register((handler, client) -> client.execute(ClientManaState::reset));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(ClientManaState::reset));
		ClientLevelEvents.AFTER_CLIENT_LEVEL_CHANGE.register((client, level) -> ClientManaState.resetIfUnavailable(client));
		ClientTickEvents.END_CLIENT_TICK.register(ClientManaState::resetIfUnavailable);
		ManaHud.initialize();
		MagicTooltips.initialize();
	}
}
