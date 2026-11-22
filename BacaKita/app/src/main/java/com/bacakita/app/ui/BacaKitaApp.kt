package com.bacakita.app.ui

import android.widget.Toast
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bacakita.app.ui.screens.AccountScreen
import com.bacakita.app.ui.screens.AiPreferencesScreen
import com.bacakita.app.ui.screens.AiRecommendationScreen
import com.bacakita.app.ui.screens.HelpScreen
import com.bacakita.app.ui.screens.HistoryScreen
import com.bacakita.app.ui.screens.HomeScreen
import com.bacakita.app.ui.screens.ImportScreen
import com.bacakita.app.ui.screens.InputTextScreen
import com.bacakita.app.ui.screens.ReaderScreen
import com.bacakita.app.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val INPUT = "input"
    const val IMPORT = "import"
    const val HISTORY = "history"
    const val ACCOUNT = "account"
    const val READER = "reader"
    const val SETTINGS = "settings"
    const val AI_PREFS = "ai_prefs"
    const val AI_REC = "ai_rec"
    const val HELP = "help"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

/** Menu utama di bagian bawah layar. */
private val tabs = listOf(
    Tab(Routes.HOME, "Beranda", Icons.Default.Home),
    Tab(Routes.IMPORT, "Impor", Icons.Default.UploadFile),
    Tab(Routes.HISTORY, "Riwayat", Icons.Default.History),
    Tab(Routes.SETTINGS, "Pengaturan", Icons.Default.Tune),
    Tab(Routes.ACCOUNT, "Akun", Icons.Default.AccountCircle),
)

private fun NavHostController.navigateTab(route: String) {
    navigate(route) {
        popUpTo(Routes.HOME) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
fun BacaKitaApp(vm: MainViewModel = viewModel()) {
    val nav = rememberNavController()
    val context = LocalContext.current

    val prefs by vm.prefs.collectAsStateWithLifecycle()
    val content by vm.content.collectAsStateWithLifecycle()
    val paragraphs by vm.paragraphs.collectAsStateWithLifecycle()
    val importState by vm.importState.collectAsStateWithLifecycle()
    val aiInput by vm.aiInput.collectAsStateWithLifecycle()
    val recState by vm.recState.collectAsStateWithLifecycle()
    val ttsState by vm.ttsState.collectAsStateWithLifecycle()
    val user by vm.user.collectAsStateWithLifecycle()
    val authState by vm.authState.collectAsStateWithLifecycle()
    val history by vm.history.collectAsStateWithLifecycle()

    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    LaunchedEffect(Unit) {
        vm.events.collect { event ->
            when (event) {
                UiEvent.OpenReader -> nav.navigate(Routes.READER) { launchSingleTop = true }
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.navigateTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = { Text(tab.label) },
                            alwaysShowLabel = true,
                        )
                    }
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(innerPadding).consumeWindowInsets(innerPadding),
        ) {
            composable(Routes.HOME) {
                HomeScreen(
                    username = user?.username,
                    hasContent = content != null,
                    onContinue = { nav.navigate(Routes.READER) },
                    onInput = { nav.navigate(Routes.INPUT) },
                    onAi = { nav.navigate(Routes.AI_PREFS) },
                    onSample = vm::openSample,
                    onHelp = { nav.navigate(Routes.HELP) },
                )
            }
            composable(Routes.INPUT) {
                InputTextScreen(
                    text = vm.draftText,
                    onTextChange = { vm.draftText = it },
                    onOpen = vm::openDraft,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.IMPORT) {
                ImportScreen(
                    state = importState,
                    loggedIn = user != null,
                    onPick = vm::importDocument,
                    onDismissError = vm::clearImportError,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.HISTORY) {
                HistoryScreen(
                    items = history,
                    loggedIn = user != null,
                    onOpen = vm::openHistory,
                    onDelete = vm::deleteHistory,
                    onClearAll = vm::clearHistory,
                    onGoAccount = { nav.navigateTab(Routes.ACCOUNT) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.ACCOUNT) {
                AccountScreen(
                    user = user,
                    authState = authState,
                    savedCount = history.size,
                    onLogin = vm::login,
                    onRegister = vm::register,
                    onLogout = vm::logout,
                    onClearError = vm::clearAuthError,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.READER) {
                ReaderScreen(
                    content = content,
                    paragraphs = paragraphs,
                    prefs = prefs,
                    ttsState = ttsState,
                    onBack = { nav.popBackStack() },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenAi = { nav.navigate(Routes.AI_PREFS) },
                    onPlay = vm::speakFrom,
                    onPause = vm::pauseTts,
                    onResume = vm::resumeTts,
                    onStop = vm::stopTts,
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    prefs = prefs,
                    onChange = { transform -> vm.updatePrefs(transform = transform) },
                    onReset = vm::resetPrefs,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.AI_PREFS) {
                AiPreferencesScreen(
                    input = aiInput,
                    onChange = vm::updateAiInput,
                    onSubmit = {
                        vm.requestRecommendation()
                        nav.navigate(Routes.AI_REC)
                    },
                    onManual = { nav.navigate(Routes.SETTINGS) },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.AI_REC) {
                AiRecommendationScreen(
                    state = recState,
                    currentPrefs = prefs,
                    onApply = { rec ->
                        vm.applyRecommendation(rec)
                        if (vm.content.value != null) {
                            nav.navigate(Routes.READER) { popUpTo(Routes.HOME) }
                        } else {
                            Toast.makeText(context, "Pengaturan diterapkan", Toast.LENGTH_SHORT).show()
                            nav.popBackStack(Routes.HOME, inclusive = false)
                        }
                    },
                    onApplyAndEdit = { rec ->
                        vm.applyRecommendation(rec)
                        if (vm.content.value != null) {
                            nav.navigate(Routes.READER) { popUpTo(Routes.HOME) }
                            nav.navigate(Routes.SETTINGS)
                        } else {
                            nav.navigate(Routes.SETTINGS) { popUpTo(Routes.HOME) }
                        }
                    },
                    onDismiss = {
                        vm.dismissRecommendation()
                        nav.popBackStack(Routes.AI_PREFS, inclusive = true)
                    },
                    onRetry = vm::requestRecommendation,
                    onManual = {
                        vm.dismissRecommendation()
                        nav.navigate(Routes.SETTINGS) { popUpTo(Routes.AI_PREFS) { inclusive = true } }
                    },
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.HELP) { HelpScreen(onBack = { nav.popBackStack() }) }
        }
    }
}
