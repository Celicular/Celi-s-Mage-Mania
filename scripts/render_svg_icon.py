from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

from PIL import Image, ImageDraw


SCALE = 4
ROOT = Path(__file__).resolve().parents[1]


def number(value, default=0.0):
    if value is None:
        return default
    return float(str(value).replace("px", ""))


def parse_color(value, opacity=1.0):
    if not value or value == "none":
        return None
    if value.startswith("#"):
        value = value[1:]
        if len(value) == 3:
            value = "".join(character * 2 for character in value)
        red, green, blue = (int(value[index:index + 2], 16) for index in (0, 2, 4))
        return red, green, blue, round(255 * opacity)
    named = {
        "white": (255, 255, 255),
        "black": (0, 0, 0),
    }
    red, green, blue = named[value.lower()]
    return red, green, blue, round(255 * opacity)


def points(value):
    values = [number(part) for part in re.findall(r"-?\d+(?:\.\d+)?", value)]
    return [(round(values[index] * SCALE), round(values[index + 1] * SCALE))
            for index in range(0, len(values), 2)]


def paint(element, name):
    opacity = number(element.get("opacity"), 1.0)
    opacity *= number(element.get(f"{name}-opacity"), 1.0)
    return parse_color(element.get(name), opacity)


def draw_element(image, element):
    tag = element.tag.rsplit("}", 1)[-1]
    if tag in {"svg", "g"}:
        for child in element:
            draw_element(image, child)
        return

    layer = Image.new("RGBA", image.size, (0, 0, 0, 0))
    draw = ImageDraw.Draw(layer)
    fill = paint(element, "fill")
    stroke = paint(element, "stroke")
    width = max(1, round(number(element.get("stroke-width"), 1.0) * SCALE))

    if tag == "rect":
        box = tuple(round(number(element.get(name)) * SCALE) for name in ("x", "y", "width", "height"))
        box = box[0], box[1], box[0] + box[2], box[1] + box[3]
        radius = round(number(element.get("rx"), 0.0) * SCALE)
        draw.rounded_rectangle(box, radius=radius, fill=fill, outline=stroke, width=width)
    elif tag == "circle":
        center_x = number(element.get("cx")) * SCALE
        center_y = number(element.get("cy")) * SCALE
        radius = number(element.get("r")) * SCALE
        box = center_x - radius, center_y - radius, center_x + radius, center_y + radius
        draw.ellipse(box, fill=fill, outline=stroke, width=width)
    elif tag == "ellipse":
        center_x = number(element.get("cx")) * SCALE
        center_y = number(element.get("cy")) * SCALE
        radius_x = number(element.get("rx")) * SCALE
        radius_y = number(element.get("ry")) * SCALE
        box = center_x - radius_x, center_y - radius_y, center_x + radius_x, center_y + radius_y
        draw.ellipse(box, fill=fill, outline=stroke, width=width)
    elif tag == "polygon":
        draw.polygon(points(element.get("points", "")), fill=fill)
        if stroke:
            draw.line(points(element.get("points", "")) + [points(element.get("points", ""))[0]], fill=stroke, width=width, joint="curve")
    elif tag == "line":
        coordinates = tuple(round(number(element.get(name)) * SCALE) for name in ("x1", "y1", "x2", "y2"))
        draw.line(coordinates, fill=stroke or fill, width=width)
    else:
        raise ValueError(f"Unsupported SVG element: {tag}")

    image.alpha_composite(layer)


def main():
    source = ROOT / "src/main/resources/assets/celitest/icon.svg"
    destination = ROOT / "src/main/resources/assets/celitest/icon.png"
    document = ET.parse(source)
    image = Image.new("RGBA", (128 * SCALE, 128 * SCALE), (0, 0, 0, 0))
    draw_element(image, document.getroot())
    image.resize((128, 128), Image.Resampling.LANCZOS).save(destination, "PNG")
    print(f"{destination.relative_to(ROOT).as_posix()} ({image.width // SCALE}x{image.height // SCALE}, RGBA)")


if __name__ == "__main__":
    main()
