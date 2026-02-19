package com.security.phantomeye.util

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

object FileManager {

    private const val ROOT_DIR = "PhantomEye"
    private const val PHOTO_DIR = "Photos"
    private const val SESSION_DIR = "Sessions"
    private const val LOG_DIR = "Logs"
    private const val META_DIR = "Meta"

    private const val RETENTION_DAYS = 7

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    /* ================= INIT ================= */

    fun init(context: Context) {
        val root = getRootDir(context)
        if (!root.exists()) root.mkdirs()

        getPhotosRoot(context).mkdirs()
        getSessionsDir(context).mkdirs()
        getLogsDir(context).mkdirs()
        getMetaDir(context).mkdirs()
    }

    /* ================= PATH GETTERS ================= */

    fun getRootDir(context: Context): File {
        return File(context.getExternalFilesDir(null), ROOT_DIR)
    }

    fun getPhotosRoot(context: Context): File {
        return File(getRootDir(context), PHOTO_DIR)
    }

    fun getTodayPhotoDir(context: Context): File {
        val today = dateFormat.format(Date())
        val dir = File(getPhotosRoot(context), today)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    fun getSessionsDir(context: Context): File {
        return File(getRootDir(context), SESSION_DIR)
    }

    fun getLogsDir(context: Context): File {
        return File(getRootDir(context), LOG_DIR)
    }

    fun getMetaDir(context: Context): File {
        return File(getRootDir(context), META_DIR)
    }

    /* ================= FILE CREATORS ================= */

    fun createPhotoFile(context: Context, index: Int): File {
        val name = "attempt_%02d.jpg".format(index)
        return File(getTodayPhotoDir(context), name)
    }

    fun createSessionFile(context: Context, sessionId: Long): File {
        return File(getSessionsDir(context), "session_$sessionId.json")
    }

    fun getTodayLogFile(context: Context): File {
        val name = dateFormat.format(Date()) + ".log"
        return File(getLogsDir(context), name)
    }

    /* ================= CLEANUP ================= */

    fun cleanupOldData(context: Context) {
        val cutoff = System.currentTimeMillis() - (RETENTION_DAYS * 24 * 60 * 60 * 1000L)

        deleteOldDirs(getPhotosRoot(context), cutoff)
        deleteOldFiles(getSessionsDir(context), cutoff)
        deleteOldFiles(getLogsDir(context), cutoff)
    }

    private fun deleteOldDirs(parent: File, cutoff: Long) {
        if (!parent.exists()) return
        parent.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.lastModified() < cutoff) {
                dir.deleteRecursively()
            }
        }
    }

    private fun deleteOldFiles(parent: File, cutoff: Long) {
        if (!parent.exists()) return
        parent.listFiles()?.forEach { file ->
            if (file.isFile && file.lastModified() < cutoff) {
                file.delete()
            }
        }
    }
}