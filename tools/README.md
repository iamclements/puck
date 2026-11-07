# Tools

Development scripts. None of these are needed to build or run the app.

## Conformance vectors

The protocol module's crypto and serialization are validated byte-for-byte
against output from [pyatv](https://github.com/postlund/pyatv), a reference
implementation known to interoperate with real hardware. Writing these layers
against a spec alone is how subtle interop bugs get shipped.

```bash
python -m venv venv && ./venv/bin/pip install pyatv
./venv/bin/python tools/generate_vectors.py       > protocol/src/test/resources/vectors.json
./venv/bin/python tools/generate_plist_vectors.py > protocol/src/test/resources/plistvectors.json
./gradlew :protocol:test
```

The committed vectors are the ones the test suite runs against; regenerate them
only if you intend to review the diff.

## Launcher icon

`generate_icon.py` emits the adaptive-icon vector drawable and an equivalent
SVG. Android vector XML cannot be previewed directly, so rendering the SVG is
how the shipped drawable gets visually verified.

```bash
python tools/generate_icon.py
```

## Release tooling

Used by the GitHub workflows; both can be run locally.

- `check_release.py` guards a production tag before anything is built: it must
  be `vX.Y.Z`, on `main`, equal to `versionName`, with a higher `versionCode`
  than the previous release. `TAG=v1.4.2 python3 tools/check_release.py`
- `release_notes.py` builds a GitHub release body from Conventional Commits
  since the previous `v*` tag (dev pre-release tags are ignored). It reads the
  `TAG` and `REPO` environment variables.

See [`docs/RELEASING.md`](../docs/RELEASING.md) for how they fit the pipeline.

