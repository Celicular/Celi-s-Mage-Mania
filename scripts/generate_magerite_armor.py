import argparse
import struct
import zlib
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/celitest/textures/entity/equipment"
SCALE = 4
SHADOW = (15, 20, 26, 255)
RIM = (29, 36, 43, 255)
RIM_EDGE = (64, 76, 83, 255)
BLUE = (113, 188, 203, 255)
BLUE_MID = (90, 162, 183, 255)
BLUE_DARK = (45, 103, 126, 255)
BLUE_LIGHT = (193, 225, 229, 255)
BLUE_GLOW = (148, 211, 222, 255)
RUBY = (145, 40, 60, 255)
RUBY_DARK = (93, 28, 46, 255)
RUBY_LIGHT = (199, 83, 96, 255)

HEAD_VEINS = (
    (1, ((1.46, .35), (1.54, .44), (1.50, .51), (1.63, .63))),
    (2, ((1.54, .44), (1.36, .42), (1.28, .32), (1.12, .29))),
    (3, ((1.50, .51), (1.29, .62), (1.24, .73))),
    (4, ((1.63, .63), (1.83, .57), (1.95, .63), (2.18, .54))),
    (5, ((1.12, .29), (.92, .39), (.77, .33), (.56, .48))),
    (6, ((2.18, .54), (2.34, .36), (2.55, .33), (2.72, .44))),
    (7, ((.56, .48), (.42, .57), (.24, .56), (0, .67))),
    (7, ((4, .67), (3.85, .54), (3.69, .55))),
    (8, ((2.72, .44), (2.92, .36), (3.12, .48), (3.24, .47), (3.43, .65))),
    (9, ((3.43, .65), (3.60, .61), (3.69, .55))),
    (9, ((1.83, .57), (1.82, .32), (1.94, .20))),
    (10, ((2.34, .36), (2.28, .20))),
    (10, ((3.12, .48), (3.18, .27))),
    (10, ((.77, .33), (.66, .17))),
    (10, ((1.24, .73), (1.40, .85))),
)
CHEST_VEINS = (
    (1, ((.40, .24), (.48, .31), (.43, .42))),
    (2, ((.43, .42), (.58, .50), (.55, .60))),
    (3, ((.48, .31), (.64, .22), (.72, .25), (.84, .12))),
    (4, ((.55, .60), (.43, .70), (.47, .80), (.38, .92))),
    (5, ((.43, .42), (.27, .48), (.20, .38), (.09, .41))),
    (6, ((.55, .60), (.73, .63), (.79, .75), (.90, .80))),
    (7, ((.40, .24), (.27, .20), (.21, .09))),
    (8, ((.43, .70), (.27, .70), (.21, .82), (.09, .84))),
    (9, ((.73, .63), (.79, .51), (.89, .49))),
    (10, ((.64, .22), (.62, .08))),
    (10, ((.27, .48), (.19, .58))),
    (10, ((.47, .80), (.60, .88))),
)
BABY_CHEST_VEINS = (
    (1, ((.43, .30), (.50, .41))),
    (2, ((.50, .41), (.43, .54))),
    (3, ((.50, .41), (.65, .37))),
    (4, ((.43, .54), (.52, .68))),
    (5, ((.43, .30), (.31, .23), (.28, .14))),
    (6, ((.65, .37), (.75, .24))),
    (7, ((.52, .68), (.43, .82))),
    (8, ((.43, .54), (.28, .58), (.22, .68))),
    (9, ((.52, .68), (.68, .73))),
    (10, ((.68, .73), (.79, .84))),
    (10, ((.31, .23), (.15, .29))),
)
BACK_VEINS = (
    (3, ((.64, .21), (.51, .28), (.57, .40))),
    (5, ((.57, .40), (.41, .49), (.48, .60))),
    (6, ((.51, .28), (.35, .23), (.28, .09))),
    (8, ((.48, .60), (.40, .77), (.52, .87))),
    (8, ((.41, .49), (.23, .51), (.17, .61))),
    (10, ((.57, .40), (.75, .43), (.83, .54))),
    (10, ((.40, .77), (.23, .84))),
)
BABY_BACK_VEINS = (
    (4, ((.61, .24), (.48, .36))),
    (7, ((.48, .36), (.55, .52), (.44, .65))),
    (9, ((.48, .36), (.31, .32), (.25, .20))),
    (10, ((.44, .65), (.57, .79))),
)
ARM_VEINS = (
    (4, ((.52, .12), (.35, .23), (.56, .35))),
    (5, ((.56, .35), (.44, .48))),
    (6, ((.35, .23), (.22, .20))),
    (7, ((.44, .48), (.65, .59))),
    (8, ((.65, .59), (.48, .73))),
    (9, ((.56, .35), (.80, .39))),
    (10, ((.48, .73), (.58, .86))),
    (10, ((.44, .48), (.21, .54))),
)
BABY_ARM_VEINS = (
    (7, ((.48, .20), (.35, .35))),
    (8, ((.35, .35), (.57, .52))),
    (9, ((.57, .52), (.45, .66))),
    (10, ((.45, .66), (.65, .80))),
)
LEG_VEINS = (
    (1, ((1.38, .18), (1.61, .30))),
    (2, ((1.61, .30), (1.49, .43))),
    (3, ((1.49, .43), (1.73, .58))),
    (4, ((1.61, .30), (1.87, .27), (2.14, .39))),
    (5, ((1.73, .58), (1.55, .73), (1.67, .86))),
    (6, ((1.49, .43), (1.24, .52), (.88, .44))),
    (7, ((2.14, .39), (2.38, .28), (2.63, .36))),
    (8, ((.88, .44), (.63, .62), (.34, .57))),
    (9, ((2.63, .36), (2.88, .51), (3.16, .42), (3.35, .56))),
    (10, ((.34, .57), (.13, .41), (0, .44))),
    (10, ((4, .44), (3.77, .38), (3.35, .56))),
    (10, ((1.55, .73), (1.26, .81))),
)
BABY_LEG_VEINS = (
    (1, ((1.43, .21), (1.61, .36))),
    (2, ((1.61, .36), (1.46, .51))),
    (3, ((1.46, .51), (1.62, .69))),
    (4, ((1.61, .36), (1.90, .29))),
    (5, ((1.62, .69), (1.48, .85))),
    (6, ((1.90, .29), (2.21, .38))),
    (7, ((1.46, .51), (1.15, .59), (.88, .47))),
    (8, ((2.21, .38), (2.50, .25))),
    (9, ((.88, .47), (.62, .65))),
    (10, ((2.50, .25), (2.78, .39))),
    (10, ((.62, .65), (.29, .59))),
)
BOOT_VEINS = (
    (1, ((1.32, .78), (1.53, .67))),
    (2, ((1.53, .67), (1.47, .49))),
    (3, ((1.47, .49), (1.69, .35))),
    (4, ((1.69, .35), (1.92, .40), (2.13, .30))),
    (5, ((1.47, .49), (1.20, .44), (.97, .56))),
    (6, ((1.69, .35), (1.53, .20), (1.63, .10))),
    (7, ((2.13, .30), (2.39, .45), (2.65, .37))),
    (8, ((.97, .56), (.73, .42), (.49, .49))),
    (9, ((1.53, .67), (1.78, .78), (1.87, .91))),
    (10, ((2.39, .45), (2.51, .65), (2.74, .72))),
    (10, ((.73, .42), (.57, .24), (.35, .18))),
)
BABY_BOOT_TOP_VEINS = (
    (1, ((.43, .25), (.52, .40))),
    (2, ((.52, .40), (.42, .56))),
    (3, ((.42, .56), (.57, .69))),
    (4, ((.57, .69), (.49, 1))),
    (10, ((.52, .40), (.77, .28))),
)
BABY_BOOT_WRAP_VEINS = (
    (5, ((1.49, 0), (1.58, .48))),
    (6, ((1.58, .48), (1.79, .32), (2.04, .45))),
    (7, ((2.04, .45), (2.25, .68), (2.49, .42))),
    (8, ((1.58, .48), (1.30, .62), (.93, .42))),
    (9, ((.93, .42), (.63, .65))),
    (10, ((2.49, .42), (2.78, .65))),
    (10, ((.63, .65), (.36, .44))),
)


