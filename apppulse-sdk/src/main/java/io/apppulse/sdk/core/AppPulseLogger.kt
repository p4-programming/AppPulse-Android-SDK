package io.apppulse.sdk.core

import android.util.Log

/**
 * Process-wide logging facade for the AppPulse SDK.
 *
 * Modules must log through this object so debug output can be gated by
 * [AppPulseConfig.debugMode] and so call sites never depend on [Log] directly.
 * Warning and error logs are always emitted. Verbose and debug logs are emitted
 * only while [debugMode] is enabled.
 */
object AppPulseLogger {

    /**
     * When true, verbose and debug messages are written to logcat.
     * [io.apppulse.sdk.AppPulse.init] sets this from [AppPulseConfig.debugMode].
     */
    @Volatile
    var debugMode: Boolean = false

    /**
     * Writes a verbose message when [debugMode] is enabled.
     *
     * @param tag Logcat tag. Keep it at or under 23 characters for API 24 and 25.
     * @param message Text to write.
     */
    fun v(tag: String, message: String) {
        if (debugMode) {
            log(Log.VERBOSE, tag, message, throwable = null)
        }
    }

    /**
     * Writes a debug message when [debugMode] is enabled.
     *
     * @param tag Logcat tag. Keep it at or under 23 characters for API 24 and 25.
     * @param message Text to write.
     */
    fun d(tag: String, message: String) {
        if (debugMode) {
            log(Log.DEBUG, tag, message, throwable = null)
        }
    }

    /**
     * Writes an informational message.
     *
     * @param tag Logcat tag. Keep it at or under 23 characters for API 24 and 25.
     * @param message Text to write.
     */
    fun i(tag: String, message: String) {
        log(Log.INFO, tag, message, throwable = null)
    }

    /**
     * Writes a warning.
     *
     * @param tag Logcat tag. Keep it at or under 23 characters for API 24 and 25.
     * @param message Text to write.
     * @param throwable Optional cause appended after [message].
     */
    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.WARN, tag, message, throwable)
    }

    /**
     * Writes an error.
     *
     * @param tag Logcat tag. Keep it at or under 23 characters for API 24 and 25.
     * @param message Text to write.
     * @param throwable Optional cause appended after [message].
     */
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log(Log.ERROR, tag, message, throwable)
    }

    private fun log(priority: Int, tag: String, message: String, throwable: Throwable?) {
        val safeTag = tag.ifBlank { DEFAULT_TAG }
        val text = message.ifEmpty { "(empty)" }
        try {
            var start = 0
            while (start < text.length) {
                val end = minOf(start + MAX_CHUNK_LENGTH, text.length)
                Log.println(priority, safeTag, text.substring(start, end))
                start = end
            }
            if (throwable != null) {
                Log.println(priority, safeTag, Log.getStackTraceString(throwable))
            }
        } catch (loggingFailure: RuntimeException) {
            // Last resort. A failure inside logcat must not take down the host process.
            System.err.println("AppPulse logging failed: ${loggingFailure.message}")
        }
    }

    private const val DEFAULT_TAG = "AppPulse"
    private const val MAX_CHUNK_LENGTH = 3500
}
