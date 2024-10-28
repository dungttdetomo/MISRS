package com.dungtt.misrs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.dungtt.misrs.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ViewScreen(viewModel: MainViewModel, navController: NavController) {
    val systemConfig = viewModel.getCurrentSystemConfig().collectAsState(initial = null)
    val records = viewModel.getLast10Records().collectAsState(initial = emptyList())
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // System setting section
        Text(
            text = "System setting:",
            style = MaterialTheme.typography.titleMedium // Updated style
        )
        Spacer(modifier = Modifier.height(8.dp))
        systemConfig.value?.let { config ->
            Text("Device ID: ${config.device_id} ")
            Text("Check Connect Period: ${config.check_connect_period} seconds")
            Text("Data Sync Period: ${config.data_sync_period} seconds")
            Text("Get Config Period: ${config.get_config_period} seconds")
            Text("Point Distance: ${config.point_distance} meters")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Log 10 records section
        Text(
            text = "Log 10 records:",
            style = MaterialTheme.typography.titleMedium // Updated style
        )
        Spacer(modifier = Modifier.height(8.dp))

        records.value.forEach { record ->
            val formattedTime = dateFormat.format(Date(record.record_time.toLong()))
            Text(text = "$formattedTime: ${record.latitude}, ${record.longitude} - Status: ${record.connect_status}")
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(onClick = { navController.popBackStack()}) {
            Text("Back")
        }
    }
}
