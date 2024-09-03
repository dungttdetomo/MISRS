package com.example.misrs.data.repository

import android.content.Context
import com.example.misrs.data.AppDatabase
import com.example.misrs.data.entities.SystemConfig
import kotlinx.coroutines.flow.Flow

class SystemConfigRepository(context: Context) {

    private val systemConfigDao = AppDatabase.getDatabase(context).systemConfigDao()

    suspend fun getConfig(): SystemConfig? {
        return systemConfigDao.getConfig()
    }

    fun getConfigFlow(): Flow<SystemConfig?> {
        return systemConfigDao.getConfigFlow()
    }

    suspend fun storeConfig(config: SystemConfig) {
        systemConfigDao.insertConfig(config)
    }
}
