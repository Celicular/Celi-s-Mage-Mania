package com.celi.test.magic.mana;

import java.util.Locale;

public final class ManaMathRegression {
	private static int assertions;

	private ManaMathRegression() {
	}

	public static void main(String[] args) {
		constants();
		storeRows();
		independentArmorBonuses();
		boundedInputs();
		regenerationRows();
		regenerationBoundaries();
		bonusPercentages();
		formatting();
		System.out.println("ManaMath regression checks passed: " + assertions + " assertions.");
	}

	private static void constants() {
		equal(10.0, ManaMath.BASE_REGENERATION, "base regeneration");
		equal(10, ManaMath.REGENERATION_INTERVAL_TICKS, "regeneration interval in game ticks");
	}

	private static void storeRows() {
		int[][] inputs = {{10, 0}, {10, 1}, {10, 2}, {10, 3}, {4, 1}, {4, 2}, {4, 3}};
		double[] expected = {250, 287.5, 325, 362.5, 115, 130, 145};
		for (int index = 0; index < inputs.length; index++) {
			int clusters = inputs[index][0];
			int level = inputs[index][1];
			equal(expected[index], ManaMath.armorMana(clusters, level), "store row: clusters=" + clusters + ", level=" + level);
		}
	}

	private static void independentArmorBonuses() {
		double fullCapacity = 50;
		for (int piece = 0; piece < 4; piece++) {
			fullCapacity += ManaMath.armorMana(10, 3);
		}
		equal(1500, fullCapacity, "four fully upgraded Store III pieces plus base mana");
		for (int level = 0; level <= 3; level++) {
			equal(0, ManaMath.armorMana(0, level), "Store cannot create mana without clusters at level " + level);
			equal(50, 50 + ManaMath.armorMana(0, level), "Store leaves the player's base mana unchanged at level " + level);
		}
		double mixedCapacity = 50 + ManaMath.armorMana(10, 3) + ManaMath.armorMana(4, 1) + ManaMath.armorMana(4, 0) + ManaMath.armorMana(0, 3);
		equal(627.5, mixedCapacity, "Store applies separately to each armor piece");
		for (int calculation = 0; calculation < 20; calculation++) {
			equal(362.5, ManaMath.armorMana(10, 3), "repeated calculations must not compound: " + calculation);
			equal(250, ManaMath.armorMana(10, 0), "removing Store restores the unenchanted bonus: " + calculation);
		}
	}

	private static void regenerationRows() {
		int[] levels = {0, 1, 5, 4, 8, 12, 16, 20};
		double[] expected = {10, 15, 35, 30, 50, 70, 90, 110};
		for (int index = 0; index < levels.length; index++) {
			equal(expected[index], ManaMath.regeneration(levels[index]), "regeneration row: total equipped levels=" + levels[index]);
		}
		equal(20, ManaMath.regeneration(2), "single Regen II piece");
		equal(25, ManaMath.regeneration(3), "single Regen III piece");
		equal(40, ManaMath.regeneration(1 + 2 + 3 + 0), "mixed equipped regeneration levels are additive");
		for (int calculation = 0; calculation < 20; calculation++) {
			equal(110, ManaMath.regeneration(20), "repeated regeneration calculations must not compound: " + calculation);
			equal(10, ManaMath.regeneration(0), "removing Regen restores the base amount: " + calculation);
		}
	}

	private static void boundedInputs() {
		equal(0, ManaMath.armorMana(-1, 3), "negative clusters cannot create mana");
		equal(362.5, ManaMath.armorMana(11, 3), "clusters are bounded by the armor upgrade limit");
		equal(250, ManaMath.armorMana(10, -1), "negative Store levels have no bonus");
		equal(362.5, ManaMath.armorMana(10, 4), "Store levels are bounded by level III");
		equal(10, ManaMath.regeneration(-1), "negative Regen levels preserve base regeneration");
		equal(110, ManaMath.regeneration(21), "Regen total is bounded by four level V pieces");
		equal(0, ManaMath.storeBonusPercent(-1), "negative Store bonus percentage");
		equal(45, ManaMath.storeBonusPercent(4), "Store bonus percentage is bounded");
		equal(0, ManaMath.regenBonusPercent(-1), "negative Regen bonus percentage");
		equal(250, ManaMath.regenBonusPercent(6), "per-piece Regen bonus percentage is bounded");
	}

