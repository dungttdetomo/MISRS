package com.example.misrs.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.example.misrs.data.entities.StatusRecord
import com.example.misrs.data.entities.SystemConfig
import com.example.misrs.data.repository.StatusRepository
import com.example.misrs.data.repository.SystemConfigRepository
import com.example.misrs.service.MeasurementService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val statusRepository: StatusRepository,
    private val systemConfigRepository: SystemConfigRepository
) : AndroidViewModel(application) {

    private val _showPermissionDialog = MutableLiveData<Boolean>()
    val showPermissionDialog: LiveData<Boolean> get() = _showPermissionDialog

    private val _isMeasuring = MutableStateFlow(false)
    val isMeasuring = _isMeasuring

    init {
        viewModelScope.launch {
            MeasurementService.measurementStatusFlow.collectLatest { isRunning ->
                _isMeasuring.value = isRunning
            }
        }
    }

    fun startMeasurement(deviceId: String, password: String) {
        Log.d("MainViewModel", "Starting MeasurementService")
        val context = getApplication<Application>()
        if (!hasLocationPermission()) {
            _showPermissionDialog.postValue(true)
            return
        }
        val intent = Intent(context, MeasurementService::class.java).apply {
            putExtra("DEVICE_ID", deviceId)
            putExtra("PASSWORD", password)
        }
        context.startService(intent)
    }

    fun stopMeasurement() {
        Log.d("MainViewModel", "Stopping MeasurementService")
        val context = getApplication<Application>()
        val intent = Intent(context, MeasurementService::class.java)
        context.stopService(intent)
    }

    fun resetPermissionDialogState() {
        _showPermissionDialog.postValue(false)
    }

    private fun hasLocationPermission(): Boolean {
        val context = getApplication<Application>()
        val fineLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
        val coarseLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        val backgroundLocationPermission = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
        return fineLocationPermission == PackageManager.PERMISSION_GRANTED &&
                coarseLocationPermission == PackageManager.PERMISSION_GRANTED &&
                backgroundLocationPermission == PackageManager.PERMISSION_GRANTED
    }

     fun getCurrentSystemConfig(): Flow<SystemConfig?> {
        return systemConfigRepository.getConfigFlow()
    }

     fun getLast10Records(): Flow<List<StatusRecord>> {
        return statusRepository.getLast10RecordsFlow()
    }
}
