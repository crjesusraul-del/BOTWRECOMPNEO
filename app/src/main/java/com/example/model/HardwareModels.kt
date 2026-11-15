package com.example.model

enum class Edge40NeoMatchStatus {
    VERIFIED_HARDWARE,
    COMPATIBLE_MALI_ARM64,
    CALIBRATION_PROFILE_ACTIVE
}

enum class ThermalStatusLevel(val labelEs: String) {
    NONE("Frío / Óptimo"),
    LIGHT("Ligero"),
    MODERATE("Moderado"),
    SEVERE("Alto — Limitar FPS"),
    CRITICAL("Crítico — Throttling"),
    UNKNOWN("No disponible")
}

data class CpuCoreInfo(
    val index: Int,
    val currentFreqKhz: Long?,
    val maxFreqKhz: Long?,
    val clusterLabel: String
)

data class GpuCapabilities(
    val vendor: String,
    val renderer: String,
    val glVersion: String,
    val vulkanSupported: Boolean,
    val vulkanVersionString: String,
    val vulkanLevel: Int,
    val maxTextureSize: Int,
    val supportsAstcLdr: Boolean,
    val supportsEtc2: Boolean,
    val supportsFramebufferFetch: Boolean,
    val isMaliGpu: Boolean,
    val isAdrenoGpu: Boolean
)

data class DisplayRefreshMode(
    val modeId: Int,
    val width: Int,
    val height: Int,
    val refreshRateHz: Float
)

data class GamepadDeviceInfo(
    val id: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val isExternal: Boolean,
    val hasVibrator: Boolean
)

data class DeviceHardwareProfile(
    val manufacturer: String,
    val model: String,
    val deviceCode: String,
    val socModel: String,
    val supportedAbis: List<String>,
    val totalRamBytes: Long,
    val availableRamBytes: Long,
    val lowMemory: Boolean,
    val cpuCoresCount: Int,
    val cpuCores: List<CpuCoreInfo>,
    val gpu: GpuCapabilities,
    val batteryPercent: Int,
    val batteryTempCelsius: Float,
    val thermalStatus: ThermalStatusLevel,
    val displayModes: List<DisplayRefreshMode>,
    val activeDisplayModeId: Int,
    val activeRefreshRateHz: Float,
    val connectedGamepads: List<GamepadDeviceInfo>,
    val matchStatus: Edge40NeoMatchStatus,
    val safeTexturePoolMb: Int
)
