from pathlib import Path
import struct
import zlib


ROOT = Path(__file__).resolve().parents[1]
TEXTURES = ROOT / "src/main/resources/assets/celitest/textures/item"


def png_chunk(kind, data):
    checksum = zlib.crc32(kind + data) & 0xFFFFFFFF
    return struct.pack(">I", len(data)) + kind + data + struct.pack(">I", checksum)


def write_png(name, width, height, pixel):
    if not TEXTURES.is_dir():
        raise FileNotFoundError(TEXTURES)
    rows = bytearray()
    for y in range(height):
        rows.append(0)
        for x in range(width):
            rows.extend((*pixel(x, y), 255))
    header = struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0)
    image = b"\x89PNG\r\n\x1a\n"
    image += png_chunk(b"IHDR", header)
    image += png_chunk(b"IDAT", zlib.compress(bytes(rows), 9))
    image += png_chunk(b"IEND", b"")
    destination = TEXTURES / name
    destination.write_bytes(image)
    print(f"{destination.relative_to(ROOT)} ({width}x{height})")


def shift(color, amount):
    return tuple(max(0, min(255, channel + amount)) for channel in color)


def shaft_pixel(x, y):
    rune = (".##.", "#..#", "..#.", ".#..", "..#.", "#..#", ".##.")
    if x < 16:
        local = x % 4
        colors = ((20, 24, 33), (48, 51, 59), (32, 36, 46), (24, 29, 39))
        color = colors[local]
        grain = (x * 11 + y * 7 + y // 5) % 13
        color = shift(color, 4 if grain == 0 else -3 if grain == 1 else 0)
        for start in (5, 26, 47):
            if x // 4 in (0, 2) and start <= y < start + len(rune):
                if rune[y - start][local] == "#":
                    return (217, 165, 66) if local == 1 else (157, 112, 43)
        if x // 4 in (1, 3) and local == 1 and y % 21 in (5, 11):
            return (124, 90, 43)
        return color
    if x < 28:
        local = (x - 16) % 4
        wrap = (y + local * 2) % 6
        colors = ((26, 25, 29), (46, 40, 39), (37, 33, 35), (29, 28, 33))
        color = shift(colors[local], 9 if wrap == 0 else -5 if wrap == 1 else 0)
        if y % 20 in (1, 18):
            return (91, 71, 43) if local in (1, 2) else (41, 34, 31)
        return color
    edge = x in (28, 31) or y % 4 in (0, 3)
    return (24, 28, 36) if edge else (43, 46, 52)


def gold_pixel(x, y):
    if x < 4 and y < 28:
        colors = ((120, 74, 25), (255, 224, 135), (218, 169, 66), (167, 110, 32))
        if x in (2, 3) and y % 9 in (3, 4, 5):
            return (116, 74, 29) if (x + y) % 3 == 0 else colors[x]
        return shift(colors[x], -5 if y % 7 == 6 else 0)
    if x < 10 and y < 28:
        colors = ((101, 60, 22), (166, 108, 31), (203, 151, 51), (226, 180, 77), (192, 137, 43), (136, 85, 25))
        return shift(colors[x - 4], 7 if y % 9 == 1 else 0)
    if 10 <= x < 22 and y < 24:
        local_x = x - 10
        local_y = y % 8
        if local_x in (0, 11) or local_y in (0, 7):
            return (123, 78, 27)
        if local_y == 1:
            return (255, 222, 128)
        if local_y == 6:
            return (177, 117, 35)
        glyph = {(4, 3), (5, 2), (6, 3), (7, 4), (6, 5), (5, 4), (4, 5), (2, 4), (9, 4)}
        if (local_x, local_y) in glyph:
            return (114, 72, 29)
        return (223, 176, 73) if local_x < 6 else (198, 142, 43)
    if x >= 22 and y < 16:
        local_x = (x - 22) % 4
        local_y = y % 4
        if local_x in (0, 3) or local_y in (0, 3):
            return (155, 99, 31)
        return (255, 224, 137) if local_x == 1 and local_y == 1 else (212, 163, 60)
    if x >= 24 and 16 <= y < 24:
        local_x = x - 24
        local_y = y - 16
        edge = min(local_x, local_y, 7 - local_x, 7 - local_y)
        if edge == 0:
            return (123, 75, 24)
        if edge == 1:
            return (255, 221, 127) if local_x + local_y < 7 else (190, 126, 37)
        return (217, 163, 57) if (local_x + local_y) % 4 else (175, 112, 31)
    return (245, 206, 109) if (x + y) % 4 == 0 else (199, 145, 48)


def crystal_pixel(x, y):
    if y < 16:
        if x in (0, 15):
            return (29, 111, 139)
        if x in (1, 14):
            return (67, 165, 182)
        if x + y < 10:
            color = (189, 249, 242)
        elif x * 2 - y < 4:
            color = (111, 224, 222)
        elif x + y < 22:
            color = (68, 192, 205)
        else:
            color = (40, 138, 172)
        if x + y in (10, 11):
            return (221, 255, 247)
        if x - y in (6, 7) and x < 13:
            return (131, 234, 228)
        if x == 3 and 2 <= y < 9:
            return (220, 255, 246)
        return color
    local_y = y - 16
    if x + local_y < 10:
        color = (182, 248, 239)
    elif x < local_y:
        color = (71, 194, 203)
    else:
        color = (43, 146, 177)
    if x + local_y in (9, 10):
        return (219, 255, 247)
    if x in (1, 14) or local_y in (1, 14):
        return (38, 122, 154)
    return color


def ruby_pixel(x, y):
    edge = min(x, y, 15 - x, 15 - y)
    if edge == 0:
        return (73, 15, 39)
    if edge == 1:
        return (136, 22, 58)
    if x + y < 11:
        color = (252, 95, 115)
    elif x - y > 5:
        color = (196, 35, 69)
    elif x + y < 23:
        color = (225, 42, 76)
    else:
        color = (149, 22, 55)
    if x + y in (8, 9) and x < 9:
        return (255, 177, 174)
    if x == 3 and 3 <= y <= 6:
        return (255, 213, 195)
    if x - y == 5 and y < 11:
        return (255, 94, 113)
    return color


def main():
    write_png("mages_staff_shaft.png", 32, 64, shaft_pixel)
    write_png("mages_staff_gold.png", 32, 32, gold_pixel)
    write_png("mages_staff_crystal.png", 16, 32, crystal_pixel)
    write_png("mages_staff_ruby.png", 16, 16, ruby_pixel)


if __name__ == "__main__":
    main()
