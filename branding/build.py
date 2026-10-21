#!/usr/bin/env python3
"""Builds every logo version from one table: SVG sources and Android vector drawables.

Run from the repo root:  python3 branding/build.py
Outputs: branding/svg/logo-vN.svg and branding/android/vN/*.xml (N = 0..7).
The app uses the version listed in ACTIVE; `--apply` copies its Android files into app/src/main/res
and its SVG to branding/logo.svg (the file the README points to).
Colours that are gradients in the SVG are flat approximations in the Android files (no aapt gradients,
to keep the build simple); the launcher background keeps the gradient as a shape drawable.
"""
import os
import shutil
import sys

ACTIVE = 1
ROOT = os.path.dirname(os.path.abspath(__file__))
RES = os.path.join(ROOT, "..", "app", "src", "main", "res")

SHIELD = "M50,10 L83,23 V49 C83,70 69,83 50,91 C31,83 17,70 17,49 V23 Z"
ROAD = "M45,44 L55,44 L92,96 L8,96 Z"
DASHES = [
    "M49,50 L51,50 L51.6,57 L48.4,57 Z",
    "M48.3,63 L51.7,63 L52.6,73 L47.4,73 Z",
    "M47,81 L53,81 L55,95 L45,95 Z",
]
TILE = "M24,0 H76 A24,24 0 0 1 100,24 V76 A24,24 0 0 1 76,100 H24 A24,24 0 0 1 0,76 V24 A24,24 0 0 1 24,0 Z"
SPARK = "M80,6 C81.5,15 84,17.5 93,19 C84,20.5 81.5,23 80,32 C78.5,23 76,20.5 67,19 C76,17.5 78.5,15 80,6 Z"


def circle(cx, cy, r):
    return f"M{cx - r},{cy} a{r},{r} 0 1,0 {2 * r},0 a{r},{r} 0 1,0 {-2 * r},0 Z"


# Gradients: (stops, svg x1 y1 x2 y2, android angle)
GRAD = {
    "gNight": (("#22364a", "#0b131b"), (0, 0, 1, 1), 315),
    "gGreen": (("#86e657", "#35a823"), (0, 0, 0, 1), 270),
    "gSun": (("#ffd84d", "#ff9f1c"), (0, 0, 0, 1), 270),
    "gSky": (("#6fc2ff", "#dff3ff"), (0, 0, 0, 1), 270),
}
FLAT = {"gGreen": "#4cbd2c", "gSky": "#a8dcff", "gSun": "#ffb020", "gNight": "#17222d"}


def spec(bg, sf, rf, df, sc=None, sw=0, sun=False, extra=(), scale=0.72, name=""):
    return dict(bg=bg, sf=sf, rf=rf, df=df, sc=sc, sw=sw, sun=sun, extra=extra, scale=scale, name=name)


ORIG = dict(sf="#34495f", rf="#111a22", df="#ffffff", sc="#86e657", sw=4)
DOT = ("svg", [("circle", 80, 22, 10, "gSun", "#0b131b", 3), ("circle", 80, 22, 3.5, "#ffffff", None, 0)])
LENS = ("svg", [("circle", 72, 72, 17, "#0b131b", "#ffffff", 3.5), ("circle", 72, 72, 9.5, "#3a64f0", None, 0),
                ("circle", 68.5, 68.5, 3, "#ffffff", None, 0)])
VERSIONS = {
    0: spec("gNight", name="original (shield road)", scale=0.72, **ORIG),
    1: spec("gGreen", "#ffffff", "#26394d", "#ffffff", name="white on green"),
    2: spec("gNight", "gSky", "#26394d", "#ffffff", "#86e657", 4, sun=True, name="sky and sun"),
    3: spec("gNight", extra=DOT[1], scale=0.64, name="record dot", **ORIG),
    4: spec("gNight", extra=LENS[1], scale=0.64, name="lens at the corner", **ORIG),
    5: spec("gNight", extra=[("path", SPARK, "#ffffff")], scale=0.64, name="small sparkle", **ORIG),
    6: spec("gSun", "#2b3f55", "#0b131b", "#ffd84d", "#ffffff", 4, name="yellow contrast"),
    7: spec("gNight", "gGreen", "#0b131b", "#ffffff", name="flat green"),
}


