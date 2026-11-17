#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail
pkg update -y
pkg install -y git openssh curl wget unzip zip nano
echo "Termux siap. Gunakan GitHub Actions untuk build APK ARM64."
