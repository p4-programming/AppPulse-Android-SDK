package io.apppulse.sdk.health

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import android.os.Environment
import android.provider.MediaStore
import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.health.model.HealthReport
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * File format written by [HealthReportExporter].
 */
enum class HealthExportFormat {
    JSON,
    CSV
}

/**
 * Where a health report was written.
 *
 * @property uri Content or file URI of the export.
 * @property displayName File name inside Downloads.
 */
data class HealthExportResult(
    val uri: Uri,
    val displayName: String
)

/**
 * Writes a [HealthReport] as JSON or CSV in the public Downloads directory.
 *
 * On API 29 and above the file is inserted through [MediaStore.Downloads].
 * On older releases the exporter writes the public Downloads directory when
 * the platform allows it, and otherwise the app-specific downloads folder.
 *
 * @param context Any context. The application context is retained.
 */
class HealthReportExporter(
    context: Context
) {

    private val appContext = context.applicationContext

    /**
     * Writes [report] and returns the saved location.
     *
     * @throws IOException when the file cannot be created.
     */
    fun export(report: HealthReport, format: HealthExportFormat): HealthExportResult {
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date(report.generatedAt))
        val extension = if (format == HealthExportFormat.JSON) "json" else "csv"
        val mime = if (format == HealthExportFormat.JSON) "application/json" else "text/csv"
        val displayName = "apppulse-health-$stamp.$extension"
        val bytes = when (format) {
            HealthExportFormat.JSON -> toJson(report).toByteArray(Charsets.UTF_8)
            HealthExportFormat.CSV -> toCsv(report).toByteArray(Charsets.UTF_8)
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            insertDownloads(displayName, mime, bytes)
        } else {
            writeLegacyDownloads(displayName, bytes)
        }
        AppPulseLogger.i(TAG, "Health report exported: $displayName")
        return HealthExportResult(uri, displayName)
    }

    private fun toJson(report: HealthReport): String {
        val recommendations = JSONArray()
        report.recommendations.forEach { recommendations.put(it) }
        val categories = JSONArray()
        report.categories.forEach { category ->
            categories.put(
                JSONObject()
                    .put("category", category.category.name)
                    .put("score", category.score)
                    .put("level", category.level.name)
                    .put("weight", category.weight)
                    .put("detail", category.detail)
            )
        }
        return JSONObject()
            .put("overallScore", report.overallScore)
            .put("overallStatus", report.overallStatus.name)
            .put("crashScore", report.crashScore)
            .put("performanceScore", report.performanceScore)
            .put("memoryScore", report.memoryScore)
            .put("securityScore", report.securityScore)
            .put("generatedAt", report.generatedAt)
            .put("trend", report.trend.name)
            .put("recommendations", recommendations)
            .put("categories", categories)
            .toString(2)
    }

    private fun toCsv(report: HealthReport): String {
        val header = "overallScore,overallStatus,crashScore,performanceScore,memoryScore," +
            "securityScore,generatedAt,trend,recommendations"
        val recommendations = csv(report.recommendations.joinToString(" | "))
        val row = listOf(
            report.overallScore.toString(),
            report.overallStatus.name,
            report.crashScore.toString(),
            report.performanceScore.toString(),
            report.memoryScore.toString(),
            report.securityScore.toString(),
            report.generatedAt.toString(),
            report.trend.name,
            recommendations
        ).joinToString(",")
        return "$header\n$row\n"
    }

    private fun csv(value: String): String {
        val escaped = value.replace("\"", "\"\"")
        return "\"$escaped\""
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertDownloads(displayName: String, mime: String, bytes: ByteArray): Uri {
        val resolver = appContext.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, displayName)
            put(MediaStore.Downloads.MIME_TYPE, mime)
            put(MediaStore.Downloads.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            ?: throw IOException("Downloads could not be opened")
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                stream.write(bytes)
            } ?: throw IOException("Downloads could not be written")
            values.clear()
            values.put(MediaStore.Downloads.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        } catch (throwable: Throwable) {
            resolver.delete(uri, null, null)
            throw throwable
        }
    }

    @Suppress("DEPRECATION")
    private fun writeLegacyDownloads(displayName: String, bytes: ByteArray): Uri {
        val publicDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val directory = if (publicDir != null && (publicDir.exists() || publicDir.mkdirs())) {
            publicDir
        } else {
            appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: appContext.filesDir
        }
        val file = File(directory, displayName)
        file.outputStream().use { stream -> stream.write(bytes) }
        return Uri.fromFile(file)
    }

    private companion object {
        const val TAG = "AppPulseHealth"
    }
}
