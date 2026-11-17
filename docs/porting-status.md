# Porting status — GTA SA Classic / ARM64

## Included
- Android Gradle app restricted to `arm64-v8a`.
- Kotlin SAF folder picker, JNI bridge and GLES3 viewport shell.
- Starter parser for text IPL `inst` records.
- Starter parser for IMG v2 directory records.
- GitHub Actions debug APK workflow and Termux Git push scripts.

## Pending upstream integration
- Dryxio/Ariane source and exact `LIBRW_REF` pinned in upstream `.github/workflows/build-euryopa.yml`.
- Android ARM64 librw build with GLES backend (not GLFW).
- Extract game/editor core into `native/ariane-core`.
- DFF/TXD/COL, full IPL/IDE, water, object placement, undo/redo and serialization.
- SAF-backed archive reads/writes and fixture tests.

Do not enable `ARIANE_ENABLE_UPSTREAM_CORE` until the Android-adapted core CMake target exists. Never commit proprietary GTA game archives/assets to a public repository.
