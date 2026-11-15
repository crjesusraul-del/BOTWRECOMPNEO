package com.example.data.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "benchmark_runs")
data class BenchmarkRunEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestampMs: Long = System.currentTimeMillis(),
    val presetName: String,
    val executionMode: String,
    val gpuRenderer: String,
    val resolutionScale: Float,
    val refreshRateHz: Int,
    val avgFps: Float,
    val low1PercentFps: Float,
    val avgFrameTimeMs: Float,
    val startTempC: Float,
    val endTempC: Float,
    val fp16Enabled: Boolean
)
