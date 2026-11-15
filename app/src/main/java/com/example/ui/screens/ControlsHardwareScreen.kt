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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.model.DeviceHardwareProfile
import com.example.model.TouchHudLayoutConfig
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
fun ControlsHardwareScreen(
    hardware: DeviceHardwareProfile?,
    touchHud: TouchHudLayoutConfig,
    onRefreshHardware: () -> Unit,
    onUpdateTouchHud: ((TouchHudLayoutConfig) -> TouchHudLayoutConfig) -> Unit,
    onTriggerHaptics: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Real Gamepad / Controller Detection
        item {
            val gamepads = hardware?.connectedGamepads.orEmpty()
            ObsidianCard(accentColor = if (gamepads.isNotEmpty()) EmeraldOptimal else SheikahCyan) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        SectionTitleRow(
                            title = stringResource(R.string.ctrl_gamepad_section),
                            icon = Icons.Filled.Gamepad,
                            accent = if (gamepads.isNotEmpty()) EmeraldOptimal else SheikahCyan
                        )
                    }
                    IconButton(
                        onClick = onRefreshHardware,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_gamepads_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.ctrl_gamepad_scan),
                            tint = SheikahCyan
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                if (gamepads.isEmpty()) {
                    Text(
                        text = stringResource(R.string.ctrl_gamepad_none),
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary
                    )
                } else {
                    gamepads.forEach { pad ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(ObsidianElevated)
                                .border(1.dp, EmeraldOptimal.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Gamepad,
                                contentDescription = stringResource(R.string.cd_gamepad_icon),
                                tint = EmeraldOptimal
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = pad.name,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "VID:0x${pad.vendorId.toString(16)} PID:0x${pad.productId.toString(16)} • Vibración: ${if (pad.hasVibrator) "Sí" else "No"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Edge 40 Neo Curved pOLED Touch HUD Calibrator
        item {
            ObsidianCard(accentColor = EdgeAmber) {
                SectionTitleRow(
                    title = stringResource(R.string.ctrl_touch_hud_section),
                    icon = Icons.Filled.TouchApp,
                    accent = EdgeAmber,
                    trailingBadge = "${touchHud.edgeRejectionDp.roundToInt()} dp"
                )
                Spacer(modifier = Modifier.height(10.dp))

                // Interactive visual preview of Curved Edge pOLED safe zone + virtual Switch buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(155.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(ObsidianVoid)
                        .border(1.5.dp, EdgeAmber.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                ) {
                    // Curved edge rejection shading left & right
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = touchHud.edgeRejectionDp.dp)
                            .border(1.dp, SheikahCyan.copy(alpha = 0.35f), RoundedCornerShape(10.dp))
                    ) {
                        // Left stick preview
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(14.dp)
                                .size((48f * touchHud.controlScale).dp)
                                .alpha(touchHud.controlOpacity)
                                .clip(CircleShape)
                                .background(SheikahCyan.copy(alpha = 0.18f))
                                .border(1.5.dp, SheikahCyan, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("L", style = MaterialTheme.typography.labelLarge, color = SheikahCyan)
                        }

                        // Right ABXY cluster preview
                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(14.dp)
                                .alpha(touchHud.controlOpacity),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf("B", "A", "Y", "X").forEach { btn ->
                                Box(
                                    modifier = Modifier
                                        .size((28f * touchHud.controlScale).dp)
                                        .clip(CircleShape)
                                        .background(EdgeAmber.copy(alpha = 0.2f))
                                        .border(1.dp, EdgeAmber, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(btn, style = MaterialTheme.typography.labelSmall, color = EdgeAmber)
                                }
                            }
                        }

                        Text(
                            text = "ZONA SEGURA 20:9 pOLED",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted,
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "${stringResource(R.string.ctrl_edge_rejection)}: ${touchHud.edgeRejectionDp.roundToInt()} dp",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Slider(
                    value = touchHud.edgeRejectionDp,
                    onValueChange = { v -> onUpdateTouchHud { it.copy(edgeRejectionDp = v.roundToInt().toFloat()) } },
                    valueRange = 4f..32f,
                    modifier = Modifier.testTag("edge_rejection_slider"),
                    colors = SliderDefaults.colors(thumbColor = EdgeAmber, activeTrackColor = EdgeAmber)
                )

                Text(
                    text = "${stringResource(R.string.ctrl_hud_opacity)}: ${(touchHud.controlOpacity * 100).roundToInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Slider(
                    value = touchHud.controlOpacity,
                    onValueChange = { v -> onUpdateTouchHud { it.copy(controlOpacity = v) } },
                    valueRange = 0.25f..1.0f,
                    modifier = Modifier.testTag("hud_opacity_slider"),
                    colors = SliderDefaults.colors(thumbColor = SheikahCyan, activeTrackColor = SheikahCyan)
                )

                Text(
                    text = "${stringResource(R.string.ctrl_hud_scale)}: ${"%.2f".format(touchHud.controlScale)}x",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextPrimary
                )
                Slider(
                    value = touchHud.controlScale,
                    onValueChange = { v -> onUpdateTouchHud { it.copy(controlScale = v) } },
                    valueRange = 0.75f..1.30f,
                    modifier = Modifier.testTag("hud_scale_slider"),
                    colors = SliderDefaults.colors(thumbColor = SheikahCyan, activeTrackColor = SheikahCyan)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.ctrl_gyro_aim),
                        style = MaterialTheme.typography.labelLarge,
                        color = TextPrimary
                    )
                    Switch(
                        checked = touchHud.gyroAimEnabled,
                        onCheckedChange = { v -> onUpdateTouchHud { it.copy(gyroAimEnabled = v) } },
                        modifier = Modifier.testTag("gyro_aim_switch"),
                        colors = SwitchDefaults.colors(checkedThumbColor = ObsidianVoid, checkedTrackColor = SheikahCyan)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onTriggerHaptics,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("test_haptics_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EdgeAmber,
                        contentColor = ObsidianVoid
                    )
                ) {
                    Icon(Icons.Filled.Vibration, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.ctrl_haptic_test))
                }
            }
        }

        // Full Real Hardware Telemetry Diagnostics
        item {
            ObsidianCard(accentColor = SheikahCyan) {
                SectionTitleRow(
                    title = stringResource(R.string.ctrl_telemetry_section),
                    icon = Icons.Filled.Memory,
                    accent = SheikahCyan
                )
                Spacer(modifier = Modifier.height(10.dp))
                if (hardware != null) {
                    TelemetryLine("Modelo", "${hardware.manufacturer} ${hardware.model} (${hardware.deviceCode})")
                    TelemetryLine("SoC / Hardware", hardware.socModel)
                    TelemetryLine("ABIs", hardware.supportedAbis.joinToString(", "))
                    TelemetryLine("GPU Vendor", hardware.gpu.vendor)
                    TelemetryLine("GPU Renderer", hardware.gpu.renderer)
                    TelemetryLine("OpenGL ES", hardware.gpu.glVersion)
                    TelemetryLine("Vulkan", hardware.gpu.vulkanVersionString)
                    TelemetryLine("Textura Máx.", "${hardware.gpu.maxTextureSize} px • ASTC: ${if (hardware.gpu.supportsAstcLdr) "Sí" else "No"}")
                    TelemetryLine("Batería / Temp.", "${hardware.batteryPercent}% • ${"%.1f".format(hardware.batteryTempCelsius)} °C (${hardware.thermalStatus.labelEs})")

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Topología de Núcleos CPU (${hardware.cpuCoresCount})",
                        style = MaterialTheme.typography.labelMedium,
                        color = EdgeAmber
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    hardware.cpuCores.forEach { core ->
                        val freqTxt = core.currentFreqKhz?.let { "${it / 1000} MHz" } ?: "Sysfs restringido"
                        val maxTxt = core.maxFreqKhz?.let { " / max ${it / 1000} MHz" } ?: ""
                        TelemetryLine("CPU #${core.index} (${core.clusterLabel})", "$freqTxt$maxTxt")
                    }
                }
            }
        }
    }
}

@Composable
private fun TelemetryLine(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextMuted,
            modifier = Modifier.weight(0.42f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            color = TextPrimary,
            modifier = Modifier.weight(0.58f)
        )
    }
}