def svg_fill(c):
    return f"url(#{c})" if c in GRAD else c


def flat(c):
    return FLAT.get(c, c)


def svg_defs():
    out = []
    for k, (stops, (x1, y1, x2, y2), _) in GRAD.items():
        out.append(f'<linearGradient id="{k}" x1="{x1}" y1="{y1}" x2="{x2}" y2="{y2}">'
                   f'<stop offset="0" stop-color="{stops[0]}"/><stop offset="1" stop-color="{stops[1]}"/></linearGradient>')
    return "<defs>" + "".join(out) + "</defs>"


def extras_svg(v):
    out = []
    for e in v["extra"]:
        if e[0] == "circle":
            _, cx, cy, r, fill, stroke, sw = e
            st = f' stroke="{stroke}" stroke-width="{sw}"' if stroke else ""
            out.append(f'<circle cx="{cx}" cy="{cy}" r="{r}" fill="{svg_fill(fill)}"{st}/>')
        else:
            out.append(f'<path d="{e[1]}" fill="{e[2]}"/>')
    return "".join(out)


def shield_svg(v, cid):
    sun = '<circle cx="63" cy="33" r="7" fill="#ffb020"/>' if v["sun"] else ""
    road = f'<path d="{ROAD}" fill="{v["rf"]}"/>' + "".join(f'<path d="{d}" fill="{v["df"]}"/>' for d in DASHES)
    stroke = (f'<path d="{SHIELD}" fill="none" stroke="{v["sc"]}" stroke-width="{v["sw"]}" stroke-linejoin="round"/>'
              if v["sw"] else "")
    return (f'<clipPath id="{cid}"><path d="{SHIELD}"/></clipPath><path d="{SHIELD}" fill="{svg_fill(v["sf"])}"/>'
            f'<g clip-path="url(#{cid})">{sun}{road}</g>{stroke}{extras_svg(v)}')


def logo_svg(n, v):
    return ('<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="1024" height="1024">'
            f'<title>Safety Road Watch logo v{n} ({v["name"]})</title>{svg_defs()}'
            f'<path d="{TILE}" fill="{svg_fill(v["bg"])}"/>{shield_svg(v, f"c{n}")}</svg>\n')


# ---- Android ----
def a_path(d, fill=None, stroke=None, sw=0, even_odd=False):
    a = [f'android:pathData="{d}"']
    if fill:
        a.append(f'android:fillColor="{fill}"')
    if stroke:
        a.append(f'android:strokeColor="{stroke}"')
        a.append(f'android:strokeWidth="{sw}"')
        a.append('android:strokeLineJoin="round"')
    if even_odd:
        a.append('android:fillType="evenOdd"')
    return "<path " + " ".join(a) + "/>"


def extras_android(v):
    out = []
    for e in v["extra"]:
        if e[0] == "circle":
            _, cx, cy, r, fill, stroke, sw = e
            out.append(a_path(circle(cx, cy, r), flat(fill), stroke, sw))
        else:
            out.append(a_path(e[1], e[2]))
    return out


def content_android(v):
    items = [a_path(SHIELD, flat(v["sf"]))]
    clipped = []
    if v["sun"]:
        clipped.append(a_path(circle(63, 33, 7), "#ffb020"))
    clipped.append(a_path(ROAD, v["rf"]))
    clipped += [a_path(d, v["df"]) for d in DASHES]
    items.append('<group><clip-path android:pathData="' + SHIELD + '"/>' + "".join(clipped) + "</group>")
    if v["sw"]:
        items.append(a_path(SHIELD, None, v["sc"], v["sw"]))
    items += extras_android(v)
    return items


