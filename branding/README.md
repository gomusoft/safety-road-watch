# Branding

Logo of Safety Road Watch: a shield with a road running to the horizon.
Chosen by a 1 to 5 poll (see the decision log in `docs/product.md`).

## Versions

| Version | Look |
|---|---|
| 0 | original: dark blue, green outline |
| **1** | **white shield on green (in use)** |
| 2 | sky and sun in the shield |
| 3 | record dot |
| 4 | small lens at the corner |
| 5 | small sparkle |
| 6 | yellow, high contrast |
| 7 | flat green shield |

## Files and when to use them

- `logo.svg`: a copy of the version in use. The README points to it, so it never needs editing when the version changes.
- `svg/logo-vN.svg`: the master. Editable, sharp at any size. Use for the README, web, print and as the source for everything else.
- `png/logo-vN-1024.png`: 1024 px export with rounded corners. Use for chat, social posts and any place that wants a picture file. Export other sizes from the SVG.
- `android/vN/`: Android vector drawables for the same version: `ic_launcher_background.xml`, `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml` (themed icon) and `ic_logo.xml` (the plain logo for screens).

The app needs no PNG launcher icons: `minSdk` is 26, which always supports adaptive icons.

## Changing the version in use

1. Set `ACTIVE` in `build.py`.
2. `python3 branding/build.py --apply` (Python 3, standard library only). It rewrites `branding/svg`, `branding/android` and copies the active files into `app/src/main/res`.
3. Re-export the PNGs from the SVGs if you changed a design (open the SVG in a browser or Inkscape and export 1024 px).

In the Android files gradients become flat colours (the launcher background keeps its gradient). Do not edit the generated XML by hand; change `build.py`.
