package com.dungtt.misrs.mapper

import com.dungtt.misrs.data.entities.SystemConfig
import com.dungtt.misrs.network.dto.ConfigResponseDto

object SystemConfigMapper {
    fun mapToEntity(dto: ConfigResponseDto, deviceId: String, password: String): SystemConfig {
        return SystemConfig(
            device_id = deviceId,
            password = password,
            check_connect_period = dto.check_connect_period,
            data_sync_period = dto.data_sync_period,
            get_config_period = dto.get_config_period,
            point_distance = dto.point_distance
        )
    }
}
