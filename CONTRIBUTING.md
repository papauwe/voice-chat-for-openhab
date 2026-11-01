# Contributing

Thanks for your interest in contributing!

## Development setup

1. Install Android Studio (or the Android SDK command-line tools) and JDK 17+ (21 works).
2. Clone the repository and let Android Studio create `local.properties` (or set `ANDROID_HOME`).
3. Build and test:

   ```bash
   ./gradlew assembleDebug
   ./gradlew testDebugUnitTest   # report: app/build/reports/tests/testDebugUnitTest/index.html
   ```

## Ground rules

- **Keep the core logic testable.** `ChatViewModel`, `OpenHabClient`, `SettingsStore` and
  `TextUtil` must stay runnable on the JVM (no direct Android framework calls). If you add
  logic there, add unit tests (JUnit + MockWebServer, Robolectric only where unavoidable).
- **Dependency injection** uses optional constructor parameters and small interfaces
  (`OpenHabApi`, `SettingsStore`, `TtsEngine`, `SpeechEngine`, `ChatStrings`, `OpenHabTexts`).
  Please keep this pattern instead of introducing a DI framework.
- **User-facing strings** belong in `app/src/main/res/values/strings.xml` (English default);
  add German translations to `values-de`. Do not hardcode display strings in code.
- **Do not commit personal configuration.** No real server URLs, tokens, passwords, item
  names from your home setup, IDE files or local machine paths (`local.properties`,
  `gradle.properties` machine entries, keystores).
- **Language of the code base** is English (comments may be German where they explain
  domain-specific openHAB behavior).

## Pull requests

- Keep PRs focused; one topic per PR.
- Make sure `./gradlew testDebugUnitTest assembleDebug` passes before opening a PR.
- Describe what the change does and why; screenshots are welcome for UI changes.

## Licensing

By contributing you agree that your contributions are licensed under the
[Eclipse Public License 2.0](LICENSE), the same license family used by the openHAB project.
