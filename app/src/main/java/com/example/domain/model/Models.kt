package com.example.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Profile(
    @SerialName("id") val id: String,
    @SerialName("full_name") val fullName: String? = null,
    @SerialName("age") val age: Int? = null,
    @SerialName("gender") val gender: String? = null,
    @SerialName("mobile") val mobile: String? = null,
    @SerialName("address") val address: String? = null,
    @SerialName("emergency_contact") val emergencyContact: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("medical_conditions") val medicalConditions: List<String>? = emptyList(),
    @SerialName("allergies") val allergies: List<String>? = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class UserRole(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("role") val role: String // 'admin', 'user'
)

@Serializable
data class Medicine(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("name") val name: String,
    @SerialName("type") val type: String = "tablet", // tablet, syrup, injection, capsule
    @SerialName("dosage") val dosage: String? = null,
    @SerialName("image_url") val imageUrl: String? = null,
    @SerialName("start_date") val startDate: String, // String
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("frequency") val frequency: String? = null,
    @SerialName("reminder_times") val reminderTimes: List<String>? = emptyList(),
    @SerialName("notes") val notes: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

@Serializable
data class MedicineLog(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("medicine_id") val medicineId: String,
    @SerialName("scheduled_at") val scheduledAt: String,
    @SerialName("taken_at") val takenAt: String? = null,
    @SerialName("status") val status: String = "missed", // taken, missed, skipped
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class HealthLog(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("metric") val metric: String, // blood_pressure, sugar, water, weight, heart_rate
    @SerialName("value") val value: JsonElement, // using JsonElement as it's JSONB
    @SerialName("logged_at") val loggedAt: String? = null
)

@Serializable
data class Notification(
    @SerialName("id") val id: String? = null,
    @SerialName("user_id") val userId: String,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String? = null,
    @SerialName("read_at") val readAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

@Serializable
data class AdminAnnouncement(
    @SerialName("id") val id: String? = null,
    @SerialName("title") val title: String,
    @SerialName("body") val body: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

