package com.dungtt.misrs.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.dungtt.misrs.data.dao.StatusRecordDao
import com.dungtt.misrs.data.dao.SystemConfigDao
import com.dungtt.misrs.data.entities.StatusRecord
import com.dungtt.misrs.data.entities.SystemConfig

@Database(
    entities = [SystemConfig::class, StatusRecord::class],
    version = 3, // Tăng phiên bản lên 3
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun systemConfigDao(): SystemConfigDao
    abstract fun statusRecordDao(): StatusRecordDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Migration from version 1 to 2 (giữ nguyên như cũ)
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `system_config_new` (
                        `id` INTEGER PRIMARY KEY NOT NULL,
                        `device_id` TEXT NOT NULL,
                        `password` TEXT NOT NULL,
                        `check_connect_period` INTEGER NOT NULL DEFAULT 10,
                        `data_sync_period` INTEGER NOT NULL DEFAULT 3600,
                        `get_config_period` INTEGER NOT NULL DEFAULT 60,
                        `point_distance` INTEGER NOT NULL DEFAULT 5
                    )
                """.trimIndent())

                db.execSQL("""
                    INSERT INTO `system_config_new` (`id`, `device_id`, `password`, `check_connect_period`, `data_sync_period`, `get_config_period`, `point_distance`)
                    SELECT 1, `device_id`, `password`, `check_connect_period`, `data_sync_period`, `get_config_period`, `point_distance`
                    FROM `system_config`
                """.trimIndent())

                db.execSQL("DROP TABLE `system_config`")
                db.execSQL("ALTER TABLE `system_config_new` RENAME TO `system_config`")
            }
        }

        // Migration from version 2 to 3 to add 'distance' column to StatusRecord
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `status_record` ADD COLUMN `distance` REAL DEFAULT NULL")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "misrs_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3) // Thêm migration mới
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

