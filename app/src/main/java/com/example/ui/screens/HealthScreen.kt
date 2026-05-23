package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.repository.SupabaseRepository
import com.example.domain.model.HealthLog
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthScreen() {
    val repository = remember { SupabaseRepository() }
    val coroutineScope = rememberCoroutineScope()
    var logs by remember { mutableStateOf<List<HealthLog>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var showAddDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("All") }

    LaunchedEffect(Unit) {
        try {
            logs = repository.getHealthLogs().sortedByDescending { it.loggedAt }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    val filteredLogs = if (selectedFilter == "All") logs else logs.filter { 
        when (selectedFilter) {
            "BP" -> it.metric == "blood_pressure"
            "Sugar" -> it.metric == "sugar"
            "Weight" -> it.metric == "weight"
            "Vitals" -> it.metric == "heart_rate" || it.metric == "temperature"
            else -> true
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Health Tracking", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Log")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (logs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
                Text("No health logs recorded yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                    androidx.compose.foundation.lazy.LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        val filters = listOf("All", "BP", "Sugar", "Weight", "Vitals")
                        items(filters) { filterStr ->
                            FilterChip(
                                selected = selectedFilter == filterStr,
                                onClick = { selectedFilter = filterStr },
                                label = { Text(filterStr) }
                            )
                        }
                    }
                }
                
                if (filteredLogs.isEmpty()) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                            Text("No logs for $selectedFilter", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                items(filteredLogs) { log ->
                    HealthLogCard(log, onDelete = {
                        coroutineScope.launch {
                            try {
                                if (log.id != null) {
                                    repository.deleteHealthLog(log.id)
                                    logs = repository.getHealthLogs().sortedByDescending { it.loggedAt }
                                }
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    })
                }
                item { Spacer(modifier = Modifier.height(80.dp)) } // fab clearance
            }
        }
    }

    if (showAddDialog) {
        AddHealthLogDialog(
            onDismiss = { showAddDialog = false },
            onSave = { metric, valueObj ->
                coroutineScope.launch {
                    val uid = repository.getCurrentUserId()
                    if (uid != null) {
                        try {
                            val newLog = HealthLog(
                                userId = uid,
                                metric = metric,
                                value = valueObj
                            )
                            repository.addHealthLog(newLog)
                            logs = repository.getHealthLogs().sortedByDescending { it.loggedAt }
                            showAddDialog = false
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                }
            }
        )
    }
}

@Composable
fun HealthLogCard(log: HealthLog, onDelete: () -> Unit) {
    GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                val metricTitle = log.metric.replace("_", " ").replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                Text(metricTitle, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                
                Spacer(modifier = Modifier.height(4.dp))
                
                val obj = runCatching { log.value.jsonObject }.getOrNull()
                when (log.metric) {
                    "blood_pressure" -> {
                        val sys = obj?.get("systolic")?.jsonPrimitive?.content
                        val dia = obj?.get("diastolic")?.jsonPrimitive?.content
                        Text("$sys / $dia mmHg", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    "sugar" -> {
                        val v = obj?.get("level")?.jsonPrimitive?.content
                        val t = obj?.get("type")?.jsonPrimitive?.content ?: "Fasting"
                        Text("$v mg/dL ($t)", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    "temperature" -> {
                        val v = obj?.get("temperature")?.jsonPrimitive?.content
                        Text("$v °C", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    "heart_rate" -> {
                        val v = obj?.get("rate")?.jsonPrimitive?.content
                        Text("$v bpm", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    "weight" -> {
                        val v = obj?.get("weight")?.jsonPrimitive?.content
                        Text("$v kg", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                    else -> {
                        val v = obj?.get("value")?.jsonPrimitive?.content ?: runCatching { log.value.jsonPrimitive.content }.getOrNull()
                        Text("$v", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                    }
                }

                val notes = obj?.get("notes")?.jsonPrimitive?.content
                if (!notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Notes: $notes", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                val date = log.loggedAt?.take(10) ?: ""
                Text(date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f), modifier = Modifier.padding(top=8.dp))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHealthLogDialog(
    onDismiss: () -> Unit,
    onSave: (String, kotlinx.serialization.json.JsonElement) -> Unit
) {
    var selectedMetric by remember { mutableStateOf("blood_pressure") }
    var notes by remember { mutableStateOf("") }
    
    // BP
    var systolic by remember { mutableStateOf("") }
    var diastolic by remember { mutableStateOf("") }
    
    // Sugar
    var sugarLevel by remember { mutableStateOf("") }
    var sugarType by remember { mutableStateOf("Fasting") } // Fasting, Random, Postprandial
    
    // Vitals
    var weight by remember { mutableStateOf("") }
    var heartRate by remember { mutableStateOf("") }
    var temperature by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Health Log") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val metrics = listOf("blood_pressure" to "BP", "sugar" to "Sugar", "vitals" to "Vitals")
                    metrics.forEachIndexed { index, pair ->
                        val isVitalsGroup = selectedMetric in listOf("weight", "heart_rate", "temperature")
                        val isSelected = if (pair.first == "vitals") isVitalsGroup else selectedMetric == pair.first
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = metrics.size),
                            onClick = { 
                                if (pair.first == "vitals") selectedMetric = "weight" 
                                else selectedMetric = pair.first 
                            },
                            selected = isSelected
                        ) {
                            Text(pair.second, maxLines=1)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))

                if (selectedMetric in listOf("weight", "heart_rate", "temperature")) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        FilterChip(selected = selectedMetric == "weight", onClick = { selectedMetric = "weight" }, label = { Text("Weight") })
                        FilterChip(selected = selectedMetric == "heart_rate", onClick = { selectedMetric = "heart_rate" }, label = { Text("Heart") })
                        FilterChip(selected = selectedMetric == "temperature", onClick = { selectedMetric = "temperature" }, label = { Text("Temp") })
                    }
                }

                when (selectedMetric) {
                    "blood_pressure" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = systolic, onValueChange = { systolic = it },
                                label = { Text("Systolic") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                            OutlinedTextField(
                                value = diastolic, onValueChange = { diastolic = it },
                                label = { Text("Diastolic") }, modifier = Modifier.weight(1f),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                            )
                        }
                    }
                    "sugar" -> {
                        OutlinedTextField(
                            value = sugarLevel, onValueChange = { sugarLevel = it },
                            label = { Text("Sugar Level (mg/dL)") }, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        OutlinedTextField(
                            value = sugarType, onValueChange = { sugarType = it },
                            label = { Text("Type (e.g. Fasting)") }, modifier = Modifier.fillMaxWidth()
                        )
                    }
                    "weight" -> {
                        OutlinedTextField(
                            value = weight, onValueChange = { weight = it },
                            label = { Text("Weight (kg)") }, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    "heart_rate" -> {
                        OutlinedTextField(
                            value = heartRate, onValueChange = { heartRate = it },
                            label = { Text("Heart Rate (bpm)") }, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                    "temperature" -> {
                        OutlinedTextField(
                            value = temperature, onValueChange = { temperature = it },
                            label = { Text("Temperature (°C)") }, modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }
                }

                OutlinedTextField(
                    value = notes, onValueChange = { notes = it },
                    label = { Text("Notes/Symptoms") }, modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                val valueObj = buildJsonObject {
                    if (notes.isNotBlank()) put("notes", notes)
                    when (selectedMetric) {
                        "blood_pressure" -> {
                            put("systolic", systolic)
                            put("diastolic", diastolic)
                        }
                        "sugar" -> {
                            put("level", sugarLevel)
                            put("type", sugarType)
                        }
                        "weight" -> put("weight", weight)
                        "heart_rate" -> put("rate", heartRate)
                        "temperature" -> put("temperature", temperature)
                    }
                }
                onSave(selectedMetric, valueObj)
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
