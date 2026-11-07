"""Guards a production release tag. Exits non-zero with a reason if it is unsafe.

Run by the release workflow before anything is built or published:

  * the tag must be vMAJOR.MINOR.PATCH,
  * its commit must be on main (releases are cut from main, never from dev),
  * app/build.gradle.kts must say the same versionName as the tag,
  * versionCode must be higher than the previous release's, because Play and
    Android both refuse a build whose code does not increase.

Usage: TAG=v1.4.2 python3 tools/check_release.py
"""
import os
import re
import subprocess
import sys

GRADLE = "app/build.gradle.kts"


def git(*args: str) -> str:
    return subprocess.run(["git", *args], check=True, capture_output=True, text=True).stdout.strip()


def fields(gradle_text: str) -> tuple[str, int]:
    name = re.search(r'versionName\s*=\s*"([^"]+)"', gradle_text)
    code = re.search(r"versionCode\s*=\s*(\d+)", gradle_text)
    if not name or not code:
        raise SystemExit(f"could not read versionName/versionCode from {GRADLE}")
    return name.group(1), int(code.group(1))


def fail(message: str) -> None:
    print(f"::error::{message}")
    sys.exit(1)


def main() -> None:
    tag = os.environ["TAG"]
    if not re.fullmatch(r"v\d+\.\d+\.\d+", tag):
        fail(f"{tag} is not a release tag (expected vMAJOR.MINOR.PATCH)")

    # Fetched explicitly: a tag-triggered checkout does not have origin/main.
    subprocess.run(["git", "fetch", "--no-tags", "origin", "main"], check=True)
    if subprocess.run(["git", "merge-base", "--is-ancestor", tag, "origin/main"]).returncode != 0:
        fail(f"{tag} is not on main. Merge dev into main first, then tag main.")

    name, code = fields(open(GRADLE).read())
    if name != tag.removeprefix("v"):
        fail(f"{tag} does not match versionName {name} in {GRADLE}")

    try:
        previous = git("describe", "--tags", "--abbrev=0", "--match", "v*", f"{tag}^")
    except subprocess.CalledProcessError:
        previous = None
    if previous:
        _, prev_code = fields(git("show", f"{previous}:{GRADLE}"))
        if code <= prev_code:
            fail(f"versionCode {code} must be greater than {previous}'s {prev_code}")

    print(f"{tag} ok: versionName {name}, versionCode {code}, previous {previous or 'none'}")


if __name__ == "__main__":
    main()
