package com.handoff.app.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.handoff.app.BuildConfig
import com.handoff.app.HandoffApp
import com.handoff.app.R
import com.handoff.app.core.builder.PromptMode
import com.handoff.app.core.builder.TargetAi
import com.handoff.app.core.builder.TokenLimit
import com.handoff.app.data.prefs.ThemeMode
import com.handoff.app.ui.components.Card
import com.handoff.app.ui.components.ScreenTopBar
import com.handoff.app.ui.components.Segmented
import com.handoff.app.ui.theme.HandoffMotion
import com.handoff.app.util.Haptics
import com.handoff.app.ui.viewModelFactory

private const val DEVELOPER_GITHUB_URL = "https://github.com/editsu4k-coder"

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    app: HandoffApp,
) {
    val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory { SettingsViewModel(app) })
    val settings by viewModel.settings.collectAsState(initial = null)
    val view = LocalView.current
    val uriHandler = LocalUriHandler.current
    var showClearDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState()),
    ) {
        ScreenTopBar(
            title = stringResource(R.string.settings_title),
            onBack = onBack,
        )

        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            settings?.let { current ->
                Card {
                    Text(
                        text = stringResource(R.string.settings_theme),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    Segmented(
                        options = listOf(
                            stringResource(R.string.settings_theme_system),
                            stringResource(R.string.settings_theme_light),
                            stringResource(R.string.settings_theme_dark),
                        ),
                        selectedIndex = ThemeMode.entries.indexOf(current.themeMode),
                        onSelect = { index ->
                            Haptics.tap(view)
                            viewModel.setTheme(ThemeMode.entries[index])
                        },
                    )
                }

                Card {
                    Text(
                        text = stringResource(R.string.settings_default_mode),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    Segmented(
                        options = PromptMode.entries.map { it.displayName },
                        selectedIndex = PromptMode.entries.indexOf(current.defaultMode),
                        onSelect = { index ->
                            Haptics.tap(view)
                            viewModel.setDefaultMode(PromptMode.entries[index])
                        },
                    )
                }

                Card {
                    Text(
                        text = stringResource(R.string.settings_default_target),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    Segmented(
                        options = TargetAi.entries.map { it.displayName },
                        selectedIndex = TargetAi.entries.indexOf(current.defaultTarget),
                        onSelect = { index ->
                            Haptics.tap(view)
                            viewModel.setDefaultTarget(TargetAi.entries[index])
                        },
                    )
                }

                Card {
                    Text(
                        text = stringResource(R.string.settings_token_limit),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.padding(top = 10.dp))
                    Segmented(
                        options = TokenLimit.entries.map { it.label },
                        selectedIndex = TokenLimit.entries.indexOf(current.tokenLimit),
                        onSelect = { index ->
                            Haptics.tap(view)
                            viewModel.setTokenLimit(TokenLimit.entries[index])
                        },
                    )
                }

                Card {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.settings_haptics),
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = stringResource(R.string.settings_haptics_caption),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = current.hapticsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled) Haptics.tap(view)
                                viewModel.setHaptics(enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                        )
                    }
                }

                Card {
                    TextButton(onClick = { showClearDialog = true }) {
                        Text(
                            text = stringResource(R.string.settings_clear_history),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.labelLarge,
                        )
                    }
                }
            }

            Card {
                Text(
                    text = stringResource(R.string.settings_about_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(R.string.settings_about_body),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(R.string.settings_version, viewModel.version),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 8.dp),
                )
                Text(
                    text = stringResource(R.string.settings_developer),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(top = 12.dp),
                )
                Text(
                    text = stringResource(R.string.settings_github),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .clickable { uriHandler.openUri(DEVELOPER_GITHUB_URL) }
                        .padding(vertical = 4.dp),
                )
            }

            Spacer(Modifier.padding(bottom = 32.dp))
        }
    }

    if (showClearDialog) {
        AlertDialog(
            onDismissRequest = { showClearDialog = false },
            title = { Text(stringResource(R.string.settings_clear_confirm_title)) },
            text = { Text(stringResource(R.string.settings_clear_confirm_body)) },
            confirmButton = {
                TextButton(onClick = {
                    showClearDialog = false
                    viewModel.clearHistory { }
                }) {
                    Text(
                        stringResource(R.string.settings_clear_confirm_yes),
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDialog = false }) {
                    Text(stringResource(R.string.settings_cancel))
                }
            },
        )
    }
}
