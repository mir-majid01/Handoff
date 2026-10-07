package com.handoff.app.ui.screens.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.core.model.Platform
import com.handoff.app.data.repo.HandoffEntry
import com.handoff.app.ui.components.EmptyState
import com.handoff.app.ui.components.HandoffLogoMark
import com.handoff.app.ui.components.HandoffPage
import com.handoff.app.ui.components.IconButtonRound
import com.handoff.app.ui.components.PlatformBadge
import com.handoff.app.ui.components.PrimaryButton
import com.handoff.app.ui.components.SecondaryButton
import com.handoff.app.ui.components.cozyCard
import com.handoff.app.ui.components.pressScale
import com.handoff.app.ui.components.rememberPressInteraction
import com.handoff.app.ui.components.staggeredEntrance
import com.handoff.app.ui.theme.CardShape
import com.handoff.app.ui.theme.HandoffMotion
import com.handoff.app.ui.theme.PillShape
import com.handoff.app.ui.theme.SectionHeaderStyle
import com.handoff.app.util.Haptics
import com.handoff.app.util.HandoffDisplay
import com.handoff.app.ui.viewModelFactory

@Composable
fun HomeScreen(
    onStartFetching: (String) -> Unit,
    onOpenHandoff: (String) -> Unit,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenManualPaste: () -> Unit,
    app: HandoffApp,
) {
    val viewModel: HomeViewModel = viewModel(factory = viewModelFactory { HomeViewModel(app) })
    val text by viewModel.text.collectAsState()
    val platform by viewModel.detectedPlatform.collectAsState()
    val clipboardLink by viewModel.clipboardLink.collectAsState()
    val showInvalid by viewModel.showInvalid.collectAsState()
    val history by viewModel.history.collectAsState()
    val view = LocalView.current

    LaunchedEffect(Unit) { viewModel.checkClipboardForBanner() }

    HandoffPage(
        title = stringResource(R.string.app_name),
        subtitle = stringResource(R.string.home_tagline),
        logo = { HandoffLogoMark() },
        topBarTrailing = {
            IconButtonRound(
                icon = Icons.Rounded.History,
                contentDescription = stringResource(R.string.cd_history),
                onClick = onOpenHistory,
            )
            IconButtonRound(
                icon = Icons.Rounded.Settings,
                contentDescription = stringResource(R.string.cd_settings),
                onClick = onOpenSettings,
            )
        },
    ) {
        val recent = history.take(3)
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            item(key = "banner") {
                ClipboardBanner(
                    clipboardLink = clipboardLink,
                    onUse = {
                        Haptics.tap(view)
                        viewModel.useClipboardBannerLink()
                        viewModel.startIfValid()?.let { onStartFetching(viewModel.text.value) }
                    },
                    onDismiss = { viewModel.dismissClipboardBanner() },
                )
            }
            item(key = "input") {
                LinkInputCard(
                    text = text,
                    platform = platform,
                    showInvalid = showInvalid,
                    onTextChanged = { viewModel.onTextChanged(it) },
                    onPaste = { viewModel.readClipboard() },
                    onContinue = {
                        Haptics.tap(view)
                        val url = viewModel.text.value
                        if (viewModel.startIfValid() != null) onStartFetching(url)
                    },
                )
            }
            // Only offer the manual fallback after the link was rejected.
            item(key = "manual") {
                AnimatedVisibility(
                    visible = showInvalid,
                    enter = fadeIn(HandoffMotion.iosTween()) +
                        slideInVertically(HandoffMotion.iosTween()) { it / 2 },
                    exit = fadeOut(HandoffMotion.iosTween()),
                ) {
                    Text(
                        text = stringResource(R.string.manual_open),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .clip(PillShape)
                            .clickable(
                                interactionSource = rememberPressInteraction(),
                                indication = null,
                            ) { onOpenManualPaste() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                    )
                }
            }
            item(key = "recent-header") {
                if (history.isNotEmpty()) {
                    Text(
                        text = stringResource(R.string.home_recent_title).uppercase(),
                        style = SectionHeaderStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp, bottom = 2.dp),
                    )
                }
            }
            items(recent, key = { it.id }) { entry ->
                RecentHandoffRow(
                    entry = entry,
                    index = recent.indexOf(entry),
                    onClick = { onOpenHandoff(entry.id) },
                    onDelete = {
                        Haptics.reject(view)
                        viewModel.delete(entry)
                    },
                )
            }
            if (history.size > recent.size) {
                item(key = "see-all") {
                    val seeAllInteraction = rememberPressInteraction()
                    Text(
                        text = stringResource(R.string.home_see_all),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clip(PillShape)
                            .pressScale(seeAllInteraction)
                            .clickable(interactionSource = seeAllInteraction, indication = null) {
                                Haptics.tap(view)
                                onOpenHistory()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                    )
                }
            }
            if (history.isEmpty()) {
                item(key = "empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            title = stringResource(R.string.home_empty_recent),
                            caption = stringResource(R.string.home_empty_tip),
                        )
                    }
                }
            }
            item(key = "bottom-space") { Spacer(Modifier.height(72.dp)) }
        }
    }
}

