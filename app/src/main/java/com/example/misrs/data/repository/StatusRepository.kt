package com.example.misrs.data.repository

import android.content.Context
import com.example.misrs.data.AppDatabase
import com.example.misrs.data.entities.StatusRecord
import com.example.misrs.network.NetworkModule
import com.example.misrs.network.dto.UploadRecordDto
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

class StatusRepository(context: Context) {

    private val statusRecordDao = AppDatabase.getDatabase(context).statusRecordDao()

    suspend fun insertRecord(record: StatusRecord) {
        statusRecordDao.insertRecord(record)
    }

    fun mapToUploadRecordDto(record: StatusRecord): UploadRecordDto {
        return UploadRecordDto(
            uuid = record.uuid,
            latitude = record.latitude,
            longitude = record.longitude,
            timestamp = formatTimestamp(record.record_time), // Ensure this is formatted correctly
            connect_status = record.connect_status
        )
    }

    private fun formatTimestamp(timestamp: String): String {
        // Chuyển đổi từ Epoch timestamp (chuỗi) sang định dạng mong muốn
        val epochMillis = timestamp.toLong()
        val instant = Instant.ofEpochMilli(epochMillis)
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneId.systemDefault())
        return formatter.format(instant)
    }

    fun getLastRecord(): StatusRecord? {
        return statusRecordDao.getLastRecord()
    }

    suspend fun getLast10Records(): List<StatusRecord> {
        return statusRecordDao.getLast10Records()
    }

    suspend fun getUnsyncedRecords(limit: Int = 10): List<StatusRecord> {
        return statusRecordDao.getUnsyncedRecords(limit)
    }

    suspend fun markRecordsAsSynced(uuids: List<String>) {
        statusRecordDao.markAsSynced(uuids)
    }

//    suspend fun uploadUnsyncedRecords(deviceId: String, password: String) {
//        val unsyncedRecords = statusRecordDao.getUnsyncedRecords().take(10)
//        if (unsyncedRecords.isNotEmpty()) {
//            val response = NetworkModule.api.uploadData(deviceId, password, unsyncedRecords).execute()
//            if (response.isSuccessful) {
//                statusRecordDao.markAsSynced(unsyncedRecords.map { it.uuid })
//            }
//        }
//    }
}
