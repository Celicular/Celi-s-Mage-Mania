package com.celi.test.magic.affinity;

public enum StaffAbility {
    FIREBALL("fireball", Affinity.FIRE, false, 4, 4),
    INFERNO("inferno", Affinity.FIRE, true, 300, 30),
    WATER_ORB("water_orb", Affinity.WATER, false, 4, 4),
    THUNDERSTORM("thunderstorm", Affinity.WATER, true, 900, 40),
    WIND_BURST("wind_burst", Affinity.WIND, false, 4, 4),
    SKYWARD_ASCENT("skyward_ascent", Affinity.WIND, true, 300, 30),
    EARTH_SHARD("earth_shard", Affinity.EARTH, false, 20, 4),
    THORNVINE_PRISON("thornvine_prison", Affinity.EARTH, true, 300, 30);

    private final String id;
    private final Affinity affinity;
    private final boolean secondary;
    private final int cooldownTicks;
    private final int castTicks;

    StaffAbility(String id, Affinity affinity, boolean secondary, int cooldownTicks, int castTicks) {
        this.id = id;
        this.affinity = affinity;
        this.secondary = secondary;
        this.cooldownTicks = cooldownTicks;
        this.castTicks = castTicks;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "ability.celitest." + id;
    }

    public Affinity affinity() {
        return affinity;
    }

    public boolean secondary() {
        return secondary;
    }

    public int cooldownTicks() {
        return cooldownTicks;
    }

    public int castTicks() {
        return castTicks;
    }

    public int manaCost() {
        return secondary ? 200 : 50;
    }

    public static StaffAbility forAffinity(Affinity affinity, boolean secondary) {
        for (StaffAbility ability : values()) {
            if (ability.affinity == affinity && ability.secondary == secondary) {
                return ability;
            }
        }
        return null;
    }
}
