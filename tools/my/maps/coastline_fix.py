"""Make a regional OSM extract usable for coastline generation.

The generator needs closed natural=coastline rings, which a regional extract
(e.g. Geofabrik's Malaysia, Singapore and Brunei) does not have: the coastline
of Borneo and the Malay Peninsula is cut at the extract border. This script
replaces the cut coastline with closed rings taken from the OSM land polygons
(osmdata.openstreetmap.de, ODbL) clipped to the region.

Subcommands:
  bbox POLY...                  print "minlon minlat maxlon maxlat" (+ margin)
  changes COAST_WAYS GEOJSON OUT_OSC MAX_NODE_ID MAX_WAY_ID
                                write an osmChange that drops natural=coastline
                                from the existing ways and adds closed rings
"""

import json
import sys

kMarginDeg = 0.5
kCoastTag = ("natural", "coastline")


def poly_bbox(poly_paths, margin=kMarginDeg):
    """Bounding box of Osmosis .poly files, grown by margin degrees."""
    min_lon = min_lat = float("inf")
    max_lon = max_lat = float("-inf")
    for path in poly_paths:
        with open(path, encoding="utf-8") as f:
            for line in f:
                parts = line.split()
                if len(parts) != 2:
                    continue
                try:
                    lon, lat = float(parts[0]), float(parts[1])
                except ValueError:
                    continue
                min_lon, max_lon = min(min_lon, lon), max(max_lon, lon)
                min_lat, max_lat = min(min_lat, lat), max(max_lat, lat)
    if min_lon == float("inf"):
        raise ValueError(f"No points in {poly_paths}")
    return (
        max(min_lon - margin, -180.0),
        max(min_lat - margin, -90.0),
        min(max_lon + margin, 180.0),
        min(max_lat + margin, 90.0),
    )


def _clean_ring(coords):
    ring = []
    for c in coords:
        p = (round(c[0], 7), round(c[1], 7))
        if not ring or ring[-1] != p:
            ring.append(p)
    if len(ring) > 1 and ring[0] != ring[-1]:
        ring.append(ring[0])
    # A closed ring needs at least 3 distinct points.
    return ring if len(ring) >= 4 else None


def rings_from_geojson(geojson):
    """All closed rings (outer and inner) of Polygon/MultiPolygon features."""
    rings = []
    for feature in geojson.get("features", []):
        geom = feature.get("geometry") or {}
        if geom.get("type") == "Polygon":
            polygons = [geom["coordinates"]]
        elif geom.get("type") == "MultiPolygon":
            polygons = geom["coordinates"]
        else:
            continue
        for polygon in polygons:
            for coords in polygon:
                ring = _clean_ring(coords)
                if ring:
                    rings.append(ring)
    return rings


def write_changes(coast_ways_path, rings, out_path, max_node_id, max_way_id):
    """Writes an osmChange file; libosmium writes version 1 as create, others as modify."""
    import osmium

    writer = osmium.SimpleWriter(out_path)
    try:
        modified = 0
        for way in osmium.FileProcessor(coast_ways_path, osmium.osm.WAY):
            tags = {t.k: t.v for t in way.tags if (t.k, t.v) != kCoastTag}
            nodes = [n.ref for n in way.nodes]
            writer.add_way(way.replace(tags=tags, nodes=nodes, version=way.version + 1, visible=True))
            modified += 1

        node_id = max_node_id
        way_id = max_way_id
        for ring in rings:
            refs = []
            for lon, lat in ring[:-1]:
                node_id += 1
                writer.add_node(osmium.osm.mutable.Node(id=node_id, version=1, visible=True, location=(lon, lat)))
                refs.append(node_id)
            refs.append(refs[0])
            way_id += 1
            writer.add_way(osmium.osm.mutable.Way(id=way_id, version=1, visible=True, nodes=refs, tags=dict([kCoastTag])))
    finally:
        writer.close()
    return modified, len(rings)


def main(argv):
    if len(argv) >= 2 and argv[0] == "bbox":
        print(*(f"{v:.4f}" for v in poly_bbox(argv[1:])))
        return 0
    if len(argv) == 6 and argv[0] == "changes":
        with open(argv[2], encoding="utf-8") as f:
            rings = rings_from_geojson(json.load(f))
        if not rings:
            print("No land polygons in the region", file=sys.stderr)
            return 1
        modified, added = write_changes(argv[1], rings, argv[3], int(argv[4]), int(argv[5]))
        print(f"Coastline: {modified} cut ways untagged, {added} closed rings added")
        return 0
    print(__doc__, file=sys.stderr)
    return 2


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
