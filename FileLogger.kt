package com.security.phantomeye

import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {

    private const val LOG_TAG = "WTMP_LOG"
    private const val LOG_FILE_NAME = "debug_log.txt"

    fun d(tag: String, message: String) {
        val time = getTimeStamp()
        val logMessage = "$time [$tag] $message"

        // Logcat (visible in IDE logs)
        Log.d(LOG_TAG, logMessage)

        // File log
        writeToFile(logMessage)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val time = getTimeStamp()
        val logMessage = "$time [ERROR][$tag] $message"

        Log.e(LOG_TAG, logMessage, throwable)
        writeToFile(logMessage + (throwable?.message ?: ""))
    }

    private fun writeToFile(text: String) {
        try {
            val dir = File(
                AppContextProvider.context.getExternalFilesDir(null),
                "PhantomEye/Logs"
            )

            if (!dir.exists()) {
                dir.mkdirs()
            }

            val logFile = File(dir, LOG_FILE_NAME)
            val writer = FileWriter(logFile, true)
            writer.append(text).append("\n")
            writer.flush()
            writer.close()

        } catch (e: Exception) {
            Log.e(LOG_TAG, "Failed to write log file", e)
        }
    }

    private fun getTimeStamp(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }
}