	private static void regenerationBoundaries() {
		equal(10, ManaMath.regenerate(0, 50, ManaMath.regeneration(0)), "regeneration from empty mana");
		equal(22.25, ManaMath.regenerate(12.25, 50, 10), "regeneration retains fractional current mana");
		equal(12.5, ManaMath.regenerate(12.25, 50, 0.25), "fractional regeneration amount");
		equal(287.5, ManaMath.regenerate(280, 287.5, 10), "regeneration clamps to fractional maximum");
		equal(287.5, ManaMath.regenerate(287.5, 287.5, 110), "regeneration stays at full mana");
		equal(287.5, ManaMath.regenerate(287.25, 287.5, 10), "regeneration resumes after spending fractional mana");
		equal(297.5, ManaMath.regenerate(287.5, 362.5, 10), "regeneration resumes after increasing maximum mana");
		equal(250, ManaMath.regenerate(362.5, 250, 10), "current mana clamps after lowering maximum mana");
		equal(12.25, ManaMath.regenerate(12.25, 50, 0), "zero regeneration preserves fractional mana");
		equal(0, ManaMath.regenerate(0, 0, 10), "zero maximum mana");
		equal(10, ManaMath.regenerate(-5, 50, 10), "negative current mana is bounded before regeneration");
		equal(12.25, ManaMath.regenerate(12.25, 50, -10), "negative regeneration cannot consume mana");
		equal(10, ManaMath.regenerate(Double.NaN, 50, 10), "invalid current mana recovers from empty");
		equal(12.25, ManaMath.regenerate(12.25, 50, Double.NaN), "invalid regeneration amount preserves current mana");
		equal(0, ManaMath.regenerate(12.25, Double.NaN, 10), "invalid maximum cannot create mana");
		equal(50, ManaMath.regenerate(12.25, 50, Double.POSITIVE_INFINITY), "overflowing regeneration clamps to maximum mana");
		double current = 0;
		for (int interval = 0; interval < 10; interval++) {
			current = ManaMath.regenerate(current, 50, ManaMath.regeneration(0));
		}
		equal(50, current, "repeated intervals fill mana without exceeding its maximum");
		current -= 15.25;
		current = ManaMath.regenerate(current, 50, ManaMath.regeneration(0));
		equal(44.75, current, "regeneration resumes after spending from a full pool");
		equal(50, ManaMath.regenerate(current, 50, ManaMath.regeneration(0)), "resumed regeneration reaches full mana");
	}

	private static void bonusPercentages() {
		int[] store = {0, 15, 30, 45};
		for (int level = 0; level < store.length; level++) {
			equal(store[level], ManaMath.storeBonusPercent(level), "Store bonus percentage at level " + level);
		}
		int[] regeneration = {0, 50, 100, 150, 200, 250};
		for (int level = 0; level < regeneration.length; level++) {
			equal(regeneration[level], ManaMath.regenBonusPercent(level), "Regen bonus percentage at level " + level);
		}
	}

	private static void formatting() {
		Locale previous = Locale.getDefault();
		try {
			Locale.setDefault(Locale.FRANCE);
			double[] values = {0, 10, 250, 287.5, 325, 362.5, 115, 130, 145, 1500};
			String[] expected = {"0", "10", "250", "287.5", "325", "362.5", "115", "130", "145", "1500"};
			for (int index = 0; index < values.length; index++) {
				equal(expected[index], ManaMath.format(values[index]), "clear locale-independent mana formatting for " + values[index]);
			}
			equal("0.3", ManaMath.format(0.1 + 0.2), "formatting removes floating-point display noise");
			equal("0", ManaMath.format(-0.0), "formatting removes negative zero");
			equal("123.46", ManaMath.format(123.456), "display rounds to two decimal places");
			equal("123.4", ManaMath.format(123.4), "display strips trailing decimal zeros");
			equal("0.01", ManaMath.format(0.005), "display uses consistent half-up rounding");
			equal("0", ManaMath.format(Double.NaN), "display handles invalid mana");
			equal("0", ManaMath.format(Double.POSITIVE_INFINITY), "display handles infinite mana");
			equal("287.5", ManaMath.format(ManaMath.armorMana(10, 1)), "formatting preserves a calculated fractional Store bonus");
		} finally {
			Locale.setDefault(previous);
		}
	}

	private static void equal(double expected, double actual, String label) {
		assertions++;
		if (!Double.isFinite(actual) || Math.abs(expected - actual) > 0.000000001) {
			throw new AssertionError(label + ": expected " + expected + ", got " + actual);
		}
	}

	private static void equal(String expected, String actual, String label) {
		assertions++;
		if (!expected.equals(actual)) {
			throw new AssertionError(label + ": expected \"" + expected + "\", got \"" + actual + "\"");
		}
	}
}
