package com.example.chemochat

import android.content.Context
import android.content.Intent
import android.util.Log
import java.io.File

/**
 * Global logging utility with configurable verbosity levels.
 */
object LogUtils {
    private const val TAG = "ChemoChat"
    var isVerbose: Boolean = false

    fun d(message: String) {
        if (isVerbose) {
            Log.d(TAG, "[DEBUG] $message")
        }
    }

    fun i(message: String) {
        Log.i(TAG, "[INFO] $message")
    }

    fun w(message: String, throwable: Throwable? = null) {
        Log.w(TAG, "[WARN] $message", throwable)
    }

    fun e(message: String, throwable: Throwable? = null) {
        Log.e(TAG, "[ERROR] $message", throwable)
    }

    fun v(message: String) {
        if (isVerbose) {
            Log.v(TAG, "[VERBOSE] $message")
        }
    }

    fun exportLogs(context: Context) {
        try {
            val logFile = File(context.cacheDir, "chemochat_logs.txt")
            val process = Runtime.getRuntime().exec("logcat -d")
            val logs = process.inputStream.bufferedReader().use { it.readText() }
            logFile.writeText(logs)

            val uri = androidx.core.content.FileProvider.getUriForFile(context, "${context.packageName}.provider", logFile)
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Export Logs"))
        } catch (e: Exception) {
            e("Failed to export logs", e)
        }
    }
}
