package com.example.misrs.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.misrs.data.repository.SystemConfigRepository
import com.example.misrs.service.MeasurementService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class BootCompletedReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d("BootCompletedReceiver", "Device booted, checking system config.")

            // Initialize SystemConfigRepository
            val systemConfigRepository = SystemConfigRepository(context)

            // Use a coroutine to check the config and start the service if necessary
            CoroutineScope(Dispatchers.IO).launch {
                val config = runBlocking { systemConfigRepository.getConfig() }

                if (config != null && config.device_id.isNotEmpty() && config.password.isNotEmpty()) {
                    // Start the measurement service
                    val serviceIntent = Intent(context, MeasurementService::class.java).apply {
                        putExtra("DEVICE_ID", config.device_id)
                        putExtra("PASSWORD", config.password)
                    }
                    context.startForegroundService(serviceIntent)
                } else {
                    Log.d("BootCompletedReceiver", "No valid config found, service not started.")
                }
            }
        }
    }
}
