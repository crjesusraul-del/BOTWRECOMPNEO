package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SaveAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.BotwGameVersion
import com.example.model.DeviceHardwareProfile
import com.example.model.Edge40NeoMatchStatus
import com.example.model.GameDumpSummary
import com.example.model.OptimizationPreset
import com.example.model.RecompilationPlan
import com.example.model.RecompilerSettings
import com.example.ui.components.MetricCell
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
fun HomeLauncherScreen(
    hardware: DeviceHardwareProfile?,
    settings: RecompilerSettings,
    dumpSummary: GameDumpSummary?,
    recompPlan: RecompilationPlan?,
    isScanningDump: Boolean,
    onSelectDumpFolder: () -> Unit,
    onRescanDump: () -> Unit,
    onSelectGameVersion: (BotwGameVersion) -> Unit,
    onSelectPreset: (OptimizationPreset) -> Unit,
    onRebuildPlan: () -> Unit,
    onExportJson: () -> Unit,
    onOpenBenchmark: () -> Unit,
    onOpenOptimizer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Banner Card
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.large)
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(SheikahCyan.copy(alpha = 0.7f), EdgeAmber.copy(alpha = 0.5f))),
                        MaterialTheme.shapes.large
                    )
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_edge40_banner),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(190.dp),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    ObsidianVoid.copy(alpha = 0.35f),
                                    ObsidianVoid.copy(alpha = 0.92f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TelemetryBadge(
                            text = "BOTWRECOMP ENGINE",
                            color = SheikahCyan
                        )
                        TelemetryBadge(
                            text = "MALI-G610 TUNED",
                            color = EdgeAmber
                        )
                    }
                    Text(
                        text = stringResource(R.string.app_name),
                        style = MaterialTheme.typography.displaySmall,
                        color = TextPrimary
                    )
                    Text(
                        text = stringResource(R.string.hero_target_chip),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                }
            }
        }

        // Hardware Match & Live Edge 40 Neo Telemetry
        item {
            val matchColor = when (hardware?.matchStatus) {
                Edge40NeoMatchStatus.VERIFIED_HARDWARE -> EmeraldOptimal
                Edge40NeoMatchStatus.COMPATIBLE_MALI_ARM64 -> SheikahCyan
                else -> EdgeAmber
            }
            val matchLabel = when (hardware?.matchStatus) {
                Edge40NeoMatchStatus.VERIFIED_HARDWARE -> stringResource(R.string.device_match_verified)
                Edge40NeoMatchStatus.COMPATIBLE_MALI_ARM64 ->
                    "ARM64 Mali detectado (${hardware.gpu.renderer}) — Calibración Edge 40 Neo aplicada"
                else -> stringResource(
                    R.string.device_match_emulated,
                    "${hardware?.manufacturer ?: ""} ${hardware?.model ?: "Android"}".trim()
                )
            }

            ObsidianCard(accentColor = matchColor) {
                SectionTitleRow(
                    title = "Hardware del Dispositivo",
                    icon = Icons.Filled.Memory,
                    accent = matchColor,
                    trailingBadge = hardware?.activeRefreshRateHz?.let { "${it.roundToInt()} Hz" } ?: "…"
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = matchLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = matchColor
                )
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val freeGb = hardware?.let { "%.1f".format(it.availableRamBytes / 1_073_741_824f) } ?: "—"
                    val totalGb = hardware?.let { "%.1f".format(it.totalRamBytes / 1_073_741_824f) } ?: "—"
                    MetricCell(
                        label = "RAM Libre / Total",
                        value = "$freeGb / $totalGb GB",
                        accent = SheikahCyan,
                        modifier = Modifier.weight(1f)
                    )
                    MetricCell(
                        label = "GPU / API",
                        value = hardware?.gpu?.vulkanVersionString ?: "Probing…",
                        accent = EdgeAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MetricCell(
                        label = "Núcleos CPU",
                        value = "${hardware?.cpuCoresCount ?: 8} hilos (${hardware?.socModel ?: "ARM64"})",
                        accent = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    val tempStr = hardware?.let {
                        if (it.batteryTempCelsius > 0f) "${"%.1f".format(it.batteryTempCelsius)} °C" else it.thermalStatus.labelEs
                    } ?: "—"
                    MetricCell(
                        label = "Estado Térmico",
                        value = tempStr,
                        accent = if ((hardware?.batteryTempCelsius ?: 0f) >= 42f) EdgeAmber else EmeraldOptimal,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Legal Game Dump Picker (Zero-Permission SAF)
        item {
            ObsidianCard(accentColor = if (dumpSummary?.isReadyForRecomp == true) EmeraldOptimal else SheikahCyan) {
                SectionTitleRow(
                    title = stringResource(R.string.section_game_dump),
                    icon = Icons.Filled.FolderOpen,
                    accent = SheikahCyan,
                    trailingBadge = dumpSummary?.let { "${it.totalFilesScanned} arch." }
                )
                Spacer(modifier = Modifier.height(10.dp))

                AnimatedVisibility(visible = isScanningDump) {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = stringResource(R.string.dump_scanning),
                            style = MaterialTheme.typography.bodyMedium,
                            color = SheikahCyan
                        )
                        LinearProgressIndicator(
                            modifier = Modifier.fillMaxWidth(),
                            color = SheikahCyan,
                            trackColor = ObsidianElevated
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (dumpSummary == null) {
                    Text(
                        text = stringResource(R.string.dump_no_folder),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    val sizeMb = dumpSummary.totalSizeBytes / (1024.0 * 1024.0)
                    val sizeText = if (sizeMb >= 1024.0) {
                        "${"%.2f".format(sizeMb / 1024.0)} GB"
                    } else {
                        "${"%.1f".format(sizeMb)} MB"
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Carpeta: ${dumpSummary.folderName}",
                                style = MaterialTheme.typography.titleMedium,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${dumpSummary.totalFilesScanned} archivos • $sizeText",
                                style = MaterialTheme.typography.labelMedium,
                                color = SheikahCyan
                            )
                        }
                        IconButton(
                            onClick = onRescanDump,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("rescan_dump_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.dump_rescan),
                                tint = SheikahCyan
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        DumpStructureTag("exefs", dumpSummary.hasExefs)
                        DumpStructureTag("romfs", dumpSummary.hasRomfs)
                        DumpStructureTag("main", dumpSummary.hasMainBinary)
                        DumpStructureTag("Pack/", dumpSummary.hasPackDir)
                        DumpStructureTag("Shader/", dumpSummary.hasShaderDir)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = stringResource(R.string.dump_version_label),
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BotwGameVersion.entries.forEach { version ->
                        val selected = settings.gameVersion == version
                        FilterChip(
                            selected = selected,
                            onClick = { onSelectGameVersion(version) },
                            label = { Text(version.code) },
                            modifier = Modifier.testTag("version_chip_${version.code}"),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = SheikahCyan.copy(alpha = 0.2f),
                                selectedLabelColor = SheikahCyan
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onSelectDumpFolder,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("select_dump_folder_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SheikahCyan,
                        contentColor = ObsidianVoid
                    )
                ) {
                    Icon(Icons.Filled.FolderOpen, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.dump_select_folder))
                }
            }
        }

        // Edge 40 Neo Preset Profiles
        item {
            ObsidianCard(accentColor = EdgeAmber) {
                SectionTitleRow(
                    title = stringResource(R.string.section_presets),
                    icon = Icons.Filled.Speed,
                    accent = EdgeAmber,
                    trailingBadge = settings.preset.titleEs
                )
                Spacer(modifier = Modifier.height(12.dp))
                PresetOptionTile(
                    title = stringResource(R.string.preset_balanced_title),
                    description = stringResource(R.string.preset_balanced_desc),
                    selected = settings.preset == OptimizationPreset.EDGE40_BALANCED,
                    accent = SheikahCyan,
                    tag = "preset_balanced",
                    onClick = { onSelectPreset(OptimizationPreset.EDGE40_BALANCED) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PresetOptionTile(
                    title = stringResource(R.string.preset_ultra_title),
                    description = stringResource(R.string.preset_ultra_desc),
                    selected = settings.preset == OptimizationPreset.EDGE40_ULTRA_144HZ,
                    accent = EdgeAmber,
                    tag = "preset_ultra_144",
                    onClick = { onSelectPreset(OptimizationPreset.EDGE40_ULTRA_144HZ) }
                )
                Spacer(modifier = Modifier.height(8.dp))
                PresetOptionTile(
                    title = stringResource(R.string.preset_thermal_title),
                    description = stringResource(R.string.preset_thermal_desc),
                    selected = settings.preset == OptimizationPreset.EDGE40_THERMAL_30FPS,
                    accent = EmeraldOptimal,
                    tag = "preset_thermal_30",
                    onClick = { onSelectPreset(OptimizationPreset.EDGE40_THERMAL_30FPS) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenOptimizer,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("open_custom_optimizer_button")
                ) {
                    Icon(Icons.Filled.Build, contentDescription = null, tint = EdgeAmber)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Ajuste fino Mali-G610 y Dimensity 7030", color = TextPrimary)
                }
            }
        }

        // Recompilation Plan & Readiness Checklist
        item {
            ObsidianCard(accentColor = SheikahCyan) {
                SectionTitleRow(
                    title = stringResource(R.string.section_pipeline),
                    icon = Icons.Filled.Build,
                    accent = SheikahCyan,
                    trailingBadge = recompPlan?.let { "~${it.estimatedMemoryFootprintMb} MB RAM" }
                )
                Spacer(modifier = Modifier.height(10.dp))

                recompPlan?.readinessChecks?.forEach { check ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                            contentDescription = stringResource(R.string.cd_status_icon),
                            tint = if (check.passed) EmeraldOptimal else EdgeAmber,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = check.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = TextPrimary
                            )
                            Text(
                                text = check.detail,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                if (recompPlan != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(ObsidianVoid)
                            .border(1.dp, ObsidianOutline, RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = "${recompPlan.targetArchitectureFlags}\n${recompPlan.vulkanProfileFlags}",
                            style = MaterialTheme.typography.labelSmall,
                            color = SheikahCyan
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRebuildPlan,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("rebuild_plan_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SheikahCyan,
                            contentColor = ObsidianVoid
                        )
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Preparar")
                    }
                    OutlinedButton(
                        onClick = onExportJson,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("export_json_button")
                    ) {
                        Icon(Icons.Filled.SaveAlt, contentDescription = null, tint = EdgeAmber)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar JSON", color = TextPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onOpenBenchmark,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("goto_benchmark_button")
                ) {
                    Icon(Icons.Filled.Speed, contentDescription = null, tint = SheikahCyan)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Probar perfil en Benchmark Real GLES/pOLED", color = SheikahCyan)
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Proyecto compatible con mamcjr/BOTWRecompAndroid sin ROMs ni claves incluidas.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun DumpStructureTag(label: String, present: Boolean) {
    val color = if (present) EmeraldOptimal else TextMuted
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.45f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp)
    ) {
        Text(
            text = if (present) "✓ $label" else "• $label",
            style = MaterialTheme.typography.labelSmall,
            color = color
        )
    }
}

@Composable
private fun PresetOptionTile(
    title: String,
    description: String,
    selected: Boolean,
    accent: Color,
    tag: String,
    onClick: () -> Unit
) {
    val bg = if (selected) accent.copy(alpha = 0.12f) else ObsidianElevated.copy(alpha = 0.6f)
    val border = if (selected) accent else ObsidianOutline.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(RoundedCornerShape(14.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(12.dp)
            .testTag(tag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = if (selected) accent else TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        if (selected) {
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Filled.CheckCircle,
                contentDescription = null,
                tint = accent
            )
        }
    }
}
