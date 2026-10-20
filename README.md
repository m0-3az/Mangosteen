# Mangosteen

A dark, Material 3 Expressive Android chat client for **OpenCode Go**.

Mangosteen keeps the conversation in front of you: streaming replies, visible reasoning, web search, image input, model switching, slash commands, and proactive check-ins — wrapped in a custom deep-purple, pink, saffron, and teal theme.

<p align="left">
  <img alt="Android 7+" src="https://img.shields.io/badge/Android-7%2B-1A1020?style=flat-square&logo=android&logoColor=4ECDC4">
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.4-1A1020?style=flat-square&logo=kotlin&logoColor=D34B84">
  <img alt="Jetpack Compose" src="https://img.shields.io/badge/Jetpack%20Compose-Material%203-1A1020?style=flat-square&logo=jetpackcompose&logoColor=4ECDC4">
</p>

## Features

- **Streaming chat** — responses appear as they are generated, with a stop button that actually aborts the request.
- **Visible reasoning** — thinking streams into a collapsible card above each reply, so the answer never hides its work.
- **Model picker** — switch between DeepSeek, GLM, Kimi, MiniMax, Qwen, Grok, GPT, and other OpenCode Go models per chat.
- **Web search** — toggle search for models that support it, right from the composer.
- **Image input** — attach a photo and send it to vision-capable models.
- **Markdown replies** — headings, lists, quotes, inline code, fenced code blocks, and links.
- **Six themes** — Mangosteen, Ember, Porcelain, Fruit, Crimson, and Moss; switch instantly in Settings → Appearance.
- **Message actions** — long-press to copy, edit, regenerate, select text, or delete from that point.
- **Slash commands** — `/system`, `/checkin`, `/clear`, and `/delete`, with an inline command menu.
- **Proactive check-ins** — schedule a follow-up from chat or let Mangosteen offer one; WorkManager delivers it even after the app closes.
- **Encrypted API key** — sealed with an AES/GCM key in the Android Keystore.
- **Local-first history** — conversations and messages live in SQLite on the device.

## Design

The UI is built from a custom Material 3 Expressive system rather than a stock template. Mangosteen is the default; Ember, Porcelain, Fruit, Crimson, and Moss are included as alternate palettes.

| Role | Color |
| --- | --- |
| Background | `#1A1020` |
| Surface | `#24172C` |
| Raised surface | `#35243F` |
| Primary action | `#D34B84` |
| Message bubble | `#6A2C91` |
| Accent yellow | `#FFC300` |
| Accent teal | `#4ECDC4` |
| Text | `#E2E2E2` |

Screens: Chats, Chat, New chat, Long-press actions, Slash commands, Settings, Model picker.

## Requirements

- Android 7.0+ (API 24)
- JDK 21
- Android SDK 37
- An OpenCode Go API key

## Build

```bash
git clone https://github.com/im6ix/Mangosteen.git
cd Mangosteen

# Point Gradle at your Android SDK if it is not in ANDROID_HOME.
echo "sdk.dir=$HOME/Android/Sdk" > local.properties

./gradlew :app:assembleDebug
```

The APK is written to:

```text
app/build/outputs/apk/debug/app-debug.apk
```

For a release build, create `keystore.properties` at the project root:

```properties
storeFile=dist/mangosteen.jks
storePassword=...
keyAlias=...
keyPassword=...
```

Then run `./gradlew :app:assembleRelease`.

## First run

1. Open **Settings**.
2. Paste your OpenCode Go API key. It is encrypted before it touches SharedPreferences.
3. Pick a default model.
4. Go back and tap **New chat**.

Chats use the default model until you switch models inside a conversation. The app calls the OpenCode Go endpoint directly at `https://opencode.ai/zen/go/v1`.

## Slash commands

| Command | What it does |
| --- | --- |
| `/system` | Set this chat's system prompt |
| `/checkin <when>` | Schedule a check-in, e.g. `/checkin exam tomorrow at 9am` |
| `/clear` | Remove every message in the current chat |
| `/delete` | Delete the current chat |

Typing `/` opens the command menu. The `/` button in the chat header inserts it for you.

## Architecture

```text
app/src/main/java/ai/opencode/mangosteen/
├── MainActivity.kt          # Chats screen + navigation
├── ChatActivity.kt          # One conversation
├── SettingsActivity.kt      # Settings host
├── data/                    # SQLite schema and repository
├── net/                     # OpenCode Go client, model catalog, wire models
├── proactive/               # WorkManager check-ins and notifications
├── settings/                # Keystore-backed prefs, character extraction
└── ui/
    ├── ChatsScreen.kt
    ├── ChatScreen.kt
    ├── SettingsScreen.kt
    ├── MarkdownText.kt
    ├── components/          # Expressive shared UI and model picker sheet
    └── theme/               # Dark Mangosteen Material 3 theme
```

The app intentionally uses `HttpURLConnection` and Android's platform JSON parser. There is no Retrofit, OkHttp, Hilt, or navigation library in the APK.

## Privacy

- Chat history stays on the device.
- The API key is stored encrypted with an Android Keystore key.
- Requests go only to the OpenCode Go endpoint you configure the app with.
- No analytics, crash reporting, ads, or tracking SDKs.

## Status

Mangosteen 2.0. The UI is being rebuilt around the Dark Mangosteen Material 3 Expressive design. The chat, model, settings, and check-in flows are functional; the app is a personal client, not an official OpenCode product.

## License

No license has been added yet. All rights reserved unless a license is added later.
