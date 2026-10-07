package com.handoff.app.core.builder

/** What goes into the prompt. All condensation is local and rule-based (no network). */
enum class PromptMode(val displayName: String, val subtitle: String) {
    FULL_TRANSCRIPT("Full", "Every message, in order, as Markdown"),
    SMART_BRIEF("Brief", "Goal, decisions, final code, last exchanges"),
    CODE_AND_DECISIONS("Code", "Just the code blocks and what was agreed"),
}

/** Which assistant will receive the prompt. Changes only the wrapper wording. */
enum class TargetAi(val displayName: String) {
    CLAUDE("Claude"),
    CHATGPT("ChatGPT"),
    GEMINI("Gemini"),
    GENERIC("Any AI"),
}

/** User-adjustable context ceiling; splitter kicks in above it. */
enum class TokenLimit(val label: String, val tokens: Int?) {
    K8("8k", 8_000),
    K32("32k", 32_000),
    K100("100k", 100_000),
    UNLIMITED("No limit", null),
}
