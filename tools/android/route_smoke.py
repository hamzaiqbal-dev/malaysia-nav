#!/usr/bin/env python3
"""Drives a built route on an emulator: route preview, navigation HUD, tolls, rotation, arrival.

Needs a connected emulator, the app installed, and the map from route_test_map.py built by generator_tool. The GPS is
faked along the made-up road. Exits with 1 when the app crashed, the toll total is wrong or stale after a reroute, a
walking route shows a toll, or the speed covers a map button. The second language runs in night mode. Offline like
smoke_test.py.
"""

import argparse
import re
import sys
import time
from pathlib import Path

from route_test_map import COUNTRY, FINISH, OFF_ROUTE, ROUTE, SLIP_END, START, TOLL_TEXT, WALK_FINISH, WALK_START
from smoke_test import ATTR, BOUNDS, DIALOG_BUTTONS, NODE, Walker, adb, crashes

# Older than "v" in data/countries.json: a map of the current version must have the published size, and Debug aborts.
MAPS_VERSION = "260101"
KNOTS_90_KMH = "49"
DRIVE_STEP_S = 2
SHOT_EVERY_POINTS = 12
REROUTE_FIXES = 8
REROUTE_STEP_DEG = 0.0001  # About 11 m, so the last fix stays on the street.


def text_of(xml, resource_id):
    for node in NODE.findall(xml):
        attrs = dict(ATTR.findall(node))
        if attrs.get("resource-id", "").endswith(f":id/{resource_id}"):
            return attrs.get("text", "")
    return None


def bounds_of(xml, resource_id):
    for node in NODE.findall(xml):
        attrs = dict(ATTR.findall(node))
        if attrs.get("resource-id", "").endswith(f":id/{resource_id}"):
            return tuple(map(int, BOUNDS.search(attrs["bounds"]).groups()))
    return None


def overlap(xml, first, second):
    a, b = bounds_of(xml, first), bounds_of(xml, second)
    return bool(a and b and a[0] < b[2] and b[0] < a[2] and a[1] < b[3] and b[1] < a[3])


def gps(point):
    adb("emu", "geo", "fix", str(point[1]), str(point[0]), "30", "8", KNOTS_90_KMH)


def install_map(package, mwm):
    folder = f"/storage/emulated/0/Android/data/{package}/files/{MAPS_VERSION}"
    adb("shell", "mkdir", "-p", folder)
    print(adb("push", str(mwm), f"{folder}/{COUNTRY}.mwm", timeout=120), flush=True)


