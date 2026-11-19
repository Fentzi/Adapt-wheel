# Wheel of Mahoraga (Fabric, Minecraft 1.21.11, client-side)

When any entity pops a Totem of Undying, a golden eight-spoked wheel flips in over its
head, turns one notch (45 degrees), and "ADAPTS" appears beneath it.

## Build
Requires JDK 21 and Gradle 9.2+ (Loom 1.14 needs Gradle 9).
    gradle wrapper --gradle-version 9.2.0
    ./gradlew build
The jar is in build/libs/ (use the one without "-sources"). Drop it in your mods folder
with Fabric API for 1.21.11.

## If the build complains
- Dependency version not found: edit gradle.properties with the current numbers from
  https://fabricmc.net/develop/
- `getCameraPos` not found: in AdaptsRenderer.cameraPos(), use camera.getPos().
- HudRenderCallback deprecated: harmless warning in current Fabric API.

## Tweaks (AdaptsRenderer.java)
DURATION (total ticks), the 0xFFD54A gold color, 0.9 height offset, "ADAPTS" text.
