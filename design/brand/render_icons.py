#!/usr/bin/env python3
"""Render the Sampai launcher PNGs and store graphics from the Android vector drawables.

Run from the repo root after changing android/libs/branding/src/*/res/drawable/ic_launcher_*.xml:

    python3 design/brand/render_icons.py

Needs cairosvg and Pillow (pip install cairosvg pillow). The VectorDrawable subset used by the
icons (path fill/stroke, fillType, group translate) is converted to SVG and rasterised, so the
PNGs can never drift from the vectors.
"""

import io
import os
import re
import xml.etree.ElementTree as ET

import cairosvg
from PIL import Image, ImageDraw, ImageFont

ROOT = os.path.realpath(os.path.join(os.path.dirname(__file__), '..', '..'))
BRANDING = os.path.join(ROOT, 'android', 'libs', 'branding', 'src')
LISTING = os.path.join(ROOT, 'android', 'app', 'src', 'fdroid', 'play', 'listings', 'en-US', 'graphics')
OUT = os.path.dirname(os.path.abspath(__file__))

ANDROID = '{http://schemas.android.com/apk/res/android}'
DENSITIES = {'mdpi': 1, 'hdpi': 1.5, 'xhdpi': 2, 'xxhdpi': 3, 'xxxhdpi': 4}
NAVY = '#0B1220'
BETA_BG = '#FF7A45'


def vector_to_svg(path, background=None):
  """Convert a VectorDrawable (paths + translate groups) to an SVG string."""
  root = ET.parse(path).getroot()
  vw, vh = root.get(ANDROID + 'viewportWidth'), root.get(ANDROID + 'viewportHeight')
  out = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{vw}" height="{vh}" viewBox="0 0 {vw} {vh}">']
  if background:
    out.append(f'<rect width="{vw}" height="{vh}" fill="{background}"/>')

  def emit(node):
    for child in node:
      tag = child.tag
      if tag == 'group':
        tx, ty = child.get(ANDROID + 'translateX', '0'), child.get(ANDROID + 'translateY', '0')
        out.append(f'<g transform="translate({tx},{ty})">')
        emit(child)
        out.append('</g>')
      elif tag == 'path':
        attrs = {
          'd': child.get(ANDROID + 'pathData'),
          'fill': child.get(ANDROID + 'fillColor', 'none'),
          'stroke': child.get(ANDROID + 'strokeColor', 'none'),
          'stroke-width': child.get(ANDROID + 'strokeWidth', '0'),
          'stroke-linecap': child.get(ANDROID + 'strokeLineCap', 'butt'),
          'stroke-linejoin': child.get(ANDROID + 'strokeLineJoin', 'miter'),
          'fill-rule': 'evenodd' if child.get(ANDROID + 'fillType') == 'evenOdd' else 'nonzero',
        }
        out.append('<path ' + ' '.join(f'{k}="{v}"' for k, v in attrs.items()) + '/>')

  emit(root)
  out.append('</svg>')
  return '\n'.join(out)


def render(svg, size):
  png = cairosvg.svg2png(bytestring=svg.encode(), output_width=size, output_height=size)
  return Image.open(io.BytesIO(png)).convert('RGBA')


def legacy(full, size, round_mask):
  """Legacy (pre-O) launcher: the central 80/108 of the adaptive canvas under a rounded-square or circle mask."""
  big = full.size[0]
  inset = round(big * 14 / 108)
  crop = full.crop((inset, inset, big - inset, big - inset)).resize((size * 4, size * 4), Image.LANCZOS)
  mask = Image.new('L', crop.size, 0)
  draw = ImageDraw.Draw(mask)
  if round_mask:
    draw.ellipse((0, 0, crop.size[0] - 1, crop.size[1] - 1), fill=255)
  else:
    draw.rounded_rectangle((0, 0, crop.size[0] - 1, crop.size[1] - 1), radius=crop.size[0] * 0.17, fill=255)
  out = Image.new('RGBA', crop.size, (0, 0, 0, 0))
  out.paste(crop, (0, 0), mask)
  return out.resize((size, size), Image.LANCZOS)


