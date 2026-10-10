from pathlib import Path
import hashlib
import json
import re
import struct
import zlib

from generate_magic_resources import Canvas as PixelCanvas, DARK, GOLD, TRANSPARENT, rgba


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/celitest/textures/item"
PREVIEW = ROOT / "docs/previews/affinity-stones.png"
EMBER = ("#421e25", "#76282a", "#ac3630", "#d45433", "#ed833f", "#f5b956", "#ffe3a0")
WATER = ("#172b46", "#203d67", "#29588a", "#377da8", "#61afc9", "#a3dce4", "#e3f3ef")
WIND = ("#253b44", "#49636a", "#6f8b90", "#9fb8bb", "#c7dbd9", "#edf4e9", "#8fcfd5")
FONT = {
    " ": "00000/00000/00000/00000/00000/00000/00000",
    "A": "01110/10001/10001/11111/10001/10001/10001",
    "B": "11110/10001/10001/11110/10001/10001/11110",
    "C": "01111/10000/10000/10000/10000/10000/01111",
    "D": "11110/10001/10001/10001/10001/10001/11110",
    "E": "11111/10000/10000/11110/10000/10000/11111",
    "F": "11111/10000/10000/11110/10000/10000/10000",
    "G": "01111/10000/10000/10111/10001/10001/01111",
    "H": "10001/10001/10001/11111/10001/10001/10001",
    "I": "11111/00100/00100/00100/00100/00100/11111",
    "J": "00111/00010/00010/00010/00010/10010/01100",
    "K": "10001/10010/10100/11000/10100/10010/10001",
    "L": "10000/10000/10000/10000/10000/10000/11111",
    "M": "10001/11011/10101/10101/10001/10001/10001",
    "N": "10001/11001/10101/10011/10001/10001/10001",
    "O": "01110/10001/10001/10001/10001/10001/01110",
    "P": "11110/10001/10001/11110/10000/10000/10000",
    "Q": "01110/10001/10001/10001/10101/10010/01101",
    "R": "11110/10001/10001/11110/10100/10010/10001",
    "S": "01111/10000/10000/01110/00001/00001/11110",
    "T": "11111/00100/00100/00100/00100/00100/00100",
    "U": "10001/10001/10001/10001/10001/10001/01110",
    "V": "10001/10001/10001/10001/10001/01010/00100",
    "W": "10001/10001/10001/10101/10101/10101/01010",
    "X": "10001/10001/01010/00100/01010/10001/10001",
    "Y": "10001/10001/01010/00100/00100/00100/00100",
    "Z": "11111/00001/00010/00100/01000/10000/11111",
    "0": "01110/10001/10011/10101/11001/10001/01110",
    "1": "00100/01100/00100/00100/00100/00100/01110",
    "2": "01110/10001/00001/00010/00100/01000/11111",
    "3": "11110/00001/00001/01110/00001/00001/11110",
    "4": "00010/00110/01010/10010/11111/00010/00010",
    "5": "11111/10000/10000/11110/00001/00001/11110",
    "6": "01110/10000/10000/11110/10001/10001/01110",
    "7": "11111/00001/00010/00100/01000/01000/01000",
    "8": "01110/10001/10001/01110/10001/10001/01110",
    "9": "01110/10001/10001/01111/00001/00001/01110",
    "/": "00001/00001/00010/00100/01000/10000/10000",
    "+": "00000/00100/00100/11111/00100/00100/00000",
    "-": "00000/00000/00000/11111/00000/00000/00000",
    ".": "00000/00000/00000/00000/00000/00110/00110",
}


