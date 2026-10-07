package com.handoff.app.ui.navigation

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.handoff.app.HandoffApp
import com.handoff.app.data.prefs.AppSettings
import com.handoff.app.ui.screens.export.ExportScreen
import com.handoff.app.ui.screens.fetching.FetchingScreen
import com.handoff.app.ui.screens.history.HistoryScreen
import com.handoff.app.ui.screens.home.HomeScreen
import com.handoff.app.ui.screens.manual.ManualPasteScreen
import com.handoff.app.ui.screens.onboarding.OnboardingScreen
import com.handoff.app.ui.screens.preview.PreviewScreen
import com.handoff.app.ui.screens.settings.SettingsScreen
import com.handoff.app.ui.theme.HandoffMotion
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * iOS-style navigation transitions: on push the incoming page slides in from
 * the right while the outgoing one drifts left and fades; on pop the reverse.
 * Start destination flips to Onboarding until the user completes it once.
 */
private const val PUSH_FRACTION = 1f
private const val DRIFT_FRACTION = 0.28f

private fun <T> navTween(duration: Int = HandoffMotion.DURATION_NAV) =
    tween<T>(duration, easing = HandoffMotion.EaseIOSEmphasized)

private fun pushEnter(): EnterTransition =
    slideInHorizontally(navTween()) { (it * PUSH_FRACTION).toInt() } +
        fadeIn(navTween(HandoffMotion.DURATION_MEDIUM))

private fun pushExit(): ExitTransition =
    slideOutHorizontally(navTween()) { -(it * DRIFT_FRACTION).toInt() } +
        fadeOut(navTween(HandoffMotion.DURATION_MEDIUM))

private fun popEnter(): EnterTransition =
    slideInHorizontally(navTween()) { -(it * DRIFT_FRACTION).toInt() } +
        fadeIn(navTween(HandoffMotion.DURATION_MEDIUM))

private fun popExit(): ExitTransition =
    slideOutHorizontally(navTween()) { (it * PUSH_FRACTION).toInt() } +
        fadeOut(navTween(HandoffMotion.DURATION_MEDIUM))

@Composable
fun HandoffNavHost(
    app: HandoffApp,
    initialSharedUrl: String? = null,
    navController: NavHostController = rememberNavController(),
) {
    val settings by app.settingsStore.settings.collectAsState(initial = AppSettings())
    val scope = rememberCoroutineScope()

    // The start destination is decided once, from the first real DataStore
    // emission, so returning users land on Home with no onboarding flash.
    var startDestination by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        val first = app.settingsStore.settings.first()
        startDestination = if (first.onboardingDone) Routes.HOME else Routes.ONBOARDING
    }

    val settledStart = startDestination
    if (settledStart == null) {
        // Themed hold screen for the few ms before DataStore's first emission.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
        )
        return
    }

    // When onboarding completes in place, swap to Home and drop it from the back stack.
    LaunchedEffect(settings.onboardingDone) {
        if (settings.onboardingDone &&
            navController.currentDestination?.route == Routes.ONBOARDING
        ) {
            navController.navigate(Routes.HOME) {
                popUpTo(Routes.ONBOARDING) { inclusive = true }
            }
        }
    }

    // Incoming ACTION_SEND share: jump straight to Fetching.
    LaunchedEffect(initialSharedUrl) {
        if (!initialSharedUrl.isNullOrBlank() && settings.onboardingDone) {
            navController.navigate(Routes.fetching(initialSharedUrl))
        }
    }

    NavHost(
        navController = navController,
        startDestination = settledStart,
        enterTransition = { pushEnter() },
        exitTransition = { pushExit() },
        popEnterTransition = { popEnter() },
        popExitTransition = { popExit() },
    ) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onFinish = {
                    scope.launch { app.settingsStore.setOnboardingDone(true) }
                },
                app = app,
            )
        }
        composable(Routes.HOME) {
            HomeScreen(
                onStartFetching = { url -> navController.navigate(Routes.fetching(url)) },
                onOpenHandoff = { id -> navController.navigate(Routes.preview(id)) },
                onOpenHistory = { navController.navigate(Routes.HISTORY) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenManualPaste = { navController.navigate(Routes.MANUAL_PASTE) },
                app = app,
            )
        }
        composable(
            route = Routes.FETCHING,
            arguments = listOf(navArgument("url") { type = NavType.StringType }),
        ) { entry ->
            val url = entry.arguments?.getString("url").orEmpty()
            FetchingScreen(
                url = url,
                onSuccess = { handoffId ->
                    navController.navigate(Routes.preview(handoffId)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onPasteManually = {
                    navController.navigate(Routes.MANUAL_PASTE) {
                        popUpTo(Routes.HOME)
                    }
                },
                onCancel = { navController.popBackStack() },
                app = app,
            )
        }
        composable(
            route = Routes.PREVIEW,
            arguments = listOf(navArgument("handoffId") { type = NavType.StringType }),
        ) { entry ->
            val handoffId = entry.arguments?.getString("handoffId").orEmpty()
            PreviewScreen(
                handoffId = handoffId,
                onContinue = { id -> navController.navigate(Routes.export(id)) },
                onBack = { navController.popBackStack() },
                app = app,
            )
        }
        composable(
            route = Routes.EXPORT,
            arguments = listOf(navArgument("handoffId") { type = NavType.StringType }),
        ) { entry ->
            val handoffId = entry.arguments?.getString("handoffId").orEmpty()
            ExportScreen(
                handoffId = handoffId,
                onBack = { navController.popBackStack() },
                app = app,
            )
        }
        composable(Routes.HISTORY) {
            HistoryScreen(
                onOpenHandoff = { id -> navController.navigate(Routes.preview(id)) },
                onBack = { navController.popBackStack() },
                app = app,
            )
        }
        composable(Routes.MANUAL_PASTE) {
            ManualPasteScreen(
                onBuilt = { id ->
                    navController.navigate(Routes.preview(id)) {
                        popUpTo(Routes.HOME)
                    }
                },
                onBack = { navController.popBackStack() },
                app = app,
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() }, app = app)
        }
    }
}
