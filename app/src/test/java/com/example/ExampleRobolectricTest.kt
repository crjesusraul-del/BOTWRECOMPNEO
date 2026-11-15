package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.GameDumpRepository
import com.example.model.CpuCoreInfo
import com.example.model.DeviceHardwareProfile
import com.example.model.Edge40NeoMatchStatus
import com.example.model.GpuCapabilities
import com.example.model.RecompilerSettings
import com.example.model.ThermalStatusLevel
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("BOTW Recomp Neo", appName)
    }

    @Test
    fun `recompilation plan targets edge 40 neo mali and blocks turnip`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repo = GameDumpRepository(context)
        val mockHw = DeviceHardwareProfile(
            manufacturer = "motorola",
            model = "motorola edge 40 neo",
            deviceCode = "manaus",
            socModel = "MT6879",
            supportedAbis = listOf("arm64-v8a"),
            totalRamBytes = 12L * 1024L * 1024L * 1024L,
            availableRamBytes = 7L * 1024L * 1024L * 1024L,
            lowMemory = false,
            cpuCoresCount = 8,
            cpuCores = List(8) {
                CpuCoreInfo(it, 2000000L, 2500000L, if (it >= 6) "Big (Cortex-A78)" else "Little (Cortex-A55)")
            },
            gpu = GpuCapabilities(
                vendor = "ARM",
                renderer = "Mali-G610 MC3",
                glVersion = "OpenGL ES 3.2",
                vulkanSupported = true,
                vulkanVersionString = "Vulkan 1.3.0",
                vulkanLevel = 1,
                maxTextureSize = 16384,
                supportsAstcLdr = true,
                supportsEtc2 = true,
                supportsFramebufferFetch = true,
                isMaliGpu = true,
                isAdrenoGpu = false
            ),
            batteryPercent = 85,
            batteryTempCelsius = 34.5f,
            thermalStatus = ThermalStatusLevel.NONE,
            displayModes = emptyList(),
            activeDisplayModeId = 0,
            activeRefreshRateHz = 144f,
            connectedGamepads = emptyList(),
            matchStatus = Edge40NeoMatchStatus.VERIFIED_HARDWARE,
            safeTexturePoolMb = 6144
        )

        val plan = repo.buildAndPersistRecompilationPlan(
            hardware = mockHw,
            settings = RecompilerSettings(preferredRefreshRateHz = 144, pinBigCoresA78 = true),
            dump = null,
            lsfgDll = null
        )

        assertTrue(plan.targetArchitectureFlags.contains("cortex-a78"))
        assertTrue(plan.vulkanProfileFlags.contains("MALI_G610_MC3_VALHALL"))
        assertTrue(plan.vulkanProfileFlags.contains("TURNIP_OVERRIDE=0"))
        assertTrue(plan.generatedJsonConfig.contains("\"disable_adreno_turnip\": true"))
    }
}