class Canvas(PixelCanvas):
    def __init__(self, width, height=None, background=TRANSPARENT):
        self.width = width
        self.height = height if height is not None else width
        self.size = width
        self.pixels = [rgba(background)] * (self.width * self.height)

    def pixel(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            self.pixels[y * self.width + x] = rgba(color)

    def text(self, x, y, value, color, scale=1):
        for character in value.upper():
            for row, bits in enumerate(FONT[character].split("/")):
                for column, bit in enumerate(bits):
                    if bit == "1":
                        self.rect(x + column * scale, y + row * scale,
                                  x + (column + 1) * scale - 1, y + (row + 1) * scale - 1, color)
            x += 6 * scale

    def centered_text(self, x, y, value, color, scale=1):
        self.text(x - (len(value) * 6 - 1) * scale // 2, y, value, color, scale)

    def paste(self, source, left, top, scale=1, sample=1):
        for y in range(0, source.height, sample):
            for x in range(0, source.width, sample):
                color = source.pixels[y * source.width + x]
                if color[3]:
                    target_x = left + x // sample * scale
                    target_y = top + y // sample * scale
                    self.rect(target_x, target_y, target_x + scale - 1, target_y + scale - 1, color)

    def png(self):
        def chunk(kind, data):
            return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)

        raw = b"".join(b"\x00" + bytes(channel for pixel in self.pixels[y * self.width:(y + 1) * self.width]
                                      for channel in pixel) for y in range(self.height))
        header = struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0)
        return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b"")

    def save(self, path):
        if not path.parent.is_dir():
            raise FileNotFoundError(path.parent)
        image = self.png()
        if path.exists():
            if path.read_bytes() != image:
                raise FileExistsError(f"Refusing to overwrite different artwork: {path}")
            state = "unchanged"
        else:
            path.write_bytes(image)
            state = "created"
        verify_png(path.read_bytes(), self.width, self.height)
        print(f"{path.relative_to(ROOT).as_posix()} ({self.width}x{self.height}, RGBA, {state}, "
              f"sha256 {hashlib.sha256(image).hexdigest()[:16]})")


def verify_png(image, width, height):
    assert image[:8] == b"\x89PNG\r\n\x1a\n"
    offset = 8
    compressed = bytearray()
    kinds = []
    while offset < len(image):
        size = struct.unpack(">I", image[offset:offset + 4])[0]
        kind = image[offset + 4:offset + 8]
        data = image[offset + 8:offset + 8 + size]
        checksum = struct.unpack(">I", image[offset + 8 + size:offset + 12 + size])[0]
        assert checksum == zlib.crc32(kind + data) & 0xffffffff
        kinds.append(kind)
        if kind == b"IHDR":
            assert struct.unpack(">IIBBBBB", data) == (width, height, 8, 6, 0, 0, 0)
        if kind == b"IDAT":
            compressed.extend(data)
        offset += 12 + size
    assert kinds == [b"IHDR", b"IDAT", b"IEND"]
    assert offset == len(image)
    raw = zlib.decompress(compressed)
    assert len(raw) == height * (width * 4 + 1)
    assert all(raw[y * (width * 4 + 1)] == 0 for y in range(height))


def enlarge(source):
    canvas = Canvas(32)
    canvas.paste(source, 0, 0, 2)
    return canvas


def fire_stone():
    canvas = Canvas(16)
    canvas.polygon([(8, 1), (8, 5), (11, 3), (11, 7), (13, 5), (13, 9),
                    (14, 10), (14, 12), (11, 14), (5, 14), (2, 12), (2, 9),
                    (4, 6), (4, 9), (6, 6), (6, 3)], DARK[0])
    canvas.polygon([(7, 3), (7, 7), (10, 5), (10, 8), (12, 7), (12, 10),
                    (13, 11), (11, 13), (5, 13), (3, 11), (3, 9), (5, 10)], EMBER[1])
    canvas.polygon([(7, 3), (7, 7), (5, 9), (4, 11), (6, 12), (8, 10), (9, 6),
                    (10, 5), (10, 9), (12, 8), (11, 11), (10, 12), (6, 13), (5, 11)], EMBER[3])
    canvas.polygon([(7, 4), (7, 7), (6, 9), (7, 10), (9, 7), (9, 10), (10, 11),
                    (8, 12), (6, 12), (5, 10)], EMBER[4])
    canvas.polygon([(7, 8), (7, 10), (8, 9), (9, 11), (8, 12), (6, 11)], EMBER[5])
    canvas.line(7, 10, 7, 11, EMBER[6])
    canvas.line(11, 10, 12, 10, EMBER[2])
    canvas.polygon([(2, 10), (3, 10), (5, 12), (8, 13), (11, 12), (13, 10),
                    (14, 10), (13, 13), (10, 14), (5, 14), (3, 12)], GOLD[0])
    canvas.line(3, 11, 5, 13, GOLD[3])
    canvas.line(5, 13, 8, 14, GOLD[2])
    canvas.line(8, 14, 11, 13, GOLD[1])
    canvas.line(11, 13, 13, 11, GOLD[2])
    canvas.pixel(3, 10, GOLD[4])
    canvas.pixel(13, 10, GOLD[3])
    canvas.pixel(6, 13, GOLD[4])
    canvas.pixel(8, 13, EMBER[1])
    canvas.pixel(8, 14, GOLD[3])
    result = enlarge(canvas)
    result.line(13, 17, 13, 19, EMBER[6])
    result.pixel(14, 8, EMBER[5])
    result.pixel(11, 20, EMBER[5])
    result.pixel(6, 22, GOLD[4])
    result.pixel(12, 26, GOLD[4])
    return result


