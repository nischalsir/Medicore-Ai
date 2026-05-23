package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import com.example.data.repository.SupabaseRepository

@Composable
fun SplashScreen(
    onNavigateToAuth: () -> Unit,
    onNavigateToDashboard: () -> Unit
) {
    val repository = remember { SupabaseRepository() }

    LaunchedEffect(Unit) {
        try {
            repository.awaitInitialization()
            // Optional minimum delay for better UX
            delay(800)
            if (repository.getCurrentUserId() != null) {
                onNavigateToDashboard()
            } else {
                onNavigateToAuth()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            onNavigateToAuth()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "MEDITRACK",
                style = MaterialTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
