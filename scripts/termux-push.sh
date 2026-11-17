#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
[ -d .git ] || { echo "Jalankan dari root repository" >&2; exit 1; }
git status --short
git add -A
git diff --cached --quiet && { echo "Tidak ada perubahan."; exit 0; }
git commit -m "${1:-Android: update Ariane Mobile}"
git push -u origin "$(git branch --show-current)"
echo "Push selesai; cek tab Actions untuk APK."
