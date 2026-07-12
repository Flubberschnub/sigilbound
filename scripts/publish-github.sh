#!/usr/bin/env bash
set -euo pipefail

OWNER="${GITHUB_OWNER:-Flubberschnub}"
REPOSITORY="${1:-sigilbound-arcane-duel}"
VISIBILITY="${2:-private}"

command -v gh >/dev/null 2>&1 || {
  echo "GitHub CLI is required: https://cli.github.com/" >&2
  exit 1
}
gh auth status >/dev/null

case "$VISIBILITY" in
  public|private|internal) ;;
  *) echo "Visibility must be public, private, or internal." >&2; exit 1 ;;
esac

if gh repo view "$OWNER/$REPOSITORY" >/dev/null 2>&1; then
  git remote remove origin 2>/dev/null || true
  git remote add origin "https://github.com/$OWNER/$REPOSITORY.git"
  git push -u origin main
else
  gh repo create "$OWNER/$REPOSITORY" --"$VISIBILITY" --source=. --remote=origin --push
fi
