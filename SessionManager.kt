package com.security.phantomeye

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log

object SessionManager {

    private const val TAG = "WTMPSession"
    private var sessionActive = false

    // Delay timeline for photos (in ms)
    private val photoDelays = listOf(0L, 1200L, 2000L, 3000L)

    /** Start a session when screen is turned on and keyguard is locked */
    fun startSession(context: Context) {
        if (sessionActive) {
            Log.d(TAG, "Session already active, skipping")
            return
        }

        sessionActive = true
        Log.d(TAG, "Session started")

        // Post photo captures via Intents
        photoDelays.forEach { delay ->
            Handler(Looper.getMainLooper()).postDelayed({
                requestPhotoCapture(context)
            }, delay)
        }

        // End session slightly after last photo
        val totalTime = photoDelays.lastOrNull() ?: 0L + 500L
        Handler(Looper.getMainLooper()).postDelayed({
            sessionActive = false
            Log.d(TAG, "Session ended")
        }, totalTime)
    }

    /** Ask service to capture a single photo via Intent */
    private fun requestPhotoCapture(context: Context) {
        try {
            val intent = Intent(context, ScreenCaptureService::class.java)
            intent.putExtra("capture", true)
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
            Log.d(TAG, "Photo capture requested via Intent")
        } catch (e: Exception) {
            e.printStackTrace()
            Log.d(TAG, "Failed to request capture: ${e.message}")
        }
    }
}