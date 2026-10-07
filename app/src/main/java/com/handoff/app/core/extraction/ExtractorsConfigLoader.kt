package com.handoff.app.core.extraction

import android.content.Context
import kotlinx.serialization.json.Json

/** Loads the selector/paths configuration bundled as an asset. */
object ExtractorsConfigLoader {

    fun load(context: Context): ExtractorsConfig = runCatching {
        context.assets.open("extractors_config.json").bufferedReader().use { reader ->
            Json.decodeFromString(ExtractorsConfig.serializer(), reader.readText())
        }
    }.getOrElse { ExtractorsConfig() }
}
