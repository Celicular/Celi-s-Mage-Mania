package com.celi.test.magic.altar;

import com.celi.test.magic.MageriteUpgrades;
import com.celi.test.magic.MagicBlocks;
import com.celi.test.magic.MagicEnchantments;
import com.celi.test.magic.MagicItems;
import com.celi.test.magic.affinity.Affinity;
import com.celi.test.magic.affinity.AffinitySystem;
import com.celi.test.magic.mana.ManaMath;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Prediction;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.gamerules.GameRules;

public final class MagesAltarMenu extends AbstractContainerMenu {
    public static final int TAB_UPGRADE = 0;
    public static final int TAB_AFFINITY = 1;
    public static final int BUTTON_UPGRADE = 0;
    public static final int BUTTON_SET_AFFINITY = 1;
    public static final int BUTTON_TAB_UPGRADE = 2;
    public static final int BUTTON_TAB_AFFINITY = 3;
    public static final int ARMOR_SLOT = 0;
    public static final int CLUSTER_SLOT = 1;
    public static final int STONE_SLOT = 2;
    public static final int ARMOR_X = 44;
    public static final int CLUSTER_X = 80;
    public static final int UPGRADE_INPUT_Y = 70;
    public static final int STONE_X = 8;
    public static final int STONE_Y = 73;
    public static final int INVENTORY_X = 43;
    public static final int INVENTORY_Y = 158;
    public static final int HOTBAR_Y = 216;
    private static final int PLAYER_INVENTORY_START = 3;
    private static final int HOTBAR_START = 30;
    private static final int PLAYER_INVENTORY_END = 39;
    private static final Affinity[] AFFINITIES = Affinity.values();

    private final Player owner;
    private final ContainerLevelAccess access;
    private final DataSlot selectedTab = DataSlot.standalone();
    private final DataSlot currentAffinity;
    private final SimpleContainer inputs = new SimpleContainer(3) {
        @Override
        public void setChanged() {
            super.setChanged();
            MagesAltarMenu.this.slotsChanged(this);
        }
    };

