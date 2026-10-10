package com.celi.test.client.magic;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

public final class ClientManaState {
	private static LocalPlayer syncedPlayer;
	private static ClientLevel syncedLevel;
	private static double current;
	private static double maximum;

	private ClientManaState() {
	}

	public static double getCurrent() {
		return current;
	}

	public static double getMaximum() {
		return maximum;
	}

	public static boolean isSynced(Minecraft client) {
		return syncedPlayer != null && syncedPlayer == client.player && syncedLevel == client.level;
	}

	static void update(Minecraft client, double newCurrent, double newMaximum) {
		if (client.player == null || client.level == null || !Double.isFinite(newCurrent) || !Double.isFinite(newMaximum)) {
			reset();
			return;
		}
		syncedPlayer = client.player;
		syncedLevel = client.level;
		maximum = Math.max(0, newMaximum);
		current = Math.clamp(newCurrent, 0, maximum);
	}

	static void resetIfUnavailable(Minecraft client) {
		if (client.player == null || client.level == null || (syncedPlayer != null && !isSynced(client))) {
			reset();
		}
	}

	static void reset() {
		syncedPlayer = null;
		syncedLevel = null;
		current = 0;
		maximum = 0;
	}
}
