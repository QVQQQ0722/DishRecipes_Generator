"""Capture the three Figma screens on an already connected, unlocked Android phone."""
from pathlib import Path
import re
import subprocess
import time
import xml.etree.ElementTree as ET

ADB = str(Path.home() / "Android/Sdk/platform-tools/adb.exe")
OUT = Path(__file__).resolve().parents[1] / "artifacts/figma"
OUT.mkdir(parents=True, exist_ok=True)

def adb(*args):
    return subprocess.check_output([ADB, *args])

def node_for(label):
    for _ in range(5):
        adb("shell", "uiautomator", "dump", "/sdcard/recipe-ui.xml")
        root = ET.fromstring(adb("shell", "cat", "/sdcard/recipe-ui.xml"))
        for node in root.iter("node"):
            if label in (node.get("text"), node.get("content-desc")):
                return node
        time.sleep(1)
    raise RuntimeError(f"App node not found: {label}")

def tap(label):
    node = node_for(label)
    left, top, right, bottom = map(int, re.findall(r"\d+", node.attrib["bounds"]))
    adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))

def capture(name, label):
    node_for(label)
    time.sleep(.4)
    (OUT / f"{name}.png").write_bytes(adb("exec-out", "screencap", "-p"))
    print(f"Captured {name}", flush=True)

capture("figma-home", "我的菜谱")
tap("番茄罗勒意面")
capture("figma-detail", "食材 Ingredients")
tap("返回主菜单")
tap("增加菜谱")
capture("figma-add", "自己输入")
tap("返回主菜单")
