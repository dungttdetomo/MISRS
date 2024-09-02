package com.example.misrs.viewmodel

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.example.misrs.data.entities.StatusRecord
import com.example.misrs.data.repository.StatusRepository
import com.example.misrs.data.repository.SystemConfigRepository
import com.example.misrs.service.MeasurementService

class MainViewModel(
    application: Application,
    private val statusRepository: StatusRepository,
    private val systemConfigRepository: SystemConfigRepository
) : AndroidViewModel(application) {

    private val _showPermissionDialog = MutableLiveData<Boolean>()
    val showPermissionDialog: LiveData<Boolean> get() = _showPermissionDialog

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

    suspend fun getLast10Records(): List<StatusRecord> {
        val records = statusRepository.getLast10Records()
        Log.d("MainViewModel", "Fetched last 10 records: $records")
        return records
    }
}
