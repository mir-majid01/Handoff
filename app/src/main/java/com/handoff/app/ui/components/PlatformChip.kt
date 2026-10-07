package com.handoff.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.NightsStay
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Spa
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.lerp
import com.handoff.app.core.model.Platform
import com.handoff.app.ui.theme.ClaudeOrange
import com.handoff.app.ui.theme.ClaudeTint
import com.handoff.app.ui.theme.ChatGptGreen
import com.handoff.app.ui.theme.ChatGptTint
import com.handoff.app.ui.theme.DarkClaudeTint
import com.handoff.app.ui.theme.DarkChatGptTint
import com.handoff.app.ui.theme.DarkGeminiTint
import com.handoff.app.ui.theme.GeminiBlue
import com.handoff.app.ui.theme.GeminiTint
import com.handoff.app.ui.theme.PillShape

data class PlatformStyle(val color: Color, val tint: Color)

/** Brand color darkened toward ink in light mode so chip text clears 4.5:1 on its tint. */
@Composable
fun platformStyle(platform: Platform): PlatformStyle {
    val dark = isSystemInDarkTheme()
    val brand = when (platform) {
        Platform.CLAUDE -> ClaudeOrange
        Platform.CHATGPT -> ChatGptGreen
        Platform.GEMINI -> GeminiBlue
        Platform.KIMI, Platform.DEEPSEEK, Platform.QWEN, Platform.GENERIC ->
            MaterialTheme.colorScheme.primary
    }
    val tint = when (platform) {
        Platform.CLAUDE -> if (dark) DarkClaudeTint else ClaudeTint
        Platform.CHATGPT -> if (dark) DarkChatGptTint else ChatGptTint
        Platform.GEMINI -> if (dark) DarkGeminiTint else GeminiTint
        else -> MaterialTheme.colorScheme.secondaryContainer
    }
    val textColor = if (dark) brand else lerp(brand, Color(0xFF1B1816), 0.35f)
    return PlatformStyle(textColor, tint)
}

fun platformIcon(platform: Platform): ImageVector = when (platform) {
    Platform.CLAUDE -> Icons.Rounded.Psychology
    Platform.CHATGPT -> Icons.Rounded.Spa
    Platform.GEMINI -> Icons.Rounded.AutoMode
    Platform.KIMI -> Icons.Rounded.NightsStay
    Platform.DEEPSEEK -> Icons.Rounded.Waves
    Platform.QWEN -> Icons.Rounded.Cloud
    Platform.GENERIC -> Icons.AutoMirrored.Rounded.Chat
}

/** Round tinted badge with the platform's own icon; used on Home and History rows. */
@Composable
fun PlatformBadge(platform: Platform, modifier: Modifier = Modifier, size: androidx.compose.ui.unit.Dp = 40.dp) {
    val style = platformStyle(platform)
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(style.tint),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = platformIcon(platform),
            contentDescription = platform.displayName,
            tint = style.color,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}

/** Small colored dot + platform name, used on Export header and banners. */
@Composable
fun PlatformChip(platform: Platform, modifier: Modifier = Modifier) {
    val style = platformStyle(platform)
    Row(
        modifier = modifier
            .clip(PillShape)
            .background(style.tint)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(style.color),
        )
        Text(
            text = platform.displayName,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = style.color,
            modifier = Modifier.padding(start = 6.dp),
        )
    }
}
