<p align="center">
  <img src="assets/pixelpainter-icon.svg" width="128" height="128" alt="PixelPainter icon">
</p>

<h1 align="center">PixelPainter</h1>

<p align="center">Turn PNG, JPEG and WebP images into Minecraft block art with perceptual color matching and persistent undo.</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-25-007396.svg" alt="Java 25">
  <img src="https://img.shields.io/badge/Gradle-9.8.2-079ec0?logo=gradle&amp;logoColor=white" alt="Gradle 9.8.2">
  <img src="https://img.shields.io/badge/Paper-1.21.11%E2%80%9326.3-62b47a" alt="Paper 1.21.11 to 26.3">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-GPLv3-blue.svg" alt="GPLv3 license"></a>
</p>

<p align="center"><a href="#installation">Installation</a> · <a href="#compatibility">Compatibility</a> · <a href="#commands">Commands</a> · <a href="#configuration-and-upgrading">Configuration</a> · <a href="https://docs.despical.dev/pixel-painter/">Documentation</a> · <a href="https://javadoc.despical.dev/pixel-painter/">Javadocs</a> · <a href="https://github.com/Despical/PixelPainter/issues">Issues</a></p>

---

## Features

- PNG, JPEG and WebP sources painted as vertical walls or horizontal floor art in eight directions.
- 305 opaque block materials, Oklab perceptual color matching and optional Floyd-Steinberg dithering.
- Exact block-height sizing, aspect-ratio preservation and transparent-pixel handling.
- Tick-limited world edits with private progress, cancellation and bounded image workers.
- Durable undo journals, original block properties and restart/partial-operation recovery.
- Configurable MiniMessage panels, material filters and trusted HTTPS source downloads.

## Requirements

- **Java 25 or newer**, including when the server runs Minecraft 1.21.11.
- **Paper 1.21.11 through 26.3 inclusive.**
- Minecraft Java Edition clients that can join the server's protocol version.

## Compatibility

PixelPainter 2.0 compiles against the Paper 1.21.11 API and declares that minimum `api-version`. It uses current Paper APIs such as Adventure components, the plugin lifecycle and modern block data, so older servers cannot load it.

| Server software and version | Status |
| --- | --- |
| Paper 1.21.11 through 26.3 | Supported. Started and smoke-tested on Paper 1.21.11 and 26.3. |
| Paper 1.21.10 and earlier | Not supported. Use PixelPainter 1.x. |
| Spigot, CraftBukkit and Folia | Not supported. |
| Minecraft 1.8 through 1.12 | Not supported; these versions lack the block data the plugin relies on. |

Block materials that do not exist on the running server version are skipped, so the same `palette.yml` works across the supported range.

## Installation

1. Build with `./gradlew clean build` or `gradlew.bat clean build`.
2. Place `build/libs/pixel-painter-2.0.0.jar` in the server's `plugins` directory and restart.
3. Put PNG, JPEG or WebP files in `plugins/PixelPainter/images`, or use the download command with a trusted host configured in `config.yml`.

All permissions default to operators. Grant individual `pp.*` children to trusted builders as appropriate. World operations run with those administrative permissions; this plugin does not integrate with region protection or survival inventories.

## Quick start

```text
/pp list
/pp preview portrait.png 64 all true true
/pp create portrait.png north 64 true all true
/pp history
/pp undo
```

Stand at the desired **bottom-left block** of your image. Height is the exact number of block rows; width is calculated from the original aspect ratio. The example creates a 64-block-high wall, preserves transparent pixels, uses the complete palette and enables dithering. The estimate uses the same settings but never changes the world.