def water_stone():
    canvas = Canvas(16)
    canvas.polygon([(9, 1), (10, 4), (13, 8), (14, 10), (14, 12), (12, 14),
                    (5, 14), (2, 12), (2, 9), (4, 6), (7, 3)], DARK[0])
    canvas.polygon([(8, 3), (9, 3), (9, 5), (12, 8), (13, 10), (13, 12),
                    (11, 13), (5, 13), (3, 11), (3, 9), (5, 6)], WATER[1])
    canvas.polygon([(8, 3), (8, 7), (5, 10), (4, 12), (3, 10), (5, 7)], WATER[3])
    canvas.polygon([(8, 3), (9, 4), (9, 7), (12, 9), (8, 10), (8, 7)], WATER[2])
    canvas.polygon([(5, 10), (8, 8), (8, 11), (11, 12), (8, 13), (5, 12)], WATER[4])
    canvas.polygon([(9, 9), (12, 9), (13, 11), (11, 12), (8, 11)], WATER[2])
    canvas.line(7, 5, 5, 8, WATER[5])
    canvas.line(5, 9, 5, 10, WATER[6])
    canvas.line(6, 11, 8, 11, WATER[5])
    canvas.pixel(9, 2, WATER[4])
    canvas.pixel(8, 4, WATER[6])
    canvas.polygon([(2, 11), (4, 12), (7, 13), (10, 13), (13, 11), (14, 11),
                    (13, 13), (11, 14), (5, 14), (3, 13)], GOLD[0])
    canvas.line(3, 12, 5, 13, GOLD[3])
    canvas.line(5, 13, 7, 14, GOLD[2])
    canvas.line(10, 14, 13, 12, GOLD[2])
    canvas.pixel(3, 11, GOLD[4])
    canvas.pixel(13, 11, GOLD[3])
    canvas.rect(8, 13, 9, 14, GOLD[1])
    canvas.pixel(8, 13, GOLD[4])
    canvas.pixel(9, 13, WATER[3])
    result = enlarge(canvas)
    result.line(13, 13, 12, 15, WATER[6])
    result.pixel(15, 10, WATER[6])
    result.line(12, 22, 14, 22, WATER[6])
    result.pixel(25, 19, WATER[3])
    result.pixel(16, 26, GOLD[4])
    return result


def wind_stone():
    canvas = Canvas(16)
    canvas.polygon([(5, 2), (11, 2), (14, 5), (14, 9), (12, 12), (9, 14),
                    (4, 13), (1, 10), (1, 6)], DARK[0])
    canvas.polygon([(5, 3), (10, 3), (13, 6), (13, 9), (11, 11), (9, 13),
                    (4, 12), (2, 9), (2, 6)], WIND[2])
    canvas.polygon([(5, 3), (10, 3), (12, 5), (8, 5), (4, 8), (2, 7)], WIND[4])
    canvas.polygon([(8, 5), (12, 5), (13, 7), (10, 9), (5, 11), (4, 8)], WIND[3])
    canvas.polygon([(11, 9), (13, 7), (13, 9), (11, 11), (9, 13), (5, 12)], WIND[1])
    canvas.line(5, 3, 9, 3, WIND[5])
    canvas.line(3, 6, 3, 7, WIND[5])
    canvas.polygon([(11, 2), (12, 3), (11, 4), (10, 4)], TRANSPARENT)
    canvas.polygon([(2, 9), (4, 11), (6, 12), (9, 12), (10, 13), (9, 14),
                    (4, 13), (1, 10)], GOLD[0])
    canvas.line(2, 10, 4, 12, GOLD[3])
    canvas.line(4, 12, 7, 13, GOLD[2])
    canvas.line(7, 13, 9, 13, GOLD[1])
    canvas.pixel(2, 9, GOLD[4])
    canvas.pixel(5, 12, GOLD[4])
    canvas.pixel(13, 6, GOLD[3])
    canvas.pixel(13, 7, GOLD[1])
    result = enlarge(canvas)
    spiral = [(25, 11), (20, 9), (14, 9), (9, 13), (7, 18), (11, 22),
              (17, 23), (21, 20), (21, 16), (18, 13), (14, 13), (12, 16),
              (14, 19), (17, 19), (17, 17)]
    for start, end in zip(spiral, spiral[1:]):
        result.line(start[0], start[1] + 1, end[0], end[1] + 1, WIND[0])
        result.line(start[0] + 1, start[1] + 2, end[0] + 1, end[1] + 2, WIND[0])
        result.line(*start, *end, WIND[6])
    result.line(14, 9, 19, 9, WIND[5])
    result.line(9, 13, 8, 16, WIND[5])
    result.line(13, 22, 16, 23, WIND[4])
    result.pixel(17, 17, WIND[5])
    result.pixel(8, 24, GOLD[4])
    return result


