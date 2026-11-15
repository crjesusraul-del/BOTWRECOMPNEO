package com.example.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.GameDumpRepository
import com.example.data.HardwareProbeRepository
import com.example.data.SettingsDataStore
import com.example.data.db.AppDatabase
import com.example.data.db.BenchmarkRepository
import com.example.data.db.BenchmarkRunEntity
import com.example.model.BotwGameVersion
import com.example.model.DeviceHardwareProfile
import com.example.model.GameDumpSummary
import com.example.model.LsfgDllSummary
import com.example.model.LsfgScaleMode
import com.example.model.MaliDriverStrategy
import com.example.model.OptimizationPreset
import com.example.model.RecompExecutionMode
import com.example.model.RecompilationPlan
import com.example.model.RecompilerSettings
import com.example.model.TexturePipelineMode
import com.example.model.TouchHudLayoutConfig
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class LiveBenchmarkState(
    val isRunning: Boolean = false,
    val currentFps: Float = 0f,
    val avgFps: Float = 0f,
    val low1PercentFps: Float = 0f,
    val currentFrameTimeMs: Float = 0f,
    val frameTimeHistoryMs: List<Float> = emptyList(),
    val renderWidthPx: Int = 0,
    val renderHeightPx: Int = 0,
    val startTempC: Float = 0f,
    val totalFramesRendered: Int = 0
)

class RecompViewModel(application: Application) : AndroidViewModel(application) {

    private val hardwareRepo = HardwareProbeRepository(application)
    private val dumpRepo = GameDumpRepository(application)
    private val dataStore = SettingsDataStore(application)
    private val benchmarkRepo = BenchmarkRepository(AppDatabase.getInstance(application).benchmarkDao())

    private val _hardwareProfile = MutableStateFlow<DeviceHardwareProfile?>(null)
    val hardwareProfile: StateFlow<DeviceHardwareProfile?> = _hardwareProfile.asStateFlow()

