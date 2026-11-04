"""Generates the Google Play graphics: the 512 px icon and the 1024 x 500
feature graphic (German and English). Needs ImageMagick (`magick`).
Run from the repository root:

    python tools/make_store_graphics.py
"""
import math
import subprocess
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent))
import make_logo  # noqa: E402  (the dial, without its dark tile)

OUT = Path('docs/play')
FONT = 'common/src/main/res/font/aleo.ttf'
CREAM, FOAM, FOAM_WHITE, AMBER, DARK_AMBER, INK = '#FDF6E3', '#F3E7C9', '#FFFCF2', '#F6B400', '#D98300', '#595540'
W, H, FOAM_Y = 1024, 500, 300


def magick(*args):
    subprocess.run(['magick', *args], check=True)


def glass_svg():
    """Cream glass, wavy foam, beer - the app's look, as a banner."""
    def edge(x, side, shift):
        return FOAM_Y + side * 14 + 6 * math.sin(x / 180 * 2 * math.pi + shift)
    top = [(x, edge(x, -1, 0)) for x in range(0, W + 1, 8)]
    bottom = [(x, edge(x, 1, 2.2)) for x in range(W, -1, -8)]
    foam = 'M' + ' L'.join(f'{x},{y:.1f}' for x, y in top + bottom) + ' Z'
    bubbles = ''.join(
        f'<circle cx="{x}" cy="{y}" r="{r}" fill="#FFFFFF" fill-opacity="0.35"/>'
        for x, y, r in [(90, 420, 5), (180, 360, 3), (300, 450, 6), (470, 380, 4), (560, 470, 3),
                        (690, 410, 5), (820, 350, 3), (930, 440, 6), (990, 380, 4)])
    return f'''<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}">
<defs><linearGradient id="beer" x1="0" y1="{FOAM_Y}" x2="0" y2="{H}" gradientUnits="userSpaceOnUse">
<stop offset="0" stop-color="{AMBER}"/><stop offset="1" stop-color="{DARK_AMBER}"/></linearGradient></defs>
<rect width="{W}" height="{FOAM_Y}" fill="{CREAM}"/>
<rect y="{FOAM_Y}" width="{W}" height="{H - FOAM_Y}" fill="url(#beer)"/>
{bubbles}
<path d="{foam}" fill="{FOAM_WHITE}"/>
</svg>'''


def feature(lang, digit, title, subtitle):
    base = OUT / '_glass.svg'
    base.write_text(glass_svg(), encoding='utf-8')
    logo_svg, logo_png = OUT / '_logo.svg', OUT / '_logo.png'
    logo_svg.write_text(make_logo.svg(digit, background=False), encoding='utf-8')
    # The dial is 62 of 108 units wide, so 560 px of SVG give a ~320 px clock.
    magick('-background', 'none', '-density', '400', str(logo_svg), '-resize', '560x560', str(logo_png))
    magick(str(base), str(logo_png), '-geometry', '-70-30', '-composite',
           '-font', FONT, '-fill', INK,
           '-pointsize', '46', '-annotate', '+430+170', title,
           '-pointsize', '26', '-annotate', '+433+220', subtitle,
           '-alpha', 'off', str(OUT / f'feature-graphic-{lang}.png'))
    for tmp in (base, logo_svg, logo_png):
        tmp.unlink()


if __name__ == '__main__':
    OUT.mkdir(parents=True, exist_ok=True)
    magick('-background', 'none', '-density', '400', 'docs/logo/logo-vier.svg', '-resize', '512x512',
           '-background', '#15120C', '-flatten', '-alpha', 'off', str(OUT / 'icon-512.png'))
    feature('de', 4, 'Irgendwo ist immer Vier', 'Wo auf der Welt gerade Bierzeit ist')
    feature('en', 5, "5 O'Clock Somewhere", 'Where in the world it is beer o’clock')
    print('ok')
