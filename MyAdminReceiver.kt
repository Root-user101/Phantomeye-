package com.security.phantomeye

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MyAdminReceiver : DeviceAdminReceiver() {

    private fun saveDebugLog(context: Context, message: String) {
        try {
            val logsDir = File(context.getExternalFilesDir(null), "PhantomEye/Logs")
            logsDir.mkdirs()
            val debugFile = File(logsDir, "debug_log.txt")
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            debugFile.appendText("$timestamp → $message\n")
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun onPasswordFailed(context: Context, intent: Intent) {
        saveDebugLog(context, "MyAdminReceiver: Password FAILED")
        CaptureStarter.startCapture(context)
    }

    override fun onPasswordSucceeded(context: Context, intent: Intent) {
        saveDebugLog(context, "MyAdminReceiver: Password SUCCESS")
        CaptureStarter.startCapture(context)
    }
}