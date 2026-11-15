package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.db.BenchmarkRunEntity
import com.example.model.DeviceHardwareProfile
import com.example.model.RecompilerSettings
import com.example.ui.benchmark.MaliShaderBenchmarkView
import com.example.ui.components.CompactPerformanceHud
import com.example.ui.components.DetailedPerformanceHud
import com.example.ui.components.MetricCell
import com.example.ui.components.ObsidianCard
import com.example.ui.components.SectionTitleRow
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.CoralAlert
import com.example.ui.theme.EdgeAmber
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SheikahCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.LiveBenchmarkState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun BenchmarkScreen(
    hardware: DeviceHardwareProfile?,
    settings: RecompilerSettings,
    liveBenchmark: LiveBenchmarkState,
    benchmarkRuns: List<BenchmarkRunEntity>,
    onStartBenchmark: () -> Unit,
    onStopAndSaveBenchmark: () -> Unit,
    onFrameSample: (Float, Int, Int) -> Unit,
    onDeleteRun: (Long) -> Unit,
    onClearHistory: () -> Unit
) {
    var detailedOverlay by rememberSaveable { mutableStateOf(true) }
    val freeRamMb = (hardware?.availableRamBytes ?: 0L) / (1024L * 1024L)
    val tempC = hardware?.batteryTempCelsius ?: 0f
    val refreshHz = hardware?.activeRefreshRateHz ?: settings.preferredRefreshRateHz.toFloat()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            ObsidianCard(accentColor = SheikahCyan) {
                SectionTitleRow(
                    title = stringResource(R.string.bench_title),
                    icon = Icons.Filled.GraphicEq,
                    accent = SheikahCyan,
                    trailingBadge = "${(settings.resolutionScale * 100).roundToInt()}% • ${if (settings.useFp16ValhallShaders) "FP16" else "FP32"}"
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Real GLES 3.0 Cel-Shaded Shader Viewport + Live HUD Overlay
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, ObsidianOutline, RoundedCornerShape(14.dp))
                ) {
                    MaliShaderBenchmarkView(
                        isRunning = liveBenchmark.isRunning,
                        resolutionScale = settings.resolutionScale,
                        useFp16Valhall = settings.useFp16ValhallShaders,
                        modifier = Modifier.fillMaxSize(),
                        onFrameSample = onFrameSample
                    )

                    if (!detailedOverlay) {
                        CompactPerformanceHud(
                            live = liveBenchmark,
                            ramFreeMb = freeRamMb,
                            tempC = tempC,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(10.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(10.dp)
                        ) {
                            DetailedPerformanceHud(
                                live = liveBenchmark,
                                ramFreeMb = freeRamMb,
                                tempC = tempC,
                                refreshHz = refreshHz
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !detailedOverlay,
                        onClick = { detailedOverlay = false },
                        label = { Text(stringResource(R.string.bench_overlay_compact)) },
                        modifier = Modifier.testTag("overlay_compact_chip"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SheikahCyan.copy(alpha = 0.2f),
                            selectedLabelColor = SheikahCyan
                        )
                    )
                    FilterChip(
                        selected = detailedOverlay,
                        onClick = { detailedOverlay = true },
                        label = { Text(stringResource(R.string.bench_overlay_detailed)) },
                        modifier = Modifier.testTag("overlay_detailed_chip"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SheikahCyan.copy(alpha = 0.2f),
                            selectedLabelColor = SheikahCyan
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCell(
                        label = "FPS Promedio",
                        value = "${"%.1f".format(liveBenchmark.avgFps)}",
                        accent = SheikahCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCell(
                        label = "Frames",
                        value = "${liveBenchmark.totalFramesRendered}",
                        accent = EdgeAmber,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCell(
                        label = "Modo",
                        value = settings.preset.titleEs.substringBefore(" "),
                        accent = EmeraldOptimal,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (liveBenchmark.isRunning) onStopAndSaveBenchmark() else onStartBenchmark()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_benchmark_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (liveBenchmark.isRunning) CoralAlert else SheikahCyan,
                        contentColor = ObsidianVoid
                    )
                ) {
                    Icon(
                        imageVector = if (liveBenchmark.isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (liveBenchmark.isRunning) stringResource(R.string.bench_stop)
                        else stringResource(R.string.bench_start)
                    )
                }
            }
        }

        // Room Persisted Benchmark History
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.History,
                        contentDescription = null,
                        tint = EdgeAmber,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.bench_history_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary
                    )
                }
                if (benchmarkRuns.isNotEmpty()) {
                    TextButton(
                        onClick = onClearHistory,
                        modifier = Modifier.testTag("clear_benchmark_history_button")
                    ) {
                        Text(stringResource(R.string.bench_clear_history), color = EdgeAmber)
                    }
                }
            }
        }

        if (benchmarkRuns.isEmpty()) {
            item {
                ObsidianCard {
                    Text(
                        text = stringResource(R.string.bench_history_empty),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        } else {
            items(benchmarkRuns, key = { it.id }) { run ->
                BenchmarkRunRow(
                    run = run,
                    onDelete = { onDeleteRun(run.id) }
                )
            }
        }
    }
}

@Composable
private fun BenchmarkRunRow(
    run: BenchmarkRunEntity,
    onDelete: () -> Unit
) {
    val dateStr = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(run.timestampMs))
    ObsidianCard(accentColor = ObsidianOutline) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "${"%.1f".format(run.avgFps)} FPS",
                        style = MaterialTheme.typography.titleLarge,
                        color = SheikahCyan
                    )
                    TelemetryBadge(
                        text = "${(run.resolutionScale * 100).roundToInt()}% • ${run.refreshRateHz}Hz",
                        color = EdgeAmber
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${run.presetName} • ${if (run.fp16Enabled) "FP16 Valhall" else "FP32"}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary
                )
                Text(
                    text = "1% Low: ${"%.1f".format(run.low1PercentFps)} FPS • ${"%.1f".format(run.avgFrameTimeMs)} ms • $dateStr",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextMuted
                )
                Text(
                    text = "GPU: ${run.gpuRenderer}",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_run_${run.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_run),
                    tint = TextMuted
                )
            }
        }
    }
}
