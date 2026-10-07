package com.handoff.app.ui.screens.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.core.model.Platform
import com.handoff.app.data.repo.HandoffEntry
import com.handoff.app.ui.components.EmptyState
import com.handoff.app.ui.components.IconButtonRound
import com.handoff.app.ui.components.PlatformBadge
import com.handoff.app.ui.components.PrimaryButton
import com.handoff.app.ui.components.ScreenTopBar
import com.handoff.app.ui.components.SearchField
import com.handoff.app.ui.components.cozyCard
import com.handoff.app.ui.components.pressScale
import com.handoff.app.ui.components.rememberPressInteraction
import com.handoff.app.ui.components.staggeredEntrance
import com.handoff.app.ui.theme.CardShape
import com.handoff.app.ui.theme.PillShape
import com.handoff.app.ui.theme.SectionHeaderStyle
import com.handoff.app.util.HandoffDisplay
import com.handoff.app.util.Haptics
import com.handoff.app.ui.viewModelFactory
import kotlinx.coroutines.launch

@Composable
fun HistoryScreen(
    onOpenHandoff: (String) -> Unit,
    onBack: () -> Unit,
    app: HandoffApp,
) {
    val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory { HistoryViewModel(app) })
    val state by viewModel.state.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedLabel = stringResource(R.string.history_deleted)
    val undoLabel = stringResource(R.string.history_undo)
    val copiedLabel = stringResource(R.string.history_copied)

    LaunchedEffect(state.deleted) {
        val pending = state.deleted ?: return@LaunchedEffect
        val result = snackbarHostState.showSnackbar(
            message = deletedLabel,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
        )
        if (result == SnackbarResult.ActionPerformed) {
            Haptics.tap(view)
            viewModel.undoDelete()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenTopBar(
                title = stringResource(R.string.history_title),
                onBack = onBack,
            )

            SearchField(
                query = state.query,
                onQuery = { viewModel.onSearch(it) },
                hint = stringResource(R.string.history_search_hint),
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
            )

            FilterRow(
                platforms = state.allPlatforms,
                selected = state.filter,
                onSelect = { viewModel.onFilter(it) },
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
            )

            if (state.entries.isEmpty()) {
                val blank = state.query.isBlank() && state.filter == null
                val pasteAction: (@Composable () -> Unit)? = if (blank) {
                    { PrimaryButton(text = stringResource(R.string.history_empty_cta), onClick = onBack) }
                } else {
                    null
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        title = stringResource(if (blank) R.string.history_empty else R.string.history_no_results),
                        caption = stringResource(if (blank) R.string.history_empty_caption else R.string.history_no_results_caption),
                        action = pasteAction,
                    )
                }
            } else {
                val sections = groupEntries(state.entries)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    sections.forEach { (bucket, items) ->
                        item(key = "header_${bucket.name}") {
                            Text(
                                text = stringResource(bucket.labelRes).uppercase(),
                                style = SectionHeaderStyle,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp, bottom = 2.dp),
                            )
                        }
                        items.forEach { entry ->
                            item(key = entry.id) {
                                HistoryRow(
                                    entry = entry,
                                    index = state.entries.indexOf(entry),
                                    onOpen = { Haptics.tap(view); onOpenHandoff(entry.id) },
                                    onDelete = {
                                        Haptics.reject(view)
                                        viewModel.deleteWithUndo(entry)
                                    },
                                    onCopy = {
                                        scope.launch {
                                            if (viewModel.copyPrompt(entry)) {
                                                Haptics.confirm(view)
                                                snackbarHostState.showSnackbar(copiedLabel)
                                            }
                                        }
                                    },
                                    onShare = {
                                        scope.launch {
                                            viewModel.shareIntent(entry)?.let {
                                                runCatching { context.startActivity(it) }
                                            }
                                        }
                                    },
                                )
                            }
                        }
                    }
                    item { Spacer(Modifier.height(64.dp)) }
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding(),
        )
    }
}

@Composable
private fun FilterRow(
    platforms: List<Platform>,
    selected: Platform?,
    onSelect: (Platform?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val allLabel = stringResource(R.string.history_filter_all)
    val chips = listOf<Pair<Platform?, String>>(null to allLabel) + platforms.map { it to it.displayName }
    val view = LocalView.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        chips.forEach { (platform, label) ->
            FilterChip(
                label = label,
                selected = selected == platform,
                onClick = { Haptics.tap(view); onSelect(platform) },
            )
        }
    }
}

@Composable
private fun FilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val interaction = rememberPressInteraction()
    val bg = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(PillShape)
            .background(bg)
            .border(
                1.dp,
                if (selected) Color.Transparent else MaterialTheme.colorScheme.outlineVariant,
                PillShape,
            )
            .pressScale(interaction)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 7.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            color = fg,
        )
    }
}

private fun groupEntries(entries: List<HandoffEntry>): List<Pair<HistoryGroup, List<HandoffEntry>>> {
    fun toGroup(entry: HandoffEntry): HistoryGroup = when (HandoffDisplay.bucket(entry)) {
        HandoffDisplay.DayBucket.TODAY -> HistoryGroup.TODAY
        HandoffDisplay.DayBucket.YESTERDAY -> HistoryGroup.YESTERDAY
        HandoffDisplay.DayBucket.EARLIER -> HistoryGroup.EARLIER
    }
    val buckets = entries.map { it to toGroup(it) }
    return HistoryGroup.entries.mapNotNull { group ->
        val items = buckets.filter { it.second == group }.map { it.first }
        if (items.isEmpty()) null else group to items
    }
}

enum class HistoryGroup {
    TODAY, YESTERDAY, EARLIER;
    val labelRes: Int
        get() = when (this) {
            TODAY -> R.string.history_group_today
            YESTERDAY -> R.string.history_group_yesterday
            EARLIER -> R.string.history_group_earlier
        }
}

@Composable
private fun HistoryRow(
    entry: HandoffEntry,
    index: Int,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
    onCopy: () -> Unit,
    onShare: () -> Unit,
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
    val reveal = if (dismissState.targetValue == SwipeToDismissBoxValue.EndToStart) 1f else 0f
    SwipeToDismissBox(
        state = dismissState,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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
        var menuOpen by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .staggeredEntrance(index)
                .pressScale(interaction)
                .cozyCard()
                .clickable(interactionSource = interaction, indication = null, onClick = onOpen)
                .heightIn(min = 76.dp)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            PlatformBadge(platform = entry.platform, size = 44.dp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = HandoffDisplay.titleFor(entry),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = metaLine(entry),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            Box {
                IconButtonRound(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.history_menu),
                    onClick = { menuOpen = true },
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = MaterialTheme.colorScheme.surface,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_action_copy)) },
                        onClick = { menuOpen = false; onCopy() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.history_action_share)) },
                        onClick = { menuOpen = false; onShare() },
                    )
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = stringResource(R.string.history_action_delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = { menuOpen = false; onDelete() },
                    )
                }
            }
        }
    }
}

@Composable
private fun metaLine(entry: HandoffEntry): String {
    val context = LocalContext.current
    val messages = context.resources.getQuantityString(
        R.plurals.preview_messages_count,
        entry.messageCount,
        entry.messageCount,
    )
    return "${entry.platform.displayName} · $messages · ${HandoffDisplay.time(entry)}"
}
