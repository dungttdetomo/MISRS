package com.example.misrs.data.repository

import android.content.Context
import com.example.misrs.data.AppDatabase
import com.example.misrs.data.entities.SystemConfig
import com.example.misrs.mapper.SystemConfigMapper
import com.example.misrs.network.NetworkModule

class SystemConfigRepository(context: Context) {

    private val systemConfigDao = AppDatabase.getDatabase(context).systemConfigDao()

    suspend fun getConfig(deviceId: String): SystemConfig? {
        return systemConfigDao.getConfig(deviceId)
    }

    suspend fun fetchAndStoreConfig(deviceId: String, password: String) {
        val response = NetworkModule.api.fetchConfig(deviceId, password).execute()
        if (response.isSuccessful) {
            val configDto  = response.body()
            if (configDto  != null) {
                // Sử dụng mapper để chuyển đổi DTO thành Entity trước khi lưu
                val configEntity = SystemConfigMapper.mapToEntity(configDto, deviceId, password)
                systemConfigDao.insertConfig(configEntity)
            }
        }
    }

    suspend fun storeConfig(config: SystemConfig) {
        systemConfigDao.insertConfig(config)
    }
}
