package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DrawRecord
import com.example.model.MasterAnalysis
import com.example.ui.MixonaUiState
import com.example.ui.components.DigitBadge
import com.example.ui.components.MixonaPill
import com.example.ui.components.MixonaSectionCard
import com.example.ui.components.SyncControlBar
import com.example.ui.theme.MixonaTheme
import com.example.model.ApiFeedMode
import java.util.Locale
import kotlin.math.roundToInt

private fun formatPct(x: Double): String = String.format(Locale.US, "%.1f%%", x * 100.0)

@Composable
fun SignalScreen(
    state: MixonaUiState,
    isWideScreen: Boolean,
    onSelectApiMode: (ApiFeedMode) -> Unit,
    onDemoClick: () -> Unit,
    onJumpToEngines: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            SyncControlBar(
                statusMessage = state.statusMessage,
                apiMode = state.apiMode,
                onSelectApiMode = onSelectApiMode,
                onDemoClick = onDemoClick,
            )
        }

        item {
            HeroSignalCard(
                analysis = state.analysis,
                records = state.records,
                nextPeriod = state.nextPeriod,
                countdownSeconds = state.countdownSeconds,
                isWideScreen = isWideScreen,
            )
        }

        item {
            Telemetry6Grid(state = state, isWideScreen = isWideScreen)
        }

        if (state.analysis != null) {
            item {
                MultiHorizonTrendCard(analysis = state.analysis, isWideScreen = isWideScreen)
            }
            item {
                OverviewEnginesCard(
                    state = state,
                    analysis = state.analysis,
                    isWideScreen = isWideScreen,
                    onJumpToEngines = onJumpToEngines,
                )
            }
        }

        item {
            MixonaFooterDisclaimer()
        }
    }
}

