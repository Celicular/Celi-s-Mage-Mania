package com.celi.test.magic.mana;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class ManaMath {
    public static final double BASE_REGENERATION = 10.0D;
    public static final int REGENERATION_INTERVAL_TICKS = 10;

    private static final int MAX_CLUSTERS = 10;
    private static final int MANA_PER_CLUSTER = 25;
    private static final int MAX_STORE_LEVEL = 3;
    private static final int MAX_REGENERATION_LEVEL = 5;
    private static final int MAX_TOTAL_REGENERATION_LEVEL = 20;
    private static final int DISPLAY_DECIMALS = 2;

    private ManaMath() {
    }

    public static double armorMana(int clusters, int storeLevel) {
        int base = Math.clamp(clusters, 0, MAX_CLUSTERS) * MANA_PER_CLUSTER;
        return base * (100 + storeBonusPercent(storeLevel)) / 100.0D;
    }

    public static double regeneration(int totalRegenLevel) {
        return BASE_REGENERATION + 5.0D * Math.clamp(totalRegenLevel, 0, MAX_TOTAL_REGENERATION_LEVEL);
    }

    public static double regenerate(double current, double maximum, double amount) {
        if (!Double.isFinite(maximum) || maximum <= 0.0D) {
            return 0.0D;
        }
        double boundedCurrent = Double.isNaN(current) ? 0.0D : Math.clamp(current, 0.0D, maximum);
        if (Double.isNaN(amount) || amount <= 0.0D) {
            return boundedCurrent;
        }
        return Math.min(maximum, boundedCurrent + amount);
    }

    public static String format(double value) {
        if (!Double.isFinite(value)) {
            return "0";
        }
        return BigDecimal.valueOf(value).setScale(DISPLAY_DECIMALS, RoundingMode.HALF_UP)
                .stripTrailingZeros().toPlainString();
    }

    public static int storeBonusPercent(int level) {
        return Math.clamp(level, 0, MAX_STORE_LEVEL) * 15;
    }

    public static int regenBonusPercent(int level) {
        return Math.clamp(level, 0, MAX_REGENERATION_LEVEL) * 50;
    }
}
