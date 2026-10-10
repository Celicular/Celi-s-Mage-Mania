package com.celi.test.client.magic;

import com.celi.test.magic.MageriteUpgrades;
import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.altar.MagesAltarMenu;
import com.celi.test.magic.altar.MagicMenus;
import com.celi.test.magic.mana.ManaMath;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.List;

public final class MagesAltarScreen extends AbstractContainerScreen<MagesAltarMenu> {
	private static final int WIDTH = 248;
	private static final int HEIGHT = 240;
	private static final int STONE = 0xFF2B3239;
	private static final int STONE_LIGHT = 0xFF58616A;
	private static final int STONE_DARK = 0xFF141A20;
	private static final int GOLD = 0xFFE0C38A;
	private static final int GOLD_DARK = 0xFF796343;
	private static final int AQUA = 0xFF9FD8DE;
	private static final int AQUA_DARK = 0xFF3F717B;
	private static final int TEXT = 0xFFE0E4E5;
	private static final int MUTED = 0xFFA8B2BA;
	private AltarButton upgradeTab;
	private AltarButton affinityTab;
	private AltarButton applyButton;
	private AltarButton setAffinityButton;
	private Component upgradeTooltip;
	private Component affinityTooltip;

	public MagesAltarScreen(MagesAltarMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title, WIDTH, HEIGHT);
		inventoryLabelX = MagesAltarMenu.INVENTORY_X;
		inventoryLabelY = 147;
	}

	public static void initialize() {
		MenuScreens.register(MagicMenus.MAGES_ALTAR, MagesAltarScreen::new);
	}

	@Override
	protected void init() {
		super.init();
		upgradeTab = addRenderableWidget(new AltarButton(leftPos + 8, topPos + 24, 112, 18, text("upgrade_tab"), button -> selectTab(MagesAltarMenu.BUTTON_TAB_UPGRADE)));
		affinityTab = addRenderableWidget(new AltarButton(leftPos + 128, topPos + 24, 112, 18, text("affinity_tab"), button -> selectTab(MagesAltarMenu.BUTTON_TAB_AFFINITY)));
		upgradeTab.setTooltip(Tooltip.create(text("upgrade_tab_tooltip")));
		affinityTab.setTooltip(Tooltip.create(text("affinity_tab_tooltip")));
		applyButton = addRenderableWidget(new AltarButton(leftPos + 133, topPos + 68, 105, 20, text("apply"), button -> applyUpgrade()));
		setAffinityButton = addRenderableWidget(new AltarButton(leftPos + 133, topPos + 70, 105, 20, text("set_affinity"), button -> setAffinity()));
		upgradeTooltip = null;
		affinityTooltip = null;
		updateControls();
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		updateControls();
		super.extractRenderState(graphics, mouseX, mouseY, partialTick);
	}

	@Override
	public void extractTransparentBackground(GuiGraphicsExtractor graphics) {
		graphics.fill(0, 0, width, height, 0xB014181C);
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
		super.extractBackground(graphics, mouseX, mouseY, partialTick);
		graphics.pose().pushMatrix();
		graphics.pose().translate(leftPos, topPos);
		bevel(graphics, 0, 0, WIDTH, HEIGHT, STONE, STONE_LIGHT, STONE_DARK);
		graphics.outline(2, 2, WIDTH - 4, HEIGHT - 4, GOLD_DARK);
		graphics.fill(5, 5, WIDTH - 5, 20, STONE_DARK);
		graphics.fill(8, 19, WIDTH - 8, 20, GOLD_DARK);
		graphics.fill(6, 45, WIDTH - 6, 146, 0xFF20272D);
		graphics.fill(MagesAltarMenu.INVENTORY_X - 3, 146, WIDTH - MagesAltarMenu.INVENTORY_X + 3, 234, 0xFF252D34);
		diamond(graphics, 11, 11, GOLD_DARK, GOLD);
		diamond(graphics, WIDTH - 12, 11, GOLD_DARK, GOLD);
		if (menu.getSelectedTab() == MagesAltarMenu.TAB_UPGRADE) {
			diamond(graphics, 22, 78, AQUA_DARK, AQUA);
			graphics.fill(30, 77, 39, 78, GOLD_DARK);
			graphics.fill(68, 77, 73, 78, GOLD_DARK);
			graphics.fill(70, 75, 71, 80, GOLD_DARK);
			graphics.fill(99, 77, 129, 78, GOLD_DARK);
			graphics.fill(127, 75, 128, 80, GOLD_DARK);
			graphics.fill(128, 76, 129, 79, GOLD);
			inputSlot(graphics, MagesAltarMenu.ARMOR_X, MagesAltarMenu.UPGRADE_INPUT_Y, GOLD_DARK);
			inputSlot(graphics, MagesAltarMenu.CLUSTER_X, MagesAltarMenu.UPGRADE_INPUT_Y, AQUA_DARK);
			graphics.fill(8, 95, WIDTH - 8, 96, GOLD_DARK);
			for (int cluster = 0; cluster < MageriteUpgrades.MAX_CLUSTERS; cluster++) {
				int x = 8 + cluster * 23;
				graphics.fill(x, 110, x + 20, 112, cluster < menu.getClusterCount() ? AQUA_DARK : STONE_DARK);
				if (cluster < menu.getClusterCount()) {
					graphics.fill(x, 110, x + 20, 111, AQUA);
				}
			}
		} else {
			inputSlot(graphics, MagesAltarMenu.STONE_X, MagesAltarMenu.STONE_Y, AQUA_DARK);
			graphics.fill(8, 104, WIDTH - 8, 105, GOLD_DARK);
			graphics.fill(123, 108, 124, 142, STONE_LIGHT);
		}
		for (int row = 0; row < 3; row++) {
			for (int column = 0; column < 9; column++) {
				inventorySlot(graphics, MagesAltarMenu.INVENTORY_X + column * 18, MagesAltarMenu.INVENTORY_Y + row * 18);
			}
		}
		for (int column = 0; column < 9; column++) {
			inventorySlot(graphics, MagesAltarMenu.INVENTORY_X + column * 18, MagesAltarMenu.HOTBAR_Y);
		}
		graphics.fill(MagesAltarMenu.INVENTORY_X, 211, WIDTH - MagesAltarMenu.INVENTORY_X, 212, STONE_DARK);
		graphics.fill(MagesAltarMenu.INVENTORY_X, 212, WIDTH - MagesAltarMenu.INVENTORY_X, 213, GOLD_DARK);
		graphics.pose().popMatrix();
	}

	@Override
	protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		drawCenteredFitted(graphics, title, WIDTH / 2, 7, WIDTH - 38, GOLD);
		if (menu.getSelectedTab() == MagesAltarMenu.TAB_UPGRADE) {
			extractUpgradeLabels(graphics);
		} else {
			extractAffinityLabels(graphics);
		}
		drawFitted(graphics, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 160, MUTED);
	}

	private void extractUpgradeLabels(GuiGraphicsExtractor graphics) {
		ItemStack armor = menu.getArmor();
		drawCenteredFitted(graphics, armor.isEmpty() ? text("select_armor") : armor.getHoverName(), WIDTH / 2, 47, WIDTH - 16, armor.isEmpty() ? MUTED : TEXT);
		drawCenteredFitted(graphics, text("armor_input"), 52, 59, 34, GOLD);
		drawCenteredFitted(graphics, text("cluster_input"), 88, 59, 42, AQUA);
		int clusters = menu.getClusterCount();
		drawFitted(graphics, text("upgrades", clusters, MageriteUpgrades.MAX_CLUSTERS), 8, 98, 170, TEXT);
		String remaining = fitted(text("remaining", MageriteUpgrades.MAX_CLUSTERS - clusters), 54);
		graphics.text(font, remaining, WIDTH - 8 - font.width(remaining), 98, MUTED, false);
		drawFitted(graphics, text("current_bonus", ManaMath.format(menu.getCurrentBonus())), 8, 117, WIDTH - 16, AQUA);
		Component preview;
		int previewColor = AQUA;
		if (armor.isEmpty()) {
			preview = text("insert_armor");
			previewColor = MUTED;
		} else if (!MageriteUpgrades.isUpgradeable(armor)) {
			preview = text("invalid_armor");
			previewColor = MUTED;
		} else if (clusters >= MageriteUpgrades.MAX_CLUSTERS) {
			preview = text("fully_upgraded");
			previewColor = GOLD;
		} else {
			preview = text("next_bonus", ManaMath.format(menu.getNextBonus() - menu.getCurrentBonus()), ManaMath.format(menu.getNextBonus()));
		}
		drawFitted(graphics, preview, 8, 131, WIDTH - 16, previewColor);
	}

	private void extractAffinityLabels(GuiGraphicsExtractor graphics) {
		drawFitted(graphics, text("current_affinity", affinityName(menu.getCurrentAffinity())), 8, 47, WIDTH - 16, GOLD);
		drawFitted(graphics, text("stone_input"), 8, 60, 120, AQUA);
		ItemStack stone = menu.getSelectedStone();
		drawFitted(graphics, stone.isEmpty() ? text("select_stone") : stone.getHoverName(), 34, 76, 95, stone.isEmpty() ? MUTED : TEXT);
		drawCenteredFitted(graphics, affinityFeedback(), WIDTH / 2, 93, WIDTH - 16, menu.canSetAffinity() ? AQUA : MUTED);
		drawFitted(graphics, text("active"), 8, 108, 112, GOLD);
		drawFitted(graphics, text("passive"), 128, 108, 112, GOLD);
		drawWrapped(graphics, affinityDescription("active"), 8, 119, 112, TEXT);
		drawWrapped(graphics, affinityDescription("passive"), 128, 119, 112, TEXT);
	}

	@Override
	protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
		super.extractTooltip(graphics, mouseX, mouseY);
		if (!menu.getCarried().isEmpty()) {
			return;
		}
		if (menu.getSelectedTab() == MagesAltarMenu.TAB_UPGRADE) {
			if (isHovering(MagesAltarMenu.ARMOR_X, MagesAltarMenu.UPGRADE_INPUT_Y, 16, 16, mouseX, mouseY) && menu.getArmor().isEmpty()) {
				graphics.setTooltipForNextFrame(font, text("armor_input_tooltip"), mouseX, mouseY);
			} else if (isHovering(MagesAltarMenu.CLUSTER_X, MagesAltarMenu.UPGRADE_INPUT_Y, 16, 16, mouseX, mouseY) && !menu.slots.get(MagesAltarMenu.CLUSTER_SLOT).hasItem()) {
				graphics.setTooltipForNextFrame(font, text("cluster_input_tooltip"), mouseX, mouseY);
			} else if (isHovering(8, 47, WIDTH - 16, 9, mouseX, mouseY) && !menu.getArmor().isEmpty()) {
				graphics.setTooltipForNextFrame(font, menu.getArmor().getHoverName(), mouseX, mouseY);
			} else if (isHovering(8, 98, WIDTH - 16, 14, mouseX, mouseY)) {
				int clusters = menu.getClusterCount();
				graphics.setTooltipForNextFrame(font, text("capacity_tooltip", clusters, MageriteUpgrades.MAX_CLUSTERS, MageriteUpgrades.MAX_CLUSTERS - clusters), mouseX, mouseY);
			}
		} else if (isHovering(MagesAltarMenu.STONE_X, MagesAltarMenu.STONE_Y, 16, 16, mouseX, mouseY) && menu.getSelectedStone().isEmpty()) {
			graphics.setTooltipForNextFrame(font, text("stone_input_tooltip"), mouseX, mouseY);
		} else if (isHovering(34, 76, 95, 9, mouseX, mouseY) && !menu.getSelectedStone().isEmpty()) {
			graphics.setTooltipForNextFrame(font, menu.getSelectedStone().getHoverName(), mouseX, mouseY);
		} else if (isHovering(8, 93, WIDTH - 16, 9, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, affinityFeedback(), mouseX, mouseY);
		} else if (isHovering(8, 108, 112, 36, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, affinityDescription("active"), mouseX, mouseY);
		} else if (isHovering(128, 108, 112, 36, mouseX, mouseY)) {
			graphics.setTooltipForNextFrame(font, affinityDescription("passive"), mouseX, mouseY);
		}
	}

	private void applyUpgrade() {
		if (menu.canUpgrade() && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MagesAltarMenu.BUTTON_UPGRADE);
		}
	}

	private void selectTab(int buttonId) {
		if (minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, buttonId);
		}
	}

	private void setAffinity() {
		if (menu.canSetAffinity() && minecraft.gameMode != null) {
			minecraft.gameMode.handleInventoryButtonClick(menu.containerId, MagesAltarMenu.BUTTON_SET_AFFINITY);
		}
	}

	private void updateControls() {
		boolean upgrade = menu.getSelectedTab() == MagesAltarMenu.TAB_UPGRADE;
		boolean connected = minecraft.gameMode != null;
		upgradeTab.selected = upgrade;
		upgradeTab.hasInputs = !menu.getArmor().isEmpty() || menu.slots.get(MagesAltarMenu.CLUSTER_SLOT).hasItem();
		upgradeTab.active = !upgrade && connected;
		affinityTab.selected = !upgrade;
		affinityTab.hasInputs = !menu.getSelectedStone().isEmpty();
		affinityTab.active = upgrade && connected;
		applyButton.visible = upgrade;
		applyButton.active = upgrade && menu.canUpgrade() && connected;
		setAffinityButton.visible = !upgrade;
		setAffinityButton.active = !upgrade && menu.canSetAffinity() && connected;
		if (!applyButton.visible) {
			applyButton.setFocused(false);
		} else {
			setAffinityButton.setFocused(false);
		}
		Component tooltip;
		if (menu.getArmor().isEmpty()) {
			tooltip = text("insert_armor");
		} else if (!MageriteUpgrades.isUpgradeable(menu.getArmor())) {
			tooltip = text("invalid_armor");
		} else if (menu.getClusterCount() >= MageriteUpgrades.MAX_CLUSTERS) {
			tooltip = text("fully_upgraded");
		} else if (!menu.canUpgrade()) {
			tooltip = text("missing_cluster");
		} else {
			tooltip = text("apply_tooltip", ManaMath.format(menu.getNextBonus() - menu.getCurrentBonus()), ManaMath.format(menu.getNextBonus()));
		}
		if (!tooltip.equals(upgradeTooltip)) {
			upgradeTooltip = tooltip;
			applyButton.setTooltip(Tooltip.create(tooltip));
		}
		Component stoneTooltip = menu.canSetAffinity()
				? text("set_affinity_tooltip", menu.getSelectedStone().getHoverName(), affinityName(menu.getStoneAffinity()))
				: affinityFeedback();
		if (!stoneTooltip.equals(affinityTooltip)) {
			affinityTooltip = stoneTooltip;
			setAffinityButton.setTooltip(Tooltip.create(stoneTooltip));
		}
	}

	private Component affinityFeedback() {
		if (menu.getSelectedStone().isEmpty()) {
			return text("missing_stone");
		}
		if (menu.getStoneAffinity() == Affinity.NONE) {
			return text("invalid_stone");
		}
		if (menu.getStoneAffinity() == menu.getCurrentAffinity()) {
			return text("same_affinity");
		}
		return text("ready_affinity");
	}

	private Component affinityDescription(String detail) {
		Affinity affinity = menu.getStoneAffinity();
		if (affinity == Affinity.NONE) {
			affinity = menu.getCurrentAffinity();
		}
		return Component.translatable("affinity.celitest." + affinity.id() + "." + detail);
	}

	private static Component affinityName(Affinity affinity) {
		return Component.translatable(affinity.translationKey());
	}

	private static Component text(String key, Object... arguments) {
		return Component.translatable("screen.celitest.mages_altar." + key, arguments);
	}

	private String fitted(Component component, int availableWidth) {
		String value = component.getString();
		return font.width(value) <= availableWidth ? value : font.plainSubstrByWidth(value, availableWidth - font.width("...")) + "...";
	}

	private void drawFitted(GuiGraphicsExtractor graphics, Component component, int x, int y, int availableWidth, int color) {
		graphics.text(font, fitted(component, availableWidth), x, y, color, false);
	}

	private void drawCenteredFitted(GuiGraphicsExtractor graphics, Component component, int centerX, int y, int availableWidth, int color) {
		String value = fitted(component, availableWidth);
		graphics.text(font, value, centerX - font.width(value) / 2, y, color, false);
	}

	private void drawWrapped(GuiGraphicsExtractor graphics, Component component, int x, int y, int availableWidth, int color) {
		List<FormattedCharSequence> lines = font.split(component, availableWidth);
		for (int line = 0; line < Math.min(3, lines.size()); line++) {
			graphics.text(font, lines.get(line), x, y + line * 9, color, false);
		}
	}

	private static void inventorySlot(GuiGraphicsExtractor graphics, int x, int y) {
		bevel(graphics, x - 1, y - 1, 18, 18, 0xFF303C44, STONE_DARK, STONE_LIGHT);
	}

	private static void inputSlot(GuiGraphicsExtractor graphics, int x, int y, int accent) {
		graphics.fill(x - 2, y - 2, x + 18, y + 18, accent);
		inventorySlot(graphics, x, y);
	}

	private static void diamond(GuiGraphicsExtractor graphics, int x, int y, int edge, int center) {
		graphics.fill(x - 1, y - 3, x + 2, y + 4, edge);
		graphics.fill(x - 3, y - 1, x + 4, y + 2, edge);
		graphics.fill(x, y - 1, x + 1, y + 2, center);
		graphics.fill(x - 1, y, x + 2, y + 1, center);
	}

	private static void bevel(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int fill, int light, int dark) {
		graphics.fill(x, y, x + width, y + height, dark);
		graphics.fill(x, y, x + width - 1, y + 1, light);
		graphics.fill(x, y, x + 1, y + height - 1, light);
		graphics.fill(x + 1, y + 1, x + width - 1, y + height - 1, fill);
	}

	private static final class AltarButton extends Button {
		private boolean selected;
		private boolean hasInputs;

		private AltarButton(int x, int y, int width, int height, Component label, OnPress onPress) {
			super(x, y, width, height, label.copy().withStyle(style -> style.withColor(GOLD & 0xFFFFFF)), onPress, DEFAULT_NARRATION);
		}

		@Override
		protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
			boolean highlighted = active && isHoveredOrFocused();
			int edge = selected || highlighted ? GOLD : active ? GOLD_DARK : STONE_DARK;
			bevel(graphics, getX(), getY(), getWidth(), getHeight(), selected ? 0xFF3B4243 : highlighted ? 0xFF43453C : STONE, edge, STONE_DARK);
			graphics.outline(getX() + 1, getY() + 1, getWidth() - 2, getHeight() - 2, selected || active ? GOLD_DARK : 0xFF3C454D);
			if (selected) {
				graphics.fill(getX() + 3, getBottom() - 2, getRight() - 3, getBottom() - 1, AQUA);
			}
			if (hasInputs) {
				diamond(graphics, getRight() - 8, getY() + getHeight() / 2, AQUA_DARK, AQUA);
			}
			extractDefaultLabel(graphics.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
		}
	}
}
