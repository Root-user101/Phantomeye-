package com.security.phantomeye.security

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class AdminReceiver : DeviceAdminReceiver() {

    override fun onPasswordFailed(context: Context, intent: Intent) {
        super.onPasswordFailed(context, intent)

        try {
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
            val time = sdf.format(Date())

            val file = File(context.filesDir, "attempt_log.txt")
            file.appendText("Wrong PIN at: $time\n")
        } catch (e: Exception) {
            // ignore
        }
    }
}