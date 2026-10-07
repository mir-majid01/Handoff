# DECISIONS.md

One line per decision, newest phases at the bottom within each section.

## Product & platform
- Native Android app, Kotlin + Jetpack Compose, single-activity MVVM + unidirectional state (per brief assumption).
- Package id `com.handoff.app`; app name "Handoff".

## Toolchain (verified against maven repos before use)
- AGP 8.7.3 + Gradle 8.9 + Kotlin 2.0.21 (compose compiler via `org.jetbrains.kotlin.plugin.compose`).
- JDK: Temurin 17 (installed locally at `C:\Users\HP\handoff-tools\jdk` — machine-local, not part of the repo).
- compileSdk/targetSdk 35 (not 36): AGP 8.7.3 supports 35 warning-free; 36 would need AGP 8.9+ and buy nothing for this app. Revisit only if a required API is 36-only.
- minSdk 26 (per brief).
- KSP 2.0.21-1.0.28 for Room; Compose BOM 2024.12.01 (Compose 1.7.6 / Material3 1.3.1); Navigation Compose 2.8.5.
- Version catalog (`gradle/libs.versions.toml`) is the single source of versions.
- Robolectric 4.14.1 chosen for the UI smoke test (no emulator needed in CI-less environment).

## Design system
- Fonts: Inter TTFs (400/500/600/700, latin subset) bundled from Fontsource CDN into `res/font`.
- `surfaceTint = Color.Transparent` everywhere: kills default M3 elevation tinting for a flat iOS look.
- Springs at damping 0.8 for taps/layout; nav pushes use a 340ms emphasized ease-out tween (spring position on nav slides feels floaty).
- Dark secondary text #A89C91, dark hairline #3A342E, dark surfaces #252120/#2E2A27 — derived, since brief only fixed bg/text/accent.
- material-icons-extended included as the icon baseline; custom drawn vectors added where the design needs them.

## Architecture
- Route strings in `Routes` object; iOS push/pop transitions centralized in `HandoffNavHost`.
- `HandoffPage` is the standard page chrome (background + safe areas + large title); all screens build on it.
- `HandoffApp` is the composition root (Room, DataStore, ExtractionEngine); ViewModels are AndroidViewModels pulling from it — no DI framework at this scale.
- Conversation persisted as polymorphic JSON (`HandoffJson`, discriminator `type`) in a single Room column; schema stays trivial while the model evolves.

## Extraction
- Embedded-JSON parsing has three named strategies (`chatgpt_mapping`, `message_array`, `deep_search`); `deep_search` (DFS for role+content shapes) is the default so a site moving its state doesn't break the app.
- Next.js app-router pages (ChatGPT/Claude) ship state in `self.__next_f.push([1,"…"])` chunks; StaticExtractor unescapes and concatenates them, then scans for balanced JSON objects (capped attempts) — needed because there is no single `__NEXT_DATA__` script on those pages.
- ChatGPT share `mapping` tree is walked from `current_node` via parent links; if parent links are missing, falls back to JSON insertion order.
- Gemini relies on WebView DOM extraction; selectors are best-effort and config-driven, with the heuristic parser and manual paste as backstops.
- Code payload in embedded JSON read from `code`, then `content`, then `text` keys — ChatGPT parts use `content_type`+`parts`, Claude uses `type`+`text`.

## Export & data
- PDF renders the formatted *transcript* (roles, code blocks, page numbers); the wrapper prompt is delivered via copy/.txt/share — a prompt PDF would mostly be wrapper noise. Logged as a deliberate split.
- History delete uses a 5s undo window with the full record held in memory; Room row is re-upserted on undo (no tombstone tables).
- Manual paste: labeled paragraphs ("You:", "Assistant:", "ChatGPT:"…) merge into their speaker turn; unlabeled paragraphs alternate strictly starting with the user.

## Hardening
- Prompt building for huge chats runs on Dispatchers.Default; buttons use heightIn so 200% font scale grows them.
