package com.security.phantomeye

import android.content.Context
import android.content.Intent
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object CaptureStarter {

    fun startCapture(context: Context, trigger: String = "UNKNOWN") {

        // Log debug
        try {
            val logsDir = File(context.getExternalFilesDir(null), "/Logs")
            logsDir.mkdirs()
            val debugFile = File(logsDir, "debug_log.txt")
            val timestamp =
                SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            debugFile.appendText("$timestamp → CaptureStarter called, trigger=$trigger\n")
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Send capture command to running service
        val intent = Intent(context, ScreenCaptureService::class.java)
        intent.putExtra("capture", true)
        intent.putExtra("trigger", trigger)

        context.startService(intent)
    }
}