package com.example.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BenchmarkDao {
    @Query("SELECT * FROM benchmark_runs ORDER BY timestampMs DESC")
    fun observeRuns(): Flow<List<BenchmarkRunEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRun(run: BenchmarkRunEntity)

    @Query("DELETE FROM benchmark_runs WHERE id = :id")
    suspend fun deleteRunById(id: Long)

    @Query("DELETE FROM benchmark_runs")
    suspend fun clearAll()
}
