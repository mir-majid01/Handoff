package com.handoff.app.ui.screens.export

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.core.builder.PromptMode
import com.handoff.app.core.builder.TargetAi
import com.handoff.app.core.builder.TokenEstimator
import com.handoff.app.core.builder.TokenLimit
import com.handoff.app.ui.components.Card
import com.handoff.app.ui.components.ScreenTopBar
import com.handoff.app.ui.components.Segmented
import com.handoff.app.ui.components.SuccessToast
import com.handoff.app.ui.components.pressScale
import com.handoff.app.ui.components.rememberPressInteraction
import com.handoff.app.ui.theme.CardShape
import com.handoff.app.ui.theme.HandoffMotion
import com.handoff.app.ui.theme.MeterAmber
import com.handoff.app.ui.theme.SectionHeaderStyle
import com.handoff.app.ui.viewModelFactory
import com.handoff.app.util.Haptics
import com.handoff.app.util.formatTokens
import kotlinx.coroutines.launch

@Composable
fun ExportScreen(
    handoffId: String,
    onBack: () -> Unit,
    app: HandoffApp,
) {
    val viewModel: ExportViewModel = viewModel(factory = viewModelFactory { ExportViewModel(app) })
    val state by viewModel.state.collectAsState()
    val view = LocalView.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var successTrigger by remember { mutableIntStateOf(0) }
    var successMessage by remember { mutableStateOf("") }
    var promptExpanded by remember { mutableStateOf(false) }

    val copiedLabel = stringResource(R.string.export_copied)
    val savedLabel = stringResource(R.string.export_saved)

    LaunchedEffect(handoffId) { viewModel.load(handoffId) }

    val txtLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/plain"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                if (viewModel.writeText(uri)) {
                    Haptics.confirm(view)
                    successMessage = savedLabel
                    successTrigger++
                }
            }
        }
    }
    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf"),
    ) { uri ->
        if (uri != null) {
            scope.launch {
                if (viewModel.writePdfToUri(uri)) {
                    Haptics.confirm(view)
                    successMessage = savedLabel
                    successTrigger++
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        ) {
            ScreenTopBar(
                title = stringResource(R.string.export_title),
                onBack = onBack,
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // One grouped iOS-style section: style, target, limit.
                Card(contentPadding = androidx.compose.foundation.layout.PaddingValues(vertical = 4.dp)) {
                    GroupedSection(
                        header = stringResource(R.string.export_mode_label),
                        helper = state.mode.subtitle,
                    ) {
                        Segmented(
                            options = PromptMode.entries.map { it.displayName },
                            selectedIndex = PromptMode.entries.indexOf(state.mode),
                            onSelect = { index ->
                                Haptics.tap(view)
                                viewModel.setMode(PromptMode.entries[index])
                            },
                        )
                    }
                    SectionDivider()
                    GroupedSection(
                        header = stringResource(R.string.export_target_label),
                        helper = stringResource(R.string.export_helper_target),
                    ) {
                        Segmented(
                            options = TargetAi.entries.map { it.displayName },
                            selectedIndex = TargetAi.entries.indexOf(state.target),
                            onSelect = { index ->
                                Haptics.tap(view)
                                viewModel.setTarget(TargetAi.entries[index])
                            },
                        )
                    }
                    SectionDivider()
                    GroupedSection(
                        header = stringResource(R.string.export_limit_label),
                        helper = stringResource(R.string.export_helper_limit),
                    ) {
                        Segmented(
                            options = TokenLimit.entries.map { it.label },
                            selectedIndex = TokenLimit.entries.indexOf(state.limit),
                            onSelect = { index ->
                                Haptics.tap(view)
                                viewModel.setLimit(TokenLimit.entries[index])
                            },
                        )
                    }
                }

                state.prompt?.let { prompt ->
                    val chatTokens = state.conversation?.includedMessages?.sumOf {
                        TokenEstimator.estimateTokens(it.plainText)
                    } ?: 0
                    Card {
                        TokenMeter(
                            totalTokens = prompt.tokenEstimate,
                            chatTokens = chatTokens,
                            limit = state.limit,
                            wasSplit = prompt.wasSplit,
                            partCount = prompt.partCount,
                            messageCount = state.conversation?.includedMessages?.size ?: 0,
                        )
                    }

                    // Collapsible first-lines prompt preview.
                    Card(contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
                        Column(modifier = Modifier.animateContentSize(animationSpec = HandoffMotion.iosTween())) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = stringResource(R.string.export_preview_prompt),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f),
                                )
                                val lines = prompt.parts.firstOrNull()?.lines().orEmpty()
                                if (lines.size > 6) {
                                    Text(
                                        text = stringResource(
                                            if (promptExpanded) R.string.export_show_less else R.string.export_show_more,
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(10.dp))
                                            .clickable {
                                                Haptics.tap(view)
                                                promptExpanded = !promptExpanded
                                            }
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                    )
                                    Icon(
                                        imageVector = if (promptExpanded) {
                                            Icons.Rounded.ExpandLess
                                        } else {
                                            Icons.Rounded.ExpandMore
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp),
                                    )
                                }
                            }
                            Text(
                                text = prompt.parts.joinToString("\n\n"),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (promptExpanded) Int.MAX_VALUE else 6,
                                overflow = TextOverflow.Clip,
                                modifier = Modifier.padding(top = 8.dp),
                            )
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Pinned action bar: file row above, one big Copy button.
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        ActionTile(
                            icon = Icons.Rounded.Description,
                            label = stringResource(R.string.export_save_txt),
                            modifier = Modifier.weight(1f),
                        ) {
                            Haptics.tap(view)
                            txtLauncher.launch(viewModel.fileName("txt"))
                        }
                        ActionTile(
                            icon = Icons.Rounded.PictureAsPdf,
                            label = stringResource(R.string.export_save_pdf),
                            modifier = Modifier.weight(1f),
                        ) {
                            Haptics.tap(view)
                            pdfLauncher.launch(viewModel.fileName("pdf"))
                        }
                        ActionTile(
                            icon = Icons.Rounded.IosShare,
                            label = stringResource(R.string.export_share),
                            modifier = Modifier.weight(1f),
                        ) {
                            Haptics.tap(view)
                            runCatching { view.context.startActivity(viewModel.shareIntent()) }
                        }
                    }
                    CopyBanner(
                        label = stringResource(R.string.export_copy),
                        onClick = {
                            Haptics.confirm(view)
                            viewModel.copyToClipboard(view.context)
                            successMessage = copiedLabel
                            successTrigger++
                        },
                    )
                }
            }
        }

        SuccessToast(
            message = successMessage,
            trigger = successTrigger,
            modifier = Modifier.align(Alignment.Center),
        )
    }
}

