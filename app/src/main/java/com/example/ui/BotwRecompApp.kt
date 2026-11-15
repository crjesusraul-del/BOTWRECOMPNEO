package com.example.ui

import android.app.Activity
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.outlined.Gamepad
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.ui.screens.BenchmarkScreen
import com.example.ui.screens.ControlsHardwareScreen
import com.example.ui.screens.HomeLauncherScreen
import com.example.ui.screens.OptimizerScreen
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SheikahCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.viewmodel.RecompViewModel
import kotlin.math.abs

object Routes {
    const val HOME = "home"
    const val OPTIMIZER = "optimizer"
    const val BENCHMARK = "benchmark"
    const val CONTROLS = "controls"
}

private data class NavTabItem(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val navTabs = listOf(
    NavTabItem(Routes.HOME, R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home, "tab_home"),
    NavTabItem(Routes.OPTIMIZER, R.string.nav_optimizer, Icons.Filled.Tune, Icons.Outlined.Tune, "tab_optimizer"),
    NavTabItem(Routes.BENCHMARK, R.string.nav_benchmark, Icons.Filled.Speed, Icons.Outlined.Speed, "tab_benchmark"),
    NavTabItem(Routes.CONTROLS, R.string.nav_controls, Icons.Filled.Gamepad, Icons.Outlined.Gamepad, "tab_controls")
)

@Composable
fun BotwRecompApp(
    viewModel: RecompViewModel = viewModel()
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val hardware by viewModel.hardwareProfile.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val touchHud by viewModel.touchHud.collectAsStateWithLifecycle()
    val dumpSummary by viewModel.dumpSummary.collectAsStateWithLifecycle()
    val lsfgDll by viewModel.lsfgDll.collectAsStateWithLifecycle()
    val recompPlan by viewModel.recompPlan.collectAsStateWithLifecycle()
    val isScanningDump by viewModel.isScanningDump.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusBannerMessage.collectAsStateWithLifecycle()
    val liveBenchmark by viewModel.liveBenchmark.collectAsStateWithLifecycle()
    val benchmarkRuns by viewModel.benchmarkRuns.collectAsStateWithLifecycle()

    LaunchedEffect(statusMsg) {
        val msg = statusMsg
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    val dumpFolderLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            viewModel.onDumpFolderSelected(uri)
        }
    }

    val lsfgDllLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (_: SecurityException) {
            }
            viewModel.onLsfgDllSelected(uri)
        }
    }

    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            viewModel.exportPlanToUri(uri)
        }
    }

    fun applyWindowRefreshRate(targetHz: Int) {
        val modes = hardware?.displayModes.orEmpty()
        val matched = modes.minByOrNull { abs(it.refreshRateHz - targetHz) }
        if (activity != null && matched != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val attrs = activity.window.attributes
            attrs.preferredDisplayModeId = matched.modeId
            activity.window.attributes = attrs
            viewModel.refreshHardwareNow()
        }
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianVoid)
    ) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = ObsidianVoid,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar(
                        containerColor = ObsidianSurface,
                        contentColor = TextPrimary,
                        windowInsets = WindowInsets.navigationBars
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = {
                                    Text(
                                        text = stringResource(tab.labelRes),
                                        maxLines = 1
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SheikahCyan,
                                    selectedTextColor = SheikahCyan,
                                    indicatorColor = ObsidianElevated,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useNavRail) {
                    NavigationRail(
                        containerColor = ObsidianSurface,
                        modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
                    ) {
                        navTabs.forEach { tab ->
                            val selected = currentDestination?.hierarchy?.any { it.route == tab.route } == true
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                modifier = Modifier.testTag(tab.testTag),
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = stringResource(tab.labelRes)
                                    )
                                },
                                label = { Text(stringResource(tab.labelRes)) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = SheikahCyan,
                                    selectedTextColor = SheikahCyan,
                                    indicatorColor = ObsidianElevated,
                                    unselectedIconColor = TextMuted,
                                    unselectedTextColor = TextMuted
                                )
                            )
                        }
                    }
                }

                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Box(modifier = Modifier.widthIn(max = 760.dp).fillMaxSize()) {
                        NavHost(
                            navController = navController,
                            startDestination = Routes.HOME
                        ) {
                            composable(Routes.HOME) {
                                HomeLauncherScreen(
                                    hardware = hardware,
                                    settings = settings,
                                    dumpSummary = dumpSummary,
                                    recompPlan = recompPlan,
                                    isScanningDump = isScanningDump,
                                    onSelectDumpFolder = { dumpFolderLauncher.launch(null) },
                                    onRescanDump = { viewModel.rescanSavedDump() },
                                    onSelectGameVersion = { viewModel.updateGameVersion(it) },
                                    onSelectPreset = { preset ->
                                        viewModel.applyPreset(preset)
                                    },
                                    onRebuildPlan = { viewModel.rebuildPlanNow() },
                                    onExportJson = { exportJsonLauncher.launch("edge40_neo_recomp.json") },
                                    onOpenBenchmark = { navigateToTab(Routes.BENCHMARK) },
                                    onOpenOptimizer = { navigateToTab(Routes.OPTIMIZER) }
                                )
                            }

                            composable(Routes.OPTIMIZER) {
                                OptimizerScreen(
                                    hardware = hardware,
                                    settings = settings,
                                    lsfgDll = lsfgDll,
                                    onUpdateSettings = { transform -> viewModel.updateSettings(transform) },
                                    onApplyRefreshRate = { hz -> applyWindowRefreshRate(hz) },
                                    onPickLsfgDll = { lsfgDllLauncher.launch(arrayOf("*/*")) },
                                    onClearLsfgDll = { viewModel.clearLsfgDll() }
                                )
                            }

                            composable(Routes.BENCHMARK) {
                                BenchmarkScreen(
                                    hardware = hardware,
                                    settings = settings,
                                    liveBenchmark = liveBenchmark,
                                    benchmarkRuns = benchmarkRuns,
                                    onStartBenchmark = { viewModel.startBenchmark() },
                                    onStopAndSaveBenchmark = { viewModel.stopAndSaveBenchmark() },
                                    onFrameSample = { ms, w, h ->
                                        viewModel.onBenchmarkFrameSample(ms, w, h)
                                    },
                                    onDeleteRun = { id -> viewModel.deleteBenchmarkRun(id) },
                                    onClearHistory = { viewModel.clearBenchmarkHistory() }
                                )
                            }

                            composable(Routes.CONTROLS) {
                                ControlsHardwareScreen(
                                    hardware = hardware,
                                    touchHud = touchHud,
                                    onRefreshHardware = { viewModel.refreshHardwareNow() },
                                    onUpdateTouchHud = { transform -> viewModel.updateTouchHud(transform) },
                                    onTriggerHaptics = { viewModel.triggerHapticTest() }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
