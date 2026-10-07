package com.handoff.app.core.model

import kotlinx.serialization.json.Json

/** Single JSON configuration for persisting conversations and parsing extractor output. */
val HandoffJson: Json = Json {
    classDiscriminator = "type"
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}
