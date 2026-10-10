package com.celi.test.magic.affinity;

import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public final class AffinityData {
    private static final String SAVE_KEY = "celitest:affinity";
    private static final StaffAbility[] ABILITIES = StaffAbility.values();

    private Affinity affinity = Affinity.NONE;
    private final int[] cooldowns = new int[ABILITIES.length];

    public void read(ValueInput input) {
        ValueInput saved = input.childOrEmpty(SAVE_KEY);
        affinity = Affinity.fromId(saved.getStringOr("selected", Affinity.NONE.id()));
        ValueInput savedCooldowns = saved.childOrEmpty("cooldowns");
        for (StaffAbility ability : ABILITIES) {
            cooldowns[ability.ordinal()] = Math.clamp(savedCooldowns.getIntOr(ability.id(), 0),
                    0, ability.cooldownTicks());
        }
    }

    public void write(ValueOutput output) {
        ValueOutput saved = output.child(SAVE_KEY);
        saved.putString("selected", affinity.id());
        ValueOutput savedCooldowns = saved.child("cooldowns");
        for (StaffAbility ability : ABILITIES) {
            savedCooldowns.putInt(ability.id(), cooldowns[ability.ordinal()]);
        }
    }

    public Affinity affinity() {
        return affinity;
    }

    public int getCooldown(StaffAbility ability) {
        return cooldowns[ability.ordinal()];
    }

    public void startCooldown(StaffAbility ability) {
        cooldowns[ability.ordinal()] = ability.cooldownTicks();
    }

    public void tickCooldowns() {
        for (int index = 0; index < cooldowns.length; index++) {
            if (cooldowns[index] > 0) {
                cooldowns[index]--;
            }
        }
    }

    void setAffinity(Affinity affinity) {
        this.affinity = affinity;
    }

    void copyFrom(AffinityData source) {
        affinity = source.affinity;
        System.arraycopy(source.cooldowns, 0, cooldowns, 0, cooldowns.length);
    }
}
