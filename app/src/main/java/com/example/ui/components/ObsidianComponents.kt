package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ui.theme.EdgeAmber
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SheikahCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LiveBenchmarkState

@Composable
fun ObsidianCard(
    modifier: Modifier = Modifier,
    accentColor: Color = ObsidianOutline,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderBrush = Brush.linearGradient(
        colors = listOf(
            accentColor.copy(alpha = 0.65f),
            ObsidianOutline.copy(alpha = 0.45f)
        )
    )
    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = ObsidianSurface,
            border = BorderStroke(1.dp, borderBrush),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            color = ObsidianSurface,
            border = BorderStroke(1.dp, borderBrush),
            tonalElevation = 2.dp
        ) {
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}

@Composable
fun SectionTitleRow(
    title: String,
    icon: ImageVector,
    accent: Color = SheikahCyan,
    trailingBadge: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(accent.copy(alpha = 0.14f))
                    .border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingBadge != null) {
            Spacer(modifier = Modifier.width(8.dp))
            TelemetryBadge(text = trailingBadge, color = accent)
        }
    }
}

@Composable
fun TelemetryBadge(
    text: String,
    color: Color = SheikahCyan,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.13f))
            .border(1.dp, color.copy(alpha = 0.4f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontWeight = FontWeight.Bold,
            maxLines = 1
        )
    }
}

@Composable
fun MetricCell(
    label: String,
    value: String,
    accent: Color = SheikahCyan,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianElevated.copy(alpha = 0.85f))
            .border(1.dp, ObsidianOutline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.labelLarge,
            color = accent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FrameTimeSparkline(
    samplesMs: List<Float>,
    modifier: Modifier = Modifier
) {
    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(92.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(ObsidianVoid.copy(alpha = 0.85f))
            .border(1.dp, ObsidianOutline.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
            .padding(8.dp)
    ) {
        val maxMs = 40f
        val w = size.width
        val h = size.height

        // 16.6ms (60 FPS) guide line
        val y60 = h - (16.6f / maxMs).coerceIn(0f, 1f) * h
        drawLine(
            color = EmeraldOptimal.copy(alpha = 0.35f),
            start = Offset(0f, y60),
            end = Offset(w, y60),
            strokeWidth = 1.5f
        )

        // 8.3ms (120 FPS) guide line
        val y120 = h - (8.33f / maxMs).coerceIn(0f, 1f) * h
        drawLine(
            color = SheikahCyan.copy(alpha = 0.30f),
            start = Offset(0f, y120),
            end = Offset(w, y120),
            strokeWidth = 1.5f
        )

        if (samplesMs.size < 2) return@Canvas

        val path = Path()
        val stepX = w / (samplesMs.size - 1).coerceAtLeast(1)
        samplesMs.forEachIndexed { idx, ms ->
            val x = idx * stepX
            val norm = (ms / maxMs).coerceIn(0.02f, 1f)
            val y = h - norm * h
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = SheikahCyan,
            style = Stroke(width = 3.2f, cap = StrokeCap.Round)
        )
    }
}

@Composable
fun CompactPerformanceHud(
    live: LiveBenchmarkState,
    ramFreeMb: Long,
    tempC: Float,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(ObsidianVoid.copy(alpha = 0.82f))
            .border(1.dp, SheikahCyan.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (live.isRunning) EmeraldOptimal else EdgeAmber)
        )
        Text(
            text = "${"%.0f".format(live.currentFps)} FPS",
            style = MaterialTheme.typography.labelMedium,
            color = SheikahCyan,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "${"%.1f".format(live.currentFrameTimeMs)} ms",
            style = MaterialTheme.typography.labelMedium,
            color = TextPrimary
        )
        Text(
            text = "RAM ${ramFreeMb}MB",
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        if (tempC > 0f) {
            Text(
                text = "${"%.1f".format(tempC)}°C",
                style = MaterialTheme.typography.labelMedium,
                color = if (tempC >= 42f) EdgeAmber else EmeraldOptimal
            )
        }
    }
}

@Composable
fun DetailedPerformanceHud(
    live: LiveBenchmarkState,
    ramFreeMb: Long,
    tempC: Float,
    refreshHz: Float,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(ObsidianVoid.copy(alpha = 0.84f))
            .border(1.dp, SheikahCyan.copy(alpha = 0.45f), RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "HUD MALI-G610 • ${refreshHz.toInt()}Hz pOLED",
                style = MaterialTheme.typography.labelSmall,
                color = EdgeAmber,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${live.renderWidthPx}×${live.renderHeightPx}",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricCell(
                label = "FPS Actual",
                value = "${"%.1f".format(live.currentFps)}",
                accent = SheikahCyan,
                modifier = Modifier.weight(1f)
            )
            MetricCell(
                label = "1% Low",
                value = "${"%.1f".format(live.low1PercentFps)}",
                accent = EdgeAmber,
                modifier = Modifier.weight(1f)
            )
            MetricCell(
                label = "Frame",
                value = "${"%.1f".format(live.currentFrameTimeMs)} ms",
                accent = EmeraldOptimal,
                modifier = Modifier.weight(1f)
            )
        }
        FrameTimeSparkline(samplesMs = live.frameTimeHistoryMs)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "RAM Libre: $ramFreeMb MB",
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Text(
                text = if (tempC > 0f) "Batería/Térmica: ${"%.1f".format(tempC)} °C" else "Térmica: N/D",
                style = MaterialTheme.typography.labelSmall,
                color = if (tempC >= 42f) EdgeAmber else TextSecondary
            )
        }
    }
}
