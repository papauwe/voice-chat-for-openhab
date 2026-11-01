# openHAB Voice Chat

An unofficial, community **voice and chat client for [openHAB](https://www.openhab.org) 5.x**.
Talk to your smart home: speak or type a question, openHAB (HABot or an LLM-based
Human Language Interpreter such as the ChatGPT binding) answers – as text and read aloud.

> This is an independent community project and is **not** affiliated with or endorsed
> by the openHAB Foundation e.V. "openHAB" is a trademark of the openHAB Foundation.

## Features

- **Voice input** via the Android SpeechRecognizer (press-to-talk, partial results shown live)
- **Text-to-speech** for answers (can be toggled in the top bar)
- **Multiple chat modes**:
  - *openHAB 5 Chat* – the Main-UI assistant (`POST /rest/voice/interpreters`), with
    server-side conversation history per conversation ID (including clearing it)
  - *HABot Chat* – `POST /rest/habot/chat`
  - *Voice Interpreter* – `POST /rest/voice/interpret` (executes commands)
  - *Custom endpoint* – POST `{"message": …}` to any path, answer is read from
    `answer`/`text`/`message`/`response`/`result`
- **Markdown rendering** of answers (GFM subset: bold, italics, lists, tables, code)
- **Authentication**: openHAB API token (Bearer / `X-OpenHAB-Token`) or Basic Auth
- **Home-screen widget** (1x1): tap to start speech recognition immediately
- Works against your local openHAB (HTTP and HTTPS) or an openHAB Cloud instance

## Requirements

- Android 8.0+ (API 26)
- An openHAB 5.x server. For the chat experience install the **HABot** add-on and/or an
  LLM-based Human Language Interpreter (e.g. the ChatGPT binding with an OpenRouter/OpenAI key).

## Building

```bash
./gradlew assembleDebug      # debug APK: app/build/outputs/apk/debug/
./gradlew testDebugUnitTest  # JVM unit tests (JUnit, MockWebServer, Robolectric)
```

Requirements: JDK 17+ (JDK 21 works too) and an Android SDK. `local.properties` with
`sdk.dir=…` is created by Android Studio automatically, or set `ANDROID_HOME`.

## Architecture notes

- The core logic (`ChatViewModel`, `OpenHabClient`, `SettingsStore`, `TextUtil`) is covered
  by JVM unit tests; Android services (TTS/STT wrappers, UI) are intentionally not unit-tested.
- Dependency injection is done via optional constructor parameters in `ChatViewModel`
  (interfaces: `OpenHabApi`, `SettingsStore`, `TtsEngine`, `SpeechEngine`, `ChatStrings`).
- User-facing strings live in `app/src/main/res/values/strings.xml` (English default) with a
  German translation in `values-de`. A `tools/dummy_server.py` simulates an openHAB server
  for manual testing (port 8080).

## Roadmap

- [ ] Release signing + reproducible release builds
- [ ] GitHub Actions CI
- [ ] F-Droid distribution (via IzzyOnDroid, later official F-Droid)
- [ ] More translations

## License

[EPL-2.0](LICENSE) – same license family as the openHAB project itself.