def contact_sheet(stones):
    canvas = Canvas(960, 552, "#171e25")
    canvas.frame(8, 8, 951, 543, GOLD[0])
    canvas.line(24, 62, 935, 62, GOLD[2])
    canvas.text(24, 25, "AFFINITY STONES", GOLD[4], 3)
    canvas.text(674, 28, "CELITEST / 26.3", "#a8b4ba", 2)
    captions = (
        ("FIRE", EMBER[4], "EMBER / FLAME", "GOLD PRONGS"),
        ("WATER", WATER[4], "FACETED DROPLET", "DEEP BLUE CRYSTAL"),
        ("WIND", WIND[4], "SPIRAL CUT STONE", "SILVER / CYAN"),
    )
    for index, (stone, (title, accent, motif, material)) in enumerate(zip(stones, captions)):
        x = 24 + index * 312
        center = x + 144
        canvas.rect(x, 80, x + 287, 478, "#202930")
        canvas.frame(x, 80, x + 287, 478, "#455058")
        canvas.line(x + 1, 81, x + 286, 81, GOLD[1])
        canvas.centered_text(center, 94, title, accent, 3)
        canvas.rect(x + 44, 123, x + 243, 322, "#263039")
        for row in range(25):
            for column in range(25):
                if (row + column) % 2 == 0:
                    canvas.rect(x + 44 + column * 8, 123 + row * 8,
                                x + 51 + column * 8, 130 + row * 8, "#2c3740")
        canvas.frame(x + 43, 122, x + 244, 323, "#141b21")
        canvas.paste(stone, x + 48, 127, 6)
        canvas.centered_text(center, 331, "6X / NEAREST", "#87979e")
        for offset, size, label in ((23, 16, "16 PX"), (111, 32, "32 PX"), (199, 64, "2X")):
            tile_x = x + offset
            canvas.rect(tile_x, 350, tile_x + 65, 415, "#141b21")
            canvas.frame(tile_x, 350, tile_x + 65, 415, "#3b474e")
            inset = (66 - size) // 2
            canvas.paste(stone, tile_x + inset, 350 + inset, 2 if size == 64 else 1, 2 if size == 16 else 1)
            canvas.centered_text(tile_x + 33, 422, label, "#a8b4ba")
        canvas.centered_text(center, 442, motif, accent, 2)
        canvas.centered_text(center, 462, material, "#a8b4ba")
    canvas.text(24, 498, "32 X 32 RGBA / TRANSPARENT / HAND-PLACED PIXELS", "#b9c7c8", 2)
    canvas.text(24, 523, "16 PX INVENTORY CHECK + NATIVE 32 PX + NEAREST-NEIGHBOR ENLARGEMENTS", "#87979e")
    return canvas


