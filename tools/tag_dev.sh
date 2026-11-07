#!/usr/bin/env bash
# Cut a dev build: tag the tip of dev as dev-N and push the tag, which starts
# .github/workflows/dev-release.yml.
#
#   tools/tag_dev.sh          # tag and push
#   tools/tag_dev.sh --dry    # say what it would do
#
# N is one more than the highest existing dev-N. Refuses unless you are on dev,
# the tree is clean, and your dev is exactly origin/dev, so what gets built is
# what was reviewed and merged.
set -euo pipefail

dry=0
[ "${1:-}" = "--dry" ] && dry=1

branch=$(git rev-parse --abbrev-ref HEAD)
[ "$branch" = "dev" ] || { echo "Switch to dev first (on '$branch')." >&2; exit 1; }
[ -z "$(git status --porcelain --untracked-files=no)" ] || { echo "Working tree has changes; commit or stash them." >&2; exit 1; }

git fetch --quiet --tags origin
[ "$(git rev-parse HEAD)" = "$(git rev-parse origin/dev)" ] || { echo "dev is not at origin/dev; pull or push first." >&2; exit 1; }

last=$(git tag -l 'dev-[0-9]*' | sed 's/^dev-//' | sort -n | tail -1)
next=$(( ${last:-0} + 1 ))
tag="dev-$next"

echo "Tagging $(git rev-parse --short HEAD) as $tag"
if [ "$dry" = 1 ]; then
  echo "(dry run: nothing created)"
  exit 0
fi
git tag "$tag"
git push origin "$tag"
echo "Pushed. Watch it: gh run watch --workflow dev-release.yml"
