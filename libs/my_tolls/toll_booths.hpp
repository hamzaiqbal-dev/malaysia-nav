#pragma once

#include "geometry/latlon.hpp"
#include "geometry/point2d.hpp"

#include <cstddef>
#include <string>
#include <vector>

class DataSource;

// Malaysia Nav tolls: toll booths come from the map data (OpenStreetMap barrier=toll_booth), prices
// from data/my_tolls.csv matched by name on the app side.
namespace my_tolls
{
struct TollBooth
{
  std::string m_name;
  ms::LatLon m_latLon;
  // Distance from the route start to the booth's projection on the route, in meters.
  double m_distanceAlongRouteM = 0.0;
};

// Projection of |point| on |polyline| (both in mercator): the polyline length before the nearest
// point and the distance to it, both in meters. |polyline| must have at least two points.
struct Projection
{
  double m_alongM = 0.0;
  double m_distanceM = 0.0;
};
Projection ProjectOnPolyline(std::vector<m2::PointD> const & polyline, m2::PointD const & point);

// Named toll booths of the loaded maps lying within |maxDistanceM| of |polyline| (mercator), ordered
// from the route start. Unnamed booths are skipped: they cannot be priced. Safe to call from any
// thread; reads only the map data.
std::vector<TollBooth> FindTollBoothsAlongRoute(DataSource const & dataSource, std::vector<m2::PointD> const & polyline,
                                                double maxDistanceM);
}  // namespace my_tolls
