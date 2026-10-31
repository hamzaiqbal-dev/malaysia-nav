#include "testing/testing.hpp"

#include "my_tolls/toll_booths.hpp"

#include "geometry/mercator.hpp"

#include <vector>

namespace toll_booths_tests
{
using namespace my_tolls;

// LDP near Penchala, roughly south to north; distances below are from the mercator-based model.
std::vector<m2::PointD> MakePolyline()
{
  return {mercator::FromLatLon(3.1500, 101.6200), mercator::FromLatLon(3.1600, 101.6200),
          mercator::FromLatLon(3.1600, 101.6300)};
}

UNIT_TEST(ProjectOnPolyline_PointOnFirstSegment)
{
  auto const polyline = MakePolyline();
  auto const projection = ProjectOnPolyline(polyline, mercator::FromLatLon(3.1550, 101.6200));
  TEST_LESS(projection.m_distanceM, 1.0, (projection.m_distanceM));
  // Half of the first segment (0.01 degrees of latitude is about 1.1 km).
  TEST_ALMOST_EQUAL_ABS(projection.m_alongM, 553.0, 20.0, (projection.m_alongM));
}

UNIT_TEST(ProjectOnPolyline_PointBesideSecondSegment)
{
  auto const polyline = MakePolyline();
  // 0.0005 degrees of latitude (~55 m) north of the middle of the second segment.
  auto const projection = ProjectOnPolyline(polyline, mercator::FromLatLon(3.1605, 101.6250));
  TEST_ALMOST_EQUAL_ABS(projection.m_distanceM, 55.0, 5.0, (projection.m_distanceM));
  double const firstSegmentM = mercator::DistanceOnEarth(polyline[0], polyline[1]);
  double const secondSegmentM = mercator::DistanceOnEarth(polyline[1], polyline[2]);
  TEST_ALMOST_EQUAL_ABS(projection.m_alongM, firstSegmentM + secondSegmentM / 2, 5.0, (projection.m_alongM));
}

UNIT_TEST(ProjectOnPolyline_PointBeyondEndClampsToLastVertex)
{
  auto const polyline = MakePolyline();
  auto const projection = ProjectOnPolyline(polyline, mercator::FromLatLon(3.1600, 101.6400));
  double const lengthM =
      mercator::DistanceOnEarth(polyline[0], polyline[1]) + mercator::DistanceOnEarth(polyline[1], polyline[2]);
  TEST_ALMOST_EQUAL_ABS(projection.m_alongM, lengthM, 1.0, (projection.m_alongM));
  TEST_ALMOST_EQUAL_ABS(projection.m_distanceM,
                        mercator::DistanceOnEarth(polyline[2], mercator::FromLatLon(3.1600, 101.6400)), 1.0,
                        (projection.m_distanceM));
}
}  // namespace toll_booths_tests
