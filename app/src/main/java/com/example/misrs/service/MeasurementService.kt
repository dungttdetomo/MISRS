package com.example.misrs.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.misrs.R
import com.example.misrs.data.entities.StatusRecord
import com.example.misrs.data.entities.SystemConfig
import com.example.misrs.data.repository.StatusRepository
import com.example.misrs.data.repository.SystemConfigRepository
import com.example.misrs.mapper.SystemConfigMapper
import com.example.misrs.network.NetworkModule
import com.example.misrs.network.dto.UploadBatchRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.util.*

class MeasurementService : Service() {

    private var measurementJob: Job? = null
    private var dataSyncJob: Job? = null
    private var getConfigJob: Job? = null
    private lateinit var statusRepository: StatusRepository
    private lateinit var systemConfigRepository: SystemConfigRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    companion object {
        val measurementStatusFlow = MutableSharedFlow<Boolean>(replay = 1) // Ensure the latest status is replayed to new collectors
    }

    override fun onCreate() {
        super.onCreate()
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        startForegroundService()
        emitServiceStatus(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceId = intent?.getStringExtra("DEVICE_ID") ?: ""
        val password = intent?.getStringExtra("PASSWORD") ?: ""

        statusRepository = StatusRepository(applicationContext)
        systemConfigRepository = SystemConfigRepository(applicationContext)

        startMeasurement(deviceId, password)
        return START_STICKY
    }

    override fun onDestroy() {
        Log.d("MeasurementService", "onDestroy called, stopping service")
        super.onDestroy()
        stopMeasurement()
    }

    private fun emitServiceStatus(isRunning: Boolean) {
        measurementStatusFlow.tryEmit(isRunning)
    }


    private fun startMeasurement(deviceId: String, password: String) {
        measurementJob = CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val config = getUpdatedConfig(deviceId)
                if (config == null) {
                    Log.w("MeasurementService", "Failed to load config")
                    break
                }
                Log.d("MeasurementService", "Starting new measurement cycle")
                val location = getCurrentLocation()
                val connectStatus = if (location != null) {
                    Log.d("MeasurementService", "Location obtained: ${location.latitude}, ${location.longitude}")
                    withContext(Dispatchers.IO) {
                        val response = NetworkModule.api.checkConnection(deviceId, password).execute()
                        if (response.isSuccessful) 1 else 0
                    }
                } else {
                    Log.w("MeasurementService", "Failed to obtain location")
                    0
                }

                val newRecord = StatusRecord(
                    uuid = UUID.randomUUID().toString(),
                    device_id = deviceId,
                    record_time = System.currentTimeMillis().toString(),
                    latitude = location?.latitude?.toFloat() ?: 0f,
                    longitude = location?.longitude?.toFloat() ?: 0f,
                    connect_status = connectStatus
                )

                val lastRecord = statusRepository.getLastRecord()
                if (shouldSaveNewRecord(lastRecord, newRecord, config.point_distance)) {
                    Log.d("MeasurementService", "Saving new record to database: $newRecord")
                    statusRepository.insertRecord(newRecord)
                } else {
                    Log.d("MeasurementService", "New record not saved, point distance too small or no config")
                }

                Log.d("MeasurementService", "Waiting for next measurement cycle: ${config.check_connect_period} seconds")
                delay(config.check_connect_period * 1000L)
            }
        }