def verify_resources():
    assets = ROOT / "src/main/resources/assets/celitest"
    language_pairs = json.loads((assets / "lang/en_us.json").read_text(encoding="utf-8"), object_pairs_hook=list)
    language = dict(language_pairs)
    assert len(language) == len(language_pairs), "Duplicate English translation keys"
    required = set()
    for source in (ROOT / "src").rglob("*.java"):
        required.update(key for key in re.findall(r'"([a-z_]+(?:\.[a-z_]+)?\.celitest\.[a-z0-9_.]+)"',
                                                  source.read_text(encoding="utf-8")) if not key.endswith("."))
    client = ROOT / "src/client/java/com/celi/test/client/magic"
    common = ROOT / "src/main/java/com/celi/test/magic"
    screen_keys = set(re.findall(r'text\("([a-z_]+)"', (client / "MagesAltarScreen.java").read_text(encoding="utf-8")))
    required.update("screen.celitest.mages_altar." + key for key in screen_keys)
    for enum, prefix in (("Affinity", "affinity"), ("StaffAbility", "ability")):
        ids = re.findall(r'\b[A-Z][A-Z_]+\("([a-z_]+)"', (common / f"affinity/{enum}.java").read_text(encoding="utf-8"))
        for identifier in ids:
            required.add(f"{prefix}.celitest.{identifier}")
            if enum == "Affinity":
                required.update(f"affinity.celitest.{identifier}.{detail}" for detail in ("active", "passive"))
                if identifier != "none":
                    required.add(f"tooltip.celitest.affinity.{identifier}.passive")
            else:
                required.add(f"tooltip.celitest.ability.{identifier}")
    casting = (common / "spell/StaffCasting.java").read_text(encoding="utf-8")
    for call in re.findall(r"feedback\(player,[^;]+;", casting):
        required.update("message.celitest.staff." + reason for reason in re.findall(r'"([a-z_]+)"', call))
    arguments = {"accepted": 1, "cast": 1, "casting": 1, "cooldown": 2, "busy": 0,
                 "no_affinity": 0, "unsafe": 0, "interrupted": 1, "unavailable": 0, "insufficient_mana": 3}
    for reason, count in arguments.items():
        key = "message.celitest.staff." + reason
        required.add(key)
        assert {int(index) for index in re.findall(r"%(\d+)\$s", language[key])} == set(range(1, count + 1)), key
    missing = sorted(required - language.keys())
    assert not missing, f"Missing English translations: {missing}"
    registrations = (common / "MagicItems.java").read_text(encoding="utf-8")
    assert re.search(r'register\("mages_staff",[^;]+MagesStaffItem::new\)', registrations)
    for affinity, center in (("fire", "lava_bucket"), ("water", "water_bucket"), ("wind", "wind_charge")):
        identifier = f"{affinity}_affinity_stone"
        definition = json.loads((assets / f"items/{identifier}.json").read_text(encoding="utf-8"))
        model = json.loads((assets / f"models/item/{identifier}.json").read_text(encoding="utf-8"))
        assert definition == {"model": {"type": "minecraft:model", "model": f"celitest:item/{identifier}"}}
        assert model == {"parent": "minecraft:item/generated", "textures": {"layer0": f"celitest:item/{identifier}"}}
        assert f"item.celitest.{identifier}" in language
        assert re.search(rf'public static final Item {identifier.upper()}\s*=\s*register\("{identifier}"', registrations)
        recipe = json.loads((ROOT / f"src/main/resources/data/celitest/recipe/{identifier}.json").read_text(encoding="utf-8"))
        assert recipe == {
            "type": "minecraft:crafting_shaped", "category": "misc", "pattern": ["SSS", "SCS", "SSS"],
            "key": {"S": "celitest:magerald_shard", "C": f"minecraft:{center}"},
            "result": {"id": f"celitest:{identifier}", "count": 1},
        }, identifier
    assert "tooltip.celitest.affinity_stone.creative_only" not in language
    assert re.findall(r"%(\d+)\$s", language["tooltip.celitest.affinity_stone.recipe"]) == ["1"]
    print(f"Verified stone/staff registrations, 6 item/model JSON files, 3 crafting recipes, and {len(language)} unique English keys; "
          f"{len(required)} Java translation references covered, including {len(screen_keys)} altar keys and {len(arguments)} staff messages.")


def main():
    destinations = [TEXTURES / f"{affinity}_affinity_stone.png" for affinity in ("fire", "water", "wind")]
    for destination in [*destinations, PREVIEW]:
        if not destination.parent.is_dir():
            raise FileNotFoundError(destination.parent)
    verify_resources()
    factories = (fire_stone, water_stone, wind_stone)
    stones = [factory() for factory in factories]
    silhouettes = []
    for stone, factory in zip(stones, factories):
        assert stone.width == stone.height == 32
        assert stone.pixels == factory().pixels
        assert {pixel[3] for pixel in stone.pixels} == {0, 255}
        assert all(stone.pixels[y * 32 + x][3] == 0 for y in range(32) for x in range(32)
                   if x in (0, 31) or y in (0, 31))
        silhouettes.append(tuple(pixel[3] for pixel in stone.pixels))
    assert len(set(silhouettes)) == 3
    outputs = [*zip(destinations, stones), (PREVIEW, contact_sheet(stones))]
    for destination, canvas in outputs:
        if destination.exists() and destination.read_bytes() != canvas.png():
            raise FileExistsError(f"Refusing to overwrite different artwork: {destination}")
    for destination, canvas in outputs:
        canvas.save(destination)
    print("Verified deterministic pixels, 3 distinct silhouettes, transparent borders, binary alpha, PNG dimensions/CRC/rows.")
    print("Preview includes 16px samples, native 32px textures, and exact nearest-neighbor 2x/6x views.")


if __name__ == "__main__":
    main()
