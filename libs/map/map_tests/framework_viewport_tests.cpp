#include "testing/testing.hpp"

#include "map/framework.hpp"

#include "geometry/any_rect2d.hpp"
#include "geometry/mercator.hpp"

#include "base/math.hpp"

namespace framework_viewport_tests
{
namespace
{
class TestFramework final : public Framework
{
public:
  TestFramework() : Framework({}, false /* loadMaps */) {}

  void SetViewport(ScreenBase const & screen, m2::RectD const & visibleViewport)
  {
    m_currentModelView = screen;
    m_visibleViewport = visibleViewport;
  }
};
}  // namespace

UNIT_TEST(Framework_GetViewportCenter)
{
  m2::RectD const pixelRect(0.0, 0.0, 1000.0, 1000.0);
  m2::RectD const visibleViewport(0.0, 0.0, 1000.0, 700.0);
  m2::PointD const target(185.0, 5.0);
  double constexpr kEps = 1e-7;

  TestFramework framework;
  for (bool const perspective : {false, true})
  {
    ScreenBase screen;
    screen.SetFromRects(m2::AnyRectD(m2::RectD(170.0, -10.0, 190.0, 10.0)), pixelRect);
    if (perspective)
      screen.ApplyPerspective(math::pi4, math::pi4, math::pi / 3.0);
    screen.MatchGandP3d(target, visibleViewport.Center());

    framework.SetViewport(screen, visibleViewport);
    auto const actual = framework.GetViewportCenter();
    TEST_ALMOST_EQUAL_ABS(actual.x, mercator::WrapX(target.x), kEps, (perspective));
    TEST_ALMOST_EQUAL_ABS(actual.y, target.y, kEps, (perspective));
  }
}
UNIT_TEST(Framework_FirstStartRectCoversMalaysia)
{
  m2::RectD const rect = Framework::GetFirstStartRect();
  TEST(rect.IsPointInside(mercator::FromLatLon(3.139, 101.687)), ("Kuala Lumpur"));
  TEST(rect.IsPointInside(mercator::FromLatLon(6.33, 99.73)), ("Langkawi"));
  TEST(rect.IsPointInside(mercator::FromLatLon(1.36, 103.95)), ("Johor, Desaru"));
  TEST(rect.IsPointInside(mercator::FromLatLon(1.55, 110.35)), ("Kuching"));
  TEST(rect.IsPointInside(mercator::FromLatLon(5.98, 116.07)), ("Kota Kinabalu"));
  TEST(rect.IsPointInside(mercator::FromLatLon(4.48, 118.61)), ("Semporna"));
  TEST(!rect.IsPointInside(mercator::FromLatLon(13.75, 100.5)), ("Bangkok"));
  TEST(!rect.IsPointInside(mercator::FromLatLon(-6.2, 106.8)), ("Jakarta"));
}
}  // namespace framework_viewport_tests
