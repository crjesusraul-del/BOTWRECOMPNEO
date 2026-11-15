package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeveloperBoard
import androidx.compose.material.icons.filled.DisplaySettings
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceHardwareProfile
import com.example.model.LsfgDllSummary
import com.example.model.LsfgScaleMode
import com.example.model.MaliDriverStrategy
import com.example.model.RecompExecutionMode
import com.example.model.RecompilerSettings
import com.example.model.TexturePipelineMode
import com.example.ui.components.ObsidianCard
import com.example.ui.components.SectionTitleRow
import com.example.ui.components.TelemetryBadge
import com.example.ui.theme.EdgeAmber
import com.example.ui.theme.EmeraldOptimal
import com.example.ui.theme.ObsidianElevated
import com.example.ui.theme.ObsidianOutline
import com.example.ui.theme.ObsidianVoid
import com.example.ui.theme.SheikahCyan
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.roundToInt

@Composable
fun OptimizerScreen(
    hardware: DeviceHardwareProfile?,
    settings: RecompilerSettings,
    lsfgDll: LsfgDllSummary?,
    onUpdateSettings: ((RecompilerSettings) -> RecompilerSettings) -> Unit,
    onApplyRefreshRate: (Int) -> Unit,
    onPickLsfgDll: () -> Unit,
    onClearLsfgDll: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Execution Mode
        item {
            ObsidianCard(accentColor = SheikahCyan) {
                SectionTitleRow(
                    title = stringResource(R.string.opt_execution_mode),
                    icon = Icons.Filled.DeveloperBoard,
                    accent = SheikahCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                RecompExecutionMode.entries.forEach { mode ->
                    val selected = settings.executionMode == mode
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (selected) SheikahCyan.copy(alpha = 0.14f)
                                else ObsidianElevated.copy(alpha = 0.6f)
                            )
                            .border(
                                1.dp,
                                if (selected) SheikahCyan else ObsidianOutline.copy(alpha = 0.5f),
                                RoundedCornerShape(12.dp)
                            )
                            .clickable {
                                onUpdateSettings { it.copy(executionMode = mode) }
                            }
                            .padding(12.dp)
                            .testTag("exec_mode_${mode.id}")
                    ) {
                        Text(
                            text = mode.titleEs,
                            style = MaterialTheme.typography.titleMedium,
                            color = if (selected) SheikahCyan else TextPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = mode.detailEs,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        // Mali-G610 MC3 Vulkan Graphics Tuning
        item {
            ObsidianCard(accentColor = EdgeAmber) {
                SectionTitleRow(
                    title = stringResource(R.string.opt_graphics_section),
                    icon = Icons.Filled.Tune,
                    accent = EdgeAmber,
                    trailingBadge = "${(settings.resolutionScale * 100).roundToInt()}%"
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.opt_driver_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = EdgeAmber
                )
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Estrategia de Driver Vulkan",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MaliDriverStrategy.entries.forEach { strategy ->
                        val selected = settings.driverStrategy == strategy
                        FilterChip(
                            selected = selected,
                            onClick = { onUpdateSettings { it.copy(driverStrategy = strategy) } },
                            label = { Text(strategy.labelEs) },
                            modifier = Modifier.testTag("driver_${strategy.id}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EdgeAmber.copy(alpha = 0.2f),
                                selectedLabelColor = EdgeAmber
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val renderW = (1920f * settings.resolutionScale).roundToInt()
                val renderH = (1080f * settings.resolutionScale).roundToInt()
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.opt_resolution_scale),
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${"%.2f".format(settings.resolutionScale)}x (${renderW}×${renderH})",
                        style = MaterialTheme.typography.labelMedium,
                        color = SheikahCyan
                    )
                }
                Slider(
                    value = settings.resolutionScale,
                    onValueChange = { valRounded ->
                        val snapped = ((valRounded * 20).roundToInt() / 20f).coerceIn(0.50f, 1.0f)
                        onUpdateSettings { it.copy(resolutionScale = snapped) }
                    },
                    valueRange = 0.50f..1.00f,
                    modifier = Modifier.testTag("resolution_scale_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = SheikahCyan,
                        activeTrackColor = SheikahCyan
                    )
                )

                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = stringResource(R.string.opt_texture_pipeline),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                TexturePipelineMode.entries.forEach { mode ->
                    val selected = settings.texturePipeline == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .minimumInteractiveComponentSize()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (selected) SheikahCyan.copy(alpha = 0.12f)
                                else ObsidianElevated.copy(alpha = 0.55f)
                            )
                            .border(
                                1.dp,
                                if (selected) SheikahCyan else ObsidianOutline.copy(alpha = 0.45f),
                                RoundedCornerShape(10.dp)
                            )
                            .clickable { onUpdateSettings { it.copy(texturePipeline = mode) } }
                            .padding(10.dp)
                            .testTag("texture_${mode.id}"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.labelEs,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) SheikahCyan else TextPrimary
                            )
                            Text(
                                text = mode.detailEs,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OptimizerToggleRow(
                    title = stringResource(R.string.opt_fp16_valhall),
                    subtitle = "Acelera sombreado cel-shaded en Mali-G610 hasta +18% rendimiento",
                    checked = settings.useFp16ValhallShaders,
                    tag = "toggle_fp16",
                    onCheckedChange = { v -> onUpdateSettings { it.copy(useFp16ValhallShaders = v) } }
                )
                OptimizerToggleRow(
                    title = stringResource(R.string.opt_async_shader),
                    subtitle = "Elimina tirones (stutter) al compilar pipelines NVN en paralelo",
                    checked = settings.asyncVulkanPipelineCompile,
                    tag = "toggle_async_shader",
                    onCheckedChange = { v -> onUpdateSettings { it.copy(asyncVulkanPipelineCompile = v) } }
                )
                OptimizerToggleRow(
                    title = stringResource(R.string.opt_tbdr_opt),
                    subtitle = "Evita cargas innecesarias de memoria en arquitectura Tile-Based Mali",
                    checked = settings.enableTbdrTileOptimization,
                    tag = "toggle_tbdr",
                    onCheckedChange = { v -> onUpdateSettings { it.copy(enableTbdrTileOptimization = v) } }
                )
            }
        }

        // pOLED 144Hz Display Refresh Rate & LSFG
        item {
            ObsidianCard(accentColor = SheikahCyan) {
                SectionTitleRow(
                    title = stringResource(R.string.opt_display_refresh),
                    icon = Icons.Filled.DisplaySettings,
                    accent = SheikahCyan,
                    trailingBadge = "${settings.preferredRefreshRateHz} Hz"
                )
                Spacer(modifier = Modifier.height(8.dp))
                val supportedHz = hardware?.displayModes
                    ?.map { it.refreshRateHz.roundToInt() }
                    ?.distinct()
                    ?.sorted()
                    ?.takeIf { it.isNotEmpty() }
                    ?: listOf(60, 90, 120, 144)

                Text(
                    text = "Modos soportados por pantalla actual: ${supportedHz.joinToString(" / ")} Hz",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(60, 90, 120, 144).forEach { hz ->
                        val selected = settings.preferredRefreshRateHz == hz
                        val isRealSupported = supportedHz.any { kotlin.math.abs(it - hz) <= 2 }
                        FilterChip(
                            selected = selected,
                            onClick = {
                                onUpdateSettings { it.copy(preferredRefreshRateHz = hz) }
                                onApplyRefreshRate(hz)
                            },
                            label = {
                                Text(if (isRealSupported) "${hz}Hz" else "${hz}Hz (Perfil)")
                            },
                            modifier = Modifier.testTag("refresh_chip_${hz}hz"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SheikahCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SheikahCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Límite Nativo de FPS del Juego",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(30, 40, 48, 60).forEach { fps ->
                        FilterChip(
                            selected = settings.targetFpsCap == fps,
                            onClick = { onUpdateSettings { it.copy(targetFpsCap = fps) } },
                            label = { Text("$fps FPS") },
                            modifier = Modifier.testTag("fps_cap_$fps"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldOptimal.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldOptimal
                            )
                        )
                    }
                }
            }
        }

        // LSFG Frame Generation & Lossless.dll SAF
        item {
            ObsidianCard(accentColor = EdgeAmber) {
                SectionTitleRow(
                    title = stringResource(R.string.opt_lsfg_section),
                    icon = Icons.Filled.Layers,
                    accent = EdgeAmber,
                    trailingBadge = settings.lsfgMode.labelEs
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    LsfgScaleMode.entries.forEach { mode ->
                        FilterChip(
                            selected = settings.lsfgMode == mode,
                            onClick = { onUpdateSettings { it.copy(lsfgMode = mode) } },
                            label = { Text(mode.id.uppercase()) },
                            modifier = Modifier.testTag("lsfg_${mode.id}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EdgeAmber.copy(alpha = 0.2f),
                                selectedLabelColor = EdgeAmber
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                if (lsfgDll != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Archivo: ${lsfgDll.fileName}",
                                style = MaterialTheme.typography.labelLarge,
                                color = EmeraldOptimal
                            )
                            Text(
                                text = "${lsfgDll.sizeBytes / 1024} KB • SHA-256: ${lsfgDll.sha256Short}…",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                        OutlinedButton(
                            onClick = onClearLsfgDll,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_lsfg_dll_button")
                        ) {
                            Text("Quitar", color = EdgeAmber)
                        }
                    }
                } else {
                    Button(
                        onClick = onPickLsfgDll,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("pick_lsfg_dll_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EdgeAmber,
                            contentColor = ObsidianVoid
                        )
                    ) {
                        Text(stringResource(R.string.opt_lsfg_pick_dll))
                    }
                }
            }
        }

        // CPU Dimensity 7030 & Thermal Guard
        item {
            ObsidianCard(accentColor = EmeraldOptimal) {
                SectionTitleRow(
                    title = stringResource(R.string.opt_cpu_section),
                    icon = Icons.Filled.Bolt,
                    accent = EmeraldOptimal,
                    trailingBadge = "${settings.workerThreadCount} hilos"
                )
                Spacer(modifier = Modifier.height(10.dp))
                OptimizerToggleRow(
                    title = stringResource(R.string.opt_pin_a78),
                    subtitle = "Prioriza física y bucle NVN en los 2 núcleos grandes a 2.5 GHz",
                    checked = settings.pinBigCoresA78,
                    tag = "toggle_pin_a78",
                    onCheckedChange = { v -> onUpdateSettings { it.copy(pinBigCoresA78 = v) } }
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Hilos Workers (Descompresión RomFS)",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${settings.workerThreadCount} / ${hardware?.cpuCoresCount ?: 8}",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldOptimal
                    )
                }
                Slider(
                    value = settings.workerThreadCount.toFloat(),
                    onValueChange = { v ->
                        onUpdateSettings { it.copy(workerThreadCount = v.roundToInt().coerceIn(2, 8)) }
                    },
                    valueRange = 2f..8f,
                    steps = 5,
                    modifier = Modifier.testTag("worker_threads_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldOptimal,
                        activeTrackColor = EmeraldOptimal
                    )
                )

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TelemetryBadge(text = "TÉRMICO", color = EdgeAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Límite Pool Texturas: ${settings.texturePoolLimitMb} MB",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                }
                Slider(
                    value = settings.texturePoolLimitMb.toFloat(),
                    onValueChange = { v ->
                        val snapped = ((v / 256f).roundToInt() * 256).coerceIn(1024, 6144)
                        onUpdateSettings { it.copy(texturePoolLimitMb = snapped) }
                    },
                    valueRange = 1024f..6144f,
                    modifier = Modifier.testTag("texture_pool_slider"),
                    colors = SliderDefaults.colors(
                        thumbColor = EdgeAmber,
                        activeTrackColor = EdgeAmber
                    )
                )

                OptimizerToggleRow(
                    title = stringResource(R.string.opt_thermal_guard),
                    subtitle = "Reduce carga de resolución al superar ${settings.thermalLimitCelsius} °C",
                    checked = settings.thermalGuardEnabled,
                    tag = "toggle_thermal_guard",
                    onCheckedChange = { v -> onUpdateSettings { it.copy(thermalGuardEnabled = v) } }
                )
            }
        }
    }
}

@Composable
private fun OptimizerToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextMuted
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = ObsidianVoid,
                checkedTrackColor = SheikahCyan
            )
        )
    }
}
