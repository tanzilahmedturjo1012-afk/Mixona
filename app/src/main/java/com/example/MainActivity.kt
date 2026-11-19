package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.MixonaUiState
import com.example.ui.MixonaViewModel
import com.example.ui.components.MixonaLogoMark
import com.example.ui.components.MixonaPill
import com.example.ui.components.MixonaToastOverlay
import com.example.ui.components.MixonaTopBar
import com.example.ui.screens.EnginesScreen
import com.example.ui.screens.LabScreen
import com.example.ui.screens.LedgerScreen
import com.example.ui.screens.MatrixScreen
import com.example.ui.screens.SignalScreen
import com.example.ui.theme.MixonaTheme

enum class MixonaTab(
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    SIGNAL("Signal", Icons.Rounded.Dashboard, Icons.Outlined.Dashboard),
    ENGINES("Engines", Icons.Rounded.Memory, Icons.Outlined.Memory),
    LAB("Lab", Icons.Rounded.Analytics, Icons.Outlined.Analytics),
    MATRIX("Matrix", Icons.Rounded.GridView, Icons.Outlined.GridView),
    LEDGER("Ledger", Icons.Rounded.ReceiptLong, Icons.Outlined.ReceiptLong),
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val viewModel: MixonaViewModel = viewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            LaunchedEffect(uiState.isDarkTheme) {
                if (uiState.isDarkTheme) {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                        navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
                    )
                } else {
                    enableEdgeToEdge(
                        statusBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                        ),
                        navigationBarStyle = SystemBarStyle.light(
                            android.graphics.Color.TRANSPARENT,
                            android.graphics.Color.argb(0x80, 0x1b, 0x1b, 0x1b),
                        ),
                    )
                }
            }

            MixonaTheme(darkTheme = uiState.isDarkTheme) {
                MixonaAppShell(
                    state = uiState,
                    viewModel = viewModel,
                )
            }
        }
    }
}

@Composable
fun MixonaAppShell(
    state: MixonaUiState,
    viewModel: MixonaViewModel,
) {
    val p = MixonaTheme.palette
    var currentTab by rememberSaveable { mutableStateOf(MixonaTab.SIGNAL) }

    BackHandler(enabled = currentTab != MixonaTab.SIGNAL) {
        currentTab = MixonaTab.SIGNAL
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        p.bgDeep,
                        p.bgDeep,
                    ),
                ),
            ),
    ) {
        val isTabletOrWide = maxWidth >= 720.dp
        val isWideContent = maxWidth >= 600.dp

        Box(modifier = Modifier.fillMaxSize()) {
            if (isTabletOrWide) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .navigationBarsPadding(),
                ) {
                    MixonaNavigationRail(
                        currentTab = currentTab,
                        onSelectTab = { currentTab = it },
                    )
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        MixonaTopBar(
                            connectionState = state.connectionState,
                            clockText = state.clockText,
                            isDarkTheme = state.isDarkTheme,
                            onToggleTheme = viewModel::toggleTheme,
                            showBrand = false,
                        )
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth(),
                            contentAlignment = Alignment.TopCenter,
                        ) {
                            Box(modifier = Modifier.widthIn(max = 1040.dp).fillMaxWidth()) {
                                MixonaTabContent(
                                    currentTab = currentTab,
                                    state = state,
                                    isWideScreen = true,
                                    viewModel = viewModel,
                                    onJumpToEngines = { currentTab = MixonaTab.ENGINES },
                                )
                            }
                        }
                    }
                }
            } else {
                Scaffold(
                    containerColor = Color.Transparent,
                    contentWindowInsets = WindowInsets.safeDrawing,
                    topBar = {
                        Box(modifier = Modifier.statusBarsPadding()) {
                            MixonaTopBar(
                                connectionState = state.connectionState,
                                clockText = state.clockText,
                                isDarkTheme = state.isDarkTheme,
                                onToggleTheme = viewModel::toggleTheme,
                                showBrand = true,
                            )
                        }
                    },
                    bottomBar = {
                        MixonaBottomNavigation(
                            currentTab = currentTab,
                            onSelectTab = { currentTab = it },
                        )
                    },
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    ) {
                        MixonaTabContent(
                            currentTab = currentTab,
                            state = state,
                            isWideScreen = isWideContent,
                            viewModel = viewModel,
                            onJumpToEngines = { currentTab = MixonaTab.ENGINES },
                        )
                    }
                }
            }

            MixonaToastOverlay(
                toast = state.toast,
                onDismiss = viewModel::dismissToast,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 56.dp)
                    .widthIn(max = 420.dp),
            )
        }
    }
}

