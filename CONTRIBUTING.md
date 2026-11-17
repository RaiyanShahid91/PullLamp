# Contributing to PullLamp

Thanks for helping! A few ground rules keep the project healthy.

## How changes get in

- **Nobody pushes directly to `main`.** Every change, including the maintainer's, goes through a pull
  request.
- **Every pull request needs the maintainer's approval** before it can be merged. Code owners are
  listed in [`.github/CODEOWNERS`](.github/CODEOWNERS).
- CI must pass (unit tests and a sample build).

## Before you start

For anything bigger than a small fix, please open an issue first so we can agree on the approach
before you spend time on it.

## Development setup

1. Install the latest stable Android Studio (JDK 17 or newer).
2. Fork the repository and clone your fork.
3. Open the folder in Android Studio and let Gradle sync.
4. Run the tests: `./gradlew :pulllamp:testDebugUnitTest`
5. Run the `sample` app to try changes on a device or emulator.
6. With an emulator running, run the on-device tests: `./gradlew :pulllamp:connectedDebugAndroidTest :sample:connectedDebugAndroidTest`

## Pull request checklist

- [ ] The change does one thing and the description explains why.
- [ ] Unit tests added or updated (the cord engine is plain Kotlin and easy to test).
- [ ] `./gradlew :pulllamp:testDebugUnitTest` passes.
- [ ] Public API changes are intentional (the library uses Kotlin explicit API mode) and documented in
      `README.md` / `docs/USAGE.md` and `CHANGELOG.md`.
- [ ] New source files carry the Apache 2.0 license header.

## Code style

Official Kotlin style (`kotlin.code.style=official`). Keep public APIs small and documented with KDoc.

## Changing the effect

The maths lives in `CordSimulation` and `LampPendulum` and has no Android dependencies, so it is tested
with plain JUnit. If you change how the cord moves, add or update a test that pins the behaviour you
changed, and try it in the `sample` app.

## Licensing

By contributing you agree that your contributions are licensed under the
[Apache License 2.0](LICENSE).
