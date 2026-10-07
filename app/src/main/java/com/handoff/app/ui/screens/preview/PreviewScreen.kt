package com.handoff.app.ui.screens.preview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.core.model.Block
import com.handoff.app.core.model.Message
import com.handoff.app.core.model.Role
import com.handoff.app.ui.components.PrimaryButton
import com.handoff.app.ui.components.ScreenTopBar
import com.handoff.app.ui.components.SearchField
import com.handoff.app.ui.components.staggeredEntrance
import com.handoff.app.ui.viewModelFactory
import com.handoff.app.util.Haptics
import com.handoff.app.util.formatTokens

@Composable
fun PreviewScreen(
    handoffId: String,
    onContinue: (String) -> Unit,
    onBack: () -> Unit,
    app: HandoffApp,
) {
    val viewModel: PreviewViewModel = viewModel(factory = viewModelFactory { PreviewViewModel(app) })
    val state by viewModel.state.collectAsState()
    val view = LocalView.current

    LaunchedEffect(handoffId) { viewModel.load(handoffId) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(
            title = state.conversation?.title ?: stringResource(R.string.preview_title),
            onBack = onBack,
        )
        val context = LocalContext.current
        Text(
            text = buildString {
                append(
                    context.resources.getQuantityString(
                        R.plurals.preview_messages_count,
                        state.includedCount,
                        state.includedCount,
                    ),
                )
                if (state.excludedCount > 0) {
                    append(" · ")
                    append(
                        context.resources.getQuantityString(
                            R.plurals.preview_excluded_count,
                            state.excludedCount,
                            state.excludedCount,
                        ),
                    )
                }
                append(" · ")
                append(stringResource(R.string.preview_token_estimate, formatTokens(state.tokenEstimate)))
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 8.dp),
        )
        SearchField(
            query = state.searchQuery,
            onQuery = { viewModel.onSearch(it) },
            hint = stringResource(R.string.preview_search_hint),
            modifier = Modifier.padding(horizontal = 20.dp),
        )

        val backgroundColor = MaterialTheme.colorScheme.background
        Box(modifier = Modifier.weight(1f)) {
            if (state.visibleMessages.isEmpty() && state.conversation != null) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.SearchOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.outline,
                        modifier = Modifier.size(36.dp),
                    )
                    Text(
                        text = stringResource(R.string.preview_empty),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    items(state.visibleMessages, key = { (index, _) -> index }) { (index, message) ->
                        MessageBubble(
                            message = message,
                            excluded = message.excluded,
                            codeCollapsed = state.collapsedCode.contains(index),
                            platformLabel = state.conversation?.platform?.displayName
                                ?: stringResource(R.string.preview_title),
                            onToggleExcluded = {
                                Haptics.tap(view)
                                viewModel.toggleExcluded(index)
                            },
                            onToggleCode = { viewModel.toggleCodeCollapsed(index) },
                            index = index,
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
                // Soft fades so messages dissolve at both ends of the list.
                Box(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .height(20.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(backgroundColor, backgroundColor.copy(alpha = 0f)),
                            ),
                        ),
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(28.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(backgroundColor.copy(alpha = 0f), backgroundColor),
                            ),
                        ),
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
            shadowElevation = 8.dp,
        ) {
            Row(
                modifier = Modifier
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = context.resources.getQuantityString(
                        R.plurals.preview_selected_count,
                        state.includedCount,
                        state.includedCount,
                        state.conversation?.messages?.size ?: state.includedCount,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                PrimaryButton(
                    text = stringResource(R.string.preview_continue_count, state.includedCount),
                    onClick = {
                        Haptics.tap(view)
                        viewModel.saveChanges(handoffId)
                        onContinue(handoffId)
                    },
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: Message,
    excluded: Boolean,
    codeCollapsed: Boolean,
    platformLabel: String,
    onToggleExcluded: () -> Unit,
    onToggleCode: () -> Unit,
    index: Int,
) {
    val isUser = message.role == Role.USER
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .staggeredEntrance(index.coerceAtMost(10))
            .alpha(if (excluded) 0.4f else 1f),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        Text(
            text = if (isUser) stringResource(R.string.preview_you) else platformLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!isUser) {
                ExcludeToggle(excluded = excluded, onToggle = onToggleExcluded)
            }
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .clip(
                        RoundedCornerShape(
                            topStart = 18.dp,
                            topEnd = 18.dp,
                            bottomStart = if (isUser) 18.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 18.dp,
                        ),
                    )
                    .background(
                        if (isUser) {
                            MaterialTheme.colorScheme.surfaceVariant
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                    )
                    .then(
                        if (isUser) {
                            Modifier
                        } else {
                            Modifier.border(
                                1.dp,
                                MaterialTheme.colorScheme.outlineVariant,
                                RoundedCornerShape(
                                    topStart = 18.dp,
                                    topEnd = 18.dp,
                                    bottomStart = 4.dp,
                                    bottomEnd = 18.dp,
                                ),
                            )
                        },
                    )
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            ) {
                message.blocks.forEachIndexed { blockIndex, block ->
                    BlockView(
                        block = block,
                        isUser = isUser,
                        collapsed = codeCollapsed,
                        onToggleCode = onToggleCode,
                        key = index * 100 + blockIndex,
                    )
                }
            }
            if (isUser) {
                ExcludeToggle(excluded = excluded, onToggle = onToggleExcluded)
            }
        }
    }
}

/** 24dp circular selector: visible stroke, sage checkmark when included. */
@Composable
private fun ExcludeToggle(excluded: Boolean, onToggle: () -> Unit) {
    val label = stringResource(
        if (excluded) R.string.cd_include_message else R.string.cd_exclude_message,
    )
    Box(
        modifier = Modifier
            .size(48.dp)
            .semantics {
                contentDescription = label
                if (excluded) stateDescription = "Hidden"
            }
            .clickable(onClick = onToggle),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(
                    1.5.dp,
                    if (excluded) {
                        MaterialTheme.colorScheme.outline
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (!excluded) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp),
                )
            }
        }
    }
}

@Composable
private fun BlockView(
    block: Block,
    isUser: Boolean,
    collapsed: Boolean,
    onToggleCode: () -> Unit,
    key: Int,
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    when (block) {
        is Block.Text -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        is Block.Heading -> Text(
            text = block.text,
            style = MaterialTheme.typography.titleMedium,
            color = textColor,
            modifier = Modifier.padding(vertical = 2.dp),
        )
        is Block.ListBlock -> Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            block.items.forEachIndexed { itemIndex, item ->
                Row(modifier = Modifier.padding(vertical = 1.dp)) {
                    if (block.ordered) {
                        Text(
                            text = "${itemIndex + 1}.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = textColor,
                            modifier = Modifier.width(20.dp),
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(textColor.copy(alpha = 0.6f)),
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    // Hanging indent: wrapped lines align under the text, not the bullet.
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
        is Block.Code -> Column(
            modifier = Modifier
                .padding(vertical = 4.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.secondaryContainer)
                .border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    RoundedCornerShape(12.dp),
                )
                .clickable(onClick = onToggleCode)
                .padding(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.Code,
                    contentDescription = stringResource(
                        if (collapsed) R.string.preview_expand_code else R.string.preview_collapse_code,
                    ),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    text = block.language.ifBlank { "code" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            if (!collapsed) {
                Text(
                    text = block.content,
                    style = MaterialTheme.typography.bodySmall,
                    color = textColor,
                    maxLines = 12,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        }
        is Block.Image -> Text(
            text = "*[image: ${block.alt}]*",
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            modifier = Modifier.padding(vertical = 2.dp),
        )
    }
}
