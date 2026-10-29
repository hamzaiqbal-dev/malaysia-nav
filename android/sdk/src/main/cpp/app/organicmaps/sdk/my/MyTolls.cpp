#include <jni.h>
#include "app/organicmaps/sdk/Framework.hpp"
#include "app/organicmaps/sdk/core/jni_helper.hpp"

#include "my_tolls/toll_booths.hpp"

#include "geometry/mercator.hpp"

#include <vector>

extern "C"
{
// Route geometry as [lat0, lon0, lat1, lon1, ...] or null without a valid route. UI thread only:
// the routing session is thread-checked.
JNIEXPORT jdoubleArray JNICALL Java_app_organicmaps_sdk_my_MyTolls_nativeGetRoutePolyline(JNIEnv * env, jclass)
{
  auto const & session = frm()->GetRoutingManager().RoutingSession();
  if (!session.IsRouteValid())
    return nullptr;

  auto const & points = session.GetRoute()->GetPoly().GetPoints();
  std::vector<jdouble> latLons;
  latLons.reserve(points.size() * 2);
  for (auto const & point : points)
  {
    auto const ll = mercator::ToLatLon(point);
    latLons.push_back(ll.m_lat);
    latLons.push_back(ll.m_lon);
  }

  jsize const size = static_cast<jsize>(latLons.size());
  jdoubleArray const result = env->NewDoubleArray(size);
  env->SetDoubleArrayRegion(result, 0, size, latLons.data());
  return result;
}

// Named toll booths of the loaded maps within |maxDistanceM| of the polyline [lat0, lon0, ...].
// Reads map data only, so it may run on a background thread.
JNIEXPORT jobjectArray JNICALL Java_app_organicmaps_sdk_my_MyTolls_nativeFindTollBooths(JNIEnv * env, jclass,
                                                                                        jdoubleArray latLons,
                                                                                        jdouble maxDistanceM)
{
  jsize const size = env->GetArrayLength(latLons);
  std::vector<jdouble> values(static_cast<size_t>(size));
  env->GetDoubleArrayRegion(latLons, 0, size, values.data());

  std::vector<m2::PointD> polyline;
  polyline.reserve(values.size() / 2);
  for (size_t i = 0; i + 1 < values.size(); i += 2)
    polyline.push_back(mercator::FromLatLon(values[i], values[i + 1]));

  auto const booths = my_tolls::FindTollBoothsAlongRoute(frm()->GetDataSource(), polyline, maxDistanceM);

  static jclass const clazz = jni::GetGlobalClassRef(env, "app/organicmaps/sdk/my/TollBooth");
  // Java signature : TollBooth(String name, double lat, double lon, double distanceAlongRouteM)
  static jmethodID const ctor = jni::GetConstructorID(env, clazz, "(Ljava/lang/String;DDD)V");
  return jni::ToJavaArray(env, clazz, booths, [](JNIEnv * env, my_tolls::TollBooth const & booth)
  {
    jni::TScopedLocalRef const name(env, jni::ToJavaString(env, booth.m_name));
    return env->NewObject(clazz, ctor, name.get(), booth.m_latLon.m_lat, booth.m_latLon.m_lon,
                          booth.m_distanceAlongRouteM);
  });
}
}  // extern "C"
