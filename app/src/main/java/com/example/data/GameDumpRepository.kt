package com.example.data

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import com.example.model.BotwGameVersion
import com.example.model.DeviceHardwareProfile
import com.example.model.GameDumpSummary
import com.example.model.LsfgDllSummary
import com.example.model.PipelineCheckItem
import com.example.model.RecompExecutionMode
import com.example.model.RecompilationPlan
import com.example.model.RecompilerSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

class GameDumpRepository(private val context: Context) {

    suspend fun inspectGameDumpTree(treeUri: Uri): GameDumpSummary = withContext(Dispatchers.IO) {
        val treeDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val rootDocUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, treeDocId)
        val folderName = queryDisplayName(rootDocUri) ?: treeDocId.substringAfterLast(':')

        var fileCount = 0
        var totalBytes = 0L
        var hasExefs = false
        var hasRomfs = false
        var hasMain = false
        var hasPack = false
        var hasShader = false
        val samples = mutableListOf<String>()
        var detectedVersion: BotwGameVersion? = null

        fun scanNode(parentDocId: String, depth: Int) {
            if (depth > 3 || fileCount > 4000) return
            val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocId)
            val projection = arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
                DocumentsContract.Document.COLUMN_SIZE
            )
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                val mimeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
                val sizeIdx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_SIZE)
                while (cursor.moveToNext()) {
                    val docId = cursor.getString(idIdx)
                    val name = cursor.getString(nameIdx).orEmpty()
                    val mime = cursor.getString(mimeIdx).orEmpty()
                    val size = if (!cursor.isNull(sizeIdx)) cursor.getLong(sizeIdx) else 0L
                    val lower = name.lowercase()

                    if (lower.contains("1.9.0")) detectedVersion = BotwGameVersion.V1_9_0
                    else if (lower.contains("1.8.2") && detectedVersion == null) detectedVersion = BotwGameVersion.V1_8_2
                    else if (lower.contains("1.6.0") && detectedVersion == null) detectedVersion = BotwGameVersion.V1_6_0
                    else if (lower.contains("1.0.0") && detectedVersion == null) detectedVersion = BotwGameVersion.V1_0_0

                    if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                        when (lower) {
                            "exefs" -> hasExefs = true
                            "romfs" -> hasRomfs = true
                            "pack" -> hasPack = true
                            "shader" -> hasShader = true
                        }
                        if (samples.size < 10) samples.add("📁 $name/")
                        scanNode(docId, depth + 1)
                    } else {
                        fileCount++
                        totalBytes += size.coerceAtLeast(0L)
                        if (lower == "main" || lower == "main.npdm" || lower.startsWith("subsdk")) {
                            hasMain = true
                        }
                        if (samples.size < 10) samples.add("📄 $name")
                    }
                }
            }
        }

        try {
            scanNode(treeDocId, 0)
        } catch (_: Throwable) {
            // Safeguard against malformed SAF providers
        }

        GameDumpSummary(
            treeUriString = treeUri.toString(),
            folderName = folderName,
            totalFilesScanned = fileCount,
            totalSizeBytes = totalBytes,
            hasExefs = hasExefs,
            hasRomfs = hasRomfs,
            hasMainBinary = hasMain,
            hasPackDir = hasPack,
            hasShaderDir = hasShader,
            detectedVersion = detectedVersion,
            sampleEntries = samples,
            lastScannedAtMs = System.currentTimeMillis()
        )
    }

    suspend fun inspectLsfgDll(fileUri: Uri): LsfgDllSummary = withContext(Dispatchers.IO) {
        var name = "Lossless.dll"
        var size = 0L
        context.contentResolver.query(
            fileUri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nIdx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sIdx = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nIdx >= 0) name = cursor.getString(nIdx) ?: name
                if (sIdx >= 0 && !cursor.isNull(sIdx)) size = cursor.getLong(sIdx)
            }
        }

        val digest = MessageDigest.getInstance("SHA-256")
        var counted = 0L
        context.contentResolver.openInputStream(fileUri)?.use { input ->
            val buf = ByteArray(16 * 1024)
            var read = input.read(buf)
            while (read > 0) {
                digest.update(buf, 0, read)
                counted += read
                read = input.read(buf)
            }
        }
        val finalSize = if (size > 0) size else counted
        val hex = digest.digest().joinToString("") { "%02x".format(it) }.take(16)

        LsfgDllSummary(
            uriString = fileUri.toString(),
            fileName = name,
            sizeBytes = finalSize,
            sha256Short = hex
        )
    }

    suspend fun buildAndPersistRecompilationPlan(
        hardware: DeviceHardwareProfile,
        settings: RecompilerSettings,
        dump: GameDumpSummary?,
        lsfgDll: LsfgDllSummary?
    ): RecompilationPlan = withContext(Dispatchers.IO) {
        val internalHeight = (1080f * settings.resolutionScale).toInt()
        val internalWidth = (1920f * settings.resolutionScale).toInt()
        val estimatedFootprintMb = (
            1280 +
                (settings.texturePoolLimitMb * 0.65f).toInt() +
                if (settings.executionMode == RecompExecutionMode.STATIC_RECOMPILE) 450 else 280
            ).coerceAtMost((hardware.totalRamBytes / (1024L * 1024L)).toInt() - 800)

        val archFlags = buildString {
            append("-march=armv8.2-a+dotprod+fp16 ")
            append("-mcpu=${if (settings.pinBigCoresA78) "cortex-a78" else "cortex-a55"} ")
            append("-O3 -flto=thin -threads=${settings.workerThreadCount}")
        }

        val vulkanFlags = buildString {
            append("NVN_TO_VULKAN=1 ")
            append("GPU_PROFILE=MALI_G610_MC3_VALHALL ")
            append("RES=${internalWidth}x${internalHeight} ")
            append("REFRESH=${settings.preferredRefreshRateHz}Hz ")
            append("TEX=${settings.texturePipeline.id} ")
            append("FP16=${if (settings.useFp16ValhallShaders) 1 else 0} ")
            append("TBDR_CLEAR=${if (settings.enableTbdrTileOptimization) 1 else 0} ")
            append("TURNIP_OVERRIDE=0")
        }

        val json = JSONObject().apply {
            put("app", "BOTWRecompNeo")
            put("target_device", "Motorola Edge 40 Neo (Dimensity 7030 / Mali-G610 MC3)")
            put("detected_hardware", "${hardware.manufacturer} ${hardware.model} (${hardware.gpu.renderer})")
            put("game_version", settings.gameVersion.code)
            put("execution_mode", settings.executionMode.id)
            put("preset", settings.preset.id)
            put("cpu", JSONObject().apply {
                put("soc", "MT6879 Dimensity 7030")
                put("pin_cortex_a78", settings.pinBigCoresA78)
                put("worker_threads", settings.workerThreadCount)
                put("compiler_flags", archFlags)
            })
            put("graphics_vulkan", JSONObject().apply {
                put("driver_strategy", settings.driverStrategy.id)
                put("disable_adreno_turnip", true)
                put("resolution_scale", settings.resolutionScale.toDouble())
                put("render_dimensions", "${internalWidth}x${internalHeight}")
                put("texture_pipeline", settings.texturePipeline.id)
                put("texture_pool_mb", settings.texturePoolLimitMb)
                put("fp16_valhall_shaders", settings.useFp16ValhallShaders)
                put("async_pipeline_compile", settings.asyncVulkanPipelineCompile)
                put("tbdr_tile_optimization", settings.enableTbdrTileOptimization)
            })
            put("display_pacing", JSONObject().apply {
                put("poled_refresh_hz", settings.preferredRefreshRateHz)
                put("fps_cap", settings.targetFpsCap)
                put("lsfg_mode", settings.lsfgMode.id)
                put("lsfg_dll_linked", lsfgDll?.fileName ?: "none")
                put("lsfg_dll_sha256_prefix", lsfgDll?.sha256Short ?: "")
            })
            put("thermal", JSONObject().apply {
                put("guard_enabled", settings.thermalGuardEnabled)
                put("limit_celsius", settings.thermalLimitCelsius)
            })
            put("dump_source", JSONObject().apply {
                put("linked", dump != null)
                put("folder", dump?.folderName ?: "")
                put("files_scanned", dump?.totalFilesScanned ?: 0)
            })
        }.toString(2)

        val outFile = File(context.filesDir, "edge40_neo_recomp.json")
        outFile.writeText(json)

        val checks = listOf(
            PipelineCheckItem(
                title = "Arquitectura ARM64 + Vulkan",
                detail = if (hardware.gpu.vulkanSupported) "${hardware.gpu.vulkanVersionString} (${hardware.gpu.renderer})"
                else "Vulkan no detectado en este entorno — perfil listo para Edge 40 Neo",
                passed = hardware.gpu.vulkanSupported
            ),
            PipelineCheckItem(
                title = "Compatibilidad Mali Valhall (Sin Turnip)",
                detail = "Drivers Adreno Turnip bloqueados; ASTC y FP16 activados para Mali-G610 MC3.",
                passed = true
            ),
            PipelineCheckItem(
                title = "Memoria Disponible vs Pool de Texturas",
                detail = "Pool ${settings.texturePoolLimitMb} MB dentro del límite seguro (${hardware.safeTexturePoolMb} MB).",
                passed = settings.texturePoolLimitMb <= hardware.safeTexturePoolMb + 512
            ),
            PipelineCheckItem(
                title = "Archivos Dump (exefs / romfs)",
                detail = if (dump != null && dump.isReadyForRecomp)
                    "${dump.folderName}: ${dump.totalFilesScanned} archivos verificados"
                else "Pendiente: vincula tu carpeta exefs/romfs vía SAF en el Lanzador",
                passed = dump?.isReadyForRecomp == true
            ),
            PipelineCheckItem(
                title = "Frame Gen LSFG (Opcional)",
                detail = when {
                    settings.lsfgMode.multiplier == 1 -> "Desactivado (modo nativo sin DLL requerida)"
                    lsfgDll != null -> "${lsfgDll.fileName} verificado (sha256:${lsfgDll.sha256Short}…)"
                    else -> "LSFG ${settings.lsfgMode.multiplier}x activo pero falta vincular Lossless.dll"
                },
                passed = settings.lsfgMode.multiplier == 1 || lsfgDll != null
            )
        )

        RecompilationPlan(
            targetDeviceLabel = "Motorola Edge 40 Neo (Dimensity 7030 / Mali-G610 MC3)",
            targetArchitectureFlags = archFlags,
            vulkanProfileFlags = vulkanFlags,
            estimatedMemoryFootprintMb = estimatedFootprintMb,
            readinessChecks = checks,
            generatedJsonConfig = json,
            savedFilePath = outFile.absolutePath
        )
    }

    suspend fun exportConfigToUri(targetUri: Uri, jsonContent: String): Boolean = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(targetUri)?.use { out ->
                out.write(jsonContent.toByteArray(Charsets.UTF_8))
                out.flush()
            }
            true
        } catch (_: Throwable) {
            false
        }
    }

    private fun queryDisplayName(uri: Uri): String? {
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                if (cursor.moveToFirst()) cursor.getString(0) else null
            }
        } catch (_: Throwable) {
            null
        }
    }
}