def vector(w, h, vw, vh, body, tint=None):
    t = f' android:tint="{tint}"' if tint else ""
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<vector xmlns:android="http://schemas.android.com/apk/res/android"\n'
            f'    android:width="{w}dp"\n    android:height="{h}dp"\n'
            f'    android:viewportWidth="{vw}"\n    android:viewportHeight="{vh}"{t}>\n'
            + "".join(f"    {line}\n" for line in body) + "</vector>\n")


def foreground(v):
    s = v["scale"]
    tx, ty = 54 - 50 * s, 54 - 50.5 * s
    body = [f'<group android:translateX="{tx:.2f}" android:translateY="{ty:.2f}" android:scaleX="{s}" android:scaleY="{s}">']
    body += content_android(v) + ["</group>"]
    return vector(108, 108, 108, 108, body)


def monochrome(v):
    # Themed icon: the shield with the road cut out (even-odd), the dashes filled again.
    s = 0.72
    tx, ty = 54 - 50 * s, 54 - 50.5 * s
    shape = "M0,0 H100 V100 H0 Z " + ROAD + " " + " ".join(DASHES)
    body = [f'<group android:translateX="{tx:.2f}" android:translateY="{ty:.2f}" android:scaleX="{s}" android:scaleY="{s}">',
            f'<group><clip-path android:pathData="{SHIELD}"/>' + a_path(shape, "#FFFFFFFF", even_odd=True) + "</group>",
            "</group>"]
    return vector(108, 108, 108, 108, body)


def background(v):
    c = v["bg"]
    if c in GRAD:
        (a, b), _, ang = GRAD[c]
        return ('<?xml version="1.0" encoding="utf-8"?>\n'
                '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
                f'    <gradient android:angle="{ang}" android:startColor="{a}" android:endColor="{b}" />\n</shape>\n')
    return ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<shape xmlns:android="http://schemas.android.com/apk/res/android" android:shape="rectangle">\n'
            f'    <solid android:color="{c}" />\n</shape>\n')


def in_app_logo(v):
    body = [a_path(TILE, flat(v["bg"]))] + content_android(v)
    return vector(96, 96, 100, 100, body)


LAUNCHER = ('<?xml version="1.0" encoding="utf-8"?>\n'
            '<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">\n'
            '    <background android:drawable="@drawable/ic_launcher_background" />\n'
            '    <foreground android:drawable="@drawable/ic_launcher_foreground" />\n'
            '    <monochrome android:drawable="@drawable/ic_launcher_monochrome" />\n'
            '</adaptive-icon>\n')


def write(path, text):
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8") as f:
        f.write(text)


def main():
    for n, v in VERSIONS.items():
        write(os.path.join(ROOT, "svg", f"logo-v{n}.svg"), logo_svg(n, v))
        d = os.path.join(ROOT, "android", f"v{n}")
        write(os.path.join(d, "ic_launcher_background.xml"), background(v))
        write(os.path.join(d, "ic_launcher_foreground.xml"), foreground(v))
        write(os.path.join(d, "ic_launcher_monochrome.xml"), monochrome(v))
        write(os.path.join(d, "ic_logo.xml"), in_app_logo(v))
    if "--apply" in sys.argv:
        src = os.path.join(ROOT, "android", f"v{ACTIVE}")
        for f in os.listdir(src):
            shutil.copy(os.path.join(src, f), os.path.join(RES, "drawable", f))
        write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher.xml"), LAUNCHER)
        write(os.path.join(RES, "mipmap-anydpi-v26", "ic_launcher_round.xml"), LAUNCHER)
        shutil.copy(os.path.join(ROOT, "svg", f"logo-v{ACTIVE}.svg"), os.path.join(ROOT, "logo.svg"))
        print(f"applied v{ACTIVE} to app resources and branding/logo.svg")
    print("built", len(VERSIONS), "versions")


if __name__ == "__main__":
    main()
