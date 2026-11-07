# Releasing

Two environments, one branch each.

| | **dev** | **prod** |
|---|---|---|
| Branch | `dev` (long-lived) | `main` |
| Trigger | every push to `dev` that passes CI | a `vX.Y.Z` tag on `main` |
| Output | GitHub **pre-release** `dev-N` with an APK | GitHub **Release** with APK + AAB, optional Play upload |
| App on the phone | **Puck Dev** (`dev.danielclements.puck.dev`) | **Puck** (`dev.danielclements.puck`) |
| Signed with | release key (debug key on forks) | release key |
| Approval | none | optional reviewer gate on the `prod` environment |

The dev app has its own application id, so it installs *beside* the real one
and never touches its pairings. Dev builds get `versionCode = CI run number`,
so each new one upgrades the last.

## Day to day

```
feature branch ──PR──▶ dev ──(test the dev APK on a phone)──PR──▶ main ──tag──▶ release
```

1. Branch from `dev`, open a PR **into `dev`**. CI runs tests, lint, a debug
   build, a minified build, and commitlint on the PR.
2. Merge. CI publishes a `dev-N` pre-release (the ten newest are kept). Install
   the APK from the [Releases page](../../../releases) and try it.
3. When `dev` is good, open a PR `dev` → `main` (a merge commit or squash, as
   you prefer). CI runs again.
4. To release, bump `versionName` and `versionCode` in `app/build.gradle.kts`
   (on `dev`, so it is tested first), add
   `fastlane/metadata/android/en-US/changelogs/<versionCode>.txt`, and merge to
   `main`. Then tag main:

   ```bash
   git checkout main && git pull
   git tag v1.4.2 && git push origin v1.4.2
   ```

5. `Release` runs. **verify** refuses the tag unless it is `vX.Y.Z`, on `main`,
   equal to `versionName`, and has a higher `versionCode` than the last release
   (`tools/check_release.py`). Then **release** waits on the `prod`
   environment (if reviewers are set), tests, builds the signed APK and AAB, and
   publishes the GitHub Release with notes generated from Conventional Commits.
6. After release, merge `main` back into `dev` so they do not drift:
   `git checkout dev && git merge main && git push`.

## Distribution channels

- **GitHub Releases:** automatic, above.
- **F-Droid:** builds from the tag on its own servers; nothing to upload. The
  recipe's `UpdateCheckMode` only matches `vX.Y.Z`, so `dev-N` pre-releases are
  never picked up. Bump `CurrentVersion`/`CurrentVersionCode` in the recipe if
  you maintain it by hand.
- **Google Play:** set the repository **variable** `PLAY_UPLOAD=true` and the
  **secret** `PLAY_SERVICE_ACCOUNT_JSON` (a Play Console service account with
  release permission). Each release then uploads the AAB to the **internal**
  track. Promote to closed/production by hand in Play Console. Do this only
  after the app has been created in Play Console and its first upload made
  manually.

## One-time repository setup

- **Environments** (Settings → Environments): `dev` and `prod`. On `prod`, add
  yourself under *Required reviewers* to approve every release.
- **Secrets** (repo or per environment): `ANDROID_KEYSTORE_BASE64`,
  `ANDROID_KEYSTORE_PASSWORD`, `ANDROID_KEY_ALIAS`, `ANDROID_KEY_PASSWORD`.
- **Branch protection** (Settings → Branches), for `main` and `dev`: require a
  pull request and the `build` and `commitlint` checks. Block force pushes to
  `main`.
- Default branch can stay `main`.

## Guardrails worth knowing

- Tags that are not `vX.Y.Z`, or are not on `main`, never publish.
- Re-running a failed `Release` job re-runs from the same tag; if it failed
  after the GitHub Release was created, delete that release before retrying.
- Dev builds are minified like production ones, so R8 problems show up on `dev`
  rather than on release day.
