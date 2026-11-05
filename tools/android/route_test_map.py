#!/usr/bin/env python3
"""Writes a tiny made-up road network near Kuala Lumpur, for the emulator route test.

The roads are invented, so the test needs no map data from any server. One toll expressway runs east with two priced
booths and one unnamed booth, then leaves by a slip road with turn lanes:

  town -> Jalan Mula -> E23 [Damansara] [Pantai] [no name] -> slip road -> town

The router refuses to start or finish in a road island under 250 segments, hence the two street grids. Writes <out>/route.osm and <out>/borders/Malaysia_West.poly for generator_tool. START, FINISH, ROUTE and TOLL_TEXT are
what tools/android/route_smoke.py checks against.
"""

import argparse
from pathlib import Path
from xml.sax.saxutils import quoteattr

COUNTRY = "Malaysia_West"
LAT = 3.1
WEST, EAST, EXIT = 101.6, 101.71, 101.7
STEP = 0.002
BOOTHS = {101.624: {"name": "Plaza Tol Damansara"}, 101.648: {"name": "Plaza Tol Pantai"}, 101.672: {}}
TOLL_TEXT = "RM 4.50+"  # Damansara 2.00 + Pantai 2.50 in data/my_tolls.csv; + for the unnamed booth.

GRID_SIZE = 10
WEST_TOWN = (3.09, 101.578)  # South-west corners.
EAST_TOWN = (3.08, 101.704)
START = (3.1, 101.59)
FEEDER = (LAT, 101.596)
SLIP_END = (3.094, 101.704)
FINISH = (3.088, 101.71)
OFF_ROUTE = [(3.096, 101.708), (3.096, 101.714), (3.096, 101.72)]  # A street in the east town, 300 m from the route.
WALK_START = START
WALK_FINISH = (3.092, 101.59)  # Same town.
EXPRESSWAY_TAGS = {"highway": "motorway", "ref": "E23", "name": "Lebuhraya SPRINT", "maxspeed": "90", "lanes": "3",
                   "oneway": "yes", "toll": "yes"}


def expressway_points():
    count = round((EAST - WEST) / STEP)
    return [(LAT, round(WEST + i * STEP, 6)) for i in range(count + 1)]


# The way the car drives, for the fake GPS.
ROUTE = [START, (LAT, 101.592), (LAT, 101.594), FEEDER, *[p for p in expressway_points() if p[1] <= EXIT], SLIP_END,
         (3.092, 101.704), (3.09, 101.704), (3.088, 101.704), (3.088, 101.706), (3.088, 101.708), FINISH]


class Osm:
    def __init__(self):
        self.nodes = []
        self.ways = []
        self.node_ids = {}

    def node(self, point, tags=None):
        if point not in self.node_ids:
            self.node_ids[point] = len(self.node_ids) + 1
        node_id = self.node_ids[point]
        self.nodes.append((node_id, point, tags or {}))
        return node_id

    def way(self, points, tags):
        self.ways.append(([self.node(p) for p in points], tags))

    def xml(self):
        rows = ['<?xml version="1.0" encoding="UTF-8"?>', '<osm version="0.6">']
        seen = {}
        for node_id, point, tags in self.nodes:
            seen.setdefault(node_id, (point, {}))[1].update(tags)
        for node_id, (point, tags) in sorted(seen.items()):
            rows.append(f'<node id="{node_id}" version="1" lat="{point[0]}" lon="{point[1]}">')
            rows += self.tags(tags)
            rows.append("</node>")
        for way_id, (node_ids, tags) in enumerate(self.ways, start=1):
            rows.append(f'<way id="{way_id}" version="1">')
            rows += [f'<nd ref="{node_id}"/>' for node_id in node_ids]
            rows += self.tags(tags)
            rows.append("</way>")
        rows.append("</osm>")
        return "\n".join(rows) + "\n"

    @staticmethod
    def tags(tags):
        return [f"<tag k={quoteattr(k)} v={quoteattr(v)}/>" for k, v in tags.items()]


def add_town(osm, corner, name):
    def point(row, column):
        return (round(corner[0] + row * STEP, 6), round(corner[1] + column * STEP, 6))

    for i in range(GRID_SIZE):
        osm.way([point(i, j) for j in range(GRID_SIZE)], {"highway": "residential", "name": f"Jalan {name} {i + 1}"})
        osm.way([point(j, i) for j in range(GRID_SIZE)], {"highway": "residential", "name": f"Lorong {name} {i + 1}"})


def network():
    osm = Osm()
    points = expressway_points()
    for point in points:
        if point[1] in BOOTHS:
            osm.node(point, {"barrier": "toll_booth", **BOOTHS[point[1]]})
    before_exit = [p for p in points if p[1] <= EXIT]
    osm.way(before_exit[:-2], EXPRESSWAY_TAGS)
    osm.way(before_exit[-3:], {**EXPRESSWAY_TAGS, "turn:lanes": "through|through|right", "destination": "Bangsar"})
    osm.way([p for p in points if p[1] >= EXIT], EXPRESSWAY_TAGS)
    osm.way([(LAT, EXIT), SLIP_END], {"highway": "motorway_link", "oneway": "yes", "destination": "Bangsar"})
    osm.way([FEEDER, (LAT, WEST)], {"highway": "tertiary", "name": "Jalan Mula"})
    add_town(osm, WEST_TOWN, "Mula")
    add_town(osm, EAST_TOWN, "Tujuan")
    return osm


def border():
    south, west, north, east = 2.9, 101.4, 3.3, 101.9
    corners = [(west, south), (east, south), (east, north), (west, north), (west, south)]
    return "\n".join([COUNTRY, "1", *[f"  {lon} {lat}" for lon, lat in corners], "END", "END", ""])


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--out", type=Path, required=True)
    out = parser.parse_args().out
    (out / "borders").mkdir(parents=True, exist_ok=True)
    (out / "route.osm").write_text(network().xml(), encoding="utf-8")
    (out / "borders" / f"{COUNTRY}.poly").write_text(border(), encoding="utf-8")


if __name__ == "__main__":
    main()
