package com.dungtt.misrs.network.dto

data class UploadRecordDto(
    val uuid: String,
    val latitude: Float,
    val longitude: Float,
    val timestamp: String, // This should match the expected timestamp format
    val connect_status: Int
)

data class UploadBatchRequest(
    val records: List<UploadRecordDto>
)
