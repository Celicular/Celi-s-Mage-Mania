package com.celi.test.client.magic;

import com.celi.test.CeliTest;
import com.celi.test.magic.mana.ManaMath;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

public final class ManaHud {
	private static final Identifier ID = Identifier.fromNamespaceAndPath(CeliTest.MOD_ID, "mana");
	private static final int WIDTH = 81;
	private static final int HEIGHT = 18;
	private static final int BORDER_COLOR = 0xFF335764;
	private static final int EMPTY_COLOR = 0xDD0D1C23;
	private static final int FILL_COLOR = 0xFF55AEC5;
	private static final int HIGHLIGHT_COLOR = 0xFF8BCFE0;
	private static final int TEXT_COLOR = 0xFF9ACFDE;

	private ManaHud() {
	}

	public static void initialize() {
		HudElementRegistry.attachElementAfter(VanillaHudElements.AIR_BAR, ID, ManaHud::extractRenderState);
		HudStatusBarHeightRegistry.addRight(ID, player -> isVisible(Minecraft.getInstance(), player) ? HEIGHT : 0);
	}

	private static boolean isVisible(Minecraft client, Player player) {
		return player != null && player == client.player && client.getCameraEntity() == player
				&& client.gameMode != null && client.gameMode.canHurtPlayer()
				&& !client.gui.hud.isHidden() && ClientManaState.isSynced(client);
	}

	private static void extractRenderState(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
		Minecraft client = Minecraft.getInstance();
		if (!isVisible(client, client.player)) {
			return;
		}
		int left = Math.max(4, graphics.guiWidth() / 2 + 10);
		int right = Math.min(graphics.guiWidth() - 4, left + WIDTH);
		int width = right - left;
		if (width < 4) {
			return;
		}
		int top = Math.max(2, graphics.guiHeight() - HudStatusBarHeightRegistry.getHeight(ID));
		int barTop = top + 10;
		double maximum = ClientManaState.getMaximum();
		double fraction = maximum > 0 ? ClientManaState.getCurrent() / maximum : 0;
		int filledWidth = (int) Math.round((width - 2) * fraction);
		graphics.fill(left, barTop, right, barTop + 6, BORDER_COLOR);
		graphics.fill(left + 1, barTop + 1, right - 1, barTop + 5, EMPTY_COLOR);
		if (filledWidth > 0) {
			graphics.fill(left + 1, barTop + 1, left + 1 + filledWidth, barTop + 5, FILL_COLOR);
			graphics.fill(left + 1, barTop + 1, left + 1 + filledWidth, barTop + 2, HIGHLIGHT_COLOR);
		}
		Component label = Component.translatable("hud.celitest.mana",
				ManaMath.format(ClientManaState.getCurrent()), ManaMath.format(maximum));
		int textWidth = Math.max(1, client.font.width(label));
		float textScale = Math.min(1.0F, (width - 2.0F) / textWidth);
		graphics.pose().pushMatrix();
		try {
			graphics.pose().translate(left + width / 2.0F, top);
			graphics.pose().scale(textScale, textScale);
			graphics.text(client.font, label, -textWidth / 2, 0, TEXT_COLOR, true);
		} finally {
			graphics.pose().popMatrix();
		}
	}
}
