package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.repository.SupabaseRepository
import com.example.domain.model.Profile
import com.example.ui.components.GlassCard
import kotlinx.coroutines.launch
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onLogout: () -> Unit
) {
    val repository = remember { SupabaseRepository() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    var profile by remember { mutableStateOf<Profile?>(null) }
    var email by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isEditing by remember { mutableStateOf(false) }

    // Edit states
    var editFullName by remember { mutableStateOf("") }
    var editAge by remember { mutableStateOf("") }
    var editGender by remember { mutableStateOf("") }
    var editMobile by remember { mutableStateOf("") }
    var editAddress by remember { mutableStateOf("") }
    var editEmergencyContact by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var currentAvatarUrl by remember { mutableStateOf<String?>(null) }

    val pickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
        }
    }

    LaunchedEffect(Unit) {
        try {
            profile = repository.getProfile()
            email = repository.getCurrentUserEmail()
            profile?.let { p ->
                editFullName = p.fullName ?: ""
                editAge = p.age?.toString() ?: ""
                editGender = p.gender ?: ""
                editMobile = p.mobile ?: ""
                editAddress = p.address ?: ""
                editEmergencyContact = p.emergencyContact ?: ""
                currentAvatarUrl = p.avatarUrl
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (!isLoading && profile != null) {
                        if (isEditing) {
                            TextButton(onClick = {
                                coroutineScope.launch {
                                    isLoading = true
                                    try {
                                        var uploadedAvatarUrl = currentAvatarUrl
                                        if (imageUri != null) {
                                            val stream = context.contentResolver.openInputStream(imageUri!!)
                                            val bytes = stream?.readBytes()
                                            if (bytes != null) {
                                                val uid = repository.getCurrentUserId()
                                                if (uid != null) {
                                                    val path = "$uid/${UUID.randomUUID()}.jpg"
                                                    uploadedAvatarUrl = repository.uploadImage("profile-images", path, bytes)
                                                }
                                            }
                                        }

                                        val updatedProfile = profile!!.copy(
                                            fullName = editFullName,
                                            age = editAge.toIntOrNull(),
                                            gender = editGender,
                                            mobile = editMobile,
                                            address = editAddress,
                                            emergencyContact = editEmergencyContact,
                                            avatarUrl = uploadedAvatarUrl
                                        )
                                        repository.updateProfile(updatedProfile)
                                        profile = updatedProfile
                                        currentAvatarUrl = uploadedAvatarUrl
                                        isEditing = false
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    } finally {
                                        isLoading = false
                                    }
                                }
                            }) {
                                Text("Save", color = MaterialTheme.colorScheme.primary)
                            }
                        } else {
                            IconButton(onClick = { isEditing = true }) {
                                Icon(Icons.Filled.Edit, contentDescription = "Edit Profile")
                            }
                        }
                    }
                },
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
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
                    .animateContentSize(),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.Start
            ) {
                if (profile != null) {
                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .clickable(enabled = isEditing) { pickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            if (imageUri != null) {
                                AsyncImage(model = imageUri, contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else if (currentAvatarUrl != null) {
                                AsyncImage(model = currentAvatarUrl, contentDescription = "Avatar", modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                            } else {
                                GlassCard(modifier = Modifier.fillMaxSize()) {
                                    Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                        if (isEditing) {
                                            Icon(Icons.Filled.AddCircle, contentDescription = "Add Photo", modifier = Modifier.size(32.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        } else {
                                            Text(profile!!.fullName?.take(1) ?: "?", style = MaterialTheme.typography.titleLarge)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isEditing) {
                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Edit Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                OutlinedTextField(value = editFullName, onValueChange = { editFullName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = editAge, onValueChange = { editAge = it }, label = { Text("Age") }, modifier = Modifier.fillMaxWidth(), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                                OutlinedTextField(value = editGender, onValueChange = { editGender = it }, label = { Text("Gender") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = editMobile, onValueChange = { editMobile = it }, label = { Text("Mobile") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = editAddress, onValueChange = { editAddress = it }, label = { Text("Address") }, modifier = Modifier.fillMaxWidth())
                                OutlinedTextField(value = editEmergencyContact, onValueChange = { editEmergencyContact = it }, label = { Text("Emergency Contact") }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                            Text(profile!!.fullName ?: "No Name", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
                            if (email != null) {
                                Text(email!!, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Personal Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                if (profile!!.age != null) Text("Age: ${profile!!.age}", style = MaterialTheme.typography.bodyMedium)
                                if (profile!!.gender != null) Text("Gender: ${profile!!.gender}", style = MaterialTheme.typography.bodyMedium)
                                if (profile!!.mobile != null) Text("Mobile: ${profile!!.mobile}", style = MaterialTheme.typography.bodyMedium)
                                if (profile!!.address != null) Text("Address: ${profile!!.address}", style = MaterialTheme.typography.bodyMedium)
                                if (profile!!.emergencyContact != null) Text("Emergency Contact: ${profile!!.emergencyContact}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        GlassCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Health Info", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text("Medical Conditions", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                val mc = profile!!.medicalConditions
                                if (mc.isNullOrEmpty()) {
                                    Text("None reported", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Text(mc.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                                }

                                Spacer(modifier = Modifier.height(8.dp))

                                Text("Allergies", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                val al = profile!!.allergies
                                if (al.isNullOrEmpty()) {
                                    Text("None reported", style = MaterialTheme.typography.bodyMedium)
                                } else {
                                    Text(al.joinToString(", "), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            repository.logout()
                            onLogout()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Logout")
                }
            }
        }
    }
}
