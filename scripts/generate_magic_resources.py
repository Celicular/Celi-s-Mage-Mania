from pathlib import Path
import random
import struct
import zlib


ROOT = Path(__file__).resolve().parent.parent
TEXTURES = ROOT / "src/main/resources/assets/celitest/textures"
TRANSPARENT = (0, 0, 0, 0)
STONE = ("#666968", "#727572", "#7f827d", "#8a8d86", "#999b93")
DARK = ("#20282e", "#283238", "#303b41", "#39464b", "#435156")
GOLD = ("#58412c", "#8a6133", "#b58b45", "#d9b96b", "#f1d797")
AQUA = ("#24464e", "#356d7c", "#529ab0", "#84c4d5", "#b5e0e8", "#e2f5f2")
RUBY = ("#401f30", "#6b293e", "#98334d", "#bc4f66", "#df7e87", "#f1b1ac")


def rgba(value):
    if isinstance(value, tuple):
        return value
    return tuple(int(value[index:index + 2], 16) for index in (1, 3, 5)) + (255,)


class Canvas:
    def __init__(self, size, background=TRANSPARENT):
        self.size = size
        self.pixels = [rgba(background)] * (size * size)

    def pixel(self, x, y, color):
        if 0 <= x < self.size and 0 <= y < self.size:
            self.pixels[y * self.size + x] = rgba(color)

    def rect(self, left, top, right, bottom, color):
        for y in range(top, bottom + 1):
            for x in range(left, right + 1):
                self.pixel(x, y, color)

    def line(self, x0, y0, x1, y1, color):
        dx = abs(x1 - x0)
        dy = -abs(y1 - y0)
        sx = 1 if x0 < x1 else -1
        sy = 1 if y0 < y1 else -1
        error = dx + dy
        while True:
            self.pixel(x0, y0, color)
            if x0 == x1 and y0 == y1:
                break
            doubled = 2 * error
            if doubled >= dy:
                error += dy
                x0 += sx
            if doubled <= dx:
                error += dx
                y0 += sy

    def polygon(self, points, color):
        for y in range(min(point[1] for point in points), max(point[1] for point in points) + 1):
            for x in range(min(point[0] for point in points), max(point[0] for point in points) + 1):
                inside = False
                previous = points[-1]
                for current in points:
                    if (current[1] > y + 0.5) != (previous[1] > y + 0.5):
                        crossing = current[0] + (y + 0.5 - current[1]) * (previous[0] - current[0]) / (previous[1] - current[1])
                        if x + 0.5 < crossing:
                            inside = not inside
                    previous = current
                if inside:
                    self.pixel(x, y, color)
        for start, end in zip(points, points[1:] + points[:1]):
            self.line(*start, *end, color)

    def frame(self, left, top, right, bottom, color):
        self.line(left, top, right, top, color)
        self.line(left, bottom, right, bottom, color)
        self.line(left, top, left, bottom, color)
        self.line(right, top, right, bottom, color)

    def save(self, path):
        if not path.parent.is_dir():
            raise FileNotFoundError(path.parent)
        raw = b"".join(b"\x00" + bytes(channel for pixel in self.pixels[y * self.size:(y + 1) * self.size] for channel in pixel) for y in range(self.size))

        def chunk(kind, data):
            return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", zlib.crc32(kind + data) & 0xffffffff)

        header = struct.pack(">IIBBBBB", self.size, self.size, 8, 6, 0, 0, 0)
        path.write_bytes(b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", header) + chunk(b"IDAT", zlib.compress(raw, 9)) + chunk(b"IEND", b""))
        print(path.relative_to(ROOT).as_posix())


def stone(size, palette, seed):
    canvas = Canvas(size)
    rng = random.Random(seed)
    for y in range(size):
        for x in range(size):
            canvas.pixel(x, y, palette[rng.choices(range(len(palette)), weights=(1, 4, 8, 4, 1))[0]])
    return canvas


def ore():
    canvas = stone(16, STONE, 741)
    for x0, y0, x1, y1 in ((0, 3, 4, 4), (8, 1, 11, 1), (1, 10, 4, 10), (10, 14, 15, 15), (6, 7, 9, 8)):
        canvas.line(x0, y0, x1, y1, STONE[1])
        canvas.line(x0, y0 + 1, x1, y1 + 1, STONE[3])
    for points in (
        [(3, 1), (5, 2), (5, 5), (3, 6), (2, 4)],
        [(10, 3), (13, 4), (13, 6), (11, 7), (9, 6)],
        [(6, 8), (8, 9), (8, 12), (6, 13), (4, 11)],
        [(12, 10), (14, 11), (14, 13), (12, 14), (11, 12)],
        [(0, 13), (2, 13), (2, 15), (0, 15)],
    ):
        canvas.polygon(points, AQUA[0])
    canvas.rect(3, 2, 4, 4, AQUA[3])
    canvas.line(3, 2, 3, 5, AQUA[4])
    canvas.pixel(4, 5, AQUA[2])
    canvas.line(10, 4, 12, 4, AQUA[4])
    canvas.rect(10, 5, 12, 5, AQUA[3])
    canvas.line(11, 6, 12, 6, AQUA[2])
    canvas.line(6, 9, 6, 12, AQUA[4])
    canvas.line(7, 10, 7, 11, AQUA[3])
    canvas.pixel(5, 11, AQUA[2])
    canvas.pixel(12, 11, AQUA[4])
    canvas.rect(12, 12, 13, 12, AQUA[3])
    canvas.pixel(13, 13, AQUA[2])
    canvas.pixel(1, 14, AQUA[4])
    canvas.pixel(1, 15, AQUA[2])
    canvas.pixel(8, 3, AQUA[2])
    canvas.pixel(10, 9, AQUA[3])
    return canvas


def shard():
    canvas = Canvas(16)
    canvas.polygon([(8, 1), (11, 4), (11, 9), (6, 14), (3, 11), (5, 5)], AQUA[0])
    canvas.polygon([(8, 2), (8, 8), (5, 12), (4, 10), (6, 5)], AQUA[3])
    canvas.polygon([(8, 2), (10, 4), (10, 8), (8, 8)], AQUA[4])
    canvas.polygon([(8, 9), (10, 8), (6, 13), (5, 12)], AQUA[2])
    canvas.line(7, 4, 5, 9, AQUA[5])
    canvas.line(8, 2, 9, 3, AQUA[5])
    canvas.pixel(6, 11, AQUA[4])
    canvas.polygon([(12, 10), (14, 11), (12, 14), (11, 13)], AQUA[0])
    canvas.line(12, 11, 12, 13, AQUA[3])
    canvas.pixel(13, 11, AQUA[5])
    canvas.pixel(3, 3, AQUA[4])
    canvas.pixel(12, 2, AQUA[3])
    return canvas


def cluster():
    canvas = Canvas(16)
    canvas.polygon([(3, 5), (5, 4), (8, 10), (6, 14), (3, 11)], AQUA[0])
    canvas.polygon([(4, 6), (5, 5), (7, 10), (6, 12), (4, 10)], AQUA[3])
    canvas.line(4, 6, 5, 9, AQUA[5])
    canvas.polygon([(11, 3), (14, 4), (14, 9), (10, 13), (8, 10)], AQUA[0])
    canvas.polygon([(11, 4), (12, 5), (11, 9), (9, 11), (9, 9)], AQUA[4])
    canvas.polygon([(13, 5), (13, 8), (11, 11), (11, 9)], AQUA[2])
    canvas.line(11, 4, 12, 4, AQUA[5])
    canvas.polygon([(8, 1), (10, 3), (10, 9), (8, 12), (5, 9), (6, 4)], RUBY[0])
    canvas.polygon([(8, 2), (8, 8), (6, 10), (6, 6)], RUBY[3])
    canvas.polygon([(8, 2), (9, 4), (9, 8), (8, 8)], RUBY[4])
    canvas.polygon([(8, 9), (9, 8), (8, 11), (7, 10)], RUBY[1])
    canvas.line(7, 4, 7, 6, RUBY[5])
    canvas.polygon([(2, 11), (5, 10), (8, 12), (12, 10), (14, 12), (11, 14), (5, 14)], GOLD[0])
    canvas.line(3, 12, 6, 13, GOLD[3])
    canvas.line(9, 13, 12, 12, GOLD[3])
    canvas.pixel(7, 13, RUBY[4])
    canvas.pixel(8, 13, RUBY[2])
    canvas.pixel(2, 3, AQUA[3])
    return canvas


def altar_base():
    canvas = stone(32, DARK, 812)
    canvas.frame(0, 0, 31, 31, DARK[0])
    canvas.frame(1, 1, 30, 30, DARK[3])
    for x, y in ((4, 5), (20, 8), (10, 23), (23, 26)):
        canvas.line(x, y, x + 4, y, DARK[0])
        canvas.line(x + 2, y + 1, x + 5, y + 1, DARK[3])
    return canvas


def altar_side():
    canvas = altar_base()
    canvas.frame(2, 3, 29, 28, GOLD[1])
    canvas.line(3, 4, 28, 4, GOLD[3])
    canvas.line(3, 5, 28, 5, GOLD[2])
    canvas.line(3, 27, 28, 27, GOLD[2])
    canvas.polygon([(15, 7), (23, 16), (15, 24), (8, 16)], GOLD[0])
    canvas.polygon([(15, 9), (21, 16), (15, 22), (10, 16)], GOLD[2])
    canvas.polygon([(15, 11), (19, 16), (15, 20), (12, 16)], RUBY[0])
    canvas.polygon([(15, 12), (15, 18), (13, 16)], RUBY[4])
    canvas.polygon([(16, 13), (18, 16), (16, 19)], RUBY[2])
    canvas.pixel(15, 12, RUBY[5])
    for x in (5, 25):
        canvas.line(x, 11, x, 20, GOLD[2])
        canvas.line(x - 1, 12, x + 1, 12, GOLD[3])
        canvas.line(x - 1, 19, x + 1, 19, GOLD[3])
        canvas.pixel(x + 1, 15, AQUA[3])
        canvas.pixel(x - 1, 17, AQUA[3])
    return canvas


def altar_top():
    canvas = stone(32, DARK, 913)
    canvas.frame(0, 0, 31, 31, GOLD[0])
    canvas.frame(1, 1, 30, 30, GOLD[3])
    canvas.frame(2, 2, 29, 29, GOLD[1])
    canvas.frame(6, 6, 25, 25, GOLD[2])
    canvas.frame(7, 7, 24, 24, DARK[0])
    canvas.polygon([(15, 8), (23, 15), (16, 23), (8, 16)], GOLD[1])
    canvas.polygon([(15, 10), (21, 15), (16, 21), (10, 16)], DARK[1])
    canvas.frame(12, 12, 19, 19, RUBY[2])
    for x, y in ((4, 4), (27, 4), (4, 27), (27, 27)):
        canvas.rect(x - 1, y - 1, x + 1, y + 1, GOLD[1])
        canvas.pixel(x, y, RUBY[4])
    for x, y, dx, dy in ((15, 3, 1, 0), (28, 15, 0, 1), (15, 28, 1, 0), (3, 15, 0, 1)):
        canvas.line(x - dx * 3, y - dy * 3, x + dx * 3, y + dy * 3, GOLD[2])
        canvas.pixel(x, y, AQUA[4])
    for x, y in ((9, 4), (22, 4), (9, 27), (22, 27)):
        canvas.line(x, y - 1, x, y + 1, GOLD[2])
        canvas.pixel(x + 1, y - 1, GOLD[3])
    return canvas


def altar_gold():
    canvas = Canvas(16, GOLD[2])
    canvas.frame(0, 0, 15, 15, GOLD[0])
    canvas.line(1, 1, 14, 1, GOLD[4])
    canvas.line(1, 2, 14, 2, GOLD[3])
    canvas.line(1, 1, 1, 14, GOLD[3])
    canvas.line(2, 13, 14, 13, GOLD[1])
    canvas.line(14, 2, 14, 14, GOLD[1])
    for x, y in ((4, 5), (10, 7), (6, 10)):
        canvas.line(x, y, x + 2, y, GOLD[1])
        canvas.pixel(x, y - 1, GOLD[3])
    return canvas


def altar_ruby():
    canvas = Canvas(16, RUBY[1])
    canvas.polygon([(0, 0), (15, 0), (8, 6), (0, 12)], RUBY[3])
    canvas.polygon([(0, 0), (5, 1), (8, 6), (0, 12)], RUBY[4])
    canvas.polygon([(15, 1), (15, 15), (8, 8), (8, 6)], RUBY[2])
    canvas.polygon([(0, 13), (8, 8), (14, 15), (0, 15)], RUBY[0])
    canvas.line(1, 1, 4, 1, RUBY[5])
    canvas.line(1, 2, 1, 5, RUBY[5])
    canvas.line(8, 6, 8, 8, RUBY[4])
    return canvas


def altar_crystal():
    canvas = Canvas(16, AQUA[2])
    canvas.polygon([(0, 0), (6, 0), (8, 7), (4, 15), (0, 15)], AQUA[4])
    canvas.polygon([(7, 0), (15, 0), (11, 8), (8, 7)], AQUA[3])
    canvas.polygon([(12, 8), (15, 4), (15, 15), (5, 15)], AQUA[1])
    canvas.polygon([(8, 8), (11, 8), (5, 15)], AQUA[2])
    canvas.line(1, 0, 3, 0, AQUA[5])
    canvas.line(1, 1, 1, 11, AQUA[5])
    canvas.line(2, 12, 3, 14, AQUA[3])
    canvas.line(8, 7, 10, 7, AQUA[5])
    return canvas


def main():
    outputs = (
        ("block/magerald_ore.png", ore),
        ("item/magerald_shard.png", shard),
        ("item/magerite_cluster.png", cluster),
        ("block/mages_altar_base.png", altar_base),
        ("block/mages_altar_side.png", altar_side),
        ("block/mages_altar_top.png", altar_top),
        ("block/mages_altar_gold.png", altar_gold),
        ("block/mages_altar_ruby.png", altar_ruby),
        ("block/mages_altar_crystal.png", altar_crystal),
    )
    for relative_path, factory in outputs:
        factory().save(TEXTURES / relative_path)


if __name__ == "__main__":
    main()
