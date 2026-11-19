package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoGraph
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Science
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.engine.GokuAuraEngine
import com.example.model.MasterAnalysis
import com.example.ui.MixonaUiState
import com.example.ui.components.DigitBadge
import com.example.ui.components.MixonaPill
import com.example.ui.components.MixonaSectionCard
import com.example.ui.components.ProbabilityMeterRow
import com.example.ui.theme.MixonaTheme
import java.util.Locale

private fun pctStr(v: Double): String = String.format(Locale.US, "%.1f%%", v * 100.0)

@Composable
fun EnginesScreen(
    state: MixonaUiState,
    onDemoClick: () -> Unit,
) {
    val p = MixonaTheme.palette
    val a = state.analysis

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (a == null) {
            item {
                EngineWaitingState(onDemoClick = onDemoClick)
            }
            return@LazyColumn
        }

        item {
            MixonaSectionCard(
                title = "A–Z Multi-Engine Breakdown",
                icon = Icons.Rounded.Memory,
                trailing = {
                    MixonaPill(text = "Active: ${a.goku.name}", tint = p.mint)
                },
            ) {
                Text(
                    text = "Live factor diagnostics across Phantom Core, Renox v2.0, Hidan Trio, and Goku + Aura 500+.",
                    style = MaterialTheme.typography.bodySmall,
                    color = p.textSecondary,
                )
            }
        }

        item {
            EngineDetailCard(
                title = "1. Phantom Core (Full V1.0 + Forward Fix)",
                subtitle = "6-Component Context, Entropy & Transitions",
                numbers = a.phantom.numbers,
            ) {
                a.phantom.components.forEach { c ->
                    ProbabilityMeterRow(
                        label = c.name,
                        fraction = c.p.toFloat(),
                        valueText = pctStr(c.p),
                    )
                }
            }
        }

        item {
            EngineDetailCard(
                title = "2. Renox Engine v2.0 (Forward Markov)",
                subtitle = "8-Factor Weighted Markov-1/2 + MA5/20",
                numbers = a.renox.numbers,
            ) {
                a.renox.factors.forEach { (k, v) ->
                    ProbabilityMeterRow(
                        label = k.uppercase(Locale.US),
                        fraction = v.big.toFloat(),
                        valueText = if (v.big >= v.small) "BIG" else "SMALL",
                    )
                }
            }
        }

        item {
            EngineDetailCard(
                title = "3. Hidan Trend Trio",
                subtitle = "CRM · PSM · MDR",
                numbers = a.hidan.numbers,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    MixonaPill(
                        text = "CRM: ${a.hidan.signals.crm}",
                        tint = if (a.hidan.signals.crm == "BIG") p.bigGold else p.smallCyan,
                    )
                    MixonaPill(
                        text = "PSM: ${a.hidan.signals.psm}",
                        tint = if (a.hidan.signals.psm == "BIG") p.bigGold else p.smallCyan,
                    )
                    MixonaPill(
                        text = "MDR: ${a.hidan.signals.mdr}",
                        tint = if (a.hidan.signals.mdr == "BIG") p.bigGold else p.smallCyan,
                    )
                }
            }
        }

        item {
            EngineDetailCard(
                title = "4. Goku + Aura 500+",
                subtitle = "Dragon Streak + Aura Pattern Hash",
                numbers = a.goku.numbers,
            ) {
                TagWrapRow(
                    tags = listOf(
                        a.goku.source,
                        a.goku.name,
                        "Conf: ${a.goku.confidencePct}%",
                        "Aura DB: ${GokuAuraEngine.totalAuraPatterns}",
                    ),
                )
            }
        }

        item {
            val maxPct = a.rankedNumbers.firstOrNull()?.pct ?: 1.0
            MixonaSectionCard(
                title = "0–9 Fused Probability Ranking",
                icon = Icons.Rounded.BarChart,
                trailing = { MixonaPill(text = "Top 2 Auto-Selected", tint = p.bigGold) },
            ) {
                a.rankedNumbers.forEachIndexed { idx, item ->
                    val side = if (item.n >= 5) "BIG" else "SML"
                    val star = if (idx < 2) " ★" else ""
                    ProbabilityMeterRow(
                        label = "#${item.n} ($side)$star",
                        fraction = (item.pct / maxPct).toFloat(),
                        valueText = String.format(Locale.US, "%.1f%%", item.pct),
                        highlight = idx < 2,
                    )
                }
            }
        }
    }
}

