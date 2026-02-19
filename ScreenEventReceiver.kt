package com.security.phantomeye

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import com.security.phantomeye.util.FileLogger

class ScreenEventReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        
        // Log the incoming system event
        FileLogger.d("SER", "Event received: $action")

        when (action) {
            Intent.ACTION_SCREEN_ON -> {
                FileLogger.d("SER", "SCREEN_ON detected")
                // Note: ScreenCaptureService already monitors this internally 
                // if it's currently running.
            }

            "android.intent.action.SIM_STATE_CHANGED" -> {
                val state = intent.getStringExtra("ss")
                FileLogger.d("SER", "SIM State Change: $state")

                // "ABSENT" is the system code for a removed SIM card
                if (state == "ABSENT") {
                    FileLogger.d("SER", "CRITICAL: SIM REMOVED! Forcing Capture Service...")
                    
                    val serviceIntent = Intent(context, ScreenCaptureService::class.java).apply {
                        putExtra("TRIGGER_REASON", "SIM_REMOVAL")
                    }

                    // Start the service to capture the thief's face immediately
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
    }
}
