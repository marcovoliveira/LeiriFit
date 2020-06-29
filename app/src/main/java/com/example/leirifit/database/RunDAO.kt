package com.example.leirifit.database

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RunDAO {
    @Insert
    fun insert(run: Run)

    @Update
    fun update(run: Run)

    @Query("SELECT * from runs_table WHERE runId = :key")
    fun get(key: Long): Run?

    @Query("DELETE FROM runs_table")
    fun clear()

    @Query("SELECT * FROM runs_table ORDER BY runId DESC LIMIT 1")
    fun getRun(): Run?

    @Query("SELECT * FROM runs_table ORDER BY runId DESC")
    fun getAllRuns(): LiveData<List<Run>>
}