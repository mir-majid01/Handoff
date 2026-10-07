package com.handoff.app.ui.screens.fetching

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.core.extraction.ExtractionAction
import com.handoff.app.core.extraction.ExtractionError
import com.handoff.app.core.extraction.FetchStage
import com.handoff.app.ui.components.PrimaryButton
import com.handoff.app.ui.components.SecondaryButton
import com.handoff.app.ui.components.cozyCard
import com.handoff.app.ui.components.shimmer
import com.handoff.app.ui.theme.CardShape
import com.handoff.app.ui.viewModelFactory

/**
 * Staged progress with real states — never a blank screen: connecting pulse,
 * stage list with checkmarks, error card with one clear action.
 */
@Composable
fun FetchingScreen(
    url: String,
    onSuccess: (String) -> Unit,
    onPasteManually: () -> Unit,
    onCancel: () -> Unit,
    app: HandoffApp,
) {
    val viewModel: FetchingViewModel = viewModel(factory = viewModelFactory { FetchingViewModel(app) })
    val state by viewModel.state.collectAsState()

    LaunchedEffect(url) { viewModel.start(url) }
    LaunchedEffect(state) {
        if (state is FetchUiState.Success) {
            onSuccess((state as FetchUiState.Success).handoffId)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedContent(
            targetState = state,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(180)) },
            label = "fetchState",
        ) { current ->
            when (current) {
                is FetchUiState.Running -> RunningCard(current.stage, url, onCancel)
                is FetchUiState.Failed -> ErrorCard(
                    error = current.error,
                    onRetry = { viewModel.start(url) },
                    onPasteManually = onPasteManually,
                    onBack = onCancel,
                )
                is FetchUiState.Success -> Spacer(Modifier.height(1.dp))
            }
        }
    }
}

@Composable
private fun RunningCard(stage: FetchStage, url: String, onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .cozyCard()
            .padding(24.dp)
            .animateContentSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ConnectingOrb()
        Spacer(Modifier.height(20.dp))
        Text(
            text = stringResource(R.string.fetching_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = url,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
        Spacer(Modifier.height(20.dp))
        StageList(currentStage = stage)
        Spacer(Modifier.height(20.dp))
        SecondaryButton(
            text = stringResource(R.string.fetching_cancel),
            onClick = onCancel,
        )
    }
}

@Composable
private fun ConnectingOrb() {
    val base = MaterialTheme.colorScheme.secondaryContainer
    val highlight = MaterialTheme.colorScheme.primaryContainer
    val transition = rememberInfiniteTransition(label = "orbPulse")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "orbScale",
    )
    Box(
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer {
                scaleX = pulse
                scaleY = pulse
            }
            .clip(CircleShape)
            .shimmer(base, highlight),
    )
}

@Composable
private fun StageList(currentStage: FetchStage) {
    val stages = listOf(
        FetchStage.CONNECTING to R.string.fetching_stage_connecting,
        FetchStage.READING to R.string.fetching_stage_reading,
        FetchStage.CLEANING to R.string.fetching_stage_cleaning,
        FetchStage.BUILDING to R.string.fetching_stage_building,
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        stages.forEach { (stage, labelRes) ->
            val reached = stage.ordinal <= currentStage.ordinal
            val completed = stage.ordinal < currentStage.ordinal
            Row(verticalAlignment = Alignment.CenterVertically) {
                val dotScale by animateFloatAsState(
                    targetValue = if (reached) 1f else 0.7f,
                    animationSpec = com.handoff.app.ui.theme.HandoffMotion.springSnappy,
                    label = "stageDot",
                )
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                reached -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            },
                        )
                        .graphicsLayer {
                            scaleX = dotScale
                            scaleY = dotScale
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (completed) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(12.dp),
                        )
                    }
                }
                Text(
                    text = stringResource(labelRes),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (reached) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
        }
    }
}

@Composable
private fun ErrorCard(
    error: ExtractionError,
    onRetry: () -> Unit,
    onPasteManually: () -> Unit,
    onBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .cozyCard()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Rounded.Close,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(40.dp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.fetching_failed_title),
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = error.userMessage,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(
                when (error.suggestedAction) {
                    ExtractionAction.RETRY -> R.string.fetching_retry
                    ExtractionAction.PASTE_MANUALLY -> R.string.fetching_paste_manually
                    ExtractionAction.CHECK_LINK -> R.string.fetching_back
                    ExtractionAction.OPEN_IN_BROWSER -> R.string.fetching_retry
                },
            ),
            onClick = when (error.suggestedAction) {
                ExtractionAction.RETRY, ExtractionAction.OPEN_IN_BROWSER -> onRetry
                ExtractionAction.PASTE_MANUALLY -> onPasteManually
                ExtractionAction.CHECK_LINK -> onBack
            },
        )
        if (error.suggestedAction == ExtractionAction.RETRY ||
            error.suggestedAction == ExtractionAction.CHECK_LINK
        ) {
            Spacer(Modifier.height(10.dp))
            SecondaryButton(
                text = stringResource(R.string.fetching_paste_manually),
                onClick = onPasteManually,
            )
        }
    }
}
