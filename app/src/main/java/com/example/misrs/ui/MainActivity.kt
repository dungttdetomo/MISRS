package com.example.misrs.ui
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.misrs.data.entities.SystemConfig
import com.example.misrs.data.repository.StatusRepository
import com.example.misrs.data.repository.SystemConfigRepository
import com.example.misrs.viewmodel.MainViewModel
import com.example.misrs.viewmodel.SettingsViewModel
import kotlinx.coroutines.flow.collectLatest



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val settingsViewModel = SettingsViewModel(SystemConfigRepository(this))
        val mainViewModel = MainViewModel(application, StatusRepository(this), SystemConfigRepository(this))

        setContent {
            val navController = rememberNavController()

            // Use remember to store the retrieved config state
            val systemConfigState = remember { mutableStateOf<SystemConfig?>(null) }
            val startDestination = remember { mutableStateOf("settings") }


            // Retrieve the system config in a composable context
            LaunchedEffect(Unit) {
                mainViewModel.getCurrentSystemConfig().collectLatest { config ->
                    systemConfigState.value = config

                    // If deviceId and password exist in the config, start the measurement service
                    if (config != null && config.device_id.isNotEmpty() && config.password.isNotEmpty()) {
                        startDestination.value = "main/${config.device_id}/${config.password}"
                        Log.d("MainActivity", "Starting MeasurementService with device_id: ${config.device_id}, password: ${config.password}")
                        mainViewModel.startMeasurement(config.device_id, config.password)
                    }
                }
            }

            // Wait until systemConfigState has been updated before starting NavHost

            NavHost(navController = navController, startDestination = startDestination.value) {
                composable("settings") {
                    SettingsScreen(viewModel = settingsViewModel) { deviceId, password ->
                        mainViewModel.startMeasurement(deviceId, password)
                        navController.navigate("main/$deviceId/$password") {
                            popUpTo("settings") { inclusive = true }
                        }
                    }
                }
                composable(
                    route = "main/{deviceId}/{password}",
                    arguments = listOf(
                        navArgument("deviceId") { type = NavType.StringType },
                        navArgument("password") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val deviceId = backStackEntry.arguments?.getString("deviceId") ?: ""
                    val password = backStackEntry.arguments?.getString("password") ?: ""
                    MainScreen(viewModel = mainViewModel, navController = navController, deviceId = deviceId, password = password)
                }
                composable("view") {
                    ViewScreen(viewModel = mainViewModel, navController = navController)
                }
            }

        }
    }
}