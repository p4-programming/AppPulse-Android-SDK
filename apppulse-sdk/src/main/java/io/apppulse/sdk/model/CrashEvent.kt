package io.apppulse.sdk.model

/**
 * Immutable record of an uncaught exception captured by AppPulse.
 *
 * @property timestampEpochMillis Wall-clock time when the exception was captured.
 * @property threadName Name of the thread that threw [exceptionName].
 * @property threadId Platform thread id of the crashing thread.
 * @property exceptionName Fully qualified exception class name.
 * @property message Exception message, or null when the throwable has none.
 * @property stackTrace Formatted stack trace, including causes.
 * @property deviceInfo Device and application identity at capture time.
 */
data class CrashEvent(
    val timestampEpochMillis: Long,
    val threadName: String,
    val threadId: Long,
    val exceptionName: String,
    val message: String?,
    val stackTrace: String,
    val deviceInfo: DeviceInfo
) {

    /**
     * Builds a single-line description for logs. The stack trace is omitted here
     * so the logger can attach the original [Throwable] separately.
     */
    fun summary(): String {
        val detail = message?.takeIf { it.isNotBlank() }?.let { ": $it" }.orEmpty()
        return "Uncaught ${exceptionName}$detail on thread '$threadName' ($threadId) " +
            "device=${deviceInfo.manufacturer} ${deviceInfo.model}, " +
            "android=${deviceInfo.androidVersion}, app=${deviceInfo.appVersion}, " +
            "at=$timestampEpochMillis"
    }

    companion object {
        /**
         * Creates a [CrashEvent] from an uncaught [throwable].
         *
         * @param thread Thread that did not catch [throwable].
         * @param throwable Exception that escaped the thread.
         * @param deviceInfo Identity collected for this capture.
         * @param timestampEpochMillis Capture time. Defaults to the current time.
         */
        fun capture(
            thread: Thread,
            throwable: Throwable,
            deviceInfo: DeviceInfo,
            timestampEpochMillis: Long = System.currentTimeMillis()
        ): CrashEvent {
            return CrashEvent(
                timestampEpochMillis = timestampEpochMillis,
                threadName = thread.name ?: "unknown",
                threadId = thread.identifier(),
                exceptionName = throwable.javaClass.name,
                message = throwable.message,
                stackTrace = throwable.stackTraceString(),
                deviceInfo = deviceInfo
            )
        }

        @Suppress("DEPRECATION")
        private fun Thread.identifier(): Long = id

        private fun Throwable.stackTraceString(): String {
            val buffer = java.io.StringWriter()
            printStackTrace(java.io.PrintWriter(buffer))
            return buffer.toString()
        }
    }
}
