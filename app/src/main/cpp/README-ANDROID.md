# Android native integration boundary

`third_party/ariane-upstream` and `third_party/librw` are tracked as Git submodules.
The upstream project currently generates desktop Premake targets (GLFW/OpenGL or D3D9).
Those targets are **not** Android-compatible as-is. Do not add the entire desktop source
set to this Android CMake target.

Porting sequence:
1. Inventory Ariane source dependencies and isolate format/model/world logic from desktop UI.
2. Add an Android platform layer for file IO (SAF), input, threading and GLES context.
3. Configure a librw Android build with its desktop window/context backend disabled and GLES backend selected/implemented.
4. Port the RenderWare plugin/stream pieces required by GTA SA Classic assets.
5. Link only the compatible core targets here; keep the Ariane desktop front-end excluded.

The local `formats/IPLReader` is an initial text-IPL parser for `inst` records only. It is not
a replacement for Ariane's complete IPL implementation and does not yet handle binary IPL,
`cull`, `zone`, `occl`, `path`, or water sections.
