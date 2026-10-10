import json
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[1]
COMMON = ROOT / "src/main/java/com/celi/test/magic"
CLIENT = ROOT / "src/client/java/com/celi/test/client/magic"


def read(path):
    return path.read_text(encoding="utf-8")


def method(source, name):
    declaration = re.search(rf"\b(?:public|private|static)[^\n]*\b{re.escape(name)}\([^;{{]*\)\s*\{{", source)
    if declaration is None:
        raise AssertionError(f"Method not found: {name}")
    start = declaration.end()
    depth = 1
    position = start
    while depth:
        if source[position] == "{":
            depth += 1
        elif source[position] == "}":
            depth -= 1
        position += 1
    return source[start:position - 1]


class StaffManaContracts(unittest.TestCase):
    def setUp(self):
        self.abilities = read(COMMON / "affinity/StaffAbility.java")
        self.casting = read(COMMON / "spell/StaffCasting.java")
        self.mana = read(COMMON / "mana/ManaSystem.java")
        self.language = json.loads(read(ROOT / "src/main/resources/assets/celitest/lang/en_us.json"))

    def test_all_six_abilities_share_the_requested_costs(self):
        expression = re.search(r"return secondary \? (\d+) : (\d+);", method(self.abilities, "manaCost"))
        self.assertIsNotNone(expression)
        secondary_cost, primary_cost = map(int, expression.groups())
        self.assertEqual((200, 50), (secondary_cost, primary_cost))
        abilities = re.findall(r'^\s*([A-Z_]+)\("[a-z_]+", Affinity\.(FIRE|WATER|WIND), (true|false),',
                               self.abilities, re.MULTILINE)
        self.assertEqual(6, len(abilities))
        for affinity in ("FIRE", "WATER", "WIND"):
            self.assertEqual({"true", "false"}, {secondary for _, element, secondary in abilities if element == affinity})

    def test_rejected_mana_checks_precede_use_and_cooldown(self):
        begin = method(self.casting, "begin")
        self.assertLess(begin.index("data.getCooldown(ability)"), begin.index("ManaSystem.getCurrentMana(player)"))
        self.assertLess(begin.index("ManaSystem.getCurrentMana(player)"), begin.index("player.startUsingItem(hand)"))
        self.assertLess(begin.index("ManaSystem.trySpend(player, ability.manaCost())"), begin.index("ACTIVE_CASTS.put"))
        self.assertLess(begin.index("ManaSystem.trySpend(player, ability.manaCost())"), begin.index("data.startCooldown(ability)"))
        self.assertEqual(2, begin.count('feedback(player, ability, "insufficient_mana"'))

    def test_an_accepted_cast_is_billed_only_once(self):
        self.assertEqual(1, method(self.casting, "begin").count("ManaSystem.trySpend"))
        self.assertNotIn("trySpend", method(self.casting, "advance"))
        self.assertNotIn("trySpend", method(self.casting, "cancel"))
        self.assertLess(method(self.casting, "begin").index("ManaSystem.trySpend"),
                        method(self.casting, "begin").index("StaffProjectiles.launch"))

    def test_spending_checks_balance_before_mutation_and_accepts_exact_balance(self):
        spending = method(self.mana, "trySpend")
        self.assertIn("requireServerThread(player)", spending)
        self.assertIn("!Double.isFinite(amount) || amount < 0.0D", spending)
        self.assertIn("double current = snapshot(player).current();", spending)
        self.assertIn("if (current < amount)", spending)
        self.assertNotIn("current <= amount", spending)
        self.assertLess(spending.index("return false;"), spending.index("dataFor(player).setCurrent(current - amount)"))

    def test_client_does_not_overwrite_authoritative_rejection_reason(self):
        feedback = method(read(CLIENT / "StaffClient.java"), "applyFeedback")
        rejected = feedback.split("if (!payload.accepted()) {", 1)[1].split("if (ability.secondary())", 1)[0]
        self.assertIn("stopPredictedUse(client)", rejected)
        self.assertNotIn("showMessage", rejected)
        self.assertNotIn("showCooldown", rejected)

    def test_tooltips_and_insufficient_mana_placeholders(self):
        tooltips = read(CLIENT / "MagicTooltips.java")
        self.assertIn("primary.manaCost()", tooltips)
        self.assertIn("secondary.manaCost()", tooltips)
        for key, count in (("tooltip.celitest.mages_staff.primary", 2),
                           ("tooltip.celitest.mages_staff.secondary", 3),
                           ("message.celitest.staff.insufficient_mana", 3)):
            self.assertEqual(set(range(1, count + 1)), {int(value) for value in re.findall(r"%(\d+)\$s", self.language[key])})


