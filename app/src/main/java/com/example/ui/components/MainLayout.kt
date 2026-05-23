package com.example.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.PrimaryAccent
import com.example.ui.navigation.DashboardRoute
import com.example.ui.navigation.MedicinesRoute
import com.example.ui.navigation.HealthRoute
import com.example.ui.navigation.ReportsRoute
import com.example.ui.navigation.ProfileRoute
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination

@Composable
fun MainLayout(
    navController: NavHostController,
    content: @Composable (Modifier) -> Unit
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    
    val currentRoute = currentDestination?.route
    
    val isBottomBarVisible = currentRoute?.contains("DashboardRoute") == true ||
            currentRoute?.contains("MedicinesRoute") == true ||
            currentRoute?.contains("HealthRoute") == true ||
            currentRoute?.contains("ReportsRoute") == true ||
            currentRoute?.contains("ProfileRoute") == true

    Scaffold(
        bottomBar = {
            if (isBottomBarVisible) {
                NavigationBar(
                    containerColor = SurfaceCard,
                    contentColor = Color.White,
                    modifier = Modifier.padding(16.dp).clip(RoundedCornerShape(32.dp))
                ) {
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Home, contentDescription = "Home") },
                        label = { Text("Home") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("DashboardRoute") == true } == true,
                        onClick = {
                            navController.navigate(DashboardRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryAccent, selectedIconColor = Color.White)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.List, contentDescription = "Meds") },
                        label = { Text("Meds") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("MedicinesRoute") == true } == true,
                        onClick = {
                            navController.navigate(MedicinesRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryAccent, selectedIconColor = Color.White)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Favorite, contentDescription = "Health") },
                        label = { Text("Health") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("HealthRoute") == true } == true,
                        onClick = {
                            navController.navigate(HealthRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryAccent, selectedIconColor = Color.White)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Star, contentDescription = "Reports") },
                        label = { Text("Reports") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("ReportsRoute") == true } == true,
                        onClick = {
                            navController.navigate(ReportsRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryAccent, selectedIconColor = Color.White)
                    )
                    NavigationBarItem(
                        icon = { Icon(Icons.Filled.Person, contentDescription = "Profile") },
                        label = { Text("Profile") },
                        selected = currentDestination?.hierarchy?.any { it.route?.contains("ProfileRoute") == true } == true,
                        onClick = {
                            navController.navigate(ProfileRoute) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        colors = NavigationBarItemDefaults.colors(indicatorColor = PrimaryAccent, selectedIconColor = Color.White)
                    )
                }
            }
        }
    ) { paddingValues ->
        content(Modifier.padding(paddingValues))
    }
}
