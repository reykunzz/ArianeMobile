# Ariane Mobile architecture

Target: Android 10+; GTA San Andreas only. Both standalone editing and editing against a user-selected game folder are product modes.

## Modules
- Kotlin app: lifecycle, touch/UI, Android Storage Access Framework (SAF), permission persistence.
- JNI bridge: narrow API between Kotlin and native core.
- Native core: adapted Ariane editing model and pinned librw integration.
- GLES renderer: OpenGL ES 3 viewport; no desktop GLFW dependency.
- Formats: IPL/IDE, IMG, DFF/TXD, water data. Parsers should use stream interfaces rather than filesystem paths so SAF-backed documents can be read.

## Porting constraints
1. Do not copy desktop windowing/input code (GLFW) into Android.
2. Keep game-format parsing independent from UI and renderer.
3. First milestone is an Android build + blank GLES viewport, then text IPL parse/write tests.
4. Add librw only after pinning the exact upstream integration commit used by Ariane; audit desktop-only dependencies and replace platform services.
5. Never overwrite the user's game files directly by default. Export to a project/mod folder and keep backups.

## Current status
Bootstrap only. The original Ariane source and its pinned librw dependency are not vendored yet. The current IPL file is a placeholder, not a working parser.
