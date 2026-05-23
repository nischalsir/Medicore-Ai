package com.example.data.repository

import com.example.data.remote.SupabaseNetwork
import com.example.domain.model.AdminAnnouncement
import com.example.domain.model.HealthLog
import com.example.domain.model.Medicine
import com.example.domain.model.MedicineLog
import com.example.domain.model.Profile
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class SupabaseRepository {
    private val client by lazy { SupabaseNetwork.client }

    // -- Auth --
    suspend fun login(email: String, pword: String) {
        client.auth.signInWith(Email) {
            this.email = email
            password = pword
        }
    }

    suspend fun register(email: String, pword: String, fullName: String) {
        client.auth.signUpWith(Email) {
            this.email = email
            password = pword
            data = buildJsonObject {
                put("full_name", fullName)
            }
        }
    }

    suspend fun logout() {
        client.auth.signOut()
    }

    fun getCurrentUserId(): String? = client.auth.currentUserOrNull()?.id
    
    fun getCurrentUserEmail(): String? = client.auth.currentUserOrNull()?.email

    // -- Profile --
    suspend fun getProfile(): Profile? {
        val uid = getCurrentUserId() ?: return null
        return client.from("profiles").select { filter { eq("id", uid) } }.decodeSingleOrNull<Profile>()
    }

    suspend fun updateProfile(profile: Profile) {
        client.from("profiles").update(profile) { filter { eq("id", profile.id) } }
    }

    // -- Medicines --
    suspend fun getMedicines(): List<Medicine> {
        val uid = getCurrentUserId() ?: return emptyList()
        return client.from("medicines").select { filter { eq("user_id", uid) } }.decodeList<Medicine>()
    }

    suspend fun addMedicine(medicine: Medicine): Medicine {
        return client.from("medicines").insert(medicine) { select() }.decodeSingle<Medicine>()
    }
    
    suspend fun updateMedicine(medicine: Medicine) {
        client.from("medicines").update(medicine) { filter { eq("id", medicine.id!!) } }
    }

    suspend fun deleteMedicine(id: String) {
        client.from("medicines").delete { filter { eq("id", id) } }
    }

    // -- Health Logs --
    suspend fun getHealthLogs(): List<HealthLog> {
        val uid = getCurrentUserId() ?: return emptyList()
        return client.from("health_logs").select { filter { eq("user_id", uid) } }.decodeList<HealthLog>()
    }
    
    suspend fun addHealthLog(log: HealthLog) {
        client.from("health_logs").insert(log)
    }

    suspend fun deleteHealthLog(id: String) {
        client.from("health_logs").delete { filter { eq("id", id) } }
    }

    // -- Admin --
    suspend fun getAnnouncements(): List<AdminAnnouncement> {
        return client.from("admin_announcements").select().decodeList<AdminAnnouncement>()
    }
    
    suspend fun addAnnouncement(announcement: AdminAnnouncement) {
        client.from("admin_announcements").insert(announcement)
    }

    // -- Storage Upload --
    suspend fun uploadImage(bucketName: String, path: String, data: ByteArray): String {
        client.storage.from(bucketName).upload(path, data) {
            upsert = true
        }
        return getImageUrl(bucketName, path)
    }
    
    fun getImageUrl(bucketName: String, path: String): String {
        return client.storage.from(bucketName).publicUrl(path)
    }
}