class Driver(Walker):
    def __init__(self, package, out):
        super().__init__(package, out)
        self.overlaps = []

    def read(self, name, *ids):
        self.shot(name)
        xml = (self.out / f"{self.step:02d}_{name}.xml").read_text(encoding="utf-8")
        if overlap(xml, "my_speed_pill", "btn_bookmarks") or overlap(xml, "my_speed_pill", "btn_search"):
            self.overlaps.append(name)
        return {resource_id: text_of(xml, resource_id) for resource_id in ids}

    def build_route(self, start=START, finish=FINISH, router="vehicle"):
        url = (f"om://route?sll={start[0]},{start[1]}&saddr=Mula"
               f"&dll={finish[0]},{finish[1]}&daddr=Tiba&type={router}")
        adb("shell", f"am start -a android.intent.action.VIEW -d '{url}' {self.package}")
        time.sleep(15)
        self.tap(text=DIALOG_BUTTONS)

    def drive(self, seen):
        for index, point in enumerate(ROUTE):
            gps(point)
            time.sleep(DRIVE_STEP_S)
            if index and index % SHOT_EVERY_POINTS == 0:
                seen.append(self.read(f"drive_{index}", "my_toll_amount", "my_eta", "my_turn_street"))

    def start_navigation(self):
        # The first tap offers to start from the real position and rebuilds the route.
        for _ in range(3):
            if not self.tap("start"):
                break
            self.tap(text=DIALOG_BUTTONS)
            time.sleep(8)

    def stop_navigation(self):
        self.tap("my_end")
        self.tap(text=DIALOG_BUTTONS)

    def reroute(self):
        """Leaves the route after the booths: the rebuilt route has no toll, so the chip must go."""
        gps(START)
        self.build_route()
        self.start_navigation()
        before = self.read("before_reroute", "my_toll_amount", "my_eta")
        for point in [SLIP_END, *OFF_ROUTE]:
            gps(point)
            time.sleep(DRIVE_STEP_S)
        for fix in range(REROUTE_FIXES):
            # The core ignores a fix at the same distance from the route, so keep moving along the street.
            gps((OFF_ROUTE[-1][0], OFF_ROUTE[-1][1] + fix * REROUTE_STEP_DEG))
            time.sleep(DRIVE_STEP_S)
        after = self.read("after_reroute", "my_toll_amount", "my_eta")
        self.stop_navigation()
        return before, after

    def walk(self):
        """A walking route: the HUD must come up with no toll chip."""
        gps(WALK_START)
        self.build_route(WALK_START, WALK_FINISH, "pedestrian")
        preview = self.read("walk_preview", "my_toll_amount", "time")
        self.start_navigation()
        hud = self.read("walk_hud", "my_toll_amount", "my_eta", "my_turn_street")
        self.stop_navigation()
        return preview, hud

    def run(self):
        seen = []
        self.launch()
        self.get_past_first_start()
        gps(START)
        self.build_route()
        seen.append(self.read("route_preview", "time_vehicle"))
        self.start_navigation()
        self.shot("navigation")
        seen.append(self.read("navigation_hud", "my_toll_amount", "my_eta", "my_turn_street"))
        adb("shell", "wm", "user-rotation", "lock", "1")
        time.sleep(4)
        seen.append(self.read("navigation_landscape", "my_toll_amount", "my_eta"))
        adb("shell", "wm", "user-rotation", "lock", "0")
        time.sleep(4)
        self.drive(seen)
        time.sleep(4)
        self.shot("arrived")
        self.stop_navigation()
        self.shot("after_stop")
        return seen


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, required=True)
    parser.add_argument("--map", type=Path, required=True)
    parser.add_argument("--locales", default="en,ms")
    args = parser.parse_args()

    packages = re.findall(r"package:(\S*sampai\S*)", adb("shell", "pm", "list", "packages"))
    if len(packages) != 1:
        sys.exit(f"Expected one Sampai package, found {packages}")
    package = packages[0]

    adb("shell", "cmd", "connectivity", "airplane-mode", "enable")
    adb("shell", "settings", "put", "secure", "location_mode", "3")
    adb("logcat", "-G", "16M")
    adb("logcat", "-c")
    args.out.mkdir(parents=True, exist_ok=True)
    first = Walker(package, args.out)
    first.launch()
    first.get_past_first_start()
    install_map(package, args.map)
    adb("logcat", "-c", "-b", "crash")

    results = []
    overlaps = []
    first_locale = args.locales.split(",")[0]
    for locale in args.locales.split(","):
        out = args.out / locale
        out.mkdir(parents=True, exist_ok=True)
        adb("shell", "cmd", "locale", "set-app-locales", package, "--locales", locale)
        adb("shell", "cmd", "uimode", "night", "yes" if locale != first_locale else "no")
        driver = Driver(package, out)
        results.append((locale, driver.run()))
        if locale == first_locale:
            rerouted = driver.reroute()
            walked = driver.walk()
        overlaps += [f"{locale}/{name}" for name in driver.overlaps]

    crash_log = adb("logcat", "-d", "-b", "crash", timeout=120)
    full_log = adb("logcat", "-d", "-b", "all", timeout=120)
    (args.out / "crash.log").write_text(crash_log, encoding="utf-8")
    (args.out / "logcat.txt").write_text(full_log, encoding="utf-8")
    crash_lines = crashes(package, crash_log)
    anrs = [line for line in full_log.splitlines() if f"ANR in {package}" in line]

    tolls = [step.get("my_toll_amount") for _, steps in results for step in steps]
    previews = [steps[0]["time_vehicle"] or "" for _, steps in results]
    toll_ok = TOLL_TEXT in tolls and all(TOLL_TEXT in preview for preview in previews)
    reroute_ok = rerouted[0]["my_toll_amount"] == TOLL_TEXT and rerouted[1]["my_toll_amount"] is None
    walk_ok = bool(walked[1]["my_eta"]) and not any(step["my_toll_amount"] for step in walked)
    report = ["# Android route test", f"- Crashes: {len(crash_lines)}", f"- ANRs: {len(anrs)}",
              f"- Expected toll {TOLL_TEXT} shown: {toll_ok}", f"- Speed over map buttons: {overlaps}",
              f"- Toll follows a reroute: {reroute_ok} {rerouted}",
              f"- Walking route has HUD, no toll: {walk_ok} {walked}"]
    for locale, steps in results:
        report += [f"- {locale}:", *[f"  - {step}" for step in steps]]
    report += [f"  - {line}" for line in crash_lines[:20]]
    (args.out / "report.md").write_text("\n".join(report) + "\n", encoding="utf-8")
    print("\n".join(report))
    return 1 if crash_lines or overlaps or not (toll_ok and reroute_ok and walk_ok) else 0


if __name__ == "__main__":
    sys.exit(main())