class Canvas:
    def __init__(self, baby=False, width=None, height=None):
        self.width = width or 64 * SCALE
        self.height = height or (64 if baby else 32) * SCALE
        self.pixels = bytearray(self.width * self.height * 4)

    def put(self, x, y, color):
        if 0 <= x < self.width and 0 <= y < self.height:
            start = (y * self.width + x) * 4
            self.pixels[start:start + 4] = bytes(color)

    def write(self, path):
        path.parent.mkdir(parents=True, exist_ok=True)
        rows = b"".join(
            b"\x00" + self.pixels[y * self.width * 4:(y + 1) * self.width * 4]
            for y in range(self.height)
        )
        header = struct.pack(">IIBBBBB", self.width, self.height, 8, 6, 0, 0, 0)
        path.write_bytes(
            b"\x89PNG\r\n\x1a\n"
            + chunk(b"IHDR", header)
            + chunk(b"IDAT", zlib.compress(rows, 9))
            + chunk(b"IEND", b"")
        )


def chunk(kind, data):
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data))


def layout(slot, side, baby):
    mirror = "x" if side == "left" else None
    if slot == "head":
        width = 9 if baby else 8
        regions = ((0, 8, 8, 8), (8, 8, width, 8), (8 + width, 8, 8, 8), (16 + width, 8, width, 8))
        return ((regions, HEAD_VEINS, None),)
    if slot == "chest":
        if baby:
            return (
                (((3, 20, 6, 5),), BABY_CHEST_VEINS, None),
                (((12, 20, 6, 5),), BABY_BACK_VEINS, None),
                (((33, 20, 2, 5),), BABY_ARM_VEINS, None),
                (((33, 28, 2, 5),), BABY_ARM_VEINS, "x"),
            )
        return (
            (((20, 20, 8, 12),), CHEST_VEINS, None),
            (((32, 20, 8, 12),), BACK_VEINS, None),
            (((44, 20, 4, 12),), ARM_VEINS, None),
        )
    if slot == "legs":
        if baby:
            y = 20 if side == "right" else 27
            regions = ((18, y, 3, 4), (21, y, 3, 4), (24, y, 3, 4))
            return ((regions, BABY_LEG_VEINS, mirror),)
        regions = tuple((x, 20, 4, 7) for x in (0, 4, 8, 12))
        return ((regions, LEG_VEINS, "y" if side == "left" else None),)
    if baby:
        y = 29 if side == "right" else 25
        return (
            (((3, y, 3, 3),), BABY_BOOT_TOP_VEINS, mirror),
            (tuple((x, y + 3, 3, 1) for x in (0, 3, 6)), BABY_BOOT_WRAP_VEINS, mirror),
        )
    regions = tuple((x, 25.5, 4, 6.5) for x in (0, 4, 8))
    return ((regions, BOOT_VEINS, mirror),)


