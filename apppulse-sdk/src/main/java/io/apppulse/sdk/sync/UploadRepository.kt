package io.apppulse.sdk.sync

import io.apppulse.sdk.core.AppPulseLogger
import io.apppulse.sdk.network.ApiClient
import io.apppulse.sdk.network.dto.CrashDto
import io.apppulse.sdk.network.dto.HealthDto
import io.apppulse.sdk.network.dto.MemoryDto
import io.apppulse.sdk.network.dto.PerformanceDto
import io.apppulse.sdk.network.dto.SecurityDto
import kotlinx.coroutines.delay
import okhttp3.ResponseBody
import retrofit2.Response
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.io.IOException

/**
 * Result of one sync attempt.
 */
enum class SyncOutcome {
    SUCCESS,
    RETRY,
    FAILURE
}

/**
 * Upload failure that tells [SyncManager] whether WorkManager should retry.
 */
class SyncCallException(
    val outcome: SyncOutcome,
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Sends DTO batches and retries timeouts and server errors.
 *
 * Each call is attempted up to three times. The delay doubles after every
 * failure, starting at one second. Authentication and other 4xx responses
 * are not retried.
 *
 * @param apiClient Retrofit client for the configured base URL.
 */
class UploadRepository(
    private val apiClient: ApiClient
) {

    /**
     * Uploads [crashes]. An empty list is ignored.
     */
    suspend fun uploadCrashes(crashes: List<CrashDto>) {
        if (crashes.isEmpty()) return
        execute { apiClient.api().uploadCrashes(crashes) }
    }

    /**
     * Uploads [body]. An empty batch is ignored.
     */
    suspend fun uploadPerformance(body: PerformanceDto) {
        if (body.isEmpty()) return
        execute { apiClient.api().uploadPerformance(body) }
    }

    /**
     * Uploads [body]. An empty batch is ignored.
     */
    suspend fun uploadMemory(body: MemoryDto) {
        if (body.isEmpty()) return
        execute { apiClient.api().uploadMemory(body) }
    }

    /**
     * Uploads [body]. An empty batch is ignored.
     */
    suspend fun uploadSecurity(body: SecurityDto) {
        if (body.isEmpty()) return
        execute { apiClient.api().uploadSecurity(body) }
    }

    /**
     * Uploads [body]. An empty batch is ignored.
     */
    suspend fun uploadHealth(body: HealthDto) {
        if (body.isEmpty()) return
        execute { apiClient.api().uploadHealth(body) }
    }

    private suspend fun execute(call: suspend () -> Response<ResponseBody>) {
        var delayMs = INITIAL_DELAY_MS
        var last: SyncCallException? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            try {
                val response = call()
                response.body()?.close()
                response.errorBody()?.close()
                if (!response.isSuccessful) {
                    throw httpFailure(response.code())
                }
                return
            } catch (error: SyncCallException) {
                last = error
                if (error.outcome != SyncOutcome.RETRY || attempt == MAX_ATTEMPTS - 1) {
                    throw error
                }
            } catch (error: SocketTimeoutException) {
                last = SyncCallException(SyncOutcome.RETRY, "Timeout", error)
            } catch (error: InterruptedIOException) {
                last = SyncCallException(SyncOutcome.RETRY, "Timeout", error)
            } catch (error: UnknownHostException) {
                last = SyncCallException(SyncOutcome.RETRY, "No Internet", error)
                throw last
            } catch (error: ConnectException) {
                last = SyncCallException(SyncOutcome.RETRY, "No Internet", error)
                throw last
            } catch (error: IOException) {
                last = SyncCallException(SyncOutcome.RETRY, error.message ?: "Network error", error)
            }
            if (attempt == MAX_ATTEMPTS - 1) {
                throw last ?: SyncCallException(SyncOutcome.FAILURE, "Upload failed")
            }
            AppPulseLogger.w(TAG, "Sync retry ${attempt + 1}: ${last?.message}")
            delay(delayMs)
            delayMs *= 2
        }
    }

    private fun httpFailure(code: Int): SyncCallException {
        val outcome = when {
            code == 408 || code == 429 -> SyncOutcome.RETRY
            code in 500..599 -> SyncOutcome.RETRY
            else -> SyncOutcome.FAILURE
        }
        return SyncCallException(outcome, "Server error $code")
    }

    private companion object {
        const val TAG = "AppPulseSync"
        const val MAX_ATTEMPTS = 3
        const val INITIAL_DELAY_MS = 1_000L
    }
}
