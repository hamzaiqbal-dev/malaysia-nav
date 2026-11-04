#!/usr/bin/env python3
"""Walks an installed debug build through its main screens on an emulator, then runs monkey.

Needs a connected emulator and the app installed. Screenshots, UI dumps, logcat and report.md go to
the output directory. Exits with 1 when the app crashed. The device is kept offline: the point is an offline
first start, and CI must not download maps from a third-party server.
"""

import argparse
import re
import subprocess
import sys
import time
from pathlib import Path

BRAND = re.compile(r"organic\s*maps|organicmaps|omaps", re.I)
NODE = re.compile(r"<node [^>]*>")
ATTR = re.compile(r'([a-z-]+)="([^"]*)"')
BOUNDS = re.compile(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]")
DIALOG_BUTTONS = r"^(Accept|Agree|Continue|OK|Next|Done|Got it|Terima|Setuju|Teruskan|Seterusnya|Selesai|Allow.*)$"
UI_TIMEOUT_S = 40
MONKEY_ROUNDS = ((1234, 1500), (5678, 1500))
KUALA_LUMPUR = ("101.6869", "3.1390")


def adb(*args, check=False, timeout=UI_TIMEOUT_S):
    try:
        run = subprocess.run(["adb", *args], capture_output=True, text=True, timeout=timeout, check=check)
    except subprocess.TimeoutExpired:
        return ""
    return run.stdout


class Walker:
    def __init__(self, package, out):
        self.package = package
        self.out = out
        self.step = 0
        self.brand_hits = []

    def nodes(self):
        for _ in range(3):
            output = adb("shell", "uiautomator dump /sdcard/ui.xml >/dev/null && cat /sdcard/ui.xml")
            if "<hierarchy" in output:
                return output
            time.sleep(2)
        return ""

    def shot(self, name):
        self.step += 1
        stem = f"{self.step:02d}_{name}"
        with open(self.out / f"{stem}.png", "wb") as image:
            subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=image, timeout=UI_TIMEOUT_S, check=False)
        xml = self.nodes()
        (self.out / f"{stem}.xml").write_text(xml, encoding="utf-8")
        for node in NODE.findall(xml):
            attrs = dict(ATTR.findall(node))
            for text in (attrs.get("text", ""), attrs.get("content-desc", "")):
                if BRAND.search(text):
                    self.brand_hits.append(f"{stem}: {text[:120]}")
        print(f"step {stem}", flush=True)

    def tap(self, resource_id=None, text=None):
        """Taps the first node with this resource id suffix or text regex. False when the screen has none."""
        for node in NODE.findall(self.nodes()):
            attrs = dict(ATTR.findall(node))
            by_id = resource_id and attrs.get("resource-id", "").endswith(f":id/{resource_id}")
            by_text = text and re.search(text, attrs.get("text", "") or attrs.get("content-desc", ""), re.I)
            if by_id or by_text:
                x1, y1, x2, y2 = map(int, BOUNDS.search(attrs["bounds"]).groups())
                adb("shell", "input", "tap", str((x1 + x2) // 2), str((y1 + y2) // 2))
                time.sleep(3)
                return True
        return False

    def back(self):
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        time.sleep(2)

    def launch(self):
        adb("shell", "am", "force-stop", self.package)
        adb("shell", "monkey", "-p", self.package, "-c", "android.intent.category.LAUNCHER", "1")
        time.sleep(25)

    def get_past_first_start(self):
        for _ in range(8):
            self.shot("first_start")
            if not self.tap(text=DIALOG_BUTTONS):
                return

    def visit(self, name, resource_id=None, text=None, then=()):
        """Opens a screen from the map, photographs it and its [then] children, then comes back."""
        if not self.tap(resource_id, text):
            print(f"step {name}: not found", flush=True)
            return
        self.shot(name)
        for child_name, child_text in then:
            if self.tap(text=child_text):
                self.shot(child_name)
                self.back()
        self.back()

    def walk(self):
        self.launch()
        self.get_past_first_start()
        self.shot("map")
        self.visit("about", "help_button", then=(
            ("faq", r"FAQ|Soalan"),
            ("licences", r"licen[cs]e|lesen"),
        ))
        self.visit("search", "search_button")
        self.visit("bookmarks", "bookmarks_button")
        self.visit("layers", "layers_button")
        if self.tap("menu_button"):
            self.shot("menu")
            self.visit("settings", text=r"^(Settings|Tetapan)$")
        self.back()
        if self.tap("menu_button"):
            self.visit("downloader", text=r"Download maps|Muat turun|peta")
        self.back()
        self.long_press_map()

    def long_press_map(self):
        size = re.search(r"(\d+)x(\d+)", adb("shell", "wm", "size"))
        x, y = int(size[1]) // 2, int(size[2]) // 2
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "900")
        time.sleep(3)
        self.shot("place_page")
        self.back()


def crashes(package, log):
    marker = re.compile(rf"Process: {re.escape(package)}\b|>>> {re.escape(package)} <<<")
    return [line for line in log.splitlines() if marker.search(line)]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--locales", default="en,ms")
    args = parser.parse_args()

    packages = re.findall(r"package:(\S*sampai\S*)", adb("shell", "pm", "list", "packages"))
    if len(packages) != 1:
        sys.exit(f"Expected one Sampai package, found {packages}")
    package = packages[0]

    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    adb("emu", "geo", "fix", *KUALA_LUMPUR)
    adb("logcat", "-c")

    hits = []
    for locale in args.locales.split(","):
        out = args.out / locale
        out.mkdir(parents=True, exist_ok=True)
        adb("shell", "cmd", "locale", "set-app-locales", package, "--locales", locale)
        walker = Walker(package, out)
        walker.walk()
        hits += [f"{locale}/{hit}" for hit in walker.brand_hits]

    monkey_failed = False
    for seed, events in MONKEY_ROUNDS:
        run = subprocess.run(
            ["adb", "shell", "monkey", "-p", package, "-s", str(seed), "--throttle", "150", "--pct-syskeys", "0",
             "--pct-appswitch", "5", "-v", str(events)],
            capture_output=True, text=True, timeout=600, check=False)
        (args.out / f"monkey_{seed}.txt").write_text(run.stdout + run.stderr, encoding="utf-8")
        if "Monkey finished" not in run.stdout:
            monkey_failed = True

    crash_log = adb("logcat", "-d", "-b", "crash", timeout=120)
    full_log = adb("logcat", "-d", "-b", "all", timeout=120)
    (args.out / "crash.log").write_text(crash_log, encoding="utf-8")
    (args.out / "logcat.txt").write_text(full_log, encoding="utf-8")
    crash_lines = crashes(package, crash_log)
    anrs = [line for line in full_log.splitlines() if f"ANR in {package}" in line]

    report = [
        "# Android smoke test",
        f"- Package: {package}",
        f"- Crashes: {len(crash_lines)}",
        f"- ANRs: {len(anrs)}",
        f"- Monkey finished every round: {not monkey_failed}",
        f"- Old brand on screen: {len(hits)}",
        *[f"  - {hit}" for hit in sorted(set(hits))],
        *[f"  - {line}" for line in crash_lines[:20]],
    ]
    (args.out / "report.md").write_text("\n".join(report) + "\n", encoding="utf-8")
    print("\n".join(report))
    return 1 if crash_lines or monkey_failed else 0


if __name__ == "__main__":
    sys.exit(main())
