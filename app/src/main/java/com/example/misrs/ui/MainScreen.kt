package com.example.misrs.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.misrs.viewmodel.MainViewModel
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.runtime.livedata.observeAsState
import androidx.navigation.NavController

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController,
    deviceId: String,
    password: String
) {
    val isMeasuring by viewModel.isMeasuring.collectAsState()
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
            if (isMeasuring) {
                viewModel.stopMeasurement()
            } else {
                viewModel.startMeasurement(deviceId, password)
            }
        }) {
            Text(if (isMeasuring) "Stop" else "Start")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Điều hướng đến màn hình Setting
            navController.navigate("settings")
        }) {
            Text("Setting")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            // Điều hướng đến màn hình View
            navController.navigate("view")
        }) {
            Text("View")
        }
    }
}
