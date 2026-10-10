package com.celi.test.magic.mana;

import java.util.Arrays;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class ManaData {
    private static final String SAVE_KEY = "celitest:current_mana";

    private double current = ManaAttributes.BASE_MANA;
    private ManaSyncPayload lastSynced;
    private int regenerationTicks;
    private final ItemStack[] armor = new ItemStack[4];
    private double armorManaBonus;
    private double armorRegenerationBonus;

    public void read(ValueInput input) {
        double stored = input.getDoubleOr(SAVE_KEY, ManaAttributes.BASE_MANA);
        current = Double.isFinite(stored) ? Math.max(0.0D, stored) : ManaAttributes.BASE_MANA;
        resetSession();
    }

    public void write(ValueOutput output) {
        output.putDouble(SAVE_KEY, current);
    }

    double current() {
        return current;
    }

    void setCurrent(double current) {
        this.current = current;
    }

    ManaSyncPayload lastSynced() {
        return lastSynced;
    }

    void setLastSynced(ManaSyncPayload lastSynced) {
        this.lastSynced = lastSynced;
    }

    boolean advanceRegeneration() {
        if (++regenerationTicks < ManaMath.REGENERATION_INTERVAL_TICKS) {
            return false;
        }
        resetRegeneration();
        return true;
    }

    void resetRegeneration() {
        regenerationTicks = 0;
    }

    boolean updateArmor(int index, ItemStack stack) {
        if (armor[index] != null && ItemStack.matches(armor[index], stack)) {
            return false;
        }
        armor[index] = stack.copy();
        return true;
    }

    ItemStack armor(int index) {
        return armor[index];
    }

    double armorManaBonus() {
        return armorManaBonus;
    }

    double armorRegenerationBonus() {
        return armorRegenerationBonus;
    }

    void setArmorBonuses(double manaBonus, double regenerationBonus) {
        armorManaBonus = manaBonus;
        armorRegenerationBonus = regenerationBonus;
    }

    void resetSession() {
        lastSynced = null;
        resetRegeneration();
        Arrays.fill(armor, null);
        armorManaBonus = 0.0D;
        armorRegenerationBonus = 0.0D;
    }

    void copyFrom(ManaData source) {
        current = source.current;
        resetSession();
    }
}
