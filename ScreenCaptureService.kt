package com.security.phantomeye

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.app.*
import android.content.*
import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.media.ImageReader
import android.os.*
import android.view.Surface
import androidx.core.app.NotificationCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.atomic.AtomicBoolean

class ScreenCaptureService : Service() {

    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var imageReader: ImageReader? = null
    private var previewSurface: Surface? = null

    private var handlerThread: HandlerThread? = null
    private var handler: Handler? = null

    private val isCameraOpening = AtomicBoolean(false)
    private var cameraReady = false

    // -------- SCREEN EVENT RECEIVER --------

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_SCREEN_ON) {
                FileLogger.d("SCS", "SCREEN_ON detected")
                if (cameraReady) {
                    captureStill()
                } else {
                    FileLogger.d("SCS", "Camera not ready on screen on, attempting recovery")
                    safeRebuildCamera()
                }
            }
        }
    }

    // -------- SERVICE LIFECYCLE --------

    override fun onCreate() {
        super.onCreate()
        startForegroundNotification()
        
        registerReceiver(screenReceiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        openCameraAndStartPreview()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    // -------- CAMERA SETUP --------

    private fun openCameraAndStartPreview() {
        if (isCameraOpening.get()) return
        isCameraOpening.set(true)

        initHandler()

        val cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        try {
            val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                cameraManager.getCameraCharacteristics(id)
                    .get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT
            } ?: return

            imageReader = ImageReader.newInstance(640, 480, ImageFormat.JPEG, 2)
            imageReader!!.setOnImageAvailableListener({ reader ->
                val image = reader.acquireLatestImage() ?: return@setOnImageAvailableListener
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)
                image.close()
                savePhoto(bytes)
            }, handler)

            cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
                override fun onOpened(camera: CameraDevice) {
                    cameraDevice = camera
                    isCameraOpening.set(false)
                    startPreviewSession()
                }

                override fun onDisconnected(camera: CameraDevice) {
                    FileLogger.d("SCS", "Camera evicted/disconnected")
                    isCameraOpening.set(false)
                    cameraReady = false
                    closeCameraInternal() // Critical: Release immediately
                }

                override fun onError(camera: CameraDevice, error: Int) {
                    FileLogger.d("SCS", "Camera Error: $error")
                    isCameraOpening.set(false)
                    cameraReady = false
                    closeCameraInternal()
                }
            }, handler)

        } catch (e: Exception) {
            isCameraOpening.set(false)
            FileLogger.d("SCS", "Failed to open camera: ${e.message}")
        }
    }

    private fun startPreviewSession() {
        val texture = SurfaceTexture(10) // Changed from 0 to 10 for better compatibility
        texture.setDefaultBufferSize(640, 480)
        previewSurface = Surface(texture)

        cameraDevice?.createCaptureSession(
            listOf(previewSurface!!, imageReader!!.surface),
            object : CameraCaptureSession.StateCallback() {
                override fun onConfigured(session: CameraCaptureSession) {
                    captureSession = session
                    try {
                        val previewRequest = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                        previewRequest.addTarget(previewSurface!!)
                        session.setRepeatingRequest(previewRequest.build(), null, handler)
                        cameraReady = true
                        FileLogger.d("SCS", "Camera session active")
                    } catch (e: Exception) {
                        FileLogger.d("SCS", "Session request failed")
                    }
                }

                override fun onConfigureFailed(session: CameraCaptureSession) {
                    cameraReady = false
                }
            }, handler
        )
    }

    // -------- RECOVERY & CAPTURE --------

    private fun captureStill() {
        if (!cameraReady || cameraDevice == null || captureSession == null) return

        try {
            val request = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
            request.addTarget(imageReader!!.surface)
            captureSession!!.capture(request.build(), null, handler)
        } catch (e: Exception) {
            FileLogger.d("SCS", "Capture failed: ${e.message}")
            safeRebuildCamera()
        }
    }

    private fun safeRebuildCamera() {
        if (isCameraOpening.get()) return
        
        // Wait 2 seconds before retrying to ensure the other app has released hardware
        Handler(Looper.getMainLooper()).postDelayed({
            FileLogger.d("SCS", "Running safe rebuild...")
            closeCameraInternal()
            openCameraAndStartPreview()
        }, 2000)
    }

    private fun initHandler() {
        if (handlerThread == null) {
            handlerThread = HandlerThread("WTMP-Camera").apply { start() }
            handler = Handler(handlerThread!!.looper)
        }
    }

    private fun closeCameraInternal() {
        try { captureSession?.close() } catch (_: Exception) {}
        try { cameraDevice?.close() } catch (_: Exception) {}
        try { imageReader?.close() } catch (_: Exception) {}
        try { handlerThread?.quitSafely() } catch (_: Exception) {}

        captureSession = null
        cameraDevice = null
        imageReader = null
        handlerThread = null
        handler = null
        cameraReady = false
    }

    // -------- UTILITIES --------

    private fun savePhoto(bytes: ByteArray) {
    val date = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val dir = File(getExternalFilesDir(null), "PhantomEye/Photos/$date").apply { mkdirs() }
    val file = File(dir, "unlock_${System.currentTimeMillis()}.jpg")
    
    try {
        // Decode and rotate 270° for front camera
        val options = BitmapFactory.Options()
        val originalBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
        
        if (originalBitmap != null) {
            // Rotate 270° for front camera portrait mode
            val matrix = Matrix()
            matrix.postRotate(270f)
            
            val rotatedBitmap = Bitmap.createBitmap(
                originalBitmap, 
                0, 0, 
                originalBitmap.width, 
                originalBitmap.height, 
                matrix, 
                true
            )
            
            // Save rotated image
            FileOutputStream(file).use { fos ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos)
            }
            
            originalBitmap.recycle()
            rotatedBitmap.recycle()
            FileLogger.d("SCS", "Photo saved with rotation: ${file.name}")
        }
    } catch (e: Exception) {
        // Fallback: save original if rotation fails
        FileOutputStream(file).use { it.write(bytes) }
        FileLogger.d("SCS", "Photo saved (no rotation): ${file.name}")
    }
}

    private fun startForegroundNotification() {
        val channelId = "WTMP_CAPTURE_CHANNEL"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(channelId, "WTMP Guard", NotificationManager.IMPORTANCE_LOW)
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("PhantomEye Active")
            .setContentText("Monitoring unlock attempts")
            .setSmallIcon(R.drawable.ic_shield)
            .build()

        // Android 14+ requires service type in startForeground
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notification, 64)
        } else {
            startForeground(1, notification)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(screenReceiver)
        closeCameraInternal()
    }

    override fun onBind(intent: Intent?) = null
}
