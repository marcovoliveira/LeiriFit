package com.example.leirifit.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "runs_table")
data class Run(
    @PrimaryKey(autoGenerate = true)
    var runId: Long = 0L,

    @ColumnInfo(name = "start_time_milli")
    val startTimeMilli: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "end_time_milli")
    var endTimeMilli: Long = startTimeMilli,

    @ColumnInfo(name = "distance")
    var distance: Float = 0F,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "age")
    var age: String = "",

    @ColumnInfo(name = "sexo")
    var sexo: String = ""

)