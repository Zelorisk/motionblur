# motionblur

a professional-grade motion blur mod for minecraft 1.21.1 (fabric). supports three blur types, full config gui, and refresh rate optimization.

made by chris

## features

- **type 1 - accumulation blur**: blends previous frames with the current one using weighted averaging. lightweight and retro-style.
- **type 2 - velocity blur**: per-pixel velocity from camera rotation delta. samples along the motion direction.
- **type 3 - enhanced velocity blur**: gaussian-weighted sampling along velocity vectors, similar to badlion/lunar v3. smoothest result.

## installation

1. install [fabric loader](https://fabricmc.net/) for 1.21.11
2. install [fabric api](https://modrinth.com/mod/fabric-api)
3. install [cloth config](https://modrinth.com/mod/cloth-config) (bundled in the jar)
4. optionally install [modmenu](https://modrinth.com/mod/modmenu) for gui access
5. drop the mod jar into your mods folder

## config

open the settings screen via modmenu or press the toggle keybind (unbound by default, set it in controls).

| setting | default | description |
|---|---|---|
| enabled | true | master toggle |
| blur type | 2 | 1 = accumulation, 2 = velocity, 3 = enhanced |
| intensity | 0.6 | blur strength (0.01–1.0) |
| sample count | 7 | quality/performance tradeoff (3–15) |
| refresh rate optimization | true | auto-adjusts for 60hz vs 120hz+ |
| camera only mode | false | blur only on camera rotation |
| auto-disable low fps | true | disables blur below fps threshold |
| fps threshold | 30 | fps floor before auto-disable |
| show hud | false | shows blur info in top-left corner |

## presets

- **performance**: type 1 · intensity 0.4 · samples 3
- **balanced**: type 2 · intensity 0.6 · samples 7
- **quality**: type 3 · intensity 0.7 · samples 11
- **ultra**: type 3 · intensity 0.8 · samples 15

## building

```
./gradlew build
```

output jar is in `build/libs/`.

## compatibility

- minecraft 1.21.11 (fabric)
- sodium: compatible (no render pipeline conflicts)
- iris/optifine shader packs: graceful degradation (blur disabled when shaders active)
- fast render mods: check console for compatibility warnings

## license

mit
