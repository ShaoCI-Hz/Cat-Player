package com.example.smbplayer.analytics

import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Simple crash reporter that logs crashes to local files.
 * Can be extended to send to Firebase Crashlytics.
 */
@Singleton
class CrashReporter @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val crashDir: File by lazy {
        File(context.filesDir, "crash_reports").apply { mkdirs() }
    }

    /**
     * Initialize crash reporting with uncaught exception handler.
     */
    fun initialize() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            try {
                logCrash(throwable, thread.name)
            } catch (_: Exception) {}
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }

    /**
     * Log a crash to local storage.
     */
    fun logCrash(throwable: Throwable, threadName: String = "main") {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(Date())
            val file = File(crashDir, "crash_$timestamp.txt")

            val sw = StringWriter()
            val pw = PrintWriter(sw)
            pw.println("=== Cat Player Crash Report ===")
            pw.println("Time: ${Date()}")
            pw.println("Thread: $threadName")
            pw.println("Exception: ${throwable.javaClass.name}")
            pw.println("Message: ${throwable.message}")
            pw.println()
            pw.println("Stack Trace:")
            throwable.printStackTrace(pw)
            pw.println()
            pw.println("Device Info:")
            pw.println("  Model: ${android.os.Build.MODEL}")
            pw.println("  Brand: ${android.os.Build.BRAND}")
            pw.println("  SDK: ${android.os.Build.VERSION.SDK_INT}")
            pw.println("  Android: ${android.os.Build.VERSION.RELEASE}")

            file.writeText(sw.toString())
            Log.e("CrashReporter", "Crash logged to ${file.absolutePath}")
        } catch (_: Exception) {}
    }

    /**
     * Get all crash reports.
     */
    fun getCrashReports(): List<File> {
        return crashDir.listFiles()?.sortedByDescending { it.name } ?: emptyList()
    }

    /**
     * Clear all crash reports.
     */
    fun clearReports() {
        crashDir.listFiles()?.forEach { it.delete() }
    }

    /**
     * Get crash report count.
     */
    fun getCrashCount(): Int = crashDir.listFiles()?.size ?: 0
}
