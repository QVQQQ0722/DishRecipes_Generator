"""Losslessly convert the downloaded path-only SVGs to Android VectorDrawables."""
from pathlib import Path
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
ANDROID = "http://schemas.android.com/apk/res/android"
ET.register_namespace("android", ANDROID)

def attr(name):
    return f"{{{ANDROID}}}{name}"

for source in (ROOT / "app/src/main/assets/figma").glob("*.svg"):
    svg = ET.parse(source).getroot()
    x, y, width, height = svg.attrib["viewBox"].split()
    assert x == y == "0"
    vector = ET.Element("vector", {
        attr("width"): f"{width}dp", attr("height"): f"{height}dp",
        attr("viewportWidth"): width, attr("viewportHeight"): height,
    })
    for element in svg.iter():
        tag = element.tag.split("}")[-1]
        assert tag in {"svg", "g", "path"}, f"Unsupported SVG element: {tag}"
        assert "transform" not in element.attrib
        if tag != "path":
            continue
        path = {attr("pathData"): element.attrib["d"], attr("fillColor"): "#00000000"}
        for a, b in {"stroke": "strokeColor", "stroke-width": "strokeWidth", "stroke-linecap": "strokeLineCap", "stroke-linejoin": "strokeLineJoin"}.items():
            if a in element.attrib:
                path[attr(b)] = "#FFFFFF" if element.attrib[a] == "white" else element.attrib[a]
        path.setdefault(attr("strokeWidth"), "1")
        ET.SubElement(vector, "path", path)
    ET.indent(vector)
    target = ROOT / "app/src/main/res/drawable" / (source.stem + ".xml")
    target.parent.mkdir(parents=True, exist_ok=True)
    ET.ElementTree(vector).write(target, encoding="utf-8", xml_declaration=True)
print("Converted original Figma SVG paths to Android vector resources.")
