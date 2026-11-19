package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.model.HistoryFilter
import com.example.model.LedgerEntry
import com.example.ui.MixonaUiState
import com.example.ui.components.ConfirmActionDialog
import com.example.ui.components.MixonaPill
import com.example.ui.components.MixonaSectionCard
import com.example.ui.theme.MixonaTheme
import kotlin.math.roundToInt

@Composable
fun MatrixScreen(
    state: MixonaUiState,
    isWideScreen: Boolean,
    onSelectFilter: (HistoryFilter) -> Unit,
    onDemoClick: () -> Unit,
) {
    val p = MixonaTheme.palette
    val filtered = remember(state.records, state.historyFilter) {
        when (state.historyFilter) {
            HistoryFilter.ALL -> state.records
            HistoryFilter.BIG -> state.records.filter { it.size == "BIG" }
            HistoryFilter.SMALL -> state.records.filter { it.size == "SMALL" }
        }
    }

    val cols = if (isWideScreen) 10 else 5

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (state.records.isEmpty()) {
            item {
                EngineWaitingState(onDemoClick = onDemoClick)
            }
            return@LazyColumn
        }

        item {
            MixonaSectionCard(
                title = "Last 100 Results · Newest First",
                icon = Icons.Rounded.GridView,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(p.cardElevated)
                        .border(1.dp, p.borderSubtle, RoundedCornerShape(12.dp))
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    HistoryFilter.entries.forEach { filter ->
                        val active = state.historyFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (active) p.mint else androidx.compose.ui.graphics.Color.Transparent)
                                .clickable { onSelectFilter(filter) }
                                .testTag("matrix_filter_${filter.name.lowercase()}"),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (active) p.bgDeep else p.textSecondary,
                            )
                        }
                    }
                }

                if (filtered.isEmpty()) {
                    Text(
                        text = "No draws match the selected filter.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textTertiary,
                        modifier = Modifier.padding(vertical = 12.dp),
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        filtered.chunked(cols).forEachIndexed { rowIndex, rowDraws ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                rowDraws.forEachIndexed { colIndex, draw ->
                                    val drawIdx = rowIndex * cols + colIndex + 1
                                    val isBig = draw.size == "BIG"
                                    val accent = if (isBig) p.bigGold else p.smallCyan
                                    Surface(
                                        color = accent.copy(alpha = 0.10f),
                                        shape = RoundedCornerShape(10.dp),
                                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f)),
                                        modifier = Modifier.weight(1f),
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(vertical = 7.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            Text(
                                                text = "${draw.number}",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = accent,
                                            )
                                            Text(
                                                text = "#$drawIdx",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = accent.copy(alpha = 0.7f),
                                            )
                                        }
                                    }
                                }
                                repeat(cols - rowDraws.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            MixonaSectionCard(
                title = "Draw History Feed",
                icon = Icons.Rounded.History,
                trailing = { MixonaPill(text = "${minOf(40, filtered.size)} shown") },
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    filtered.take(40).forEachIndexed { idx, draw ->
                        val isBig = draw.size == "BIG"
                        val accent = if (isBig) p.bigGold else p.smallCyan
                        Surface(
                            color = p.cardElevated,
                            shape = RoundedCornerShape(11.dp),
                            border = BorderStroke(1.dp, p.borderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    Text(
                                        text = "#${idx + 1}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = p.textTertiary,
                                        modifier = Modifier.width(34.dp),
                                    )
                                    Text(
                                        text = draw.period.ifEmpty { "—" },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = p.textPrimary,
                                    )
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Text(
                                        text = "${draw.number}",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = p.textPrimary,
                                    )
                                    MixonaPill(text = draw.size, tint = accent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LedgerScreen(
    state: MixonaUiState,
    isWideScreen: Boolean,
    onSearchChange: (String) -> Unit,
    onClearLedger: () -> Unit,
    onResetLearning: () -> Unit,
    onJackpotThresholdChange: (Double) -> Unit,
    onToggleTheme: () -> Unit,
) {
    val p = MixonaTheme.palette
    var showClearDialog by remember { mutableStateOf(false) }
    var showResetDialog by remember { mutableStateOf(false) }

    val filteredRows = remember(state.ledger, state.ledgerSearch) {
        val q = state.ledgerSearch.trim().lowercase()
        if (q.isEmpty()) {
            state.ledger
        } else {
            state.ledger.filter {
                it.period.lowercase().contains(q) ||
                    it.action.lowercase().contains(q) ||
                    it.rawMasterSize.lowercase().contains(q) ||
                    it.status.lowercase().contains(q)
            }
        }
    }

    if (showClearDialog) {
        ConfirmActionDialog(
            title = "Clear Ledger & Streaks?",
            description = "Resets saved round history and win/loss streak counters.",
            confirmText = "Clear Ledger",
            onConfirm = {
                showClearDialog = false
                onClearLedger()
            },
            onDismiss = { showClearDialog = false },
        )
    }

    if (showResetDialog) {
        ConfirmActionDialog(
            title = "Reset Self-Learning Memory?",
            description = "Resets adaptive engine weights and clears guarded mistake memory.",
            confirmText = "Reset Learning",
            onConfirm = {
                showResetDialog = false
                onResetLearning()
            },
            onDismiss = { showResetDialog = false },
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            LedgerSummaryStrip(state = state, isWideScreen = isWideScreen)
        }

        item {
            MixonaSectionCard(
                title = "Automatic Prediction & Self-Learn Ledger",
                icon = Icons.Rounded.History,
            ) {
                OutlinedTextField(
                    value = state.ledgerSearch,
                    onValueChange = onSearchChange,
                    placeholder = {
                        Text(
                            text = "Search period, BIG/SMALL, status…",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = "Search ledger",
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = p.mint,
                        unfocusedBorderColor = p.borderSubtle,
                        focusedContainerColor = p.cardElevated,
                        unfocusedContainerColor = p.cardElevated,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("ledger_search_input"),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { showClearDialog = true },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = p.lossCoral.copy(alpha = 0.16f),
                            contentColor = p.lossCoral,
                        ),
                        border = BorderStroke(1.dp, p.lossCoral.copy(alpha = 0.4f)),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("clear_ledger_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DeleteSweep,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CLEAR LEDGER", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = { showResetDialog = true },
                        border = BorderStroke(1.dp, p.borderSubtle),
                        shape = RoundedCornerShape(11.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("reset_learning_button"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = null,
                            tint = p.textPrimary,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("RESET LEARN", style = MaterialTheme.typography.labelLarge, color = p.textPrimary)
                    }
                }
            }
        }

        if (filteredRows.isEmpty()) {
            item {
                Surface(
                    color = p.cardSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, p.borderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = "Waiting for live draws to record automatic predictions…",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textTertiary,
                        modifier = Modifier.padding(24.dp),
                    )
                }
            }
        } else {
            items(filteredRows.take(60), key = { "${it.period}_${it.at}" }) { entry ->
                LedgerEntryRowCard(entry = entry)
            }
        }

        item {
            MixonaSectionCard(
                title = "Self-Learning & UI Preferences",
                icon = Icons.Rounded.Tune,
            ) {
                Surface(
                    color = p.cardElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, p.borderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = "JACKPOT ALERT THRESHOLD",
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textTertiary,
                            )
                            Text(
                                text = "${(state.jackpotThreshold * 100).roundToInt()}%",
                                style = MaterialTheme.typography.titleSmall,
                                color = p.mint,
                            )
                        }
                        Slider(
                            value = state.jackpotThreshold.toFloat(),
                            onValueChange = { onJackpotThresholdChange(it.toDouble()) },
                            valueRange = 0.60f..0.90f,
                            colors = SliderDefaults.colors(
                                thumbColor = p.mint,
                                activeTrackColor = p.mint,
                                inactiveTrackColor = p.borderSubtle,
                            ),
                            modifier = Modifier.testTag("jackpot_threshold_slider"),
                        )
                        Text(
                            text = "Controls Jackpot highlight sensitivity. All predictions & learning remain 100% automatic.",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textSecondary,
                        )
                    }
                }

                Surface(
                    color = p.cardElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, p.borderSubtle),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "APPEARANCE",
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textTertiary,
                            )
                            Text(
                                text = if (state.isDarkTheme) "Obsidian Dark Mode" else "Alabaster Light Mode",
                                style = MaterialTheme.typography.bodyMedium,
                                color = p.textPrimary,
                            )
                        }
                        OutlinedButton(
                            onClick = onToggleTheme,
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, p.borderStrong),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("settings_theme_button"),
                        ) {
                            Text(
                                text = if (state.isDarkTheme) "Switch to Light" else "Switch to Dark",
                                style = MaterialTheme.typography.labelLarge,
                                color = p.mint,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerSummaryStrip(
    state: MixonaUiState,
    isWideScreen: Boolean,
) {
    val p = MixonaTheme.palette
    val s = state.streaks
    val metrics = listOf(
        Triple("Settled", "${s.settled}", p.textPrimary),
        Triple("Wins", "${s.wins}", p.winEmerald),
        Triple("Losses", "${s.losses}", p.lossCoral),
        Triple("Max Win", "${s.maxWin}", p.winEmerald),
        Triple("Max Loss", "${s.maxLoss}", p.lossCoral),
        Triple("Guard Saves", "${state.learnState.guardSaves}", p.guardAmber),
    )
    val cols = if (isWideScreen) 6 else 3

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        metrics.chunked(cols).forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                row.forEach { (label, value, color) ->
                    Surface(
                        color = p.cardSurface,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.borderSubtle),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = label.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleLarge,
                                color = color,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LedgerEntryRowCard(entry: LedgerEntry) {
    val p = MixonaTheme.palette
    val pShort = if (entry.period.length > 7) entry.period.takeLast(7) else entry.period.ifEmpty { "pending" }
    val callText = if (entry.action == "SKIP") "SKIP (${entry.rawMasterSize})" else entry.action
    val callColor = when (entry.action) {
        "SKIP" -> p.guardAmber
        "BIG" -> p.bigGold
        else -> p.smallCyan
    }
    val statusColor = when (entry.status) {
        "JACKPOT" -> p.bigGold
        "SURE SHOT" -> p.winEmerald
        "LOSS" -> p.lossCoral
        "SAVED", "GUARDED" -> p.guardAmber
        else -> p.textSecondary
    }

    Surface(
        color = p.cardSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, p.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "#$pShort",
                        style = MaterialTheme.typography.labelMedium,
                        color = p.textPrimary,
                    )
                    Text(
                        text = callText,
                        style = MaterialTheme.typography.labelLarge,
                        color = callColor,
                    )
                    Text(
                        text = "[${entry.masterNums.joinToString(" · ")}]",
                        style = MaterialTheme.typography.labelMedium,
                        color = p.textSecondary,
                    )
                }
                MixonaPill(text = entry.status, tint = statusColor)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = entry.trendContext.ifEmpty { "—" },
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (entry.actualNum != null) "Actual: ${entry.actualSize} (${entry.actualNum})" else "Actual: —",
                    style = MaterialTheme.typography.labelSmall,
                    color = p.textSecondary,
                )
            }
        }
    }
}
