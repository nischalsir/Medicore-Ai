package com.example.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.MedicinesScreen
import com.example.ui.screens.AddMedicineScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.screens.HealthScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.components.MainLayout
import kotlinx.serialization.Serializable

@Serializable
object AuthRoute

@Serializable
object DashboardRoute

@Serializable
object MedicinesRoute

@Serializable
object AddMedicineRoute

@Serializable
object ProfileRoute

@Serializable
object HealthRoute

@Serializable
object ReportsRoute

@Composable
fun AppNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    MainLayout(navController = navController) { innerModifier ->
        NavHost(
            navController = navController,
            startDestination = AuthRoute,
            modifier = modifier.then(innerModifier)
        ) {
            composable<AuthRoute> {
                AuthScreen(
                    onNavigateToDashboard = {
                        navController.navigate(DashboardRoute) {
                            popUpTo(AuthRoute) { inclusive = true }
                        }
                    }
                )
            }
            composable<DashboardRoute> {
                DashboardScreen(
                    onNavigateToMedicines = { navController.navigate(MedicinesRoute) },
                    onNavigateToProfile = { navController.navigate(ProfileRoute) },
                    onLogout = { navController.navigate(AuthRoute) { popUpTo(0) } }
                )
            }
            composable<MedicinesRoute> {
                MedicinesScreen(
                    onNavigateToAdd = { navController.navigate(AddMedicineRoute) },
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable<AddMedicineRoute> {
                AddMedicineScreen(
                    onNavigateBack = { navController.navigateUp() }
                )
            }
            composable<HealthRoute> {
                HealthScreen()
            }
            composable<ReportsRoute> {
                ReportsScreen()
            }
            composable<ProfileRoute> {
                ProfileScreen(
                    onNavigateBack = { navController.navigateUp() },
                    onLogout = {
                        navController.navigate(AuthRoute) {
                            popUpTo(0)
                        }
                    }
                )
            }
        }
    }
}
