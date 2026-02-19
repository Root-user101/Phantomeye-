package com.security.phantomeye

import android.app.Activity
import android.hardware.Camera
import android.os.Bundle
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

class TransparentCaptureActivity : Activity() {

    private var camera: Camera? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            camera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT)
            camera?.startPreview()

            camera?.takePicture(null, null) { data, cam ->
                savePhoto(data)
                cam.release()
                finish()
            }

        } catch (e: Exception) {
            Log.e("PhantomEye", "Camera error", e)
            camera?.release()
            finish()
        }
    }

    private fun savePhoto(bytes: ByteArray) {
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
        val dir = File(getExternalFilesDir(null), "PhantomEye/Photos/$date")
        dir.mkdirs()

        val file = File(dir, "IMG_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { it.write(bytes) }

        // BLACK IMAGE FILTER
        if (file.length() < 15_000) file.delete()
    }
}