@Composable
private fun GroupedSection(
    header: String,
    helper: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = header.uppercase(),
            style = SectionHeaderStyle,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        content()
        Text(
            text = helper,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SectionDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

/** "8.6k / 32k" meter bar: amber past 80%, red past the limit. */
@Composable
private fun TokenMeter(
    totalTokens: Int,
    chatTokens: Int,
    limit: TokenLimit,
    wasSplit: Boolean,
    partCount: Int,
    messageCount: Int,
) {
    val context = LocalContext.current
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.export_tokens, formatTokens(totalTokens)),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            val instructions = totalTokens - chatTokens
            if (instructions > 0) {
                Text(
                    text = stringResource(
                        R.string.export_instructions_extra,
                        formatTokens(instructions),
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                text = context.resources.getQuantityString(
                    R.plurals.preview_messages_count,
                    messageCount,
                    messageCount,
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(10.dp))
        val limitTokens = limit.tokens
        val fraction = if (limitTokens == null) {
            0f
        } else {
            (totalTokens.toFloat() / limitTokens).coerceIn(0f, 1f)
        }
        val overLimit = limitTokens != null && totalTokens > limitTokens
        val nearLimit = limitTokens != null && totalTokens > limitTokens * 0.8f
        if (limitTokens != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(
                            when {
                                overLimit -> MaterialTheme.colorScheme.error
                                nearLimit -> MeterAmber
                                else -> MaterialTheme.colorScheme.primary
                            },
                        ),
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(
                    R.string.export_progress,
                    formatTokens(totalTokens),
                    formatTokens(limitTokens),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = when {
                    overLimit -> MaterialTheme.colorScheme.error
                    nearLimit -> MeterAmber
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        } else {
            Text(
                text = stringResource(R.string.export_no_limit),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (wasSplit || overLimit) {
            Text(
                text = if (wasSplit) {
                    context.resources.getQuantityString(
                        R.plurals.export_split_banner,
                        partCount,
                        partCount,
                    )
                } else {
                    stringResource(R.string.export_split_suggestion)
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
    }
}

/** Large primary copy banner with press feedback. */
@Composable
private fun CopyBanner(label: String, onClick: () -> Unit) {
    val interaction = rememberPressInteraction()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.primary)
            .pressScale(interaction, 0.97f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 15.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.size(20.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(start = 8.dp),
        )
    }
}

@Composable
private fun ActionTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val interaction = rememberPressInteraction()
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CardShape)
            .pressScale(interaction, 0.97f)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(top = 6.dp),
        )
    }
}
