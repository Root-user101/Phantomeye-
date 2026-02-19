package com.security.phantomeye

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.app.ActivityManager
import android.Manifest
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private val CAMERA_PERMISSION_CODE = 101
    private var isServiceRunning = false

    // UI Components for PhantomEye UI
    private lateinit var deactivateButton: MaterialButton
    private lateinit var systemStatusText: android.widget.TextView
    private lateinit var shieldIcon: android.widget.ImageView
    private lateinit var lastCaptureTime: android.widget.TextView
    private lateinit var eventsTodayCount: android.widget.TextView
    private lateinit var statusDot: android.view.View
    private lateinit var statusLabel: android.widget.TextView
    private lateinit var statsCard: CardView
    
    // Animation for pulsating shield
    private var shieldPulseAnimator: ObjectAnimator? = null

    private val updateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == "com.security.phantomeye.UPDATE_UI") {
                val time = intent.getStringExtra("time") ?: "--:--"
                val count = intent.getIntExtra("count", 0)
                
                // Update UI with new data
                lastCaptureTime.text = time
                eventsTodayCount.text = count.toString()
                
                // Update event count color based on severity
                updateEventCountColor(count)
            }
        }
    }

    private fun isMyServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == ScreenCaptureService::class.java.name
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set PhantomEye theme
        setTheme(R.style.Theme_MyApplication)
        
        // Make status bar transparent for edge-to-edge design
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        }
        
        setContentView(R.layout.activity_main) // Use the new XML layout

        // Initialize UI Components
        deactivateButton = findViewById(R.id.deactivateButton)
        systemStatusText = findViewById(R.id.systemStatusText)
        shieldIcon = findViewById(R.id.shieldIcon)
        lastCaptureTime = findViewById(R.id.lastCaptureTime)
        eventsTodayCount = findViewById(R.id.eventsTodayCount)
        statusDot = findViewById(R.id.statusDot)
        statusLabel = findViewById(R.id.statusLabel)
        statsCard = findViewById(R.id.statsCard)

        // Set initial state
        isServiceRunning = isMyServiceRunning()
        updateUIState()

        // Setup button click listener
        deactivateButton.setOnClickListener {
            if (isServiceRunning) {
                stopCaptureService()
            } else {
                checkPermissionAndStartService()
            }
        }

        // Start shield pulsating animation
        startShieldPulseAnimation()

        // Register for UI updates from service
        registerReceiver(updateReceiver, IntentFilter("com.security.phantomeye.UPDATE_UI"))
        
        // Update initial capture info
        updateLastCaptureInfo()
        
        // Setup card click listener for more stats
        statsCard.setOnClickListener {
            Toast.makeText(this, "View detailed security log", Toast.LENGTH_SHORT).show()
            // You can add navigation to detailed stats here
        }
    }

    private fun updateUIState() {
        if (isServiceRunning) {
            systemStatusText.text = "SYSTEM PROTECTED"
            deactivateButton.text = "DEACTIVATE"
            statusLabel.text = "Active"
            statusDot.background = ContextCompat.getDrawable(this, R.drawable.circle_green)
			
			
            startShieldPulseAnimation()
        } else {
            systemStatusText.text = "SYSTEM VULNERABLE"
            deactivateButton.text = "ACTIVATE"
            statusLabel.text = "Inactive"
			
            statusDot.background = ContextCompat.getDrawable(this, R.drawable.circle_red)
			 
            stopShieldPulseAnimation()
            shieldIcon.alpha = 0.5f // Dim the shield when inactive
          }
        
        // Update button color based on state
        if (isServiceRunning) {
            deactivateButton.setBackgroundColor(ContextCompat.getColor(this, R.color.ruby_red))
        } else {
            deactivateButton.setBackgroundColor(ContextCompat.getColor(this, R.color.emerald_glow))
        }
    }

    private fun startShieldPulseAnimation() {
        stopShieldPulseAnimation() // Stop any existing animation
        
        shieldPulseAnimator = ObjectAnimator.ofFloat(shieldIcon, "alpha", 0.7f, 1.0f).apply {
            duration = 1500
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        
        // Also add a subtle scale animation
        val scaleAnimator = ObjectAnimator.ofFloat(shieldIcon, "scaleX", 0.95f, 1.05f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
        ObjectAnimator.ofFloat(shieldIcon, "scaleY", 0.95f, 1.05f).apply {
            duration = 2000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopShieldPulseAnimation() {
        shieldPulseAnimator?.cancel()
        shieldIcon.clearAnimation()
        shieldIcon.alpha = 1.0f
        shieldIcon.scaleX = 1.0f
        shieldIcon.scaleY = 1.0f
    }

    private fun updateEventCountColor(count: Int) {
        val color = when {
            count == 0 -> R.color.emerald_glow
            count < 5 -> R.color.status_yellow
            else -> R.color.ruby_red
        }
        eventsTodayCount.setTextColor(ContextCompat.getColor(this, color))
    }

    // --- Permission and Service Control (Updated) ---

    private fun checkPermissionAndStartService() {
        // 1. Check Camera Permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
            checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), CAMERA_PERMISSION_CODE)
            return
        }

        // 2. Check Battery Optimization
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            showBatteryOptimizationDialog()
            return
        }

        // 3. Auto-Start Check
        val prefs = getSharedPreferences("phantomeye_prefs", Context.MODE_PRIVATE)
        val hasShownAutoStart = prefs.getBoolean("has_shown_autostart", false)

        if (!hasShownAutoStart) {
            openAutoStartSettingsIfFound()
            prefs.edit().putBoolean("has_shown_autostart", true).apply()
            Toast.makeText(this, "Please enable auto-start for continuous protection", Toast.LENGTH_LONG).show()
        }

        startCaptureService()
    }

    private fun showBatteryOptimizationDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Battery Optimization")
            .setMessage("For continuous protection, PhantomEye needs to be excluded from battery optimization.")
            .setPositiveButton("Allow") { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                startActivity(intent)
            }
            .setNegativeButton("Skip") { _, _ ->
                startCaptureService()
            }
            .setCancelable(false)
            .show()
    }

    private fun openAutoStartSettingsIfFound() {
        val manufacturer = Build.MANUFACTURER.lowercase()
        val intent = Intent()
        var found = true
        
        when {
            manufacturer.contains("xiaomi") -> intent.component = 
                ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity")
            manufacturer.contains("oppo") -> intent.component = 
                ComponentName("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity")
            manufacturer.contains("vivo") -> intent.component = 
                ComponentName("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity")
            manufacturer.contains("samsung") -> {
                intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                intent.data = Uri.parse("package:$packageName")
            }
            manufacturer.contains("huawei") || manufacturer.contains("honor") -> intent.component = 
                ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity")
            else -> found = false
        }

        if (found) {
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                startActivity(intent)
                Toast.makeText(this, "Enable 'Auto-start' for PhantomEye", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                // Fallback if activity not found
            }
        }
    }

    private fun startCaptureService() {
        val intent = Intent(this, ScreenCaptureService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        isServiceRunning = true
        updateUIState()
        Toast.makeText(this, "PhantomEye Activated - System Protected", Toast.LENGTH_SHORT).show()
    }

    private fun stopCaptureService() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Deactivate Protection")
            .setMessage("Are you sure you want to deactivate PhantomEye security?")
            .setPositiveButton("DEACTIVATE") { _, _ ->
                stopService(Intent(this, ScreenCaptureService::class.java))
                isServiceRunning = false
                updateUIState()
                Toast.makeText(this, "PhantomEye Deactivated", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("CANCEL", null)
            .show()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == CAMERA_PERMISSION_CODE && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            checkPermissionAndStartService()
        } else {
            Toast.makeText(this, "Camera permission is required for security monitoring", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateLastCaptureInfo() {
        val photosDir = File(getExternalFilesDir(null), "PhantomEye/Photos")
        if (!photosDir.exists()) return
        
        val todayDir = photosDir.listFiles()?.maxByOrNull { it.lastModified() } ?: return
        val lastFile = todayDir.listFiles()?.maxByOrNull { it.lastModified() } ?: return
        
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val time = timeFormat.format(Date(lastFile.lastModified()))
        
        lastCaptureTime.text = time
        eventsTodayCount.text = (todayDir.listFiles()?.size ?: 0).toString()
        updateEventCountColor(todayDir.listFiles()?.size ?: 0)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopShieldPulseAnimation()
        try { unregisterReceiver(updateReceiver) } catch (e: Exception) {}
    }

    override fun onResume() {
        super.onResume()
        // Refresh service status when returning to app
        isServiceRunning = isMyServiceRunning()
        updateUIState()
    }
}