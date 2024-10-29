package com.dungtt.misrs.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import com.dungtt.misrs.R
import com.dungtt.misrs.data.entities.StatusRecord
import com.dungtt.misrs.data.entities.SystemConfig
import com.dungtt.misrs.data.repository.StatusRepository
import com.dungtt.misrs.data.repository.SystemConfigRepository
import com.dungtt.misrs.manager.LastRecordManager
import com.dungtt.misrs.mapper.SystemConfigMapper
import com.dungtt.misrs.network.NetworkModule
import com.dungtt.misrs.network.dto.UploadBatchRequest
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import org.json.JSONObject
import java.util.*

class MeasurementService : Service() {

    private var measurementJob: Job? = null
    private var dataSyncJob: Job? = null
    private var getConfigJob: Job? = null
    private lateinit var statusRepository: StatusRepository
    private lateinit var systemConfigRepository: SystemConfigRepository
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var lastRecordManager: LastRecordManager

    companion object {
        val measurementStatusFlow = MutableSharedFlow<Boolean>(replay = 1) // Ensure the latest status is replayed to new collectors
    }

    override fun onCreate() {
        super.onCreate()
        try {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        } catch (e: Exception) {
            Log.e("MeasurementService", "Error initializing fusedLocationClient: ${e.message}")
            stopSelf()  // Dừng dịch vụ nếu không thể khởi tạo `fusedLocationClient`
        }
        // Khởi tạo lastRecordManager
        lastRecordManager = LastRecordManager(applicationContext)
        // Bắt đầu foreground service với notification
        startForegroundService()
        // Phát tín hiệu rằng service đã bắt đầu
        emitServiceStatus(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val deviceId = intent?.getStringExtra("DEVICE_ID") ?: ""
        val password = intent?.getStringExtra("PASSWORD") ?: ""

        Log.d("MeasurementService", "Received deviceId: $deviceId, password: $password")

        if (deviceId.isEmpty() || password.isEmpty()) {
            // Nếu không có deviceId hoặc password, dừng dịch vụ
            Log.e("MeasurementService", "Device ID or password is missing, stopping service.")
            stopSelf()
        } else {
            statusRepository = StatusRepository(applicationContext)
            systemConfigRepository = SystemConfigRepository(applicationContext)
            startMeasurement(deviceId, password)
        }

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
        // Ensure that the job is only started if it's not already running
        if (measurementJob?.isActive == true) return
        measurementJob = CoroutineScope(Dispatchers.IO).launch {
            while (isActive) {
                val config = getUpdatedConfig(deviceId)
                if (config == null) {
                    Log.w("MeasurementService", "Failed to load config")
                    continue
                }
                Log.d("MeasurementService", "Starting new measurement cycle")
                // Lấy vị trí mới tại mỗi chu kỳ
                val location = getCurrentLocation()
                val lastRecord = lastRecordManager.getLastRecord()
                var connectStatus: Int
                Log.d("MeasurementService", "Last location: lat=${lastRecord?.latitude}, lon=${lastRecord?.longitude}")
                Log.d("MeasurementService", "New location: lat=${location?.latitude}, lon=${location?.longitude}")

                // Tính khoảng cách một lần duy nhất
                val distance = if (location != null && lastRecord != null) {
                    val result = FloatArray(1)
                    Location.distanceBetween(
                        lastRecord.latitude.toDouble(),
                        lastRecord.longitude.toDouble(),
                        location.latitude,
                        location.longitude,
                        result
                    )
                    result[0]
                } else {
                    -1f // Nếu không có lastRecord hoặc location hiện tại null, đặt khoảng cách là -1
                }

                Log.d("MeasurementService", "Calculated distance: $distance meters")

                // Xử lý logic kiểm tra kết nối và lấy trạng thái kết nối
                connectStatus = try {
                    if (distance > 0) {
                        Log.d("MeasurementService", "Location changed, checking connection")
                        withContext(Dispatchers.IO) {
                            val response = NetworkModule.api.checkConnection(deviceId, password).execute()
                            if (response.isSuccessful) 1 else 0
                        }
                    } else if (lastRecord?.connect_status == 0) {
                        Log.d("MeasurementService", "Location unchanged but previous connection status was 0, rechecking connection")
                        withContext(Dispatchers.IO) {
                            val response = NetworkModule.api.checkConnection(deviceId, password).execute()
                            if (response.isSuccessful) 1 else 0
                        }
                    } else {
                        Log.d("MeasurementService", "Location unchanged and previous connection status was 1, skipping connection check")
                        lastRecord?.connect_status ?: 0
                    }
                } catch (e: Exception) {
                    Log.e("MeasurementService", "Network error during checkConnection: ${e.message}")
                    0 // Nếu lỗi mạng, đặt trạng thái kết nối là 0 (không kết nối)
                }

                val newRecord = StatusRecord(
                    uuid = UUID.randomUUID().toString(),
                    device_id = deviceId,
                    record_time = System.currentTimeMillis().toString(),
                    latitude = location?.latitude?.toFloat() ?: 0f,
                    longitude = location?.longitude?.toFloat() ?: 0f,
                    connect_status = connectStatus,
                    distance = if (distance >= 0) distance else null
                )

                Log.d("MeasurementService", "Configured point distance: ${config.point_distance} meters")

                if (shouldSaveNewRecord(lastRecord, newRecord, config.point_distance)) {
                    Log.d("MeasurementService", "Saving new record to database: $newRecord")
                    statusRepository.insertRecord(newRecord)
                    lastRecordManager.saveLastRecord(newRecord)  // Lưu lại bản ghi mới nhất
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

                            // Xóa các bản ghi đã tải lên thành công
                            statusRepository.deleteRecords(successfulRecords.map { it.uuid })
                        }
                    } else {
                        Log.w("MeasurementService", "Failed to sync records")
                    }
                } else {
                    Log.d("MeasurementService", "No unsynced records to sync")
                }
            } catch (e: Exception) {
                Log.e("MeasurementService", "Error syncing records: ${e.message}")
                // Thêm logic xử lý khi không có kết nối mạng
                Log.d("MeasurementService", "No network connection. Unsynced records will be retried in the next sync cycle.")
                // Không xóa các bản ghi, giữ nguyên để thử lại trong chu kỳ đồng bộ tiếp theo
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
                // Thêm logic xử lý khi không có kết nối mạng
                val lastConfig = systemConfigRepository.getConfig()
                if (lastConfig != null) {
                    Log.d("MeasurementService", "Using last known config from database: $lastConfig")
                    // Sử dụng config từ database thay vì config mới
                } else {
                    Log.w("MeasurementService", "No config available in database, continuing with default or cached config")
                }
            }
        }
    }


    @SuppressLint("MissingPermission")
    private suspend fun getCurrentLocation(): Location? {
        return try {
            // Tạo yêu cầu vị trí với độ chính xác cao
            val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 1000L  // Cập nhật mỗi 1 giây
            ).build()

            // Sử dụng `CompletableDeferred` để đợi cho đến khi nhận được vị trí
            val locationDeferred = CompletableDeferred<Location?>()

            val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                    super.onLocationResult(locationResult)
                    locationResult.lastLocation?.let { location ->
                        Log.d("MeasurementService", "New location: lat=${location.latitude}, lon=${location.longitude}")
                        locationDeferred.complete(location)  // Trả về vị trí mới
                    }
                }
            }

            // Yêu cầu cập nhật vị trí
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())

            // Chờ vị trí từ callback
            val location = locationDeferred.await()

            // Hủy đăng ký callback sau khi lấy được vị trí
            fusedLocationClient.removeLocationUpdates(locationCallback)

            if (location != null) {
                Log.d("MeasurementService", "Using new location: lat=${location.latitude}, lon=${location.longitude}")
            } else {
                Log.w("MeasurementService", "Failed to get current location")
            }
            location
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
