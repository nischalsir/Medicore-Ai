package com.example.workers

import android.content.Context
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.example.utils.NotificationHelper

class MedicineWorker(context: Context, workerParams: WorkerParameters) :
    Worker(context, workerParams) {

    override fun doWork(): Result {
        val medName = inputData.getString("medicine_name") ?: "Medicine"
        val dosage = inputData.getString("dosage") ?: ""
        
        NotificationHelper.createNotificationChannel(applicationContext)
        NotificationHelper.showNotification(
            applicationContext,
            "Medicine Reminder",
            "It's time to take your $medName ($dosage)."
        )

        return Result.success()
    }
}
