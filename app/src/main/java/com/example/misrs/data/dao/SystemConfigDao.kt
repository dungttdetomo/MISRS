package com.example.misrs.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.misrs.data.entities.SystemConfig

@Dao
interface SystemConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SystemConfig)

    @Query("SELECT * FROM system_config WHERE device_id = :deviceId LIMIT 1")
    suspend fun getConfig(deviceId: String): SystemConfig?
}
