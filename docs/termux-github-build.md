# Termux + GitHub Actions workflow

## One-time setup on Android

```bash
pkg update -y
pkg install git gh openssh curl unzip zip
 gh auth login
```

Use SSH or HTTPS according to the GitHub CLI login method. The repository must be one you own or
have write permission to. Add this port project to that repository, then push `main`.

## Each build

```bash
cd ~/ArianeMobile
git status
# Commit changes before triggering a build.
git add .
git commit -m "Describe Android port change"
git push origin main
gh workflow run android.yml --ref main
gh run watch --exit-status
```

Pushes to `main` that touch app/build files also trigger CI automatically. The APK is an Actions
artifact, not a Play Store release.
