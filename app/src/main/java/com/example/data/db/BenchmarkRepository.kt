package com.example.data.db

import kotlinx.coroutines.flow.Flow

class BenchmarkRepository(private val dao: BenchmarkDao) {
    val allRuns: Flow<List<BenchmarkRunEntity>> = dao.observeRuns()

    suspend fun recordRun(run: BenchmarkRunEntity) = dao.insertRun(run)

    suspend fun deleteRun(id: Long) = dao.deleteRunById(id)

    suspend fun clearAllRuns() = dao.clearAll()
}
