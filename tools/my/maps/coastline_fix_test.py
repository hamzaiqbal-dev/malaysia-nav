import os
import tempfile
import unittest

import coastline_fix as cf


class PolyBboxTest(unittest.TestCase):
    def test_bbox_of_two_polys_with_margin(self):
        with tempfile.TemporaryDirectory() as d:
            a = os.path.join(d, "a.poly")
            b = os.path.join(d, "b.poly")
            with open(a, "w") as f:
                f.write("A\n1\n\t100.0\t1.0\n\t101.0\t2.0\nEND\nEND\n")
            with open(b, "w") as f:
                f.write("B\n1\n\t115.5\t4.0\n\t116.0\t7.5\nEND\n!2\n\t115.8\t5.0\nEND\nEND\n")
            self.assertEqual(cf.poly_bbox([a, b], margin=0.5), (99.5, 0.5, 116.5, 8.0))

    def test_bbox_clamped_to_world(self):
        with tempfile.TemporaryDirectory() as d:
            a = os.path.join(d, "a.poly")
            with open(a, "w") as f:
                f.write("A\n1\n\t179.9\t89.9\n\t-179.9\t-89.9\nEND\nEND\n")
            self.assertEqual(cf.poly_bbox([a], margin=1.0), (-180.0, -90.0, 180.0, 90.0))

    def test_bbox_without_points_fails(self):
        with tempfile.TemporaryDirectory() as d:
            a = os.path.join(d, "a.poly")
            with open(a, "w") as f:
                f.write("A\nEND\n")
            with self.assertRaises(ValueError):
                cf.poly_bbox([a])

    def test_real_borders(self):
        borders = os.path.join(os.path.dirname(__file__), "..", "..", "..", "data", "borders")
        polys = [os.path.join(borders, n + ".poly") for n in ("Malaysia_West", "Malaysia_East", "Singapore", "Brunei")]
        min_lon, min_lat, max_lon, max_lat = cf.poly_bbox(polys)
        # Peninsula starts near 99.6E, Sabah ends near 119.3E, the region spans roughly 0.8N to 7.4N.
        self.assertLess(min_lon, 99.6)
        self.assertGreater(max_lon, 119.3)
        self.assertLess(min_lat, 0.8)
        self.assertGreater(max_lat, 7.4)


class RingsTest(unittest.TestCase):
    def test_polygon_with_hole_and_multipolygon(self):
        square = [[0, 0], [1, 0], [1, 1], [0, 1], [0, 0]]
        hole = [[0.2, 0.2], [0.3, 0.2], [0.3, 0.3], [0.2, 0.2]]
        geojson = {
            "features": [
                {"geometry": {"type": "Polygon", "coordinates": [square, hole]}},
                {"geometry": {"type": "MultiPolygon", "coordinates": [[square], [square]]}},
                {"geometry": {"type": "LineString", "coordinates": square}},
                {"geometry": None},
            ]
        }
        rings = cf.rings_from_geojson(geojson)
        self.assertEqual(len(rings), 4)
        for r in rings:
            self.assertEqual(r[0], r[-1])

    def test_ring_is_closed_and_deduplicated(self):
        geojson = {"features": [{"geometry": {"type": "Polygon", "coordinates": [[[0, 0], [0, 0], [1, 0], [1, 1]]]}}]}
        self.assertEqual(cf.rings_from_geojson(geojson), [[(0, 0), (1, 0), (1, 1), (0, 0)]])

    def test_degenerate_ring_dropped(self):
        geojson = {"features": [{"geometry": {"type": "Polygon", "coordinates": [[[0, 0], [1, 0], [0, 0]]]}}]}
        self.assertEqual(cf.rings_from_geojson(geojson), [])


class SplitRingTest(unittest.TestCase):
    def test_small_ring_is_one_way(self):
        self.assertEqual(cf.split_ring([1, 2, 3, 1], max_nodes=4), [[1, 2, 3, 1]])

    def test_long_ring_is_chained(self):
        ring = list(range(1, 11)) + [1]
        parts = cf.split_ring(ring, max_nodes=4)
        self.assertEqual(parts, [[1, 2, 3, 4], [4, 5, 6, 7], [7, 8, 9, 10], [10, 1]])
        for part in parts:
            self.assertLessEqual(len(part), 4)
            self.assertGreaterEqual(len(part), 2)


class WriteChangesTest(unittest.TestCase):
    def test_untags_cut_ways_and_adds_rings(self):
        try:
            import osmium
        except ImportError:
            self.skipTest("pyosmium is not installed")

        with tempfile.TemporaryDirectory() as d:
            coast = os.path.join(d, "coast.opl")
            with open(coast, "w") as f:
                f.write("w7 v3 Tnatural=coastline,place=island,name=Pulau%20%Kecil Nn1,n2,n3\n")
            osc = os.path.join(d, "changes.osc")
            ring = [(100.0, 1.0), (101.0, 1.0), (101.0, 2.0), (100.0, 1.0)]
            self.assertEqual(cf.write_changes(coast, [ring], osc, 1000, 50), (1, 1))

            with open(osc, encoding="utf-8") as f:
                xml = f.read()
            self.assertIn("<modify>", xml)
            self.assertIn("<create>", xml)

            nodes, ways = {}, {}
            for o in osmium.FileProcessor(osc):
                if o.is_node():
                    nodes[o.id] = (o.location.lon, o.location.lat)
                elif o.is_way():
                    ways[o.id] = (o.version, dict(o.tags), [n.ref for n in o.nodes])

            self.assertEqual(ways[7], (4, {"place": "island", "name": "Pulau Kecil"}, [1, 2, 3]))
            self.assertEqual(ways[51], (1, {"natural": "coastline"}, [1001, 1002, 1003, 1001]))
            self.assertEqual(sorted(nodes), [1001, 1002, 1003])
            self.assertAlmostEqual(nodes[1002][0], 101.0)


if __name__ == "__main__":
    unittest.main()
