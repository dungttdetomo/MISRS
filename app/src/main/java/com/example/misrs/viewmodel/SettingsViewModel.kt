package com.example.misrs.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.misrs.data.repository.SystemConfigRepository
import com.example.misrs.mapper.SystemConfigMapper
import com.example.misrs.network.NetworkModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import org.json.JSONObject

class SettingsViewModel(private val systemConfigRepository: SystemConfigRepository) : ViewModel() {

    fun checkConnection(deviceId: String, password: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            Log.d("SettingsViewModel", "Coroutine started for checkConnection")
            try {
                Log.d("SettingsViewModel", "Preparing to call API with deviceId: $deviceId")
                val response = NetworkModule.api.checkConnection(deviceId, password).execute()

                if (response.isSuccessful) {
                    val responseBody = response.body()?.string() ?: ""
                    Log.d("SettingsViewModel", "Response JSON: $responseBody")

                    // Parse the response if necessary
                    val jsonObject = JSONObject(responseBody)
                    val status = jsonObject.getString("status")

                    if (status == "ok") {
                        Log.d("SettingsViewModel", "API returned status ok")
                        // Fetch and store config in the local database
                        val configResponse = NetworkModule.api.fetchConfig(deviceId, password).execute()
                        if (configResponse.isSuccessful) {
                            val configDto = configResponse.body()
                            if (configDto != null) {
                                Log.d("SettingsViewModel", "ConfigResponseDto received: $configDto")
                                val configEntity = SystemConfigMapper.mapToEntity(configDto, deviceId, password)
                                Log.d("SettingsViewModel", "Mapped to SystemConfig: $configEntity")
                                systemConfigRepository.storeConfig(configEntity)
                                Log.d("SettingsViewModel", "Config stored successfully")
                            } else {
                                Log.w("SettingsViewModel", "Config response body was null")
                            }
                        } else {
                            Log.w("SettingsViewModel", "Failed to fetch config")
                        }
                        withContext(Dispatchers.Main) {
                            onSuccess() // Call the success callback on the main thread
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onFailure() // Handle failure if status is not ok
                        }
                    }
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.d("SettingsViewModel", "Error: $errorBody")
                    withContext(Dispatchers.Main) {
                        onFailure() // Call the failure callback on the main thread
                    }
                }
            } catch (e: Exception) {
                Log.e("SettingsViewModel", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) {
                    onFailure() // Handle the failure on the main thread
                }
            }
        }
    }
}