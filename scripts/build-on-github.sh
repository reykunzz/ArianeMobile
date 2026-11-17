#!/data/data/com.termux/files/usr/bin/bash
set -euo pipefail

BRANCH="${1:-$(git branch --show-current)}"
if [[ -z "$BRANCH" ]]; then
  echo 'Cannot detect branch. Usage: ./scripts/build-on-github.sh [branch]' >&2
  exit 2
fi

git diff --quiet && git diff --cached --quiet || {
  echo 'Working tree has uncommitted changes. Commit and push first.' >&2
  exit 3
}

git push origin "$BRANCH"
gh workflow run android.yml --ref "$BRANCH"
echo "Workflow started for branch: $BRANCH"
echo 'Use: gh run watch --exit-status'
echo 'List artifacts: gh run list --workflow android.yml'
