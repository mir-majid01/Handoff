# PROGRESS.md

Phase status. A phase is done only when it builds clean (no errors/warnings), tests pass, and its checklist is fully ticked.

## Phase 0 — Scaffold ✅
- [x] Environment: JDK 17 (Temurin, machine-local) + Gradle 8.9 + Android SDK (preinstalled)
- [x] Gradle project scaffold with version catalog, builds `assembleDebug`
- [x] Design tokens: light/dark palettes, Inter type scale (bundled TTFs), shapes, motion tokens
- [x] Adaptive app icon (sage/cream, speech-bubble motif)
- [x] Navigation shell with iOS-style transitions + themed screens
- [x] DECISIONS.md + PROGRESS.md live

## Phase 1 — UI shell ✅
- [x] Onboarding: 3 swipeable cards with code-drawn illustrations, page dots, skippable, shown once
- [x] Home: input card with live platform detection chip, Paste button, clipboard banner, recent list
- [x] Fetching: staged progress (Connecting → Reading → Cleaning → Building), shimmer orb, cancel, error card with one clear action
- [x] Preview: chat bubbles, exclude toggles, code collapse, search, sticky header with live token estimate
- [x] Export: mode/target/limit segmented controls, live estimate, split banner, copy/txt/pdf/share actions
- [x] History: list, swipe-to-delete with undo snackbar, search, empty state
- [x] Manual paste screen routed through the same pipeline
- [x] Settings: theme, default mode/target/limit, haptics, clear history (confirm dialog), about
- [x] Full flow clickable end to end, light + dark; press-scale, staggered entrances, springs throughout

## Phase 2 — Core models & builder ✅
- [x] Normalized Conversation/Message/Block model (polymorphic JSON)
- [x] PromptBuilder: Full Transcript / Smart Brief / Code & Decisions
- [x] Target AI presets (Claude/ChatGPT/Gemini/Generic) — wrapper wording only
- [x] Token estimator (chars÷4) + splitter (8k/32k/100k/unlimited) with part labels
- [x] Unit tests pass (builder, splitter, estimator, SmartBrief)

## Phase 3 — Extraction ✅
- [x] LinkDetector for claude.ai / chatgpt.com (+chat.openai.com) / gemini.google.com (+bard) share URLs
- [x] Static extractor (OkHttp+Jsoup): __NEXT_DATA__, application/json scripts, streamed RSC (`self.__next_f`) recovery
- [x] Hidden WebView extractor: config-driven injected JS, role attrs/class rules, code/list/heading split, lazy scroll, DOM-stability polling
- [x] Generic heuristic parser fallback
- [x] extractors_config.json drives all selectors/paths/parsers
- [x] Typed errors (NetworkError/NotFound/Private/ParseFailed/Timeout/UnsupportedLink) + retry with backoff
- [x] Parser unit tests on saved fixtures for all 3 platforms + deep-search + manual paste
- Note: verified against constructed fixtures capturing each site's state shape; live-link behavior covered by fallback chain + manual paste (see README limitations)

## Phase 4 — Export ✅
- [x] Copy with haptic + animated success toast
- [x] Save .txt via SAF (CreateDocument)
- [x] PDF export: A4, Inter, page numbers, monospace code blocks with background, paragraph/char wrapping (layout unit-tested)
- [x] Share sheet (ACTION_SEND chooser)
- [x] Incoming ACTION_SEND text opens straight to Fetching (manifest filter + nav hook)

## Phase 5 — Persistence ✅
- [x] Room history (save, reopen, update, delete, undo-restore, search)
- [x] DataStore settings (theme, default mode/target/limit, haptics, onboarding flag)
- [x] Onboarding shown once
- [x] Conversation JSON persisted before navigating; survives restart + process death; repository round-trip tested

## Phase 6 — Hardening ✅
- [x] Empty clipboard / invalid URL / no internet / private link handled with typed messages + actions
- [x] Rotation: ViewModels hold state; fetch job survives; process death: conversation persisted pre-navigation
- [x] Large chats: LazyColumn everywhere, extraction/prompt-building on Dispatchers.Default/IO
- [x] Accessibility: content descriptions, 48dp targets, buttons grow with font scale (heightIn), AA contrast palettes
- [x] All strings in strings.xml
- [x] UI smoke test (Robolectric) + 51 unit tests green; compile has no warnings

## Phase 7 — Polish loops (never "finished")
- [x] Loop 1
- [x] Loop 2
- [x] Loop 3
- [x] Loop 4
- [x] Loop 5
- [x] Loop 6

## Log
- Phase 0: env audited (SDK present; JDK/Gradle installed machine-local), scaffold written, first build green.
- Phases 1–6 built in one arc; 51 tests green, assembleDebug + lint green after fixing 12 test failures
  (root causes: ChatGPT `parts` arrays ignored when `content_type=text`, splitter budget floor, decision-keyword
  gaps, whitespace not collapsed in titles, manual-paste alternation, missing parent-link fallback in mapping walk).
