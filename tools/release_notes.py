"""Builds a GitHub release body from Conventional Commits since the previous tag.

Run from CI (.github/workflows/release.yml and dev-release.yml) with TAG and
REPO set in the environment, and optionally TAG_MATCH (a git tag glob, default
"v*", or "dev-*" for dev builds) choosing which earlier tags count as the
previous release; writes RELEASE_NOTES.md in the repo root. Commits that don't
follow the Conventional Commits format (everything before that was adopted,
plus any stray commit since) land in a catch-all "Other changes" section
rather than being dropped.
"""
import os
import re
import subprocess

TYPES = {
    "feat": "Features",
    "fix": "Fixes",
    "perf": "Performance",
    "refactor": "Refactoring",
    "docs": "Documentation",
    "build": "Build",
    "ci": "CI",
    "test": "Tests",
    "style": "Style",
    "chore": "Chores",
    "revert": "Reverts",
}

# Display order; anything not listed here (there isn't anything, TYPES and
# this agree by construction) would simply never print.
SECTION_ORDER = [
    "Features", "Fixes", "Performance", "Refactoring", "Documentation",
    "Build", "CI", "Tests", "Style", "Chores", "Reverts",
]

SUBJECT_RE = re.compile(r"^(\w+)(\(([^)]*)\))?(!)?:\s*(.+)$")


def git(*args: str) -> str:
    return subprocess.run(
        ["git", *args], capture_output=True, text=True, check=True,
    ).stdout.strip()


def previous_tag(tag: str) -> str | None:
    # Production notes compare against the last release, dev notes against the
    # last dev build; the two kinds of tag must not see each other.
    match = os.environ.get("TAG_MATCH", "v*")
    # The first dev build has no earlier dev build to compare with; fall back to
    # the last release so its notes are "what changed since v1.x", not the whole
    # history of the project.
    for pattern in dict.fromkeys([match, "v*"]):
        try:
            return git("describe", "--tags", "--abbrev=0", "--match", pattern, f"{tag}^")
        except subprocess.CalledProcessError:
            continue
    return None


def main() -> None:
    tag = os.environ["TAG"]
    repo = os.environ["REPO"]

    prev = previous_tag(tag)
    commit_range = f"{prev}..{tag}" if prev else tag
    subjects = [
        line for line in git(
            "log", commit_range, "--pretty=format:%s", "--no-merges",
        ).splitlines() if line
    ]

    buckets: dict[str, list[str]] = {}
    other: list[str] = []

    for subject in subjects:
        m = SUBJECT_RE.match(subject)
        kind = m.group(1) if m else None
        if m and kind in TYPES:
            scope = m.group(3)
            text = m.group(5)
            entry = f"**{scope}:** {text}" if scope else text
            buckets.setdefault(TYPES[kind], []).append(entry)
        else:
            other.append(subject)

    lines: list[str] = []
    for section in SECTION_ORDER:
        entries = buckets.get(section)
        if not entries:
            continue
        lines.append(f"### {section}")
        lines.extend(f"- {entry}" for entry in entries)
        lines.append("")

    if other:
        lines.append("### Other changes")
        lines.extend(f"- {entry}" for entry in other)
        lines.append("")

    if prev:
        lines.append(f"**Full diff:** https://github.com/{repo}/compare/{prev}...{tag}")

    with open("RELEASE_NOTES.md", "w") as f:
        f.write("\n".join(lines).strip() + "\n")


if __name__ == "__main__":
    main()