@Composable
private fun HeroSignalCard(
    analysis: MasterAnalysis?,
    records: List<DrawRecord>,
    nextPeriod: String,
    countdownSeconds: Int,
    isWideScreen: Boolean,
) {
    val p = MixonaTheme.palette
    val callColor = when (analysis?.action) {
        "BIG" -> p.bigGold
        "SMALL" -> p.smallCyan
        "SKIP" -> p.guardAmber
        else -> p.mint
    }

    val shortNext = if (nextPeriod.length > 6) nextPeriod.takeLast(6) else nextPeriod

    Surface(
        color = p.cardSurface,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.2.dp, callColor.copy(alpha = 0.45f)),
        shadowElevation = 6.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            // Subtle bioluminescent corner aura
            Canvas(modifier = Modifier.matchParentSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(callColor.copy(alpha = 0.16f), Color.Transparent),
                        center = Offset(size.width * 0.25f, size.height * 0.25f),
                        radius = size.maxDimension * 0.65f,
                    ),
                )
            }

            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // Header with Period + Regime + Arc Countdown
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            MixonaPill(text = "NEXT #$shortNext", tint = p.smallCyan)
                            if (analysis != null) {
                                MixonaPill(
                                    text = "S:${analysis.shortCall} · L:${analysis.longCall}",
                                    tint = if (analysis.trendAligned) p.winEmerald else p.textSecondary,
                                )
                            }
                        }
                        if (analysis != null) {
                            MixonaPill(
                                text = "REGIME: ${analysis.phantom.regime}",
                                tint = p.violet,
                            )
                        }
                    }

                    CountdownRing(seconds = countdownSeconds)
                }

                // Self-Learning Guard Warning Banner
                AnimatedVisibility(
                    visible = analysis?.guardTriggered == true,
                    enter = expandVertically(),
                    exit = shrinkVertically(),
                ) {
                    Surface(
                        color = p.guardAmber.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.guardAmber.copy(alpha = 0.45f)),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Row(
                            modifier = Modifier.padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(9.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.WarningAmber,
                                contentDescription = "Self-Learn Guard Active",
                                tint = p.guardAmber,
                            )
                            Text(
                                text = analysis?.guardReason.orEmpty(),
                                style = MaterialTheme.typography.bodySmall,
                                color = p.guardAmber,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }

                // Signal Call + Dual Target Orbs
                if (isWideScreen) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        SignalCallBox(
                            analysis = analysis,
                            callColor = callColor,
                            modifier = Modifier.weight(1.15f),
                        )
                        TargetNumberPairBox(
                            analysis = analysis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                } else {
                    SignalCallBox(
                        analysis = analysis,
                        callColor = callColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    TargetNumberPairBox(
                        analysis = analysis,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                // Live 16-Draw Tape
                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                ) {
                    val bigCount = records.count { it.size == "BIG" }
                    val smallCount = records.size - bigCount
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = "LIVE 16-DRAW TAPE (NEWEST LEFT)",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.textTertiary,
                        )
                        Text(
                            text = if (records.isEmpty()) "BIG — · SMALL —" else "BIG $bigCount · SMALL $smallCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.textSecondary,
                        )
                    }

                    if (records.isEmpty()) {
                        Text(
                            text = "Waiting for live stream or tap DEMO above to preview immediately.",
                            style = MaterialTheme.typography.bodySmall,
                            color = p.textTertiary,
                        )
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            itemsIndexed(records.take(16)) { index, draw ->
                                val isBig = draw.size == "BIG"
                                val chipColor = if (isBig) p.bigGold else p.smallCyan
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(chipColor.copy(alpha = 0.14f))
                                        .border(
                                            width = if (index == 0) 2.dp else 1.dp,
                                            color = if (index == 0) p.violet else chipColor.copy(alpha = 0.42f),
                                            shape = RoundedCornerShape(10.dp),
                                        ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = "${draw.number}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = chipColor,
                                    )
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
private fun CountdownRing(seconds: Int) {
    val p = MixonaTheme.palette
    val sweep = (seconds.coerceIn(0, 30) / 30f) * 360f
    Box(
        modifier = Modifier.size(54.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(50.dp)) {
            drawArc(
                color = p.bigGold.copy(alpha = 0.18f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
            drawArc(
                color = p.bigGold,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Round),
            )
        }
        Text(
            text = "${seconds}s",
            style = MaterialTheme.typography.labelMedium,
            color = p.bigGold,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun SignalCallBox(
    analysis: MasterAnalysis?,
    callColor: Color,
    modifier: Modifier = Modifier,
) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardElevated.copy(alpha = 0.85f),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, p.borderSubtle),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "MIXONA SELF-LEARNING ENSEMBLE CALL",
                style = MaterialTheme.typography.labelSmall,
                color = p.textTertiary,
                textAlign = TextAlign.Center,
            )

            val mainLabel = when (analysis?.action) {
                null -> "SYNCING"
                "SKIP" -> "SKIP · GUARDED"
                else -> analysis.action
            }
            Text(
                text = mainLabel,
                style = if (analysis?.action == "SKIP") {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displayLarge
                },
                color = callColor,
                textAlign = TextAlign.Center,
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MixonaPill(
                    text = if (analysis != null) "Confidence: ${analysis.masterConfidence}%" else "Confidence: --%",
                )
                MixonaPill(
                    text = if (analysis != null) "Consensus: ${analysis.agreementCount}/4" else "Consensus: --/4",
                )
            }

            if (analysis?.isJackpotSignal == true) {
                MixonaPill(
                    text = "★ JACKPOT SIGNAL",
                    tint = p.bigGold,
                )
            }

            val summary = when {
                analysis == null -> "Automatically analyzing 100-result long & short trends…"
                analysis.guardTriggered -> "Raw vote (${analysis.rawMasterSize}) blocked by Mistake Memory · Waiting for safer trend shift."
                else -> "${analysis.goku.name} · Renox ${analysis.renox.agreement}/8 · Short/Long ${if (analysis.trendAligned) "Aligned" else "Diverged"}"
            }
            Text(
                text = summary,
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun TargetNumberPairBox(
    analysis: MasterAnalysis?,
    modifier: Modifier = Modifier,
) {
    val p = MixonaTheme.palette
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "FUSED 2-NUMBER TARGET",
                style = MaterialTheme.typography.labelSmall,
                color = p.textTertiary,
            )
            MixonaPill(
                text = if (analysis != null) {
                    "Streak: ${analysis.renox.streak.count}x ${analysis.renox.streak.side}"
                } else {
                    "Streak: —"
                },
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val n1 = analysis?.masterNumbers?.getOrNull(0)
            val p1 = analysis?.rankedNumbers?.getOrNull(0)?.pct
            val n2 = analysis?.masterNumbers?.getOrNull(1)
            val p2 = analysis?.rankedNumbers?.getOrNull(1)?.pct

            NumberTargetTile(
                label = "PRIMARY #1",
                digitText = n1?.toString() ?: "—",
                metaText = if (p1 != null) String.format(Locale.US, "%.1f%% fused", p1) else "Score —",
                modifier = Modifier.weight(1f),
            )
            NumberTargetTile(
                label = "SECONDARY #2",
                digitText = n2?.toString() ?: "—",
                metaText = if (p2 != null) String.format(Locale.US, "%.1f%% fused", p2) else "Score —",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun NumberTargetTile(
    label: String,
    digitText: String,
    metaText: String,
    modifier: Modifier = Modifier,
) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardElevated,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, p.borderStrong),
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = p.textTertiary,
            )
            Text(
                text = digitText,
                style = MaterialTheme.typography.displayMedium.copy(fontSize = 36.sp),
                color = p.textPrimary,
            )
            Text(
                text = metaText,
                style = MaterialTheme.typography.labelSmall,
                color = p.mint,
            )
        }
    }
}

@Composable
private fun Telemetry6Grid(
    state: MixonaUiState,
    isWideScreen: Boolean,
) {
    val p = MixonaTheme.palette
    val s = state.streaks
    val items = listOf(
        Triple("Total Wins", "${s.wins}", if (s.settled > 0) "Win Rate: ${s.winRate}%" else "Win Rate: —%") to p.winEmerald,
        Triple("Total Losses", "${s.losses}", "${s.settled} settled") to p.lossCoral,
        Triple("Max Win Streak", "${s.maxWin}", "Current: ${s.currentStreak}") to p.winEmerald,
        Triple("Max Loss Streak", "${s.maxLoss}", "Consecutive max") to p.lossCoral,
        Triple("Self-Learn Saves", "${state.learnState.guardSaves}", "${state.learnState.guardTotal} guarded skips") to p.guardAmber,
        Triple("2-Num Jackpots", "${s.jackpots}", if (s.settled > 0) "Number rate: ${((s.jackpots.toDouble() / s.settled) * 100.0).roundToInt()}%" else "Number rate: —%") to p.bigGold,
    )

    val cols = if (isWideScreen) 3 else 2
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        items.chunked(cols).forEach { rowItems ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowItems.forEach { (triple, accent) ->
                    Surface(
                        color = p.cardSurface,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, p.borderSubtle),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = triple.first.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = triple.second,
                                style = MaterialTheme.typography.headlineMedium,
                                color = accent,
                            )
                            Text(
                                text = triple.third,
                                style = MaterialTheme.typography.bodySmall,
                                color = p.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MultiHorizonTrendCard(
    analysis: MasterAnalysis,
    isWideScreen: Boolean,
) {
    val p = MixonaTheme.palette
    val ph = analysis.phantom
    val horizons = listOf(
        Triple("Ultra-Short (Last 5)", formatPct(ph.r5), if (ph.r5 >= 0.5) "BIG Bias" else "SMALL Bias"),
        Triple("Short Trend (Last 10)", formatPct(ph.r10), "Flip rate: ${formatPct(ph.alternation)}"),
        Triple("Mid-Long (Last 30)", formatPct(ph.r30), if (ph.r30 >= 0.5) "BIG Trend" else "SMALL Trend"),
        Triple("Macro Long (Last 100)", formatPct(ph.r100), if (analysis.trendAligned) "Short & Long Aligned" else "Regime Divergence"),
    )

    MixonaSectionCard(
        title = "Short vs Long Trend (A–Z)",
        icon = Icons.Rounded.TrendingUp,
        trailing = {
            MixonaPill(
                text = String.format(Locale.US, "Entropy: %.2f", ph.entropyVal),
            )
        },
    ) {
        val cols = if (isWideScreen) 4 else 2
        horizons.chunked(cols).forEach { chunk ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chunk.forEach { (label, value, sub) ->
                    Surface(
                        color = p.cardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.borderSubtle),
                        modifier = Modifier.weight(1f),
                    ) {
                        Column(
                            modifier = Modifier.padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelSmall,
                                color = p.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.titleLarge,
                                color = p.textPrimary,
                            )
                            Text(
                                text = sub,
                                style = MaterialTheme.typography.bodySmall,
                                color = p.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OverviewEnginesCard(
    state: MixonaUiState,
    analysis: MasterAnalysis,
    isWideScreen: Boolean,
    onJumpToEngines: () -> Unit,
) {
    val p = MixonaTheme.palette
    MixonaSectionCard(
        title = "4 Engines · Self-Learning Weights",
        icon = Icons.Rounded.Memory,
        trailing = {
            OutlinedButton(
                onClick = onJumpToEngines,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, p.borderSubtle),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier
                    .height(36.dp)
                    .testTag("jump_engines_button"),
            ) {
                Text("Deep View", style = MaterialTheme.typography.labelLarge, color = p.mint)
            }
        },
    ) {
        val cols = if (isWideScreen) 2 else 1
        analysis.engineVotes.chunked(cols).forEach { rowVotes ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                rowVotes.forEach { vote ->
                    val st = state.learnState.stats[vote.id]
                    val w = st?.w ?: 0
                    val l = st?.l ?: 0
                    val tot = w + l
                    val acc = if (tot > 0) "${((w.toDouble() / tot) * 100.0).roundToInt()}%" else "Learning"
                    val sideColor = if (vote.pred == "BIG") p.bigGold else p.smallCyan

                    Surface(
                        color = p.cardElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, p.borderSubtle),
                        modifier = Modifier.weight(1f),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(11.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(3.dp),
                            ) {
                                Text(
                                    text = vote.label,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = p.textPrimary,
                                )
                                Text(
                                    text = "Weight: ${(vote.weight * 100).roundToInt()}% · Acc: $acc (${w}W/${l}L)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = p.textTertiary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                            ) {
                                MixonaPill(text = "${vote.pred} ${vote.conf}%", tint = sideColor)
                                vote.nums.forEach { n ->
                                    DigitBadge(digit = n)
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
fun MixonaFooterDisclaimer() {
    val p = MixonaTheme.palette
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Rounded.Shield,
            contentDescription = null,
            tint = p.textTertiary,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = "Mixona merges Phantom Core V1.0 (chronologically corrected), Renox v2.0, Hidan Trend Trio, Goku + Aura 500+, Short/Long Trend A–Z analysis, and an online Self-Learning Mistake Guard. Probabilities reflect historical sequence diagnostics.",
            style = MaterialTheme.typography.bodySmall,
            color = p.textTertiary,
        )
    }
}