@Composable
private fun MixonaTabContent(
    currentTab: MixonaTab,
    state: MixonaUiState,
    isWideScreen: Boolean,
    viewModel: MixonaViewModel,
    onJumpToEngines: () -> Unit,
) {
    when (currentTab) {
        MixonaTab.SIGNAL -> SignalScreen(
            state = state,
            isWideScreen = isWideScreen,
            onSelectApiMode = viewModel::setApiMode,
            onDemoClick = viewModel::loadDemoData,
            onJumpToEngines = onJumpToEngines,
        )
        MixonaTab.ENGINES -> EnginesScreen(
            state = state,
            onDemoClick = viewModel::loadDemoData,
        )
        MixonaTab.LAB -> LabScreen(
            state = state,
            onDemoClick = viewModel::loadDemoData,
        )
        MixonaTab.MATRIX -> MatrixScreen(
            state = state,
            isWideScreen = isWideScreen,
            onSelectFilter = viewModel::setHistoryFilter,
            onDemoClick = viewModel::loadDemoData,
        )
        MixonaTab.LEDGER -> LedgerScreen(
            state = state,
            isWideScreen = isWideScreen,
            onSearchChange = viewModel::setLedgerSearch,
            onClearLedger = viewModel::clearLedger,
            onResetLearning = viewModel::resetLearning,
            onJackpotThresholdChange = viewModel::setJackpotThreshold,
            onToggleTheme = viewModel::toggleTheme,
        )
    }
}

@Composable
private fun MixonaBottomNavigation(
    currentTab: MixonaTab,
    onSelectTab: (MixonaTab) -> Unit,
) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardSurface.copy(alpha = 0.96f),
        border = BorderStroke(1.dp, p.borderSubtle),
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.navigationBarsPadding(),
        ) {
            MixonaTab.entries.forEach { tab ->
                val selected = currentTab == tab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = p.mint,
                        selectedTextColor = p.textPrimary,
                        indicatorColor = p.mint.copy(alpha = 0.16f),
                        unselectedIconColor = p.textTertiary,
                        unselectedTextColor = p.textTertiary,
                    ),
                    modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}"),
                )
            }
        }
    }
}

@Composable
private fun MixonaNavigationRail(
    currentTab: MixonaTab,
    onSelectTab: (MixonaTab) -> Unit,
) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardSurface,
        border = BorderStroke(1.dp, p.borderSubtle),
        modifier = Modifier.fillMaxHeight(),
    ) {
        NavigationRail(
            containerColor = Color.Transparent,
            header = {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
                ) {
                    MixonaLogoMark()
                    Text(
                        text = "MIXONA",
                        style = MaterialTheme.typography.labelLarge,
                        color = p.textPrimary,
                    )
                    MixonaPill(text = "AUTO", tint = p.winEmerald)
                }
            },
        ) {
            Spacer(modifier = Modifier.width(4.dp))
            MixonaTab.entries.forEach { tab ->
                val selected = currentTab == tab
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelectTab(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                    colors = NavigationRailItemDefaults.colors(
                        selectedIconColor = p.mint,
                        selectedTextColor = p.textPrimary,
                        indicatorColor = p.mint.copy(alpha = 0.16f),
                        unselectedIconColor = p.textTertiary,
                        unselectedTextColor = p.textTertiary,
                    ),
                    modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}"),
                )
            }
        }
    }
}
