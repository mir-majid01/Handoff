package com.handoff.app.ui.screens.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.ui.components.GhostButton
import com.handoff.app.ui.components.PrimaryButton
import com.handoff.app.ui.components.platformStyle
import com.handoff.app.core.model.Platform
import com.handoff.app.ui.theme.HandoffMotion
import com.handoff.app.ui.theme.PillShape
import com.handoff.app.util.Haptics
import kotlinx.coroutines.launch

/** Three swipeable cards: Paste link → We read it → Copy to any AI. Skippable. */
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    app: HandoffApp,
) {
    val scope = rememberCoroutineScope()
    val view = LocalView.current
    androidx.activity.compose.BackHandler { onFinish() }
    val pages = listOf(
        OnboardingPage(R.string.onboarding_1_title, R.string.onboarding_1_body, IllustrationKind.LINK),
        OnboardingPage(R.string.onboarding_2_title, R.string.onboarding_2_body, IllustrationKind.READ),
        OnboardingPage(R.string.onboarding_3_title, R.string.onboarding_3_body, IllustrationKind.COPY),
    )
    val pagerState = rememberPagerState(pageCount = { pages.size })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .padding(20.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Spacer(Modifier.weight(1f))
            GhostButton(
                text = stringResource(R.string.onboarding_skip),
                onClick = onFinish,
            )
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
        ) { page ->
            OnboardingCard(page = pages[page])
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pages.size) { index ->
                val selected = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (selected) 8.dp else 6.dp)
                        .clip(CircleShape)
                        .background(
                            if (selected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        PrimaryButton(
            text = stringResource(
                if (pagerState.currentPage == pages.size - 1) {
                    R.string.onboarding_start
                } else {
                    R.string.onboarding_next
                },
            ),
            onClick = {
                Haptics.tap(view)
                if (pagerState.currentPage == pages.size - 1) {
                    onFinish()
                } else {
                    scope.launch {
                        pagerState.animateScrollToPage(
                            pagerState.currentPage + 1,
                            animationSpec = HandoffMotion.iosTween(),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
        )
    }
}

private data class OnboardingPage(
    val titleRes: Int,
    val bodyRes: Int,
    val illustration: IllustrationKind,
)

private enum class IllustrationKind { LINK, READ, COPY }

@Composable
private fun OnboardingCard(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 140.dp, max = 240.dp)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surface),
            contentAlignment = Alignment.Center,
        ) {
            Illustration(kind = page.illustration)
        }
        Spacer(Modifier.height(24.dp))
        Text(
            text = stringResource(page.titleRes),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(page.bodyRes),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
private fun Illustration(kind: IllustrationKind) {
    val sage = MaterialTheme.colorScheme.primary
    val soft = MaterialTheme.colorScheme.primaryContainer
    val claude = platformStyle(Platform.CLAUDE).color
    val chatgpt = platformStyle(Platform.CHATGPT).color
    val gemini = platformStyle(Platform.GEMINI).color
    Canvas(modifier = Modifier.size(160.dp)) {
        when (kind) {
            IllustrationKind.LINK -> {
                // Three platform dots connected by a chain into one bubble.
                val dots = listOf(claude, chatgpt, gemini)
                dots.forEachIndexed { index, color ->
                    val y = size.height * (0.2f + index * 0.3f)
                    drawCircle(color = color, radius = 14f, center = Offset(size.width * 0.2f, y))
                    drawLine(
                        color = soft,
                        start = Offset(size.width * 0.2f + 18f, y),
                        end = Offset(size.width * 0.62f, size.height * 0.5f),
                        strokeWidth = 5f,
                    )
                }
                drawRoundRect(
                    color = sage,
                    topLeft = Offset(size.width * 0.62f, size.height * 0.34f),
                    size = Size(size.width * 0.3f, size.height * 0.32f),
                    cornerRadius = CornerRadius(24f, 24f),
                )
            }
            IllustrationKind.READ -> {
                // A page with lines being scanned by a sweeping bar.
                drawRoundRect(
                    color = soft,
                    topLeft = Offset(size.width * 0.2f, size.height * 0.12f),
                    size = Size(size.width * 0.6f, size.height * 0.76f),
                    cornerRadius = CornerRadius(20f, 20f),
                )
                for (i in 0 until 5) {
                    drawRoundRect(
                        color = sage.copy(alpha = 0.7f),
                        topLeft = Offset(size.width * 0.3f, size.height * (0.24f + i * 0.11f)),
                        size = Size(size.width * (0.4f - (i % 2) * 0.1f), 8f),
                        cornerRadius = CornerRadius(4f, 4f),
                    )
                }
                drawRoundRect(
                    color = sage,
                    topLeft = Offset(size.width * 0.16f, size.height * 0.42f),
                    size = Size(size.width * 0.68f, 10f),
                    cornerRadius = CornerRadius(5f, 5f),
                )
            }
            IllustrationKind.COPY -> {
                // Two overlapping bubbles with a check, one filled one outlined.
                drawRoundRect(
                    color = soft,
                    topLeft = Offset(size.width * 0.12f, size.height * 0.16f),
                    size = Size(size.width * 0.5f, size.height * 0.38f),
                    cornerRadius = CornerRadius(24f, 24f),
                    style = Stroke(width = 6f),
                )
                drawRoundRect(
                    color = sage,
                    topLeft = Offset(size.width * 0.38f, size.height * 0.46f),
                    size = Size(size.width * 0.5f, size.height * 0.38f),
                    cornerRadius = CornerRadius(24f, 24f),
                )
                drawLine(
                    color = androidx.compose.ui.graphics.Color.White,
                    start = Offset(size.width * 0.52f, size.height * 0.65f),
                    end = Offset(size.width * 0.60f, size.height * 0.73f),
                    strokeWidth = 6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                drawLine(
                    color = androidx.compose.ui.graphics.Color.White,
                    start = Offset(size.width * 0.60f, size.height * 0.73f),
                    end = Offset(size.width * 0.78f, size.height * 0.55f),
                    strokeWidth = 6f,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
        }
    }
}
