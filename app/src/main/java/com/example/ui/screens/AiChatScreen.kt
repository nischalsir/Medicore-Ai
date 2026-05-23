package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.GlassCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import com.example.BuildConfig
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiChatScreen(
    onNavigateBack: (() -> Unit)? = null
) {
    var query by remember { mutableStateOf("") }
    var historyMessages = remember { mutableStateListOf<Pair<String, String>>() }
    var isDrawerOpen by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("MediTrack AI", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { isDrawerOpen = !isDrawerOpen }) {
                        Icon(Icons.Default.Menu, contentDescription = "History")
                    }
                },
                actions = {
                    IconButton(onClick = { historyMessages.clear() }) {
                        Icon(Icons.Default.Add, contentDescription = "New Chat")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Row(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Placeholder drawer for multiple history options
            if (isDrawerOpen) {
                Surface(
                    modifier = Modifier.width(200.dp).fillMaxHeight(),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Chat History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Current Session", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Yesterday Session", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Past Medical Advice", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp)
            ) {
                if (historyMessages.isEmpty()) {
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Text("How can I help you with your health today?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        reverseLayout = false
                    ) {
                        items(historyMessages) { msg ->
                            val isUser = msg.first == "user"
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                            ) {
                                GlassCard(modifier = Modifier.fillMaxWidth(if (isUser) 0.8f else 0.9f)) {
                                    Text(
                                        text = msg.second,
                                        modifier = Modifier.padding(12.dp),
                                        color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                        if (isLoading) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.Start
                                ) {
                                    GlassCard(modifier = Modifier.wrapContentWidth()) {
                                        CircularProgressIndicator(modifier = Modifier.padding(12.dp).size(24.dp), color = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp, top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Ask about symptoms, medicines...") },
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = !isLoading
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    FloatingActionButton(
                        onClick = {
                            if (query.isNotBlank() && !isLoading) {
                                val currentQuery = query
                                historyMessages.add("user" to currentQuery)
                                query = ""
                                isLoading = true
                                
                                coroutineScope.launch {
                                    val aiResponse = generateNvidiaNimResponse(currentQuery, historyMessages.dropLast(1))
                                    historyMessages.add("ai" to aiResponse)
                                    isLoading = false
                                }
                            }
                        },
                        containerColor = MaterialTheme.colorScheme.primary
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

suspend fun generateNvidiaNimResponse(prompt: String, previousMessages: List<Pair<String, String>>): String {
    return withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.NVIDIA_NIM_API_KEY
        if (apiKey.isEmpty() || apiKey.startsWith("YOUR_")) {
            return@withContext "API Key not configured. Please add your NVIDIA_NIM_API_KEY in the Secrets panel in AI Studio."
        }

        try {
            val endpoint = URL("https://integrate.api.nvidia.com/v1/chat/completions")
            val connection = endpoint.openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Authorization", "Bearer $apiKey")
            connection.doOutput = true

            val jsonBody = buildJsonObject {
                put("model", "google/gemma-3n-e2b-it")
                put("max_tokens", 512)
                put("temperature", 0.20)
                put("top_p", 0.70)
                put("frequency_penalty", 0.00)
                put("presence_penalty", 0.00)
                put("stream", false)
                
                put("messages", buildJsonArray {
                    var isFirstUser = true
                    for (msg in previousMessages) {
                        if (msg.first == "user") {
                            add(buildJsonObject {
                                put("role", "user")
                                val text = if (isFirstUser) {
                                    isFirstUser = false
                                    "You are an AI Medical Assistant integrated into MediTrack. Provide helpful, safe, and concise advice. Always remind the user to consult a doctor for serious issues.\n\n${msg.second}"
                                } else {
                                    msg.second
                                }
                                put("content", text)
                            })
                        } else {
                            add(buildJsonObject {
                                put("role", "assistant")
                                put("content", msg.second)
                            })
                        }
                    }
                    
                    add(buildJsonObject {
                        put("role", "user")
                        val text = if (isFirstUser) {
                            "You are an AI Medical Assistant integrated into MediTrack. Provide helpful, safe, and concise advice. Always remind the user to consult a doctor for serious issues.\n\n$prompt"
                        } else {
                            prompt
                        }
                        put("content", text)
                    })
                })
            }

            OutputStreamWriter(connection.outputStream).use { writer ->
                writer.write(jsonBody.toString())
            }

            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val responseString = connection.inputStream.bufferedReader().use { it.readText() }
                val jsonResponse = Json.parseToJsonElement(responseString).jsonObject
                val choices = jsonResponse["choices"]?.jsonArray
                return@withContext choices?.get(0)?.jsonObject?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.content ?: "Response format error"
            } else {
                val errorResponse = connection.errorStream?.bufferedReader()?.use { it.readText() } ?: "Unknown error"
                return@withContext "Error ($responseCode): $errorResponse"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext "Network Error: ${e.message}"
        }
    }
}