class ThunderstormContracts(unittest.TestCase):
    def setUp(self):
        self.weather = read(COMMON / "spell/ScopedStormWeather.java")
        self.storm = read(COMMON / "spell/ThunderstormSpells.java")
        self.casting = read(COMMON / "spell/StaffCasting.java")
        curve = re.search(r"return (progress[^;]+);", method(self.weather, "ease")).group(1)
        self.curve = re.sub(r"(\d(?:\.\d+)?)F\b", r"\1", curve)
        self.assertRegex(self.curve, r"^[\d\s.()*+\-/progress]+$")
        self.duration = int(re.search(r"FADE_TICKS = (\d+);", self.weather).group(1))

    def eased(self, progress):
        return eval(self.curve, {"__builtins__": {}}, {"progress": max(0.0, min(1.0, progress))})

    def test_weather_starts_during_the_paid_channel(self):
        begin = method(self.casting, "begin")
        self.assertLess(begin.index("ManaSystem.trySpend"), begin.index("ThunderstormSpells.beginChannel(player)"))
        self.assertIn("ability == StaffAbility.THUNDERSTORM", begin)
        self.assertIn("StaffAbility.THUNDERSTORM.castTicks()", method(self.storm, "beginChannel"))
        self.assertIn("channelTicks + 1", method(self.weather, "beginChannel"))
        self.assertIn("lease.remainingTicks = durationTicks", method(self.weather, "acquire"))
        self.assertIn("lease.channeling = false", method(self.weather, "acquire"))

    def test_two_second_curve_is_gradual_and_reaches_exact_endpoints(self):
        self.assertEqual(40, self.duration)
        values = [self.eased(tick / self.duration) for tick in range(self.duration + 1)]
        self.assertEqual(0.0, values[0])
        self.assertEqual(1.0, values[-1])
        self.assertEqual(0.5, values[self.duration // 2])
        self.assertTrue(all(left < right for left, right in zip(values, values[1:])))
        self.assertLess(max(right - left for left, right in zip(values, values[1:])), 0.04)
        self.assertIn("Math.clamp", method(self.weather, "ease"))

    def test_partial_channels_fade_back_without_a_full_strength_jump(self):
        for elapsed in (1, 10, 20, 30, 40):
            initial = self.eased(elapsed / self.duration)
            values = [initial * (1.0 - self.eased(tick / self.duration)) for tick in range(self.duration + 1)]
            self.assertEqual(initial, values[0])
            self.assertEqual(0.0, values[-1])
            self.assertTrue(all(left >= right for left, right in zip(values, values[1:])))
        transitions = method(self.weather, "updateTransitions")
        self.assertIn("transition.fadeStart = transition.strength", transitions)
        self.assertIn("transition.fadeStart * (1.0F - ease(transition.fadeElapsed, FADE_TICKS))", transitions)

    def test_existing_rain_and_thunder_are_blended_not_replaced(self):
        expression = re.search(r"return ([^;]+);", method(self.weather, "mix")).group(1)
        expression = re.sub(r"(\d(?:\.\d+)?)F\b", r"\1", expression)
        for baseline in (0.0, 0.25, 0.75, 1.0):
            for strength in (0.0, 0.5, 1.0):
                value = eval(expression, {"__builtins__": {}}, {"baseline": baseline, "strength": strength})
                self.assertLessEqual(baseline, value)
                self.assertLessEqual(value, 1.0)
                if strength == 0:
                    self.assertEqual(baseline, value)
        applied = method(self.weather, "applyStorm")
        self.assertIn("mix(baseline.rain(), transition.strength)", applied)
        self.assertIn("mix(baseline.thunder(), transition.strength)", applied)
        self.assertNotIn("level.setRainLevel(1.0F)", applied)

    def test_expiry_and_overlap_keep_transitions_until_fade_finishes(self):
        cleanup = method(self.weather, "removeEmptyLevels")
        self.assertIn("entry.getValue().isEmpty() && transitions.get(entry.getKey()).strength <= 0.0F", cleanup)
        self.assertIn("Math.max(transition.strength, lease.strength())", method(self.weather, "updateTransitions"))
        self.assertIn("transition.strength, server.getTickCount()", method(self.weather, "beginChannel"))
        self.assertRegex(method(self.weather, "updateTransitions"), r"if \(gameTick\)\s*\{\s*transition.fadeElapsed\+\+")

    def test_lightning_includes_mobs_in_a_caster_centered_sphere(self):
        cast = method(self.storm, "cast")
        self.assertIn("Vec3 center = caster.position()", cast)
        self.assertIn("level.getEntitiesOfClass(LivingEntity.class", cast)
        self.assertIn("candidate != caster", cast)
        self.assertIn("candidate.distanceToSqr(center) <= RADIUS * RADIUS", cast)
        self.assertIn("StaffCombat.isValidTarget(caster, candidate)", cast)
        self.assertNotIn("level.players()", cast)
        self.assertIn("RADIUS = 10.0D", self.storm)
        self.assertIn("DAMAGE = 16.0F", self.storm)
        self.assertIn("NAUSEA_TICKS = 100", self.storm)
        self.assertIn("DURATION_TICKS = 200", self.storm)
        self.assertIn("StaffCombat.damage(caster, target, DAMAGE)", cast)
        self.assertIn("MobEffects.NAUSEA, NAUSEA_TICKS", cast)

    def test_lightning_remains_visual_only_to_avoid_extra_vanilla_damage(self):
        cast = method(self.storm, "cast")
        self.assertIn("lightning.setVisualOnly(true)", cast)
        self.assertNotIn("super.tick()", cast)
        self.assertEqual(1, cast.count("StaffCombat.damage(caster, target, DAMAGE)"))

    def test_weather_packets_do_not_reset_the_rain_state_every_frame(self):
        applied = method(self.weather, "applyStorm")
        self.assertIn("transition.lastRaining != raining", applied)
        self.assertRegex(self.weather, r"if \(rainStateChanged\)\s*\{\s*player.connection.send\(rainState\)")
        self.assertIn("player.connection.send(rain)", self.weather)
        self.assertIn("player.connection.send(thunder)", self.weather)

    def test_saves_and_server_shutdown_restore_the_original_weather(self):
        self.assertIn("ServerLifecycleEvents.BEFORE_SAVE", self.weather)
        self.assertIn("ServerLifecycleEvents.AFTER_SAVE", self.weather)
        self.assertIn("weatherBaseline.restore(server.getWeatherData())", method(self.weather, "restoreBaseline"))
        self.assertIn("state.restoreBaseline()", method(self.weather, "stop"))
        self.assertIn("state.syncBaseline()", method(self.weather, "stop"))


if __name__ == "__main__":
    unittest.main()
