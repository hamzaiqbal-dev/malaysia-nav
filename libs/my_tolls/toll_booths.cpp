#include "my_tolls/toll_booths.hpp"

#include "indexer/classificator.hpp"
#include "indexer/data_source.hpp"
#include "indexer/feature.hpp"
#include "indexer/feature_data.hpp"

#include "geometry/mercator.hpp"
#include "geometry/parametrized_segment.hpp"
#include "geometry/rect2d.hpp"

#include "base/assert.hpp"

#include <algorithm>
#include <set>

namespace my_tolls
{
namespace
{
// Toll booths are drawn from zoom 12 (data/styles), so the geometry index has them at this scale.
int constexpr kTollBoothScale = 12;
// Querying the whole route's bounding box at once would read every feature of a long trip, so the
// polyline is cut into chunks of at most this many segments and each chunk's box is queried alone.
size_t constexpr kSegmentsPerChunk = 32;

m2::RectD ExpandedBoundingRect(std::vector<m2::PointD> const & polyline, size_t first, size_t last, double marginM)
{
  m2::RectD rect;
  for (size_t i = first; i <= last; ++i)
    rect.Add(polyline[i]);
  double const margin = mercator::MetersToMercator(marginM);
  rect.Inflate(margin, margin);
  return rect;
}
}  // namespace

Projection ProjectOnPolyline(std::vector<m2::PointD> const & polyline, m2::PointD const & point)
{
  CHECK_GREATER_OR_EQUAL(polyline.size(), 2, ());

  Projection best;
  best.m_distanceM = std::numeric_limits<double>::max();
  double alongM = 0.0;
  for (size_t i = 1; i < polyline.size(); ++i)
  {
    m2::ParametrizedSegment<m2::PointD> const segment(polyline[i - 1], polyline[i]);
    m2::PointD const closest = segment.ClosestPointTo(point);
    double const distanceM = mercator::DistanceOnEarth(closest, point);
    if (distanceM < best.m_distanceM)
    {
      best.m_distanceM = distanceM;
      best.m_alongM = alongM + mercator::DistanceOnEarth(polyline[i - 1], closest);
    }
    alongM += mercator::DistanceOnEarth(polyline[i - 1], polyline[i]);
  }
  return best;
}

std::vector<TollBooth> FindTollBoothsAlongRoute(DataSource const & dataSource, std::vector<m2::PointD> const & polyline,
                                                double maxDistanceM)
{
  std::vector<TollBooth> result;
  if (polyline.size() < 2)
    return result;

  static uint32_t const kTollBoothType = classif().GetTypeByPath({"barrier", "toll_booth"});

  std::set<FeatureID> seen;
  for (size_t first = 0; first + 1 < polyline.size(); first += kSegmentsPerChunk)
  {
    size_t const last = std::min(first + kSegmentsPerChunk, polyline.size() - 1);
    // Checking the distance against the whole polyline keeps a booth near a chunk boundary from
    // being measured against the wrong chunk.
    m2::RectD const rect = ExpandedBoundingRect(polyline, first, last, maxDistanceM);

    dataSource.ForEachInRect([&](FeatureType & ft)
    {
      if (ft.GetGeomType() != feature::GeomType::Point)
        return;
      if (!feature::TypesHolder(ft).Has(kTollBoothType))
        return;
      if (!seen.insert(ft.GetID()).second)
        return;

      std::string name(ft.GetReadableName());
      if (name.empty())
        return;

      m2::PointD const center = ft.GetCenter();
      Projection const projection = ProjectOnPolyline(polyline, center);
      if (projection.m_distanceM > maxDistanceM)
        return;

      result.push_back({std::move(name), mercator::ToLatLon(center), projection.m_alongM});
    }, rect, kTollBoothScale);
  }

  std::sort(result.begin(), result.end(), [](TollBooth const & lhs, TollBooth const & rhs)
  { return lhs.m_distanceAlongRouteM < rhs.m_distanceAlongRouteM; });
  return result;
}
}  // namespace my_tolls