@Composable
private fun EngineDetailCard(
    title: String,
    subtitle: String,
    numbers: List<Int>,
    content: @Composable () -> Unit,
) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardSurface,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, p.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleSmall,
                        color = p.textPrimary,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textTertiary,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    numbers.forEach { n -> DigitBadge(digit = n) }
                }
            }
            content()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagWrapRow(tags: List<String>) {
    val p = MixonaTheme.palette
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        tags.forEach { tag ->
            Surface(
                color = p.cardElevated,
                shape = RoundedCornerShape(8.dp),
                border = BorderStroke(1.dp, p.borderSubtle),
            ) {
                Text(
                    text = tag,
                    style = MaterialTheme.typography.labelSmall,
                    color = p.textSecondary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabScreen(
    state: MixonaUiState,
    onDemoClick: () -> Unit,
) {
    val p = MixonaTheme.palette
    val a = state.analysis

    LazyColumn(
        modifier = Modifier.fillMaxWidth(),
        contentPadding = PaddingValues(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        if (a == null) {
            item {
                EngineWaitingState(onDemoClick = onDemoClick)
            }
            return@LazyColumn
        }

        item {
            MixonaSectionCard(
                title = "Phantom Regime DNA (Short → Long)",
                icon = Icons.Rounded.AutoGraph,
            ) {
                a.phantom.rates.forEach { r ->
                    ProbabilityMeterRow(
                        label = "Big · Last ${r.w}",
                        fraction = r.p.toFloat(),
                        valueText = pctStr(r.p),
                    )
                }
            }
        }

        item {
            MixonaSectionCard(
                title = "Forward Shadow Transition Memory",
                icon = Icons.Rounded.Science,
            ) {
                a.phantom.ctx.forEach { c ->
                    ProbabilityMeterRow(
                        label = "${c.k}-bit · ${c.key}",
                        fraction = c.shrunk.toFloat(),
                        valueText = "${c.n} hits",
                    )
                }
            }
        }

        item {
            val mistakeList = state.learnState.mistakes.values
                .sortedWith(compareByDescending<com.example.model.MistakeSignature> { it.cooldown }.thenByDescending { it.losses })
            val activeGuards = mistakeList.count { it.cooldown > 0 }

            MixonaSectionCard(
                title = "Self-Learning Mistake Memory",
                icon = Icons.Rounded.Shield,
                trailing = {
                    MixonaPill(
                        text = "$activeGuards Active Guard${if (activeGuards == 1) "" else "s"}",
                        tint = p.guardAmber,
                    )
                },
            ) {
                if (mistakeList.isEmpty()) {
                    Text(
                        text = "No mistake signatures logged yet. Self-learning adapts automatically after losses.",
                        style = MaterialTheme.typography.bodySmall,
                        color = p.textTertiary,
                        modifier = Modifier.padding(vertical = 8.dp),
                    )
                } else {
                    mistakeList.take(15).forEach { m ->
                        val sideColor = if (m.side == "BIG") p.bigGold else p.smallCyan
                        val active = m.cooldown > 0
                        Surface(
                            color = p.cardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, p.borderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    Text(
                                        text = m.key,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = p.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        text = "Blocked: ${m.side} · Losses: ${m.losses} · Cooldown: ${m.cooldown}r",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = sideColor,
                                    )
                                }
                                MixonaPill(
                                    text = if (active) "GUARDING" else "CLEARED",
                                    tint = if (active) p.guardAmber else p.winEmerald,
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            MixonaSectionCard(
                title = "K-Gram Fingerprints & Backtest",
                icon = Icons.Rounded.Psychology,
            ) {
                val cloudTags = a.phantom.ctx.map { x ->
                    val pText = if (x.p == null) "none" else "${pctStr(x.p)} BIG"
                    "${x.k}-gram [${x.key}] → $pText (${x.n})"
                }
                TagWrapRow(tags = cloudTags)

                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    a.backtests.forEach { bt ->
                        val good = bt.rate >= 0.5
                        Surface(
                            color = p.cardElevated,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, p.borderSubtle),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(11.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = bt.name,
                                        style = MaterialTheme.typography.titleSmall,
                                        color = p.textPrimary,
                                    )
                                    Text(
                                        text = "${bt.correct} / ${bt.tested} chronological rounds",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = p.textSecondary,
                                    )
                                }
                                MixonaPill(
                                    text = pctStr(bt.rate),
                                    tint = if (good) p.winEmerald else p.textSecondary,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EngineWaitingState(onDemoClick: () -> Unit) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, p.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Bolt,
                contentDescription = null,
                tint = p.mint,
                modifier = Modifier.size(36.dp),
            )
            Text(
                text = "Waiting for 100-Draw Sequence",
                style = MaterialTheme.typography.titleMedium,
                color = p.textPrimary,
            )
            Text(
                text = "Auto-sync is connecting to live WinGo feeds, or launch a 100-draw Demo Preview right now.",
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary,
            )
            Spacer(modifier = Modifier.size(4.dp))
            Button(
                onClick = onDemoClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.mint,
                    contentColor = p.bgDeep,
                ),
            ) {
                Text("Run Demo Preview", fontWeight = FontWeight.Bold)
            }
        }
    }
}
