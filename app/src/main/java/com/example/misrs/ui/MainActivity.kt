package com.example.misrs.ui
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.misrs.data.repository.StatusRepository
import com.example.misrs.data.repository.SystemConfigRepository
import com.example.misrs.viewmodel.MainViewModel
import com.example.misrs.viewmodel.SettingsViewModel


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize view models with application context
        val settingsViewModel = SettingsViewModel(SystemConfigRepository(this))
        val mainViewModel = MainViewModel(application, StatusRepository(this), SystemConfigRepository(this))

        setContent {
            val navController = rememberNavController()

            NavHost(navController = navController, startDestination = "settings") {
                composable("settings") {
                    SettingsScreen(viewModel = settingsViewModel) { deviceId, password ->
                        // Start measurement service here
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
                    MainScreen(viewModel = mainViewModel,navController = navController, deviceId = deviceId, password = password)
                }
                composable("view") {
                    ViewScreen(viewModel = mainViewModel, navController = navController)
                }
            }
        }
    }
}