def map_point(regions, point, mirror):
    u, v = point
    if mirror == "x":
        u = len(regions) - u
    elif mirror == "y":
        v = 1 - v
    index = min(int(u), len(regions) - 1)
    x, y, width, height = regions[index]
    return round((x + (u - index) * width) * SCALE), round((y + v * height) * SCALE)


def line_points(start, end):
    x0, y0 = start
    x1, y1 = end
    dx, dy = abs(x1 - x0), -abs(y1 - y0)
    sx, sy = 1 if x0 < x1 else -1, 1 if y0 < y1 else -1
    error = dx + dy
    while True:
        yield x0, y0
        if (x0, y0) == (x1, y1):
            break
        twice = 2 * error
        if twice >= dy:
            error += dy
            x0 += sx
        if twice <= dx:
            error += dx
            y0 += sy


def spread(points, radius, allowed):
    return {
        (x + dx, y + dy)
        for x, y in points
        for dy in range(-radius, radius + 1)
        for dx in range(-radius, radius + 1)
        if abs(dx) + abs(dy) <= radius and (x + dx, y + dy) in allowed
    }


def generate(layer, slot, side, level):
    baby = layer == "humanoid_baby"
    canvas, glow = Canvas(baby), Canvas(baby)
    centers, allowed, ruby_sites, glow_sites = set(), set(), set(), set()
    for regions, pattern, mirror in layout(slot, side, baby):
        island = {
            (px, py)
            for x, y, width, height in regions
            for py in range(round(y * SCALE), round((y + height) * SCALE))
            for px in range(round(x * SCALE), round((x + width) * SCALE))
        }
        allowed.update(island)
        for stage, path in pattern:
            if stage > level:
                continue
            vertices = [map_point(regions, point, mirror) for point in path]
            points = [point for start, end in zip(vertices, vertices[1:]) for point in line_points(start, end)]
            points = [point for point in points if point in island]
            centers.update(points)
            facet = points[len(points) // 2]
            if stage in (3, 7, 9):
                ruby_sites.add(facet)
            if level >= stage + 4:
                glow_sites.add(facet)
    core = spread(centers, 1, allowed)
    rim = spread(centers, 2, allowed)
    teeth = {point for point in centers if (point[0] * 7 + point[1] * 11) % 37 == 0}
    rim.update(spread(teeth, 3, allowed))
    shadow = {(x + 1, y + 1) for x, y in rim if (x + 1, y + 1) in allowed}
    ruby = spread(ruby_sites, 2, core)
    for x, y in shadow | rim:
        color = SHADOW if (x, y) not in rim else RIM
        if (x + 1, y + 1) in core and (x - 1, y - 1) not in core:
            color = RIM_EDGE
        canvas.put(x, y, color)
    for x, y in core:
        upper = (x - 1, y - 1) not in core
        lower = (x + 1, y + 1) not in core
        if (x, y) in ruby:
            color = RUBY_LIGHT if upper and (x + y) % 4 == 0 else RUBY
            if lower and (x, y) not in centers:
                color = RUBY_DARK
        elif upper and (x * 3 + y * 5) % 11 == 0:
            color = BLUE_LIGHT
        elif lower and (x, y) not in centers:
            color = BLUE_DARK
        else:
            color = BLUE_MID if (x * 3 + y * 5) % 7 == 0 else BLUE
        canvas.put(x, y, color)
    for x, y in glow_sites:
        for point in ((x, y), (x + 1, y)):
            if point in core:
                glow.put(*point, RUBY_LIGHT if point in ruby else BLUE_GLOW)
    return canvas, glow, centers, core | rim | shadow


FONT = {
    "A": "010/101/111/101/101", "D": "110/101/101/101/110",
    "E": "111/100/110/100/111", "G": "111/100/101/101/111",
    "I": "111/010/010/010/111", "L": "100/100/100/100/111",
    "M": "101/111/111/101/101", "N": "101/111/111/111/101",
    "O": "111/101/101/101/111", "P": "110/101/110/100/100",
    "R": "110/101/110/101/101", "T": "111/010/010/010/010",
    "U": "101/101/101/101/111", "V": "101/101/101/101/010",
    "W": "101/101/111/111/101", "X": "101/101/010/101/101",
    "0": "111/101/101/101/111", "1": "010/110/010/010/111",
    "5": "111/100/111/001/111", "-": "000/000/111/000/000",
    " ": "000/000/000/000/000",
}


def label(canvas, text, y, color):
    left = (canvas.width - len(text) * 8 + 2) // 2
    for index, character in enumerate(text):
        for row, bits in enumerate(FONT[character].split("/")):
            for column, bit in enumerate(bits):
                if bit == "1":
                    for dy in range(2):
                        for dx in range(2):
                            canvas.put(left + index * 8 + column * 2 + dx, y + row * 2 + dy, color)


def preview(samples):
    canvas = Canvas(width=400, height=232)
    canvas.pixels[:] = bytes((23, 27, 33, 255)) * canvas.width * canvas.height
    label(canvas, "TEXTURE PREVIEW", 12, (199, 213, 218, 255))
    label(canvas, "NOT AN IN-GAME RENDER", 30, (126, 143, 152, 255))
    for column, level in enumerate((1, 5, 10)):
        source = samples[level]
        left = 24 + column * 128
        for y in range(144):
            for x in range(96):
                px, py = x // 3, y // 3
                shade = (px * 13 + py * 7) % 5 - py // 16
                color = (59 + shade, 65 + shade, 72 + shade, 255)
                offset = ((20 * SCALE + py) * source.width + 20 * SCALE + px) * 4
                if source.pixels[offset + 3]:
                    color = tuple(source.pixels[offset:offset + 4])
                canvas.put(left + x, 52 + y, color)
        text = "LEVEL " + str(level)
        text_canvas = Canvas(width=96, height=10)
        label(text_canvas, text, 0, (169, 194, 205, 255))
        for y in range(10):
            for x in range(96):
                offset = (y * 96 + x) * 4
                if text_canvas.pixels[offset + 3]:
                    canvas.put(left + x, 210 + y, tuple(text_canvas.pixels[offset:offset + 4]))
    destination = ROOT / "docs/previews/magerite-veins.png"
    canvas.write(destination)
    print("Texture preview (not an in-game render): " + str(destination.relative_to(ROOT)))


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--preview", action="store_true", help="Write a chest-front texture contact sheet.")
    args = parser.parse_args()
    if not TEXTURES.is_dir():
        raise SystemExit("The existing equipment texture directory is required.")
    variants = {
        "humanoid": (("head", None), ("chest", None), ("feet", "right"), ("feet", "left")),
        "humanoid_leggings": (("legs", "right"), ("legs", "left")),
        "humanoid_baby": (
            ("head", None), ("chest", None), ("legs", "right"), ("legs", "left"),
            ("feet", "right"), ("feet", "left"),
        ),
    }
    for layer in variants:
        if not (TEXTURES / layer / "magerite").is_dir():
            raise SystemExit("The existing Magerite texture directories are required.")
    if args.preview and not (ROOT / "docs").is_dir():
        raise SystemExit("The existing docs directory is required for the optional preview.")
    samples = {}
    for layer, parts in variants.items():
        for slot, side in parts:
            previous_centers, previous_ink = set(), set()
            for level in range(1, 11):
                canvas, glow, centers, ink = generate(layer, slot, side, level)
                if not previous_centers < centers or not previous_ink <= ink:
                    raise SystemExit("Veins must grow cumulatively at every per-item level.")
                previous_centers, previous_ink = centers, ink
                name = slot + ("_" + side if side else "") + "_" + str(level)
                destination = TEXTURES / layer / "magerite"
                canvas.write(destination / (name + ".png"))
                glow.write(destination / (name + "_glow.png"))
                if layer == "humanoid" and slot == "chest" and level in (1, 5, 10):
                    samples[level] = canvas
    print("Generated 120 cumulative Magerite armor overlays and 120 matching glow masks.")
    print("Verified strictly increasing vein paths and cumulative opaque coverage at all 10 stages.")
    if args.preview:
        preview(samples)


if __name__ == "__main__":
    main()
