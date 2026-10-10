import json
from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"
ENCHANTMENTS = {"mana_store": 3, "mana_regen": 5}
RUBY_ARMOR = {
    "head_armor": "celitest:ruby_helmet",
    "chest_armor": "celitest:ruby_chestplate",
    "leg_armor": "celitest:ruby_leggings",
    "foot_armor": "celitest:ruby_boots",
}


def unique_object(pairs):
    result = {}
    for key, value in pairs:
        if key in result:
            raise ValueError(f"Duplicate JSON key: {key}")
        result[key] = value
    return result


def resource(relative_path):
    path = RESOURCES / relative_path
    if not path.is_file():
        raise unittest.SkipTest(f"Resource not available yet: {relative_path}")
    return json.loads(path.read_text(encoding="utf-8"), object_pairs_hook=unique_object)


def enchantments():
    return {
        name: resource(f"data/celitest/enchantment/{name}.json")
        for name in ENCHANTMENTS
    }


def tag_entries(tag):
    values = tag["values"]
    if not isinstance(values, list):
        raise AssertionError("Tag values must be an array")
    entries = set()
    for value in values:
        if isinstance(value, str):
            entries.add(value)
        elif isinstance(value, dict) and value.get("required", True):
            entries.add(value["id"])
        else:
            raise AssertionError(f"Required tag entry expected, got {value!r}")
    return entries


class ManaEnchantmentResources(unittest.TestCase):
    def test_definition_levels_and_descriptions(self):
        for name, definition in enchantments().items():
            with self.subTest(enchantment=name):
                self.assertEqual(ENCHANTMENTS[name], definition["max_level"])
                self.assertEqual(f"enchantment.celitest.{name}", definition["description"]["translate"])
                self.assertIsInstance(definition["weight"], int)
                self.assertGreater(definition["weight"], 0)
                self.assertIsInstance(definition["anvil_cost"], int)
                self.assertGreater(definition["anvil_cost"], 0)

    def test_cost_ranges_allow_every_declared_level(self):
        for name, definition in enchantments().items():
            with self.subTest(enchantment=name):
                minimum = definition["min_cost"]
                maximum = definition["max_cost"]
                for cost in (minimum, maximum):
                    self.assertIsInstance(cost["base"], int)
                    self.assertIsInstance(cost["per_level_above_first"], int)
                    self.assertGreaterEqual(cost["base"], 0)
                    self.assertGreaterEqual(cost["per_level_above_first"], 0)
                for level in range(definition["max_level"]):
                    self.assertLessEqual(
                        minimum["base"] + level * minimum["per_level_above_first"],
                        maximum["base"] + level * maximum["per_level_above_first"],
                    )

    def test_armor_only_application_and_table_support(self):
        for name, definition in enchantments().items():
            with self.subTest(enchantment=name):
                self.assertEqual("#minecraft:enchantable/armor", definition["supported_items"])
                self.assertEqual(
                    definition["supported_items"],
                    definition.get("primary_items", definition["supported_items"]),
                )
                self.assertEqual({"armor"}, set(definition["slots"]))

    def test_enchantments_can_coexist_and_have_no_duplicate_effects(self):
        for name, definition in enchantments().items():
            with self.subTest(enchantment=name):
                exclusive = definition.get("exclusive_set", [])
                if isinstance(exclusive, str) and exclusive.startswith("#"):
                    namespace, tag = exclusive[1:].split(":", 1)
                    exclusive = tag_entries(resource(f"data/{namespace}/tags/enchantment/{tag}.json"))
                elif isinstance(exclusive, str):
                    exclusive = [exclusive]
                self.assertTrue(set(exclusive).isdisjoint(f"celitest:{other}" for other in ENCHANTMENTS))
                self.assertEqual({}, definition["effects"])

    def test_normal_acquisition_tag_preserves_vanilla_enchantments(self):
        enchantments()
        tag = resource("data/minecraft/tags/enchantment/non_treasure.json")
        self.assertFalse(tag.get("replace", False))
        self.assertTrue({f"celitest:{name}" for name in ENCHANTMENTS}.issubset(tag_entries(tag)))

    def test_armor_tag_adds_ruby_and_preserves_vanilla_tiers(self):
        enchantments()
        tag = resource("data/minecraft/tags/item/enchantable/armor.json")
        self.assertFalse(tag.get("replace", False))
        self.assertTrue(set(RUBY_ARMOR.values()).issubset(tag_entries(tag)))

    def test_ruby_slot_tags_match_runtime_armor_classification(self):
        for slot, item in RUBY_ARMOR.items():
            with self.subTest(slot=slot):
                tag = resource(f"data/minecraft/tags/item/{slot}.json")
                self.assertFalse(tag.get("replace", False))
                self.assertIn(item, tag_entries(tag))