- Loop 1: (1) fetch stage dots now show completed checkmarks + orb pulses; (2) export mode card animates on switch;
  (3) exclude toggle got semantics/labels (a11y) and search field hit 48dp; (4) history row decongested (chip under title);
  (5) top-bar icon spacing. Failure scenario E2E: unsupported link → error card + suggested action (deterministic),
  which flushed out a real crash: engine ran blocking OkHttp on Main — moved to Dispatchers.IO + catch-all degradation.
  Quality: duplicated token formatters unified into util/TokenFormat.kt. 52 tests green, lint 0/0.
- Loop 2: (1) Home input got IME "Go" + URI keyboard; (2) onboarding handles system back; (3) Settings version reads
  BuildConfig; (4) history delete reachable without swipe (a11y icon button); (5) shared SearchField extracted
  (Preview + History), 48dp target. Quality: dead imports/formaters swept.
- Loop 3: (1) all-messages-excluded no longer throws — builder emits an instructive prompt (crash fix + unit test);
  (2) Preview header shows the real conversation title; (3) empty search results got an icon empty state;
  (4) dead state/flag removed from ExportViewModel; (5) failure scenario E2E: empty manual paste shows inline error
  and never navigates (ManualPasteEmptyTest). 54 tests green, lint 0/0, APK 19.2MB.
- Loop 4 (bug report from user): onboarding Skip / Get started never navigated — onFinish was wired to an empty
  lambda and nothing persisted the onboarding-done flag. Fixed: onFinish writes the DataStore flag, nav effect swaps
  to Home and drops onboarding from the back stack; start destination is now decided from the first real DataStore
  emission (no onboarding flash for returning users). Onboarding card also made scrollable so small screens can't
  clip the text to zero height (found via Robolectric semantic-tree dump). Regression test: OnboardingFlowTest walks
  Get started → Home and asserts flag persistence. 55 tests green, lint 0/0.
- Loop 5 (user bug report: extraction fails on real links): root-caused against LIVE sites with a headless browser.
  (1) WebViewExtractor.parseResult never handled evaluateJavascript's JSON string encoding — the injected
  extractor's output arrived double-encoded, so Strategy 2 failed with "not valid JSON" on every site. Unwrapped
  + regression test. (2) Injected roleOf() crashed when roleByClass omitted (kotlinx encodes defaults off) and
  ignored custom-element tag names — guarded + matches tags (Gemini's user-query/response-container).
  (3) Image-only turns (Gemini generated images inside .response-container-footer, stripped by cleanClone) were
  dropped entirely — now kept as image placeholder blocks; verified 7→12 messages on a live chat.
  (4) ChatGPT killed __NEXT_DATA__ (share pages moved to React Router streams) — DOM/WebView is now primary path.
  (5) Gemini share DOM had fully drifted in config (model-response no longer exists); re-derived selectors live.
  (6) New platforms: Kimi, DeepSeek, Qwen configs + Platform enum + chips; DeepSeek/Qwen/Gemini verified on the
  user's real links (Kimi link itself was dead server-side — "Chat temporarily unavailable", best-effort config).
  (7) LinkDetector.normalize no longer collapses a whole multi-link WhatsApp paste into one broken URL — it now
  picks the first URL token. Config-parse guard test updated (static optional for SPA-only platforms).
  58 tests green, lint 0/0, APK rebuilt.
- Loop 6 (user bug report: 11 UI/layout issues across every screen): systematic pass over insets, controls, and
  visual tokens without touching the extraction/prompt data flow. (1) Added ScreenTopBar (56dp + statusBarsPadding)
  and navigationBarsPadding on every scroll/pinned surface — title/back no longer collide with the status clock.
  (2) Rebuilt SegmentedControl on weight(1f) cells inside the padded track (13–14sp labels, no clipping); root-caused
  the "stray grey tab" at the right edge to the old fixed-width thumb overflowing the unpadded pill. (3) Shortened
  labels (Any AI / No limit / Code). (4) Preview and Export now report the same TokenEstimator count, with an
  explicit "+N instructions" delta. (5) Home: trailing icons vertically centred on the title, Paste/Continue both
  48dp, Continue disabled until a platform is detected. (6) Home link field: outlined container, clear button,
  shortened host/…tail read-back when unfocused, and LinkDetector strips tracking params (og/utm/ref/…) before fetch.
  (7) "Link failed?" prompt now shows only after an error. (8) Recent handoffs: platform-coloured badge + dot, 2-line
  titles, chevron, swipe-to-delete, per-platform icons. (9) Preview: 24dp ringed selector with sage check, You/
  platform labels, #F3EDE4 vs white bubbles, top/bottom fades, hanging-indent bullets, pinned "X of Y selected" +
  "Continue (N)" bar. (10) Export: five cards merged into one grouped section (uppercase headers + one-line helpers),
  token progress meter (amber >80%, red over-limit) + split suggestion, collapsible first-6-lines preview, pinned
  Copy banner with Save/Share row. (11) Global: filled buttons #58796A (white text 4.8:1), flat cards with 1dp
  #E8E0D5 hairline + one shadow via cozyCard, semibold bordered segments, type scale, press-scale 0.97, haptics +
  animated check on Copy, illustrated Home empty state. 60 tests green, lint 0/0, APK rebuilt. Caveat: verified by
  compile + Robolectric + lint only — no physical device, so dark-mode/200%-font/360dp were not visually confirmed.
