package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.navigation.Routes
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.CleanerViewModel
import com.example.ui.viewmodel.SettingsViewModel

class MainActivity : ComponentActivity() {

    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModel.provideFactory()
    }

    private val cleanerViewModel: CleanerViewModel by viewModels {
        CleanerViewModel.provideFactory()
    }

    private val settingsViewModel: SettingsViewModel by viewModels {
        SettingsViewModel.provideFactory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Fully edge-to-edge layout styling
        enableEdgeToEdge()
        
        setContent {
            MyApplicationTheme {
                val navController = rememberNavController()
                val userSession by authViewModel.userSession.collectAsState()

                // Decide start destination based on existing user session in DataStore
                val startDestination = if (userSession?.userId != null) {
                    Routes.HOME
                } else {
                    Routes.SIGN_IN
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = startDestination,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable(Routes.SIGN_IN) {
                            SignInScreen(
                                authViewModel = authViewModel,
                                onSignInSuccess = {
                                    navController.navigate(Routes.HOME) {
                                        popUpTo(Routes.SIGN_IN) { inclusive = true }
                                    }
                                }
                            )
                        }

                        composable(Routes.HOME) {
                            HomeScreen(
                                cleanerViewModel = cleanerViewModel,
                                onNavigateToDuplicates = {
                                    navController.navigate(Routes.DUPLICATES)
                                },
                                onNavigateToSettings = {
                                    navController.navigate(Routes.SETTINGS)
                                },
                                onNavigateToPaywall = {
                                    navController.navigate(Routes.PAYWALL)
                                },
                                onLogout = {
                                    authViewModel.logout()
                                    navController.navigate(Routes.SIGN_IN) {
                                        popUpTo(Routes.HOME) { inclusive = true }
                                    }
                                },
                                onNavigateToJunk = { category ->
                                    navController.navigate(Routes.buildJunkRoute(category))
                                },
                                onNavigateToPermission = {
                                    navController.navigate(Routes.PERMISSION)
                                }
                            )
                        }

                        composable(Routes.PERMISSION) {
                            PermissionScreen(
                                onPermissionsGranted = {
                                    navController.popBackStack()
                                    cleanerViewModel.startScan()
                                },
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Routes.DUPLICATES) {
                            DuplicatesScreen(
                                cleanerViewModel = cleanerViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(Routes.SETTINGS) {
                            SettingsScreen(
                                settingsViewModel = settingsViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onNavigateToPaywall = {
                                    navController.navigate(Routes.PAYWALL)
                                }
                            )
                        }

                        composable(Routes.PAYWALL) {
                            PaywallScreen(
                                authViewModel = authViewModel,
                                onDismiss = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        composable(
                            route = Routes.JUNK_CLEANER,
                            arguments = listOf(androidx.navigation.navArgument("category") {
                                type = androidx.navigation.NavType.StringType
                            })
                        ) { backStackEntry ->
                            val category = backStackEntry.arguments?.getString("category") ?: "WhatsApp"
                            JunkCleanerScreen(
                                initialCategory = category,
                                cleanerViewModel = cleanerViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
