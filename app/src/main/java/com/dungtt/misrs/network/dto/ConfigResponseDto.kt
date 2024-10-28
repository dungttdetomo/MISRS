package com.dungtt.misrs.network.dto

data class ConfigResponseDto(
    val check_connect_period: Int,
    val data_sync_period: Int,
    val get_config_period: Int,
    val point_distance: Int
)
