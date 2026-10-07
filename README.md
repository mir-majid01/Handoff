# Handoff

Paste a share link from Claude, ChatGPT, Gemini, Kimi, DeepSeek or Qwen and get a
clean, ready-to-paste prompt that lets any other AI continue the conversation
with full context.

Everything runs on your phone: no accounts, no analytics, no server. The only
network use is fetching the shared page itself.

## Build

Requirements: JDK 17, Android SDK with platform 35, Gradle (wrapper included).

```bash
./gradlew assembleDebug        # debug APK -> app/build/outputs/apk/debug/
./gradlew testDebugUnitTest    # unit + Robolectric tests
```

The debug APK installs on any device with Android 8.0 (API 26) or newer.

## Architecture

Single-module app, MVVM + unidirectional state, Jetpack Compose throughout.

```
app/src/main/java/com/handoff/app/
  core/               Pure Kotlin, no Android UI dependencies (fully unit-tested)
    model/            Conversation / Message / Block normalized model + JSON setup
    markdown/         Blocks -> Markdown renderer
    builder/          PromptBuilder (3 modes × 4 targets), TokenEstimator,
                      PromptSplitter, SmartBrief condensation rules
    extraction/       The extraction engine (below)
    export/           PdfExporter + pure PdfLayout
  data/
    db/               Room (history; conversation persisted as JSON)
    prefs/            DataStore settings
    repo/             HandoffRepository — single gateway over Room + JSON
  ui/
    theme/            Design tokens: cream/sage palettes, Inter type scale,
                      shapes, spring motion constants
    components/       Cards, segmented control, press-scale, shimmer, toasts
    screens/          One folder per screen, ViewModel next to its screen
    navigation/       Routes + iOS-style push/pop transitions
```

ViewModels are AndroidViewModels pulling singletons from `HandoffApp`
(composition root; deliberately DI-framework-free).

## Extraction engine (how a link becomes a conversation)

`ExtractionEngine` runs a fallback chain, reporting stage progress as it goes:

1. **Static fetch** (`StaticExtractor`) — plain OkHttp fetch, then pulls the
   conversation out of embedded JSON: `<script id="__NEXT_DATA__">`, any
   `application/json` script, and finally streamed Next.js RSC payloads
   (`self.__next_f.push([1,"..."])` chunks are unescaped and scanned for
   balanced JSON objects). Parsing strategies: `chatgpt_mapping` (walks the
   ChatGPT share tree from `current_node`), `message_array` (role+content
   arrays), and `deep_search` (DFS that finds either shape anywhere — the
   resilient default).
2. **Hidden WebView** (`WebViewExtractor`) — for pages that need JS to render.
   Loads the URL in an offscreen WebView (images blocked), polls until the
   message-container selector stabilizes, then injects an extractor built from
   config that returns messages as JSON (roles from attributes/classes,
   code/headings/lists split out, UI junk stripped).
3. **Generic heuristic** (`GenericHeuristicExtractor`) — visible-text
   paragraphs with alternating roles. Low confidence by design; the UI makes
   the result easy to correct.
4. **Manual paste** (`ManualPasteParser`) — the user pastes raw chat text;
   role prefixes like "You:" / "Assistant:" / "ChatGPT:" are detected,
   otherwise paragraphs alternate.

Typed failures (`ExtractionError.NetworkError/NotFound/Private/ParseFailed/
Timeout/UnsupportedLink`) map to one human message and one suggested action.

## Updating extractor selectors

**All selectors, URL patterns and JSON paths live in
`app/src/main/assets/extractors_config.json`** — when a site changes its DOM or
state shape, edit the JSON; no code changes needed.

- `platforms.<name>.urlPatterns` — regexes that identify share links.
- `platforms.<name>.static.scripts[]` — where embedded state lives
  (`id` / `attrName`+`attrValue` matchers, optional explicit `paths`,
  `parser` = `chatgpt_mapping` | `message_array` | `deep_search`).
- `platforms.<name>.webview` — `readySelector`, `messageSelector`,
  `roleAttr` or `roleByClass`, `textSelector`, lazy-scroll and poll timings.

After editing, run the fixture tests:

```bash
./gradlew testDebugUnitTest --tests "com.handoff.app.core.extraction.*"
```

Add/refresh saved pages in `app/src/test/resources/fixtures/` when a site
changes, so regressions stay caught.

## Known limitations

- ChatGPT share pages moved off Next.js (`__NEXT_DATA__` is gone; state now ships
  in React Router stream chunks), so the WebView/DOM path is the primary route
  for ChatGPT; the static JSON path remains as a legacy fallback.
- Claude share pages now ship state through RSC chunks; the deep-search recovers
  it but is sensitive to structural changes.
- Gemini share pages render fully client-side; selectors were re-verified against
  the live share DOM (`user-query` / `response-container` turn elements under
  `share-turn-viewer`). New `share.gemini.google/...` short links are supported.
- Kimi, DeepSeek and Qwen pages are JS-only SPAs, so they rely on the WebView
  strategy. DeepSeek and Qwen selectors were verified against live share pages;
  Kimi selectors are best-effort (live verification pending — share links there
  showed "Chat temporarily unavailable").
- Links behind a login wall (private shares, org-restricted chats) surface as
  "Private" — the suggested action is manual paste.
- The PDF export renders the transcript, not the wrapper prompt; exports of
  the prompt itself are `.txt`/clipboard/share.
- The heuristic fallback (alternating paragraphs) is intentionally rough; it
  exists so the user always gets something editable rather than a dead end.

## Privacy

History and settings are local (Room + DataStore). No permissions beyond
INTERNET. Deleting the app removes everything.

## Author

Developed by **Mir Majid** — [github.com/editsu4k-coder](https://github.com/editsu4k-coder).
The same credit appears in-app under **Settings → About**.

## Credits

- **Inter** — body and UI typeface (SIL Open Font License).
- **Spectral** — wordmark and screen-title serif, bundled in `app/src/main/res/font/`
  (SIL Open Font License; full text in [`LICENSE-Spectral.txt`](LICENSE-Spectral.txt)).
- **Material Symbols (Rounded)** — platform and action icons (Apache 2.0).

