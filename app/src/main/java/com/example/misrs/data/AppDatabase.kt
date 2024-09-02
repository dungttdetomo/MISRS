package com.example.misrs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.misrs.data.dao.StatusRecordDao
import com.example.misrs.data.dao.SystemConfigDao
import com.example.misrs.data.entities.StatusRecord
import com.example.misrs.data.entities.SystemConfig

@Database(
    entities = [SystemConfig::class, StatusRecord::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun systemConfigDao(): SystemConfigDao
    abstract fun statusRecordDao(): StatusRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "misrs_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
