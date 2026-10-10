package com.celi.test.magic.affinity;

public enum Affinity {
    NONE("none"),
    FIRE("fire"),
    WATER("water"),
    WIND("wind"),
    EARTH("earth");

    private final String id;

    Affinity(String id) {
        this.id = id;
    }

    public String id() {
        return id;
    }

    public String translationKey() {
        return "affinity.celitest." + id;
    }

    public static Affinity fromId(String id) {
        for (Affinity affinity : values()) {
            if (affinity.id.equals(id)) {
                return affinity;
            }
        }
        return NONE;
    }
}
