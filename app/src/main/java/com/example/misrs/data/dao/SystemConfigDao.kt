package com.example.misrs.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.misrs.data.entities.SystemConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface SystemConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConfig(config: SystemConfig)

    @Query("SELECT * FROM system_config LIMIT 1")
    fun getConfigFlow(): Flow<SystemConfig?>

    @Query("SELECT * FROM system_config LIMIT 1")
    suspend fun getConfig(): SystemConfig?
}