    public MagesAltarMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, ContainerLevelAccess.NULL);
    }

    public MagesAltarMenu(int containerId, Inventory inventory, ContainerLevelAccess access) {
        super(MagicMenus.MAGES_ALTAR, containerId);
        this.owner = inventory.player;
        this.access = access;
        addDataSlot(selectedTab);
        currentAffinity = addDataSlot(new DataSlot() {
            private int value = Affinity.NONE.ordinal();

            @Override
            public int get() {
                return owner instanceof ServerPlayer ? AffinitySystem.getAffinity(owner).ordinal() : value;
            }

            @Override
            public void set(int value) {
                this.value = value;
            }
        });

        addSlot(new TabSlot(ARMOR_SLOT, ARMOR_X, UPGRADE_INPUT_Y, TAB_UPGRADE) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isActive() && MageriteUpgrades.isUpgradeable(stack);
            }

            @Override
            public int getMaxStackSize() {
                return 1;
            }
        });
        addSlot(new TabSlot(CLUSTER_SLOT, CLUSTER_X, UPGRADE_INPUT_Y, TAB_UPGRADE) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isActive() && stack.is(MagicItems.MAGERITE_CLUSTER);
            }
        });
        addSlot(new TabSlot(STONE_SLOT, STONE_X, STONE_Y, TAB_AFFINITY) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return isActive() && affinityForStone(stack) != Affinity.NONE;
            }
        });
        addInventoryExtendedSlots(inventory, INVENTORY_X, INVENTORY_Y);
        addInventoryHotbarSlots(inventory, INVENTORY_X, HOTBAR_Y);
    }

    public ItemStack getArmor() {
        return inputs.getItem(ARMOR_SLOT);
    }

    public int getClusterCount() {
        return MageriteUpgrades.getClusters(getArmor());
    }

    public boolean canUpgrade() {
        ItemStack armor = getArmor();
        ItemStack clusters = inputs.getItem(CLUSTER_SLOT);
        return getSelectedTab() == TAB_UPGRADE
                && owner.isAlive()
                && !owner.isSpectator()
                && armor.getCount() == 1
                && MageriteUpgrades.isUpgradeable(armor)
                && getClusterCount() < MageriteUpgrades.MAX_CLUSTERS
                && !clusters.isEmpty()
                && clusters.is(MagicItems.MAGERITE_CLUSTER);
    }

    public int getSelectedTab() {
        return selectedTab.get();
    }

    public ItemStack getSelectedStone() {
        return inputs.getItem(STONE_SLOT);
    }

    public Affinity getStoneAffinity() {
        return affinityForStone(getSelectedStone());
    }

    public Affinity getCurrentAffinity() {
        int index = currentAffinity.get();
        return index >= 0 && index < AFFINITIES.length ? AFFINITIES[index] : Affinity.NONE;
    }

    public boolean canSetAffinity() {
        Affinity affinity = getStoneAffinity();
        return getSelectedTab() == TAB_AFFINITY
                && owner.isAlive()
                && !owner.isSpectator()
                && affinity != Affinity.NONE
                && affinity != getCurrentAffinity();
    }

    public double getCurrentBonus() {
        return MageriteUpgrades.getManaBonus(getArmor());
    }

    public double getNextBonus() {
        ItemStack armor = getArmor();
        if (!MageriteUpgrades.isUpgradeable(armor)) {
            return 0;
        }
        int nextClusters = Math.min(getClusterCount() + 1, MageriteUpgrades.MAX_CLUSTERS);
        return ManaMath.armorMana(nextClusters, MagicEnchantments.getManaStoreLevel(armor));
    }

    @Override
    public boolean clickMenuButton(Player player, int buttonId) {
        if (!(player instanceof ServerPlayer serverPlayer)
                || player != owner
                || player.containerMenu != this
                || !player.isAlive()
                || player.isSpectator()
                || !stillValid(player)) {
            return false;
        }
        if (buttonId == BUTTON_TAB_UPGRADE || buttonId == BUTTON_TAB_AFFINITY) {
            selectedTab.set(buttonId == BUTTON_TAB_UPGRADE ? TAB_UPGRADE : TAB_AFFINITY);
            resetQuickCraft();
            broadcastChanges();
            return true;
        }
        if (buttonId == BUTTON_UPGRADE) {
            if (!canUpgrade() || !MageriteUpgrades.tryUpgrade(getArmor())) {
                return false;
            }
            inputs.getItem(CLUSTER_SLOT).shrink(1);
        } else if (buttonId == BUTTON_SET_AFFINITY) {
            if (!canSetAffinity() || !AffinitySystem.setAffinity(serverPlayer, getStoneAffinity())) {
                return false;
            }
            getSelectedStone().shrink(1);
        } else {
            return false;
        }
        inputs.setChanged();
        return true;
    }

    @Override
    public void clicked(int slotIndex, int button, ContainerInput input, Player player) {
        if (player != owner
                || player.containerMenu != this
                || player.isSpectator()
                || !stillValid(player)
                || slotIndex >= slots.size()
                || slotIndex >= 0 && !slots.get(slotIndex).isActive()) {
            resetQuickCraft();
            return;
        }
        super.clicked(slotIndex, button, input, player);
    }

    @Override
    public boolean canDragTo(Slot slot) {
        return slot.isActive() && super.canDragTo(slot);
    }

    @Override
    public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) {
        return slot.isActive() && super.canTakeItemForPickAll(stack, slot);
    }

    @Override
    public void setSelectedBundleItemIndex(int slotIndex, int itemIndex) {
        if (owner.containerMenu == this
                && !owner.isSpectator()
                && stillValid(owner)
                && slotIndex >= 0
                && slotIndex < slots.size()
                && slots.get(slotIndex).isActive()) {
            super.setSelectedBundleItemIndex(slotIndex, itemIndex);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (player != owner || player.isSpectator() || !stillValid(player) || slotIndex < 0 || slotIndex >= slots.size()) {
            return ItemStack.EMPTY;
        }
        Slot slot = slots.get(slotIndex);
        if (!slot.isActive() || !slot.hasItem() || !slot.mayPickup(player)) {
            return ItemStack.EMPTY;
        }

        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        boolean moved;
        if (slotIndex < PLAYER_INVENTORY_START) {
            moved = moveItemStackTo(stack, PLAYER_INVENTORY_START, PLAYER_INVENTORY_END, true);
        } else {
            moved = false;
            if (slots.get(ARMOR_SLOT).mayPlace(stack)) {
                moved = moveItemStackTo(stack, ARMOR_SLOT, ARMOR_SLOT + 1, false);
            } else if (slots.get(CLUSTER_SLOT).mayPlace(stack)) {
                moved = moveItemStackTo(stack, CLUSTER_SLOT, CLUSTER_SLOT + 1, false);
            } else if (slots.get(STONE_SLOT).mayPlace(stack)) {
                moved = moveItemStackTo(stack, STONE_SLOT, STONE_SLOT + 1, false);
            }
            if (!moved) {
                moved = slotIndex < HOTBAR_START
                        ? moveItemStackTo(stack, HOTBAR_START, PLAYER_INVENTORY_END, false)
                        : moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_START, false);
            }
        }
        if (!moved || stack.getCount() == original.getCount()) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        slot.onTake(player, stack);
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        return player == owner
                && player.isAlive()
                && !player.isRemoved()
                && access.evaluate((level, pos) -> level == player.level(), player.level().isClientSide())
                && stillValid(access, player, MagicBlocks.MAGES_ALTAR);
    }

    @Override
    public void removed(Player player) {
        if (player != owner) {
            return;
        }
        if (player instanceof ServerPlayer serverPlayer && !player.isAlive() && !serverPlayer.hasDisconnected()) {
            boolean keepInventory = serverPlayer.level().getGameRules().get(GameRules.KEEP_INVENTORY);
            ItemStack carried = getCarried();
            setCarried(ItemStack.EMPTY);
            returnAfterDeath(serverPlayer, carried, keepInventory);
            for (int slot = 0; slot < inputs.getContainerSize(); slot++) {
                returnAfterDeath(serverPlayer, inputs.removeItemNoUpdate(slot), keepInventory);
            }
        }
        super.removed(player);
        if (player instanceof ServerPlayer) {
            clearContainer(player, inputs);
        }
    }

    private void returnAfterDeath(ServerPlayer player, ItemStack stack, boolean keepInventory) {
        if (stack.isEmpty()) {
            return;
        }
        if (keepInventory) {
            player.getInventory().placeItemBackInInventory(stack, Prediction.SERVER_ONLY);
        } else {
            player.drop(stack, false, Prediction.SERVER_ONLY);
        }
    }

    private static Affinity affinityForStone(ItemStack stack) {
        if (stack.isEmpty()) {
            return Affinity.NONE;
        }
        if (stack.is(MagicItems.FIRE_AFFINITY_STONE)) {
            return Affinity.FIRE;
        }
        if (stack.is(MagicItems.WATER_AFFINITY_STONE)) {
            return Affinity.WATER;
        }
        if (stack.is(MagicItems.WIND_AFFINITY_STONE)) {
            return Affinity.WIND;
        }
        if (stack.is(MagicItems.EARTH_AFFINITY_STONE)) {
            return Affinity.EARTH;
        }
        return Affinity.NONE;
    }

    private abstract class TabSlot extends Slot {
        private final int tab;

        private TabSlot(int slot, int x, int y, int tab) {
            super(MagesAltarMenu.this.inputs, slot, x, y);
            this.tab = tab;
        }

        @Override
        public boolean isActive() {
            return getSelectedTab() == tab;
        }

        @Override
        public boolean mayPickup(Player player) {
            return isActive() && player == owner && player.isAlive() && !player.isSpectator();
        }
    }
}
