package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ApiFeedMode
import com.example.model.ConnectionState
import com.example.ui.ToastEvent
import com.example.ui.theme.MixonaTheme

@Composable
fun MixonaLogoMark(modifier: Modifier = Modifier) {
    val p = MixonaTheme.palette
    Box(
        modifier = modifier
            .size(38.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(p.mint, p.smallCyan, p.violet),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(22.dp)) {
            val w = size.width
            val h = size.height
            val path = Path().apply {
                moveTo(w * 0.1f, h * 0.82f)
                lineTo(w * 0.1f, h * 0.22f)
                lineTo(w * 0.5f, h * 0.62f)
                lineTo(w * 0.9f, h * 0.22f)
                lineTo(w * 0.9f, h * 0.82f)
            }
            drawPath(
                path = path,
                color = Color.White,
                style = Stroke(width = w * 0.14f, cap = StrokeCap.Round),
            )
            drawCircle(
                color = Color(0xFFFBBF24),
                radius = w * 0.1f,
                center = Offset(w * 0.5f, h * 0.24f),
            )
        }
    }
}

@Composable
fun MixonaTopBar(
    connectionState: ConnectionState,
    clockText: String,
    isDarkTheme: Boolean,
    onToggleTheme: () -> Unit,
    showBrand: Boolean = true,
) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardSurface.copy(alpha = 0.92f),
        border = BorderStroke(1.dp, p.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            if (showBrand) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    MixonaLogoMark()
                    Column {
                        Text(
                            text = "MIXONA",
                            style = MaterialTheme.typography.titleMedium,
                            color = p.textPrimary,
                            letterSpacing = 1.2.sp,
                        )
                        Text(
                            text = "PHANTOM + SELF-LEARN",
                            style = MaterialTheme.typography.labelSmall,
                            color = p.textTertiary,
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.width(4.dp))
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                ConnectionChip(connectionState = connectionState)

                Surface(
                    color = p.cardElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, p.borderSubtle),
                ) {
                    Text(
                        text = clockText,
                        style = MaterialTheme.typography.labelMedium,
                        color = p.textSecondary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }

                IconButton(
                    onClick = onToggleTheme,
                    modifier = Modifier
                        .size(48.dp)
                        .testTag("theme_toggle_button"),
                ) {
                    Icon(
                        imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                        contentDescription = if (isDarkTheme) "Switch to light mode" else "Switch to dark mode",
                        tint = p.textPrimary,
                    )
                }
            }
        }
    }
}

@Composable
fun ConnectionChip(connectionState: ConnectionState) {
    val p = MixonaTheme.palette
    val (label, color) = when (connectionState) {
        ConnectionState.CONNECTING -> "SYNCING" to p.smallCyan
        ConnectionState.LIVE -> "LIVE AUTO" to p.winEmerald
        ConnectionState.DEMO -> "DEMO MODE" to p.bigGold
        ConnectionState.ERROR -> "RETRYING" to p.lossCoral
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )

    Surface(
        color = color.copy(alpha = 0.12f),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.38f)),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(6.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = alpha)),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = color,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
fun SyncControlBar(
    statusMessage: String,
    apiMode: ApiFeedMode,
    onSelectApiMode: (ApiFeedMode) -> Unit,
    onDemoClick: () -> Unit,
) {
    val p = MixonaTheme.palette
    var menuExpanded by remember { mutableStateOf(false) }

    Surface(
        color = p.cardSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, p.borderSubtle),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = statusMessage,
                style = MaterialTheme.typography.bodySmall,
                color = p.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        color = p.cardElevated,
                        shape = RoundedCornerShape(11.dp),
                        border = BorderStroke(1.dp, p.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .clickable { menuExpanded = true }
                            .testTag("api_feed_selector"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = apiMode.label,
                                style = MaterialTheme.typography.labelMedium,
                                color = p.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Select API Feed",
                                tint = p.textSecondary,
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                    ) {
                        ApiFeedMode.entries.forEach { mode ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = mode.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                },
                                onClick = {
                                    menuExpanded = false
                                    onSelectApiMode(mode)
                                },
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onDemoClick,
                    shape = RoundedCornerShape(11.dp),
                    border = BorderStroke(1.dp, p.borderStrong),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = p.cardElevated,
                        contentColor = p.bigGold,
                    ),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("demo_preview_button"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "DEMO",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
fun MixonaSectionCard(
    title: String,
    icon: ImageVector? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier,
    highlightBorder: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val p = MixonaTheme.palette
    Surface(
        color = p.cardSurface,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, if (highlightBorder) p.borderStrong else p.borderSubtle),
        tonalElevation = 2.dp,
        modifier = modifier.fillMaxWidth(),
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    if (icon != null) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = p.mint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        color = p.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (trailing != null) {
                    trailing()
                }
            }
            content()
        }
    }
}

