package com.security.phantomeye.access

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import com.security.phantomeye.ScreenCaptureService

class UnlockAccessibilityService : AccessibilityService() {

    private val TAG = "WTMPAccess"

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility Service connected")

        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED or
                         AccessibilityEvent.TYPE_VIEW_CLICKED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 50
            packageNames = null // monitor all apps
        }
        serviceInfo = info
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return

        val pkg = event.packageName?.toString() ?: return
        val clazz = event.className?.toString() ?: ""

        // SCREEN WAKE / UNLOCK
        if (event.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            pkg == "com.android.systemui") {

            startCaptureService("SCREEN_WAKE")
        }

        // PASSWORD / PIN ATTEMPT
        if ((event.eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED ||
             event.eventType == AccessibilityEvent.TYPE_VIEW_CLICKED)
            && pkg == "com.android.systemui"
            && (clazz.contains("Keyguard") || clazz.contains("Password") || clazz.contains("PIN") || clazz.contains("Pattern"))
        ) {
            startCaptureService("PIN_ATTEMPT")
        }
    }

    private fun startCaptureService(trigger: String) {
        val intent = Intent(this, ScreenCaptureService::class.java)
        intent.putExtra("capture", true)     // mandatory
        intent.putExtra("trigger", trigger)  // logging reason
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility Service interrupted")
    }
}