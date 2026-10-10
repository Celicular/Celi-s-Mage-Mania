import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parent.parent
RESOURCES = ROOT / "src/main/resources"
SOURCES = ROOT / "src/main/java"


def resource(relative_path):
    path = RESOURCES / relative_path
    return json.loads(path.read_text(encoding="utf-8"))


class EarthResources(unittest.TestCase):
    def test_earth_stone_item_and_recipe(self):
        item = resource("assets/celitest/items/earth_affinity_stone.json")
        model = resource("assets/celitest/models/item/earth_affinity_stone.json")
        recipe = resource("data/celitest/recipe/earth_affinity_stone.json")
        self.assertEqual("celitest:item/earth_affinity_stone", item["model"]["model"])
        self.assertEqual("celitest:item/earth_affinity_stone", model["textures"]["layer0"])
        self.assertEqual("minecraft:dirt", recipe["key"]["C"])
        self.assertEqual("celitest:earth_affinity_stone", recipe["result"]["id"])

    def test_earth_affinity_is_connected_to_common_flow(self):
        affinity = (SOURCES / "com/celi/test/magic/affinity/Affinity.java").read_text(encoding="utf-8")
        abilities = (SOURCES / "com/celi/test/magic/affinity/StaffAbility.java").read_text(encoding="utf-8")
        items = (SOURCES / "com/celi/test/magic/MagicItems.java").read_text(encoding="utf-8")
        altar = (SOURCES / "com/celi/test/magic/altar/MagesAltarMenu.java").read_text(encoding="utf-8")
        casting = (SOURCES / "com/celi/test/magic/spell/StaffCasting.java").read_text(encoding="utf-8")
        prison = (SOURCES / "com/celi/test/magic/spell/ThornvinePrison.java").read_text(encoding="utf-8")
        affinity_system = (SOURCES / "com/celi/test/magic/affinity/AffinitySystem.java").read_text(encoding="utf-8")
        self.assertIn('EARTH("earth")', affinity)
        self.assertIn('EARTH_SHARD("earth_shard", Affinity.EARTH, false, 20, 4)', abilities)
        self.assertIn('THORNVINE_PRISON("thornvine_prison", Affinity.EARTH, true, 300, 30)', abilities)
        self.assertIn("EARTH_AFFINITY_STONE", items)
        self.assertIn("Affinity.EARTH", altar)
        self.assertIn("ThornvinePrison.cast(player)", casting)
        self.assertIn("StaffCombat.isValidTarget(caster, candidate)", prison)
        self.assertIn("HEALING_RATIO = 0.25F", prison)
        self.assertIn("DURATION_TICKS = 200", prison)
        self.assertIn("ThornvinePrison.cancel(player)", affinity_system)

    def test_earth_translations_exist(self):
        language = resource("assets/celitest/lang/en_us.json")
        for key in (
            "item.celitest.earth_affinity_stone",
            "affinity.celitest.earth",
            "affinity.celitest.earth.active",
            "affinity.celitest.earth.passive",
            "ability.celitest.earth_shard",
            "ability.celitest.thornvine_prison",
            "tooltip.celitest.ability.earth_shard",
            "tooltip.celitest.ability.thornvine_prison",
        ):
            self.assertTrue(language[key].strip())


if __name__ == "__main__":
    unittest.main()
