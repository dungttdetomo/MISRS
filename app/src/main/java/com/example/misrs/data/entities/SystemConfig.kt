package com.example.misrs.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "system_config")
data class SystemConfig(
    @PrimaryKey val id: Int = 1, // Constant primary key
    val device_id: String,
    val password: String,
    val check_connect_period: Int = 10,  // Default value
    val data_sync_period: Int = 3600,    // Default value
    val get_config_period: Int = 60,     // Default value
    val point_distance: Int = 5          // Default value
)
