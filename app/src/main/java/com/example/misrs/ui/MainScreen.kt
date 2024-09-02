package com.example.misrs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.observe
import com.example.misrs.data.entities.StatusRecord
import com.example.misrs.viewmodel.MainViewModel
import kotlinx.coroutines.launch
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.livedata.observeAsState

@Composable
fun MainScreen(viewModel: MainViewModel, deviceId: String, password: String) {
    var isMeasuring by remember { mutableStateOf(false) }
    var records by remember { mutableStateOf<List<StatusRecord>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()
    val showDialog by viewModel.showPermissionDialog.observeAsState(false)
    val context = LocalContext.current

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.resetPermissionDialogState() },
            confirmButton = {
                TextButton(onClick = {
                    // Tạo intent và khởi chạy activity
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", context.packageName, null)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(intent)
                    viewModel.resetPermissionDialogState()
                }) {
                    Text("Go to Settings")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.resetPermissionDialogState() }) {
                    Text("Cancel")
                }
            },
            title = { Text("Permission Required") },
            text = { Text("Location permission is required for this feature. Please grant the permission in the app settings.") }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Button(onClick = {
            coroutineScope.launch {
                if (isMeasuring) {
                    viewModel.stopMeasurement()
                } else {
                    viewModel.startMeasurement(deviceId, password)
                }
                isMeasuring = !isMeasuring
            }
        }) {
            Text(if (isMeasuring) "Stop" else "Start")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            coroutineScope.launch {
                records = viewModel.getLast10Records()
            }
        }) {
            Text("View Records")
        }

        if (records.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            records.forEach { record ->
                Text(text = "${record.record_time}: ${record.latitude}, ${record.longitude} - Status: ${record.connect_status}")
            }
        }
    }
}
