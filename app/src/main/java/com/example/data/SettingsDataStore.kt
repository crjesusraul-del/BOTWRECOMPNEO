package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.model.BotwGameVersion
import com.example.model.GameDumpSummary
import com.example.model.LsfgDllSummary
import com.example.model.LsfgScaleMode
import com.example.model.MaliDriverStrategy
import com.example.model.OptimizationPreset
import com.example.model.RecompExecutionMode
import com.example.model.RecompilerSettings
import com.example.model.TexturePipelineMode
import com.example.model.TouchHudLayoutConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.recompDataStore: DataStore<Preferences> by preferencesDataStore(name = "edge40_recomp_prefs")

class SettingsDataStore(private val context: Context) {

    private object Keys {
        val PRESET = stringPreferencesKey("preset")
        val EXEC_MODE = stringPreferencesKey("exec_mode")
        val GAME_VERSION = stringPreferencesKey("game_version")
        val DRIVER_STRATEGY = stringPreferencesKey("driver_strategy")
        val TEXTURE_PIPELINE = stringPreferencesKey("texture_pipeline")
        val RESOLUTION_SCALE = floatPreferencesKey("resolution_scale")
        val FPS_CAP = intPreferencesKey("fps_cap")
        val REFRESH_HZ = intPreferencesKey("refresh_hz")
        val LSFG_MODE = stringPreferencesKey("lsfg_mode")
        val PIN_A78 = booleanPreferencesKey("pin_a78")
        val WORKER_THREADS = intPreferencesKey("worker_threads")
        val FP16_VALHALL = booleanPreferencesKey("fp16_valhall")
        val ASYNC_SHADER = booleanPreferencesKey("async_shader")
        val TBDR_OPT = booleanPreferencesKey("tbdr_opt")
        val THERMAL_GUARD = booleanPreferencesKey("thermal_guard")
        val THERMAL_LIMIT = intPreferencesKey("thermal_limit")
        val TEX_POOL_MB = intPreferencesKey("tex_pool_mb")

        val HUD_EDGE_REJECTION = floatPreferencesKey("hud_edge_rejection")
        val HUD_OPACITY = floatPreferencesKey("hud_opacity")
        val HUD_SCALE = floatPreferencesKey("hud_scale")
        val GYRO_AIM = booleanPreferencesKey("gyro_aim")
        val HAPTICS = booleanPreferencesKey("haptics")

        val DUMP_URI = stringPreferencesKey("dump_uri")
        val DUMP_NAME = stringPreferencesKey("dump_name")
        val DUMP_FILES = intPreferencesKey("dump_files")
        val DUMP_BYTES = stringPreferencesKey("dump_bytes")
        val DUMP_EXEFS = booleanPreferencesKey("dump_exefs")
        val DUMP_ROMFS = booleanPreferencesKey("dump_romfs")
        val DUMP_MAIN = booleanPreferencesKey("dump_main")
        val DUMP_PACK = booleanPreferencesKey("dump_pack")
        val DUMP_SHADER = booleanPreferencesKey("dump_shader")

        val LSFG_URI = stringPreferencesKey("lsfg_uri")
        val LSFG_NAME = stringPreferencesKey("lsfg_name")
        val LSFG_SIZE = stringPreferencesKey("lsfg_size")
        val LSFG_SHA = stringPreferencesKey("lsfg_sha")
    }

    val settingsFlow: Flow<RecompilerSettings> = context.recompDataStore.data.map { prefs ->
        RecompilerSettings(
            preset = OptimizationPreset.entries.firstOrNull { it.id == prefs[Keys.PRESET] }
                ?: OptimizationPreset.EDGE40_BALANCED,
            executionMode = RecompExecutionMode.entries.firstOrNull { it.id == prefs[Keys.EXEC_MODE] }
                ?: RecompExecutionMode.STATIC_RECOMPILE,
            gameVersion = BotwGameVersion.entries.firstOrNull { it.code == prefs[Keys.GAME_VERSION] }
                ?: BotwGameVersion.V1_6_0,
            driverStrategy = MaliDriverStrategy.entries.firstOrNull { it.id == prefs[Keys.DRIVER_STRATEGY] }
                ?: MaliDriverStrategy.MALI_VALHALL_STOCK,
            texturePipeline = TexturePipelineMode.entries.firstOrNull { it.id == prefs[Keys.TEXTURE_PIPELINE] }
                ?: TexturePipelineMode.ASTC_NATIVE,
            resolutionScale = prefs[Keys.RESOLUTION_SCALE] ?: 0.80f,
            targetFpsCap = prefs[Keys.FPS_CAP] ?: 60,
            preferredRefreshRateHz = prefs[Keys.REFRESH_HZ] ?: 120,
            lsfgMode = LsfgScaleMode.entries.firstOrNull { it.id == prefs[Keys.LSFG_MODE] }
                ?: LsfgScaleMode.OFF,
            pinBigCoresA78 = prefs[Keys.PIN_A78] ?: true,
            workerThreadCount = prefs[Keys.WORKER_THREADS] ?: 6,
            useFp16ValhallShaders = prefs[Keys.FP16_VALHALL] ?: true,
            asyncVulkanPipelineCompile = prefs[Keys.ASYNC_SHADER] ?: true,
            enableTbdrTileOptimization = prefs[Keys.TBDR_OPT] ?: true,
            thermalGuardEnabled = prefs[Keys.THERMAL_GUARD] ?: true,
            thermalLimitCelsius = prefs[Keys.THERMAL_LIMIT] ?: 42,
            texturePoolLimitMb = prefs[Keys.TEX_POOL_MB] ?: 3840
        )
    }

