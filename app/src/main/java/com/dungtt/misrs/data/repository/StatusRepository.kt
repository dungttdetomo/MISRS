package com.dungtt.misrs.data.repository

import android.content.Context
import com.dungtt.misrs.data.AppDatabase
import com.dungtt.misrs.data.entities.StatusRecord
import com.dungtt.misrs.network.dto.UploadRecordDto
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter


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

    fun getLast10RecordsFlow(): Flow<List<StatusRecord>> {
        return statusRecordDao.getLast10RecordsFlow()
    }

    suspend fun getUnsyncedRecords(limit: Int = 360): List<StatusRecord> {
        return statusRecordDao.getUnsyncedRecords(limit)
    }

    suspend fun deleteRecords(uuids: List<String>) {
        statusRecordDao.deleteRecords(uuids)
    }

    suspend fun markRecordsAsSynced(uuids: List<String>) {
        statusRecordDao.markAsSynced(uuids)
    }

}