    val settings: StateFlow<RecompilerSettings> = dataStore.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = RecompilerSettings()
    )

    val touchHud: StateFlow<TouchHudLayoutConfig> = dataStore.touchHudFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TouchHudLayoutConfig()
    )

    val dumpSummary: StateFlow<GameDumpSummary?> = dataStore.savedDumpFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val lsfgDll: StateFlow<LsfgDllSummary?> = dataStore.savedLsfgDllFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val benchmarkRuns: StateFlow<List<BenchmarkRunEntity>> = benchmarkRepo.allRuns.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _recompPlan = MutableStateFlow<RecompilationPlan?>(null)
    val recompPlan: StateFlow<RecompilationPlan?> = _recompPlan.asStateFlow()

    private val _isScanningDump = MutableStateFlow(false)
    val isScanningDump: StateFlow<Boolean> = _isScanningDump.asStateFlow()

    private val _statusBannerMessage = MutableStateFlow<String?>(null)
    val statusBannerMessage: StateFlow<String?> = _statusBannerMessage.asStateFlow()

    private val _liveBenchmark = MutableStateFlow(LiveBenchmarkState())
    val liveBenchmark: StateFlow<LiveBenchmarkState> = _liveBenchmark.asStateFlow()

    private val collectedFrameTimesMs = mutableListOf<Float>()

    init {
        viewModelScope.launch {
            _hardwareProfile.value = hardwareRepo.probeHardware()
            while (isActive) {
                delay(2500L)
                _hardwareProfile.value = hardwareRepo.probeHardware()
            }
        }

        viewModelScope.launch {
            combine(
                _hardwareProfile,
                settings,
                dumpSummary,
                lsfgDll
            ) { hw, cfg, dump, dll ->
                if (hw != null) {
                    dumpRepo.buildAndPersistRecompilationPlan(hw, cfg, dump, dll)
                } else null
            }.collectLatest { plan ->
                if (plan != null) {
                    _recompPlan.value = plan
                }
            }
        }
    }

    fun refreshHardwareNow() {
        viewModelScope.launch {
            _hardwareProfile.value = hardwareRepo.probeHardware()
        }
    }

    fun applyPreset(preset: OptimizationPreset) {
        val hw = _hardwareProfile.value
        val safePool = hw?.safeTexturePoolMb ?: 3840
        val current = settings.value
        val updated = when (preset) {
            OptimizationPreset.EDGE40_BALANCED -> current.copy(
                preset = preset,
                executionMode = RecompExecutionMode.STATIC_RECOMPILE,
                driverStrategy = MaliDriverStrategy.MALI_VALHALL_STOCK,
                texturePipeline = TexturePipelineMode.ASTC_NATIVE,
                resolutionScale = 0.80f,
                targetFpsCap = 60,
                preferredRefreshRateHz = 120,
                lsfgMode = LsfgScaleMode.OFF,
                pinBigCoresA78 = true,
                workerThreadCount = 6,
                useFp16ValhallShaders = true,
                asyncVulkanPipelineCompile = true,
                enableTbdrTileOptimization = true,
                thermalGuardEnabled = true,
                thermalLimitCelsius = 42,
                texturePoolLimitMb = safePool.coerceAtMost(3840)
            )
            OptimizationPreset.EDGE40_ULTRA_144HZ -> current.copy(
                preset = preset,
                executionMode = RecompExecutionMode.EXPERIMENTAL_NATIVE,
                driverStrategy = MaliDriverStrategy.MALI_VALHALL_STOCK,
                texturePipeline = TexturePipelineMode.ASTC_NATIVE,
                resolutionScale = 0.72f,
                targetFpsCap = 60,
                preferredRefreshRateHz = 144,
                lsfgMode = LsfgScaleMode.X2,
                pinBigCoresA78 = true,
                workerThreadCount = 8.coerceAtMost(hw?.cpuCoresCount ?: 8),
                useFp16ValhallShaders = true,
                asyncVulkanPipelineCompile = true,
                enableTbdrTileOptimization = true,
                thermalGuardEnabled = true,
                thermalLimitCelsius = 45,
                texturePoolLimitMb = safePool
            )
            OptimizationPreset.EDGE40_THERMAL_30FPS -> current.copy(
                preset = preset,
                executionMode = RecompExecutionMode.STATIC_RECOMPILE,
                driverStrategy = MaliDriverStrategy.MALI_VALHALL_STOCK,
                texturePipeline = TexturePipelineMode.LOW_RAM_FALLBACK,
                resolutionScale = 0.65f,
                targetFpsCap = 30,
                preferredRefreshRateHz = 60,
                lsfgMode = LsfgScaleMode.OFF,
                pinBigCoresA78 = false,
                workerThreadCount = 4,
                useFp16ValhallShaders = true,
                asyncVulkanPipelineCompile = true,
                enableTbdrTileOptimization = true,
                thermalGuardEnabled = true,
                thermalLimitCelsius = 39,
                texturePoolLimitMb = safePool.coerceAtMost(2560)
            )
            OptimizationPreset.CUSTOM -> current.copy(preset = OptimizationPreset.CUSTOM)
        }
        viewModelScope.launch {
            dataStore.saveSettings(updated)
            _statusBannerMessage.value = "Perfil '${preset.titleEs}' aplicado y guardado"
        }
    }

    fun updateSettings(transform: (RecompilerSettings) -> RecompilerSettings) {
        viewModelScope.launch {
            val next = transform(settings.value).copy(preset = OptimizationPreset.CUSTOM)
            dataStore.saveSettings(next)
        }
    }

    fun updateGameVersion(version: BotwGameVersion) {
        viewModelScope.launch {
            dataStore.saveSettings(settings.value.copy(gameVersion = version))
        }
    }

    fun updateTouchHud(transform: (TouchHudLayoutConfig) -> TouchHudLayoutConfig) {
        viewModelScope.launch {
            dataStore.saveTouchHud(transform(touchHud.value))
        }
    }

    fun onDumpFolderSelected(uri: Uri) {
        viewModelScope.launch {
            _isScanningDump.value = true
            val summary = dumpRepo.inspectGameDumpTree(uri)
            dataStore.saveDumpSummary(summary)
            if (summary.detectedVersion != null) {
                dataStore.saveSettings(settings.value.copy(gameVersion = summary.detectedVersion))
            }
            _isScanningDump.value = false
            _statusBannerMessage.value =
                "Carpeta '${summary.folderName}' escaneada (${summary.totalFilesScanned} archivos)"
        }
    }

    fun rescanSavedDump() {
        val uriStr = dumpSummary.value?.treeUriString ?: return
        onDumpFolderSelected(Uri.parse(uriStr))
    }

    fun onLsfgDllSelected(uri: Uri) {
        viewModelScope.launch {
            val dll = dumpRepo.inspectLsfgDll(uri)
            dataStore.saveLsfgDll(dll)
            _statusBannerMessage.value = "DLL '${dll.fileName}' verificada (${dll.sha256Short}…)"
        }
    }

    fun clearLsfgDll() {
        viewModelScope.launch {
            dataStore.saveLsfgDll(null)
            _statusBannerMessage.value = "Lossless.dll desvinculada"
        }
    }

    fun exportPlanToUri(targetUri: Uri) {
        val json = _recompPlan.value?.generatedJsonConfig ?: return
        viewModelScope.launch {
            val ok = dumpRepo.exportConfigToUri(targetUri, json)
            _statusBannerMessage.value = if (ok) {
                "Configuración edge40_neo_recomp.json exportada correctamente"
            } else {
                "No se pudo escribir el archivo en esa ubicación"
            }
        }
    }

    fun rebuildPlanNow() {
        viewModelScope.launch {
            val hw = hardwareRepo.probeHardware()
            _hardwareProfile.value = hw
            _recompPlan.value = dumpRepo.buildAndPersistRecompilationPlan(
                hw,
                settings.value,
                dumpSummary.value,
                lsfgDll.value
            )
            _statusBannerMessage.value = "Plan de recompilación ARMv8.2-A + Vulkan regenerado"
        }
    }

    fun triggerHapticTest(): Boolean {
        return hardwareRepo.triggerHapticPulse(strong = true)
    }

    fun clearStatusMessage() {
        _statusBannerMessage.value = null
    }

    fun startBenchmark() {
        collectedFrameTimesMs.clear()
        val startTemp = _hardwareProfile.value?.batteryTempCelsius ?: 0f
        _liveBenchmark.value = LiveBenchmarkState(
            isRunning = true,
            startTempC = startTemp
        )
    }

    fun onBenchmarkFrameSample(frameTimeMs: Float, renderW: Int, renderH: Int) {
        if (!_liveBenchmark.value.isRunning) return
        val clampedMs = frameTimeMs.coerceIn(1.0f, 500.0f)
        collectedFrameTimesMs.add(clampedMs)
        val recent = collectedFrameTimesMs.takeLast(90)
        val avgMs = recent.average().toFloat().coerceAtLeast(0.1f)
        val instantFps = (1000f / clampedMs).coerceAtMost(240f)
        val avgFps = (1000f / avgMs).coerceAtMost(240f)

        val sortedAll = collectedFrameTimesMs.sortedDescending()
        val onePercentCount = (sortedAll.size * 0.01f).roundToInt().coerceAtLeast(1)
        val worst1PctAvgMs = sortedAll.take(onePercentCount).average().toFloat().coerceAtLeast(0.1f)
        val low1Fps = (1000f / worst1PctAvgMs).coerceAtMost(avgFps)

        _liveBenchmark.value = _liveBenchmark.value.copy(
            currentFps = instantFps,
            avgFps = avgFps,
            low1PercentFps = low1Fps,
            currentFrameTimeMs = clampedMs,
            frameTimeHistoryMs = recent,
            renderWidthPx = renderW,
            renderHeightPx = renderH,
            totalFramesRendered = collectedFrameTimesMs.size
        )
    }

    fun stopAndSaveBenchmark() {
        val current = _liveBenchmark.value
        if (!current.isRunning) return
        _liveBenchmark.value = current.copy(isRunning = false)

        if (collectedFrameTimesMs.size < 10) {
            _statusBannerMessage.value = "Benchmark detenido (pocas muestras para guardar)"
            return
        }

        val hw = _hardwareProfile.value
        val cfg = settings.value
        val avgMs = collectedFrameTimesMs.average().toFloat().coerceAtLeast(0.1f)
        val avgFps = (1000f / avgMs).coerceAtMost(240f)
        val sortedDesc = collectedFrameTimesMs.sortedDescending()
        val p1Count = (sortedDesc.size * 0.01f).roundToInt().coerceAtLeast(1)
        val low1Fps = (1000f / sortedDesc.take(p1Count).average().toFloat().coerceAtLeast(0.1f))
            .coerceAtMost(avgFps)

        val run = BenchmarkRunEntity(
            presetName = cfg.preset.titleEs,
            executionMode = cfg.executionMode.titleEs,
            gpuRenderer = hw?.gpu?.renderer ?: "GLES 3.0",
            resolutionScale = cfg.resolutionScale,
            refreshRateHz = hw?.activeRefreshRateHz?.roundToInt() ?: cfg.preferredRefreshRateHz,
            avgFps = avgFps,
            low1PercentFps = low1Fps,
            avgFrameTimeMs = avgMs,
            startTempC = current.startTempC,
            endTempC = hw?.batteryTempCelsius ?: current.startTempC,
            fp16Enabled = cfg.useFp16ValhallShaders
        )
        viewModelScope.launch {
            benchmarkRepo.recordRun(run)
            _statusBannerMessage.value = "Benchmark guardado: ${"%.1f".format(avgFps)} FPS promedio"
        }
    }

    fun deleteBenchmarkRun(id: Long) {
        viewModelScope.launch {
            benchmarkRepo.deleteRun(id)
        }
    }

    fun clearBenchmarkHistory() {
        viewModelScope.launch {
            benchmarkRepo.clearAllRuns()
        }
    }
}