class ManaSourceIntegration(unittest.TestCase):
    def test_common_sources_do_not_import_client_code(self):
        sources = sorted((ROOT / "src/main/java").rglob("*.java"))
        self.assertTrue(sources, "No common Java sources found")
        for path in sources:
            with self.subTest(source=str(path.relative_to(ROOT))):
                self.assertNotRegex(
                    path.read_text(encoding="utf-8"),
                    re.compile(r"^import\s+(?:static\s+)?(?:net\.minecraft\.client|com\.celi\.test\.client)\.", re.MULTILINE),
                )

    def test_project_imports_resolve_across_common_and_client_sources(self):
        roots = (ROOT / "src/main/java", ROOT / "src/client/java")
        sources = [path for root in roots for path in sorted(root.rglob("*.java"))]
        self.assertTrue(sources, "No Java sources found")
        declarations = set()
        imports = []
        for path in sources:
            source = path.read_text(encoding="utf-8")
            package = re.search(r"^package\s+([\w.]+)\s*;", source, re.MULTILINE)
            self.assertIsNotNone(package, f"Missing package: {path.relative_to(ROOT)}")
            declarations.add(f"{package.group(1)}.{path.stem}")
            imports.extend((path, name) for name in re.findall(
                r"^import\s+(com\.celi\.test\.[\w.]+)\s*;", source, re.MULTILINE
            ))
        self.assertTrue(imports, "No project imports found")
        for path, name in imports:
            with self.subTest(source=str(path.relative_to(ROOT)), imported=name):
                self.assertIn(name, declarations)


class ManaLanguageResources(unittest.TestCase):
    def test_enchantment_and_regeneration_attribute_names(self):
        language = resource("assets/celitest/lang/en_us.json")
        keys = {f"enchantment.celitest.{name}" for name in ENCHANTMENTS}
        keys.add("attribute.name.celitest.mana_regeneration")
        for key in sorted(keys):
            with self.subTest(key=key):
                self.assertIn(key, language)
                self.assertIsInstance(language[key], str)
                self.assertTrue(language[key].strip())

    def test_all_magic_ui_translation_references_resolve(self):
        language = resource("assets/celitest/lang/en_us.json")
        sources = ROOT / "src/client/java/com/celi/test/client/magic"
        paths = sorted(sources.rglob("*.java"))
        if not paths:
            self.skipTest("Magic UI sources not available yet")
        references = set()
        for path in paths:
            references.update(re.findall(
                r'"((?:tooltip|hud|screen|container)\.celitest\.[a-z0-9_]+(?:\.[a-z0-9_]+)*)"',
                path.read_text(encoding="utf-8"),
            ))
        self.assertTrue(references, "No magic UI translation references found")
        for key in sorted(references):
            with self.subTest(key=key):
                self.assertIn(key, language)
                self.assertIsInstance(language[key], str)
                self.assertTrue(language[key].strip())

    def test_fractional_mana_and_percentage_placeholders(self):
        language = resource("assets/celitest/lang/en_us.json")
        placeholders = {
            "hud.celitest.mana": ("%1$s", "%2$s"),
            "tooltip.celitest.magerite.base_bonus": ("%1$s",),
            "tooltip.celitest.magerite.bonus": ("%1$s",),
            "tooltip.celitest.mana_store.bonus": ("%1$s", "%2$s", "%%"),
            "tooltip.celitest.mana_regen.bonus": ("%1$s", "%2$s", "%%"),
            "tooltip.celitest.mana_regen.total": ("%1$s", "%2$s", "%%"),
            "tooltip.celitest.mana_regen.rate": ("%1$s", "%2$s"),
            "screen.celitest.mages_altar.current_bonus": ("%1$s",),
            "screen.celitest.mages_altar.next_bonus": ("%1$s", "%2$s"),
            "screen.celitest.mages_altar.apply_tooltip": ("%1$s", "%2$s"),
        }
        for key, tokens in placeholders.items():
            with self.subTest(key=key):
                self.assertIn(key, language)
                for token in tokens:
                    self.assertIn(token, language[key])
                self.assertNotRegex(language[key], r"%\d+\$[df]")


if __name__ == "__main__":
    unittest.main()
