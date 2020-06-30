package com.example.leirifit.database

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "runs_table")
data class Run(
    @PrimaryKey(autoGenerate = true)
    var runId: Long = 0L,

    @ColumnInfo(name = "duration")
    var duration: Long = 0,

    @ColumnInfo(name = "distance")
    var distance: Float = 0F,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "age")
    var age: String = "",

    @ColumnInfo(name = "sexo")
    var sexo: String = ""

)