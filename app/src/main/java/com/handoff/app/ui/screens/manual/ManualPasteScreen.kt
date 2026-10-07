package com.handoff.app.ui.screens.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.ui.components.PrimaryButton
import com.handoff.app.ui.components.ScreenTopBar
import com.handoff.app.ui.components.cozyCard
import com.handoff.app.ui.theme.CardShape
import com.handoff.app.util.Haptics
import com.handoff.app.ui.viewModelFactory
import kotlinx.coroutines.launch

@Composable
fun ManualPasteScreen(
    onBuilt: (String) -> Unit,
    onBack: () -> Unit,
    app: HandoffApp,
) {
    val viewModel: ManualPasteViewModel = viewModel(factory = viewModelFactory { ManualPasteViewModel(app) })
    val text by viewModel.text.collectAsState()
    val showEmptyError by viewModel.showEmptyError.collectAsState()
    val view = LocalView.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        ScreenTopBar(
            title = stringResource(R.string.manual_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
                .cozyCard()
                .padding(16.dp),
        ) {
            Text(
                text = stringResource(R.string.manual_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BasicTextField(
                value = text,
                onValueChange = { viewModel.onTextChange(it) },
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    color = MaterialTheme.colorScheme.onSurface,
                ),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(top = 12.dp)
                    .verticalScroll(rememberScrollState()),
            )
        }

        if (showEmptyError) {
            Text(
                text = stringResource(R.string.manual_empty),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(horizontal = 24.dp),
            )
        }

        Box(
            modifier = Modifier
                .padding(20.dp)
                .navigationBarsPadding(),
        ) {
            PrimaryButton(
                text = stringResource(R.string.manual_build),
                onClick = {
                    Haptics.tap(view)
                    scope.launch {
                        viewModel.buildAndSave()?.let { onBuilt(it) }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