    val touchHudFlow: Flow<TouchHudLayoutConfig> = context.recompDataStore.data.map { prefs ->
        TouchHudLayoutConfig(
            edgeRejectionDp = prefs[Keys.HUD_EDGE_REJECTION] ?: 16f,
            controlOpacity = prefs[Keys.HUD_OPACITY] ?: 0.65f,
            controlScale = prefs[Keys.HUD_SCALE] ?: 1.0f,
            gyroAimEnabled = prefs[Keys.GYRO_AIM] ?: true,
            hapticsEnabled = prefs[Keys.HAPTICS] ?: true
        )
    }

    val savedDumpFlow: Flow<GameDumpSummary?> = context.recompDataStore.data.map { prefs ->
        val uri = prefs[Keys.DUMP_URI] ?: return@map null
        GameDumpSummary(
            treeUriString = uri,
            folderName = prefs[Keys.DUMP_NAME] ?: "Dump",
            totalFilesScanned = prefs[Keys.DUMP_FILES] ?: 0,
            totalSizeBytes = prefs[Keys.DUMP_BYTES]?.toLongOrNull() ?: 0L,
            hasExefs = prefs[Keys.DUMP_EXEFS] ?: false,
            hasRomfs = prefs[Keys.DUMP_ROMFS] ?: false,
            hasMainBinary = prefs[Keys.DUMP_MAIN] ?: false,
            hasPackDir = prefs[Keys.DUMP_PACK] ?: false,
            hasShaderDir = prefs[Keys.DUMP_SHADER] ?: false,
            detectedVersion = null,
            sampleEntries = emptyList(),
            lastScannedAtMs = 0L
        )
    }

    val savedLsfgDllFlow: Flow<LsfgDllSummary?> = context.recompDataStore.data.map { prefs ->
        val uri = prefs[Keys.LSFG_URI] ?: return@map null
        LsfgDllSummary(
            uriString = uri,
            fileName = prefs[Keys.LSFG_NAME] ?: "Lossless.dll",
            sizeBytes = prefs[Keys.LSFG_SIZE]?.toLongOrNull() ?: 0L,
            sha256Short = prefs[Keys.LSFG_SHA] ?: ""
        )
    }

    suspend fun saveSettings(settings: RecompilerSettings) {
        context.recompDataStore.edit { prefs ->
            prefs[Keys.PRESET] = settings.preset.id
            prefs[Keys.EXEC_MODE] = settings.executionMode.id
            prefs[Keys.GAME_VERSION] = settings.gameVersion.code
            prefs[Keys.DRIVER_STRATEGY] = settings.driverStrategy.id
            prefs[Keys.TEXTURE_PIPELINE] = settings.texturePipeline.id
            prefs[Keys.RESOLUTION_SCALE] = settings.resolutionScale
            prefs[Keys.FPS_CAP] = settings.targetFpsCap
            prefs[Keys.REFRESH_HZ] = settings.preferredRefreshRateHz
            prefs[Keys.LSFG_MODE] = settings.lsfgMode.id
            prefs[Keys.PIN_A78] = settings.pinBigCoresA78
            prefs[Keys.WORKER_THREADS] = settings.workerThreadCount
            prefs[Keys.FP16_VALHALL] = settings.useFp16ValhallShaders
            prefs[Keys.ASYNC_SHADER] = settings.asyncVulkanPipelineCompile
            prefs[Keys.TBDR_OPT] = settings.enableTbdrTileOptimization
            prefs[Keys.THERMAL_GUARD] = settings.thermalGuardEnabled
            prefs[Keys.THERMAL_LIMIT] = settings.thermalLimitCelsius
            prefs[Keys.TEX_POOL_MB] = settings.texturePoolLimitMb
        }
    }

    suspend fun saveTouchHud(config: TouchHudLayoutConfig) {
        context.recompDataStore.edit { prefs ->
            prefs[Keys.HUD_EDGE_REJECTION] = config.edgeRejectionDp
            prefs[Keys.HUD_OPACITY] = config.controlOpacity
            prefs[Keys.HUD_SCALE] = config.controlScale
            prefs[Keys.GYRO_AIM] = config.gyroAimEnabled
            prefs[Keys.HAPTICS] = config.hapticsEnabled
        }
    }

    suspend fun saveDumpSummary(summary: GameDumpSummary) {
        context.recompDataStore.edit { prefs ->
            prefs[Keys.DUMP_URI] = summary.treeUriString
            prefs[Keys.DUMP_NAME] = summary.folderName
            prefs[Keys.DUMP_FILES] = summary.totalFilesScanned
            prefs[Keys.DUMP_BYTES] = summary.totalSizeBytes.toString()
            prefs[Keys.DUMP_EXEFS] = summary.hasExefs
            prefs[Keys.DUMP_ROMFS] = summary.hasRomfs
            prefs[Keys.DUMP_MAIN] = summary.hasMainBinary
            prefs[Keys.DUMP_PACK] = summary.hasPackDir
            prefs[Keys.DUMP_SHADER] = summary.hasShaderDir
        }
    }

    suspend fun saveLsfgDll(dll: LsfgDllSummary?) {
        context.recompDataStore.edit { prefs ->
            if (dll == null) {
                prefs.remove(Keys.LSFG_URI)
                prefs.remove(Keys.LSFG_NAME)
                prefs.remove(Keys.LSFG_SIZE)
                prefs.remove(Keys.LSFG_SHA)
            } else {
                prefs[Keys.LSFG_URI] = dll.uriString
                prefs[Keys.LSFG_NAME] = dll.fileName
                prefs[Keys.LSFG_SIZE] = dll.sizeBytes.toString()
                prefs[Keys.LSFG_SHA] = dll.sha256Short
            }
        }
    }
}