def write_mipmaps(variant, full, with_round):
  for density, scale in DENSITIES.items():
    size = int(48 * scale)
    folder = os.path.join(BRANDING, variant, 'res', f'mipmap-{density}')
    os.makedirs(folder, exist_ok=True)
    legacy(full, size, False).save(os.path.join(folder, 'ic_launcher.png'), optimize=True)
    if with_round:
      legacy(full, size, True).save(os.path.join(folder, 'ic_launcher_round.png'), optimize=True)


def font(size):
  for candidate in ('/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf',
                    '/usr/share/fonts/truetype/liberation/LiberationSans-Bold.ttf'):
    if os.path.exists(candidate):
      return ImageFont.truetype(candidate, size)
  return ImageFont.load_default()


def feature_graphic(full):
  """Play Store feature graphic, 1024x500: icon left, wordmark and tagline right."""
  img = Image.new('RGBA', (1024, 500), NAVY)
  icon = legacy(full, 300, False)
  img.alpha_composite(icon, (90, 100))
  draw = ImageDraw.Draw(img)
  draw.text((400, 150), 'Sampai', font=font(120), fill='#F3F6FB')
  draw.text((406, 300), 'Navigasi Malaysia, luar talian.', font=font(32), fill='#9AA8C0')
  draw.rounded_rectangle((406, 360, 406 + 150, 360 + 8), radius=4, fill='#2E8BFF')
  draw.rounded_rectangle((406 + 160, 360, 406 + 210, 360 + 8), radius=4, fill='#FFC43D')
  return img.convert('RGB')


def main():
  main_svg = vector_to_svg(os.path.join(BRANDING, 'main', 'res', 'drawable', 'ic_launcher_foreground.xml'), NAVY)
  beta_svg = vector_to_svg(os.path.join(BRANDING, 'main', 'res', 'drawable', 'ic_launcher_foreground.xml'), BETA_BG)
  debug_svg = vector_to_svg(os.path.join(BRANDING, 'debug', 'res', 'drawable', 'ic_launcher_foreground.xml'))
  mono_svg = vector_to_svg(os.path.join(BRANDING, 'main', 'res', 'drawable', 'ic_launcher_monochrome.xml'), '#1A2740')

  big = 1728  # 16 x 108
  main_full, beta_full, debug_full = render(main_svg, big), render(beta_svg, big), render(debug_svg, big)

  write_mipmaps('main', main_full, True)
  write_mipmaps('beta', beta_full, True)
  write_mipmaps('debug', debug_full, True)

  # Store graphics (Gradle Play Publisher layout) and the Android Studio preview copy.
  os.makedirs(os.path.join(LISTING, 'icon'), exist_ok=True)
  os.makedirs(os.path.join(LISTING, 'feature-graphic'), exist_ok=True)
  store = main_full.resize((512, 512), Image.LANCZOS)
  store.save(os.path.join(LISTING, 'icon', '1.png'), optimize=True)
  store.save(os.path.join(ROOT, 'android', 'app', 'ic_launcher-playstore.png'), optimize=True)
  feature = feature_graphic(main_full)
  feature.save(os.path.join(LISTING, 'feature-graphic', '1.png'), optimize=True)

  # Previews for design/brand and the PR.
  store.save(os.path.join(OUT, 'sampai-icon-512.png'), optimize=True)
  feature.save(os.path.join(OUT, 'sampai-feature-graphic.png'), optimize=True)
  sheet = Image.new('RGBA', (5 * 220 + 20, 240), '#2A3650')
  tiles = [legacy(main_full, 192, False), legacy(main_full, 192, True), legacy(beta_full, 192, False),
           legacy(debug_full, 192, False), legacy(render(mono_svg, big), 192, True)]
  for i, tile in enumerate(tiles):
    sheet.alpha_composite(tile, (20 + i * 220, 24))
  sheet.convert('RGB').save(os.path.join(OUT, 'sampai-icon-variants.png'), optimize=True)


if __name__ == '__main__':
  main()
