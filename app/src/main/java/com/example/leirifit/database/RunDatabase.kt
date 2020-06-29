package com.example.leirifit.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Run::class], version = 1, exportSchema = false)
abstract class RunDatabase : RoomDatabase() {

    abstract val runDatabaseDao: RunDAO

    companion object {

        @Volatile
        private var INSTANCE: RunDatabase? = null

        fun getInstance(context: Context): RunDatabase {
            synchronized(this) {
                var instance = INSTANCE

                if (instance == null) {
                    instance = Room.databaseBuilder(
                        context.applicationContext,
                        RunDatabase::class.java,
                        "runs_history_database"
                    )
                        .fallbackToDestructiveMigration()
                        .build()
                    INSTANCE = instance
                }
                return instance
            }
        }
    }
}