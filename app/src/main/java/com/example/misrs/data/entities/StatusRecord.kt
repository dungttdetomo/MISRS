package com.example.misrs.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "status_record")
data class StatusRecord(
    @PrimaryKey val uuid: String = UUID.randomUUID().toString(),
    val device_id: String,
    val record_time: String, // Stored as a timestamp
    val latitude: Float,
    val longitude: Float,
    val connect_status: Int,
    val sync_status: Int = 0, // 0 means not synced, 1 means synced
    var failed: Boolean = false  // New field to track failed uploads
)
