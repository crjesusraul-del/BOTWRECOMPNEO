package com.example.model

enum class BotwGameVersion(val code: String, val label: String) {
    V1_0_0("1.0.0", "Switch 1.0.0 (Base)"),
    V1_6_0("1.6.0", "Switch 1.6.0 (VR / Estable)"),
    V1_8_2("1.8.2", "Switch 1.8.2"),
    V1_9_0("1.9.0", "Switch 1.9.0 (Última)")
}

enum class RecompExecutionMode(val id: String, val titleEs: String, val detailEs: String) {
    STATIC_RECOMPILE(
        id = "static_recomp",
        titleEs = "Recompilación Estática (ARMv8.2-A)",
        detailEs = "Compila código del juego a C/ARM64 optimizado para Cortex-A78/A55."
    ),
    EXPERIMENTAL_NATIVE(
        id = "native_arm64",
        titleEs = "Modo Nativo Experimental (ARM64)",
        detailEs = "Salta compilación inicial y ejecuta instrucciones ARM64 directas."
    )
}

enum class OptimizationPreset(val id: String, val titleEs: String) {
    EDGE40_BALANCED("edge40_balanced", "Equilibrado Edge 40 Neo"),
    EDGE40_ULTRA_144HZ("edge40_ultra_144hz", "Ultra 144Hz pOLED"),
    EDGE40_THERMAL_30FPS("edge40_thermal_30fps", "Térmico y Batería 30 FPS"),
    CUSTOM("custom", "Personalizado")
}

enum class MaliDriverStrategy(val id: String, val labelEs: String) {
    MALI_VALHALL_STOCK("mali_valhall_stock", "Sistema Mali Valhall (Edge 40 Neo)"),
    MALI_COMPATIBILITY("mali_compat", "Vulkan Compatibilidad Conservadora"),
    CUSTOM_VENDOR_LIB("custom_vendor", "Librería del Sistema sin Turnip")
}

enum class TexturePipelineMode(val id: String, val labelEs: String, val detailEs: String) {
    ASTC_NATIVE("astc_native", "ASTC Hardware (Mali-G610)", "Transcodifica BCn a ASTC 6x6 en bloque nativo Mali."),
    BCN_BALANCED("bcn_balanced", "BCn Híbrido (8GB/12GB RAM)", "Mantiene BC1/BC3 en caché rápida ZRAM-friendly."),
    LOW_RAM_FALLBACK("low_ram", "Compresión Segura Low-RAM", "Reduce mipmaps >2048px para evitar OOM en Dimensity 7030.")
}

enum class LsfgScaleMode(val id: String, val labelEs: String, val multiplier: Int) {
    OFF("off", "Desactivado (Nativo)", 1),
    X2("2x", "LSFG 2x (30→60 / 60→120)", 2),
    X3("3x", "LSFG 3x (48→144Hz pOLED)", 3)
}

data class RecompilerSettings(
    val preset: OptimizationPreset = OptimizationPreset.EDGE40_BALANCED,
    val executionMode: RecompExecutionMode = RecompExecutionMode.STATIC_RECOMPILE,
    val gameVersion: BotwGameVersion = BotwGameVersion.V1_6_0,
    val driverStrategy: MaliDriverStrategy = MaliDriverStrategy.MALI_VALHALL_STOCK,
    val texturePipeline: TexturePipelineMode = TexturePipelineMode.ASTC_NATIVE,
    val resolutionScale: Float = 0.80f,
    val targetFpsCap: Int = 60,
    val preferredRefreshRateHz: Int = 120,
    val lsfgMode: LsfgScaleMode = LsfgScaleMode.OFF,
    val pinBigCoresA78: Boolean = true,
    val workerThreadCount: Int = 6,
    val useFp16ValhallShaders: Boolean = true,
    val asyncVulkanPipelineCompile: Boolean = true,
    val enableTbdrTileOptimization: Boolean = true,
    val thermalGuardEnabled: Boolean = true,
    val thermalLimitCelsius: Int = 42,
    val texturePoolLimitMb: Int = 3840
)

data class TouchHudLayoutConfig(
    val edgeRejectionDp: Float = 16f,
    val controlOpacity: Float = 0.65f,
    val controlScale: Float = 1.0f,
    val gyroAimEnabled: Boolean = true,
    val hapticsEnabled: Boolean = true
)

data class GameDumpSummary(
    val treeUriString: String,
    val folderName: String,
    val totalFilesScanned: Int,
    val totalSizeBytes: Long,
    val hasExefs: Boolean,
    val hasRomfs: Boolean,
    val hasMainBinary: Boolean,
    val hasPackDir: Boolean,
    val hasShaderDir: Boolean,
    val detectedVersion: BotwGameVersion?,
    val sampleEntries: List<String>,
    val lastScannedAtMs: Long
) {
    val isReadyForRecomp: Boolean
        get() = (hasExefs && hasRomfs) || (hasMainBinary && hasPackDir) || totalFilesScanned > 0
}

data class LsfgDllSummary(
    val uriString: String,
    val fileName: String,
    val sizeBytes: Long,
    val sha256Short: String
)

data class RecompilationPlan(
    val targetDeviceLabel: String,
    val targetArchitectureFlags: String,
    val vulkanProfileFlags: String,
    val estimatedMemoryFootprintMb: Int,
    val readinessChecks: List<PipelineCheckItem>,
    val generatedJsonConfig: String,
    val savedFilePath: String?
)

data class PipelineCheckItem(
    val title: String,
    val detail: String,
    val passed: Boolean
)