@Composable
private fun ClipboardBanner(
    clipboardLink: Pair<String, Platform>?,
    onUse: () -> Unit,
    onDismiss: () -> Unit,
) {
    AnimatedVisibility(
        visible = clipboardLink != null,
        enter = slideInVertically(HandoffMotion.iosTween()) { -it } + fadeIn(HandoffMotion.iosTween()),
        exit = slideOutVertically(HandoffMotion.iosTween()) { -it } + fadeOut(HandoffMotion.iosTween()),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(CardShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(
                    R.string.home_clipboard_banner,
                    clipboardLink?.second?.displayName ?: "",
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = stringResource(R.string.home_clipboard_cta),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(PillShape)
                    .clickable(onClick = onUse)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            )
            IconButtonRound(
                icon = Icons.Rounded.Clear,
                contentDescription = stringResource(R.string.cd_clear_search),
                onClick = onDismiss,
                modifier = Modifier.size(36.dp),
            )
        }
    }
}

@Composable
private fun LinkInputCard(
    text: String,
    platform: Platform?,
    showInvalid: Boolean,
    onTextChanged: (String) -> Unit,
    onPaste: () -> Unit,
    onContinue: () -> Unit,
) {
    val view = LocalView.current
    var focused by remember { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val fieldShape = RoundedCornerShape(14.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .padding(16.dp),
    ) {
        // Outlined container with clear button and a shortened read-back when unfocused.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(fieldShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.dp,
                    if (showInvalid) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.outline,
                    fieldShape,
                )
                .padding(horizontal = 14.dp, vertical = 12.dp),
        ) {
            BasicTextField(
                value = text,
                onValueChange = onTextChanged,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Uri,
                    imeAction = ImeAction.Go,
                ),
                keyboardActions = KeyboardActions(onGo = {
                    if (platform != null) onContinue()
                }),
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text(
                                text = stringResource(R.string.home_input_hint),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .focusRequester(focusRequester)
                    .onFocusEvent { focused = it.isFocused }
                    .padding(
                        end = when {
                            platform != null -> 70.dp
                            text.isNotEmpty() -> 32.dp
                            else -> 0.dp
                        },
                    )
                    .graphicsLayer {
                        // Hide the raw text while the short-form overlay stands in.
                        alpha = if (focused || text.length <= 46) 1f else 0f
                    },
            )
            if (!focused && text.length > 46) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            focusRequester.requestFocus()
                            focused = true
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = shortenUrl(text),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Clip,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                }
            }
            Row(
                modifier = Modifier.align(Alignment.CenterEnd),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (text.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Rounded.Clear,
                        contentDescription = stringResource(R.string.home_clear_link),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .size(20.dp)
                            .clip(PillShape)
                            .clickable { onTextChanged("") },
                    )
                }
                if (platform != null) {
                    PlatformBadge(
                        platform = platform,
                        modifier = Modifier.padding(start = 8.dp),
                        size = 26.dp,
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            SecondaryButton(
                text = stringResource(R.string.home_paste),
                onClick = {
                    Haptics.tap(view)
                    onPaste()
                },
            )
            Spacer(Modifier.weight(1f))
            PrimaryButton(
                text = stringResource(R.string.home_go),
                enabled = platform != null,
                onClick = onContinue,
            )
        }
        AnimatedVisibility(visible = showInvalid) {
            Text(
                text = stringResource(R.string.home_invalid_link),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Keeps the real text selectable while showing only host/…tail on screen. */
private fun shortenUrl(raw: String): String {
    val token = raw.trim().split(Regex("\\s+")).firstOrNull { it.contains("://") } ?: return raw
    val uri = runCatching { java.net.URI(token) }.getOrNull() ?: return raw.take(46)
    val host = (uri.host ?: "").removePrefix("www.")
    val path = uri.path.orEmpty()
    return when {
        path.length > 16 -> "$host/${path.take(9)}…${path.takeLast(5)}"
        else -> "$host$path"
    }
}

@Composable
private fun RecentHandoffRow(
    entry: HandoffEntry,
    index: Int,
    onClick: () -> Unit,
    onDelete: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.EndToStart) {
                onDelete()
                true
            } else {
                false
            }
        },
    )
    // Reveal the red delete layer only once the swipe is engaged, so it never leaks at rest.
    val reveal = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CardShape)
                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = reveal))
                    .padding(horizontal = 24.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.cd_delete),
                    tint = MaterialTheme.colorScheme.onErrorContainer,
                    modifier = Modifier.graphicsLayer { alpha = reveal },
                )
            }
        },
        enableDismissFromStartToEnd = false,
    ) {
        val interaction = rememberPressInteraction()
        val context = LocalContext.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredEntrance(index)
                .pressScale(interaction)
                .cozyCard()
                .clickable(interactionSource = interaction, indication = null, onClick = onClick)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlatformBadge(platform = entry.platform, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HandoffDisplay.titleFor(entry),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = context.resources.getQuantityString(
                        R.plurals.preview_messages_count,
                        entry.messageCount,
                        entry.messageCount,
                    ) + " · " + HandoffDisplay.time(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