@Composable
fun MixonaPill(
    text: String,
    tint: Color? = null,
    modifier: Modifier = Modifier,
) {
    val p = MixonaTheme.palette
    val resolvedTint = tint ?: p.textSecondary
    val bg = if (tint != null) tint.copy(alpha = 0.12f) else p.cardElevated
    val border = if (tint != null) tint.copy(alpha = 0.35f) else p.borderSubtle

    Surface(
        color = bg,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, border),
        modifier = modifier,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = resolvedTint,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp),
        )
    }
}

@Composable
fun ProbabilityMeterRow(
    label: String,
    fraction: Float,
    valueText: String,
    highlight: Boolean = false,
) {
    val p = MixonaTheme.palette
    val animatedProgress by animateFloatAsState(
        targetValue = fraction.coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
        label = "meterProgress",
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = if (highlight) p.bigGold else p.textSecondary,
            fontWeight = if (highlight) FontWeight.Bold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.width(126.dp),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(p.cardElevated)
                .border(0.5.dp, p.borderSubtle, RoundedCornerShape(999.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(animatedProgress)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(999.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = if (highlight) {
                                listOf(p.bigGold, p.mint)
                            } else {
                                listOf(p.mint, p.smallCyan, p.violet)
                            },
                        ),
                    ),
            )
        }
        Text(
            text = valueText,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlight) p.bigGold else p.textPrimary,
            modifier = Modifier.width(54.dp),
        )
    }
}

@Composable
fun DigitBadge(
    digit: Int,
    sizeLabel: Boolean = false,
    accentOverride: Color? = null,
) {
    val p = MixonaTheme.palette
    val bg = accentOverride?.copy(alpha = 0.14f) ?: p.mint.copy(alpha = 0.14f)
    val border = accentOverride?.copy(alpha = 0.5f) ?: p.borderStrong
    val fg = accentOverride ?: p.textPrimary

    Surface(
        color = bg,
        shape = RoundedCornerShape(9.dp),
        border = BorderStroke(1.dp, border),
    ) {
        Text(
            text = if (sizeLabel) "$digit" else "$digit",
            style = MaterialTheme.typography.labelLarge,
            color = fg,
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
        )
    }
}

@Composable
fun MixonaToastOverlay(
    toast: ToastEvent?,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val p = MixonaTheme.palette
    AnimatedVisibility(
        visible = toast != null,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier,
    ) {
        toast?.let { event ->
            val accent = when (event.tone) {
                "gold" -> p.bigGold
                "success" -> p.winEmerald
                "error" -> p.lossCoral
                else -> p.smallCyan
            }
            Surface(
                color = p.cardSurface,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, accent.copy(alpha = 0.5f)),
                shadowElevation = 10.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clickable { onDismiss() },
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bolt,
                        contentDescription = null,
                        tint = accent,
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = event.title,
                            style = MaterialTheme.typography.titleSmall,
                            color = p.textPrimary,
                        )
                        if (event.detail.isNotEmpty()) {
                            Text(
                                text = event.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = p.textSecondary,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ConfirmActionDialog(
    title: String,
    description: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val p = MixonaTheme.palette
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = p.cardSurface,
        titleContentColor = p.textPrimary,
        textContentColor = p.textSecondary,
        title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
        text = { Text(text = description, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(
                    containerColor = p.lossCoral,
                    contentColor = Color.White,
                ),
                modifier = Modifier.testTag("dialog_confirm_button"),
            ) {
                Text(text = confirmText)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_cancel_button"),
            ) {
                Text(text = "Cancel", color = p.textSecondary)
            }
        },
    )
}
