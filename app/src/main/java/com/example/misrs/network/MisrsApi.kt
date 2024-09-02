package com.example.misrs.network

import com.example.misrs.data.entities.StatusRecord
import com.example.misrs.data.entities.SystemConfig
import com.example.misrs.network.dto.ConfigResponseDto
import com.example.misrs.network.dto.UploadBatchRequest
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST

interface MisrsApi {

    @GET("api/health_check")
    fun checkConnection(
        @Header("device-id") deviceId: String,
        @Header("device-password") password: String
    ): Call<ResponseBody>

    @POST("api/upload_batch")
    fun uploadData(
        @Header("device-id") deviceId: String,
        @Header("device-password") password: String,
        @Body request: UploadBatchRequest
    ): Call<ResponseBody>

    @GET("api/config")
    fun fetchConfig(
        @Header("device-id") deviceId: String,
        @Header("device-password") password: String
    ): Call<ConfigResponseDto>
}
