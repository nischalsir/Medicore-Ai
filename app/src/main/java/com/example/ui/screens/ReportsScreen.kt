package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SupabaseRepository
import com.example.domain.model.HealthLog
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportsScreen() {
    val repository = remember { SupabaseRepository() }
    var logs by remember { mutableStateOf<List<HealthLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        try {
            logs = repository.getHealthLogs().sortedBy { it.loggedAt ?: "" }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val weightLogs = logs.filter { it.metric == "weight" }.mapNotNull { log ->
        val v = log.value.jsonObject["weight"]?.jsonPrimitive?.content?.toFloatOrNull()
        if (v != null) Pair(log.loggedAt?.take(10) ?: "", v) else null
    }

    val systolicLogs = logs.filter { it.metric == "blood_pressure" }.mapNotNull { log ->
        val v = log.value.jsonObject["systolic"]?.jsonPrimitive?.content?.toFloatOrNull()
        if (v != null) Pair(log.loggedAt?.take(10) ?: "", v) else null
    }
    
    val diastolicLogs = logs.filter { it.metric == "blood_pressure" }.mapNotNull { log ->
        val v = log.value.jsonObject["diastolic"]?.jsonPrimitive?.content?.toFloatOrNull()
        if (v != null) Pair(log.loggedAt?.take(10) ?: "", v) else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Reports & Analytics", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }
                
                item {
                    ReportChartCard(
                        title = "Weight Trend (kg)",
                        dataPoints = weightLogs,
                        color = Color(0xFF38BDF8)
                    )
                }

                item {
                    ReportChartCardMulti(
                        title = "Blood Pressure (mmHg)",
                        dataPoints1 = systolicLogs,
                        dataPoints2 = diastolicLogs,
                        color1 = Color(0xFFEF4444),
                        color2 = Color(0xFF10B981)
                    )
                }

                item {
                    val sysAvg = systolicLogs.map { it.second }.average()
                    val diaAvg = diastolicLogs.map { it.second }.average()
                    val bpColor = if (sysAvg > 130 || diaAvg > 85) Color(0xFFF59E0B) else Color(0xFF10B981)
                    val bpStatus = if (sysAvg > 130 || diaAvg > 85) "Elevated" else "Normal"
                    
                    SummaryCard(
                        title = "Avg Blood Pressure",
                        value = "${if(sysAvg.isNaN()) "--" else sysAvg.toInt()} / ${if(diaAvg.isNaN()) "--" else diaAvg.toInt()}",
                        status = bpStatus,
                        statusColor = bpColor
                    )
                }

                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
fun SummaryCard(title: String, value: String, status: String, statusColor: Color) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(value, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    contentColor = statusColor
                ) {
                    Text(status, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ReportChartCard(title: String, dataPoints: List<Pair<String, Float>>, color: Color) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dataPoints.size < 2) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Not enough data to show trend", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    val maxVal = dataPoints.maxOf { it.second } * 1.1f
                    val minVal = dataPoints.minOf { it.second } * 0.9f
                    val range = maxVal - minVal
                    
                    val stepX = size.width / (dataPoints.size - 1)
                    
                    val path = Path()
                    dataPoints.forEachIndexed { index, pair ->
                        val x = index * stepX
                        val y = size.height - ((pair.second - minVal) / range * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        
                        drawCircle(
                            color = color,
                            radius = 6f,
                            center = Offset(x, y)
                        )
                    }
                    
                    drawPath(
                        path = path,
                        color = color,
                        style = Stroke(width = 3f)
                    )
                }
            }
        }
    }
}

@Composable
fun ReportChartCardMulti(title: String, dataPoints1: List<Pair<String, Float>>, dataPoints2: List<Pair<String, Float>>, color1: Color, color2: Color) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            Spacer(modifier = Modifier.height(16.dp))
            
            if (dataPoints1.size < 2 || dataPoints2.size < 2) {
                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                    Text("Not enough data to show trend", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                Canvas(modifier = Modifier.fillMaxWidth().height(150.dp)) {
                    val maxVal = maxOf(dataPoints1.maxOf { it.second }, dataPoints2.maxOf { it.second }) * 1.1f
                    val minVal = minOf(dataPoints1.minOf { it.second }, dataPoints2.minOf { it.second }) * 0.9f
                    val range = maxVal - minVal
                    
                    val stepX = size.width / (dataPoints1.size - 1)
                    
                    val path1 = Path()
                    dataPoints1.forEachIndexed { index, pair ->
                        val x = index * stepX
                        val y = size.height - ((pair.second - minVal) / range * size.height)
                        if (index == 0) path1.moveTo(x, y) else path1.lineTo(x, y)
                        drawCircle(color = color1, radius = 6f, center = Offset(x, y))
                    }
                    drawPath(path = path1, color = color1, style = Stroke(width = 3f))

                    val path2 = Path()
                    dataPoints2.forEachIndexed { index, pair ->
                        val x = index * stepX
                        val y = size.height - ((pair.second - minVal) / range * size.height)
                        if (index == 0) path2.moveTo(x, y) else path2.lineTo(x, y)
                        drawCircle(color = color2, radius = 6f, center = Offset(x, y))
                    }
                    drawPath(path = path2, color = color2, style = Stroke(width = 3f))
                }
            }
        }
    }
}
