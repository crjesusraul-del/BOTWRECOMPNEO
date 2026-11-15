package com.example.data

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.GLES20
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.Display
import android.view.InputDevice
import com.example.model.CpuCoreInfo
import com.example.model.DeviceHardwareProfile
import com.example.model.DisplayRefreshMode
import com.example.model.Edge40NeoMatchStatus
import com.example.model.GamepadDeviceInfo
import com.example.model.GpuCapabilities
import com.example.model.ThermalStatusLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class HardwareProbeRepository(private val context: Context) {

    private var cachedGpuCapabilities: GpuCapabilities? = null

    suspend fun probeHardware(): DeviceHardwareProfile = withContext(Dispatchers.IO) {
        val manufacturer = Build.MANUFACTURER.orEmpty()
        val model = Build.MODEL.orEmpty()
        val deviceCode = Build.DEVICE.orEmpty()
        val socModel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Build.SOC_MODEL.takeIf { it.isNotBlank() && it != Build.UNKNOWN } ?: Build.HARDWARE
        } else {
            Build.HARDWARE
        }
        val abis = Build.SUPPORTED_ABIS?.toList().orEmpty()

        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memInfo)

        val coreCount = Runtime.getRuntime().availableProcessors().coerceAtLeast(1)
        val cpuCores = readCpuTopology(coreCount)
        val gpuCaps = cachedGpuCapabilities ?: probeEglAndVulkan().also { cachedGpuCapabilities = it }

        val (batteryPct, batteryTempC) = readBatteryTelemetry()
        val thermalLevel = readThermalStatus()
        val (displayModes, activeModeId, activeHz) = readDisplayModes()
        val gamepads = scanConnectedGamepads()

        val combinedDeviceStr = "$manufacturer $model $deviceCode $socModel".lowercase()
        val isEdge40Neo = combinedDeviceStr.contains("edge 40 neo") ||
            combinedDeviceStr.contains("manaus") ||
            combinedDeviceStr.contains("mt6879") ||
            combinedDeviceStr.contains("dimensity 7030")

        val matchStatus = when {
            isEdge40Neo -> Edge40NeoMatchStatus.VERIFIED_HARDWARE
            gpuCaps.isMaliGpu && abis.any { it.contains("arm64") } -> Edge40NeoMatchStatus.COMPATIBLE_MALI_ARM64
            else -> Edge40NeoMatchStatus.CALIBRATION_PROFILE_ACTIVE
        }

        val totalRamMb = (memInfo.totalMem / (1024L * 1024L)).toInt()
        val safeTexturePoolMb = when {
            totalRamMb >= 10500 -> 6144
            totalRamMb >= 7000 -> 3840
            totalRamMb >= 5000 -> 2560
            else -> 1536
        }

        DeviceHardwareProfile(
            manufacturer = manufacturer,
            model = model,
            deviceCode = deviceCode,
            socModel = socModel,
            supportedAbis = abis,
            totalRamBytes = memInfo.totalMem,
            availableRamBytes = memInfo.availMem,
            lowMemory = memInfo.lowMemory,
            cpuCoresCount = coreCount,
            cpuCores = cpuCores,
            gpu = gpuCaps,
            batteryPercent = batteryPct,
            batteryTempCelsius = batteryTempC,
            thermalStatus = thermalLevel,
            displayModes = displayModes,
            activeDisplayModeId = activeModeId,
            activeRefreshRateHz = activeHz,
            connectedGamepads = gamepads,
            matchStatus = matchStatus,
            safeTexturePoolMb = safeTexturePoolMb
        )
    }

    fun scanConnectedGamepads(): List<GamepadDeviceInfo> {
        val result = mutableListOf<GamepadDeviceInfo>()
        for (deviceId in InputDevice.getDeviceIds()) {
            val device = InputDevice.getDevice(deviceId) ?: continue
            val sources = device.sources
            val isGamepad = (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            if (isGamepad && !device.isVirtual) {
                val hasVibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    device.vibratorManager.vibratorIds.isNotEmpty()
                } else {
                    @Suppress("DEPRECATION")
                    device.vibrator.hasVibrator()
                }
                result.add(
                    GamepadDeviceInfo(
                        id = device.id,
                        name = device.name ?: "Controlador #$deviceId",
                        vendorId = device.vendorId,
                        productId = device.productId,
                        isExternal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) device.isExternal else true,
                        hasVibrator = hasVibrator
                    )
                )
            }
        }
        return result
    }

    fun triggerHapticPulse(strong: Boolean = true): Boolean {
        return try {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
                vm.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            }
            if (!vibrator.hasVibrator()) return false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val timings = if (strong) longArrayOf(0, 45, 55, 80) else longArrayOf(0, 30)
                val amplitudes = if (strong) intArrayOf(0, 180, 0, 255) else intArrayOf(0, 140)
                vibrator.vibrate(VibrationEffect.createWaveform(timings, amplitudes, -1))
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(60)
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun readCpuTopology(coreCount: Int): List<CpuCoreInfo> {
        val list = mutableListOf<CpuCoreInfo>()
        for (i in 0 until coreCount) {
            val curFreq = readSysfsLong("/sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
            val maxFreq = readSysfsLong("/sys/devices/system/cpu/cpu$i/cpufreq/cpuinfo_max_freq")
            val clusterLabel = when {
                maxFreq != null && maxFreq >= 2_300_000L -> "Big (Cortex-A78)"
                maxFreq != null -> "Little (Cortex-A55)"
                coreCount == 8 && i >= 6 -> "Big Cluster (#$i)"
                else -> "Core #$i"
            }
            list.add(
                CpuCoreInfo(
                    index = i,
                    currentFreqKhz = curFreq,
                    maxFreqKhz = maxFreq,
                    clusterLabel = clusterLabel
                )
            )
        }
        return list
    }

    private fun readSysfsLong(path: String): Long? {
        return try {
            val f = File(path)
            if (f.canRead()) f.readText().trim().toLongOrNull() else null
        } catch (_: Throwable) {
            null
        }
    }

    private fun readBatteryTelemetry(): Pair<Int, Float> {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val pct = if (level >= 0 && scale > 0) ((level * 100f) / scale).toInt() else -1
        val tenthsC = intent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1) ?: -1
        val tempC = if (tenthsC > 0) tenthsC / 10f else 0f
        return pct to tempC
    }

    private fun readThermalStatus(): ThermalStatusLevel {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return ThermalStatusLevel.UNKNOWN
        val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return when (pm.currentThermalStatus) {
            PowerManager.THERMAL_STATUS_NONE -> ThermalStatusLevel.NONE
            PowerManager.THERMAL_STATUS_LIGHT -> ThermalStatusLevel.LIGHT
            PowerManager.THERMAL_STATUS_MODERATE -> ThermalStatusLevel.MODERATE
            PowerManager.THERMAL_STATUS_SEVERE -> ThermalStatusLevel.SEVERE
            PowerManager.THERMAL_STATUS_CRITICAL,
            PowerManager.THERMAL_STATUS_EMERGENCY,
            PowerManager.THERMAL_STATUS_SHUTDOWN -> ThermalStatusLevel.CRITICAL
            else -> ThermalStatusLevel.UNKNOWN
        }
    }

    private fun readDisplayModes(): Triple<List<DisplayRefreshMode>, Int, Float> {
        val dm = context.getSystemService(Context.DISPLAY_SERVICE) as DisplayManager
        val display = dm.getDisplay(Display.DEFAULT_DISPLAY)
        if (display == null) return Triple(emptyList(), 0, 60f)
        val activeMode = display.mode
        val modes = display.supportedModes
            .map {
                DisplayRefreshMode(
                    modeId = it.modeId,
                    width = it.physicalWidth,
                    height = it.physicalHeight,
                    refreshRateHz = it.refreshRate
                )
            }
            .distinctBy { "${it.width}x${it.height}@${it.refreshRateHz.toInt()}" }
            .sortedBy { it.refreshRateHz }
        return Triple(modes, activeMode.modeId, activeMode.refreshRate)
    }

    private fun probeEglAndVulkan(): GpuCapabilities {
        val pm = context.packageManager
        val hasVulkan = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N &&
            pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)
        val vulkanVersionInt = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.systemAvailableFeatures
                .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                ?.version ?: 0
        } else 0
        val vulkanLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            pm.systemAvailableFeatures
                .firstOrNull { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_LEVEL }
                ?.version ?: 0
        } else 0

        val vulkanVerString = if (vulkanVersionInt > 0) {
            val major = (vulkanVersionInt shr 22) and 0x3FF
            val minor = (vulkanVersionInt shr 12) and 0x3FF
            val patch = vulkanVersionInt and 0xFFF
            "Vulkan $major.$minor.$patch"
        } else if (hasVulkan) {
            "Vulkan 1.1+"
        } else {
            "No reportado"
        }

        var vendor = "Desconocido"
        var renderer = "Desconocido"
        var glVersion = "OpenGL ES 3.0"
        var maxTex = 4096
        var extensions = ""

        try {
            val eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
            if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
                val version = IntArray(2)
                if (EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
                    val configAttribs = intArrayOf(
                        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                        EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                        EGL14.EGL_RED_SIZE, 8,
                        EGL14.EGL_GREEN_SIZE, 8,
                        EGL14.EGL_BLUE_SIZE, 8,
                        EGL14.EGL_NONE
                    )
                    val configs = arrayOfNulls<EGLConfig>(1)
                    val numConfigs = IntArray(1)
                    if (EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfigs, 0) && numConfigs[0] > 0) {
                        val ctxAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
                        val eglContext = EGL14.eglCreateContext(eglDisplay, configs[0], EGL14.EGL_NO_CONTEXT, ctxAttribs, 0)
                        val pbufferAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
                        val eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], pbufferAttribs, 0)
                        if (eglContext != EGL14.EGL_NO_CONTEXT && eglSurface != EGL14.EGL_NO_SURFACE) {
                            if (EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
                                vendor = GLES20.glGetString(GLES20.GL_VENDOR) ?: vendor
                                renderer = GLES20.glGetString(GLES20.GL_RENDERER) ?: renderer
                                glVersion = GLES20.glGetString(GLES20.GL_VERSION) ?: glVersion
                                extensions = GLES20.glGetString(GLES20.GL_EXTENSIONS) ?: ""
                                val maxTexArr = IntArray(1)
                                GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTexArr, 0)
                                if (maxTexArr[0] > 0) maxTex = maxTexArr[0]
                            }
                        }
                        EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                        if (eglSurface != EGL14.EGL_NO_SURFACE) EGL14.eglDestroySurface(eglDisplay, eglSurface)
                        if (eglContext != EGL14.EGL_NO_CONTEXT) EGL14.eglDestroyContext(eglDisplay, eglContext)
                    }
                    EGL14.eglTerminate(eglDisplay)
                }
            }
        } catch (_: Throwable) {
            // Fallback to defaults if EGL headless probe is unavailable
        }

        val lowerRenderer = "$vendor $renderer".lowercase()
        val isMali = lowerRenderer.contains("mali") || lowerRenderer.contains("arm")
        val isAdreno = lowerRenderer.contains("adreno") || lowerRenderer.contains("qualcomm")

        return GpuCapabilities(
            vendor = vendor,
            renderer = renderer,
            glVersion = glVersion,
            vulkanSupported = hasVulkan,
            vulkanVersionString = vulkanVerString,
            vulkanLevel = vulkanLevel,
            maxTextureSize = maxTex,
            supportsAstcLdr = extensions.contains("GL_KHR_texture_compression_astc_ldr", ignoreCase = true),
            supportsEtc2 = extensions.contains("ETC2", ignoreCase = true) || glVersion.contains("3."),
            supportsFramebufferFetch = extensions.contains("shader_framebuffer_fetch", ignoreCase = true),
            isMaliGpu = isMali,
            isAdrenoGpu = isAdreno
        )
    }
}
