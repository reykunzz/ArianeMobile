# GitHub + Termux workflow

Target: GTA San Andreas Classic Mobile, ARM64 (`arm64-v8a`).

1. Buat repository kosong di GitHub.
2. Dari Termux, ekstrak ZIP ini lalu jalankan:

```bash
pkg update -y && pkg install git openssh unzip
unzip ArianeMobile-GitHub-Termux.zip
cd ArianeMobile
git init && git add . && git commit -m "Initial Ariane Mobile port scaffold"
git branch -M main
git remote add origin https://github.com/USERNAME/REPOSITORY.git
git push -u origin main
```

Setiap push ke `main`, `master`, atau `develop` menjalankan workflow `.github/workflows/android-apk.yml`. Unduh artifact `ArianeMobile-debug-arm64` dari tab Actions. Untuk perubahan berikutnya, jalankan `bash scripts/termux-push.sh "pesan commit"`.

## Status integrasi
Saat ini tersedia shell Kotlin/JNI/OpenGL ES, parser awal IPL text `inst`, parser index IMG v2, serta workflow CI. Source upstream Ariane dan librw **belum** tertaut. Ariane upstream memakai integrasi librw cabang `ariane` dan entry point desktop Premake/GLFW; perlu pemisahan core dari UI desktop dan port backend GLES Android sebelum dapat ditautkan.

Jangan unggah arsip game atau aset GTA berhak cipta ke repository publik. Pilih aset secara lokal melalui Android Storage Access Framework.
