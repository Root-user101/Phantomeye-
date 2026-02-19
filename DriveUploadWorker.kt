package com.security.phantomeye.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.api.client.http.FileContent
import com.google.api.services.drive.Drive
import com.security.phantomeye.util.FileLogger
import java.io.File

class DriveUploadWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val filePath = inputData.getString("file_path") ?: return Result.failure()
        val file = File(filePath)

        if (!file.exists()) return Result.failure()

        return try {
            // We will build this GoogleDriveService in the next step
            val driveService: Drive = GoogleDriveService.getService(applicationContext)

            val fileMetadata = com.google.api.services.drive.model.File().apply {
                name = file.name
                // 'appDataFolder' is a private folder only your app can see
                parents = listOf("appDataFolder") 
            }

            val mediaContent = FileContent("image/jpeg", file)
            
            driveService.files().create(fileMetadata, mediaContent)
                .setFields("id")
                .execute()

            FileLogger.d("Worker", "Successfully uploaded: ${file.name}")
            Result.success()
        } catch (e: Exception) {
            FileLogger.d("Worker", "Upload failed, retrying later: ${e.message}")
            // Retries automatically when internet returns
            Result.retry() 
        }
    }
}