        dataSyncJob = scheduleDataSync(deviceId, password)
        getConfigJob = scheduleGetConfig(deviceId, password)
    }

    private fun stopMeasurement() {
        Log.d("MeasurementService", "Stopping measurement jobs")
        measurementJob?.cancel()
        dataSyncJob?.cancel()
        getConfigJob?.cancel()
        Log.d("MeasurementService", "Stopping foreground service and self")
        emitServiceStatus(false) // Emit service stopped status before stopping the service
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
        Log.d("MeasurementService", "Service should now be stopped")
    }

    private fun scheduleDataSync(deviceId: String, password: String): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val config = getUpdatedConfig(deviceId)
                if (config == null) {
                    Log.w("MeasurementService", "Failed to load config")
                    break
                }
                Log.d("MeasurementService", "Running scheduled data sync")
                syncRecords(deviceId, password)
                delay(config.data_sync_period.toLong() * 1000L)
            }
        }
    }

    private fun scheduleGetConfig(deviceId: String, password: String): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            while (true) {
                val config = getUpdatedConfig(deviceId)
                if (config == null) {
                    Log.w("MeasurementService", "Failed to load config")
                    break
                }
                Log.d("MeasurementService", "Running scheduled get config")
                getConfig(deviceId, password)
                delay(config.get_config_period.toLong() * 1000L)
            }
        }
    }

    private suspend fun getUpdatedConfig(deviceId: String): SystemConfig? {
        Log.d("MeasurementService", "Fetching updated config from database for deviceId: $deviceId")
        val config = systemConfigRepository.getConfig()
        if (config != null) {
            Log.d("MeasurementService", "Config loaded: $config")
        } else {
            Log.w("MeasurementService", "No config found for deviceId: $deviceId")
        }
        return config
    }

    private fun syncRecords(deviceId: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val unsyncedRecords = statusRepository.getUnsyncedRecords()
                if (unsyncedRecords.isNotEmpty()) {
                    val uploadRecords = unsyncedRecords.map { statusRepository.mapToUploadRecordDto(it) }
                    val requestBody = UploadBatchRequest(records = uploadRecords)
                    val response = NetworkModule.api.uploadData(deviceId, password, requestBody).execute()
                    if (response.isSuccessful) {
                        val responseBody = response.body()?.string() ?: ""
                        Log.d("MeasurementService", "Response JSON: $responseBody")
                        val jsonResponse = JSONObject(responseBody)
                        val failedRecords = jsonResponse.getJSONArray("failed_records")
                        val failedUuids = mutableSetOf<String>()
                        for (i in 0 until failedRecords.length()) {
                            failedUuids.add(failedRecords.getString(i))
                        }
                        val successfulRecords = unsyncedRecords.filter { !failedUuids.contains(it.uuid) }
                        if (successfulRecords.isNotEmpty()) {
                            Log.d("MeasurementService", "Records synced successfully: ${successfulRecords.size} records")
                            statusRepository.markRecordsAsSynced(successfulRecords.map { it.uuid })
                        }
                    } else {
                        Log.w("MeasurementService", "Failed to sync records")
                    }
                } else {
                    Log.d("MeasurementService", "No unsynced records to sync")
                }
            } catch (e: Exception) {
                Log.e("MeasurementService", "Error syncing records: ${e.message}")
            }
        }
    }

    private fun getConfig(deviceId: String, password: String) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val response = NetworkModule.api.fetchConfig(deviceId, password).execute()
                if (response.isSuccessful) {
                    Log.d("MeasurementService", "Config fetched successfully")
                    val configDto = response.body()
                    if (configDto != null) {
                        val configEntity = SystemConfigMapper.mapToEntity(configDto, deviceId, password)
                        systemConfigRepository.storeConfig(configEntity)
                        Log.d("MeasurementService", "Config stored successfully: $configEntity")
                    }
                } else {
                    Log.w("MeasurementService", "Failed to fetch config")
                }
            } catch (e: Exception) {
                Log.e("MeasurementService", "Error fetching config: ${e.message}")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            val locationTask = fusedLocationClient.lastLocation
            locationTask.await()
        } catch (e: Exception) {
            Log.e("MeasurementService", "Error getting location: ${e.message}")
            null
        }
    }

    private fun shouldSaveNewRecord(
        lastRecord: StatusRecord?,
        newRecord: StatusRecord,
        pointDistance: Int
    ): Boolean {
        if (lastRecord == null) return true

        val distance = FloatArray(1)
        Location.distanceBetween(
            lastRecord.latitude.toDouble(),
            lastRecord.longitude.toDouble(),
            newRecord.latitude.toDouble(),
            newRecord.longitude.toDouble(),
            distance
        )

        val shouldSave = distance[0] > pointDistance
        Log.d("MeasurementService", "Distance between records: ${distance[0]} meters. Should save: $shouldSave")
        return shouldSave
    }

    private fun startForegroundService() {
        val channelId = "measurement_service_channel"
        val channelName = "Measurement Service"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_LOW)
        notificationManager.createNotificationChannel(channel)

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Measurement Service")
            .setContentText("Running measurements in the background")
            .setSmallIcon(R.drawable.ic_notification)
            .build()

        startForeground(1, notification)
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }
}
