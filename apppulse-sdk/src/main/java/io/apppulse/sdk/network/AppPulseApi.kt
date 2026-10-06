package io.apppulse.sdk.network

import io.apppulse.sdk.network.dto.CrashDto
import io.apppulse.sdk.network.dto.HealthDto
import io.apppulse.sdk.network.dto.MemoryDto
import io.apppulse.sdk.network.dto.PerformanceDto
import io.apppulse.sdk.network.dto.SecurityDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

/**
 * Remote AppPulse API.
 *
 * Paths are relative to [io.apppulse.sdk.config.AppPulseConfig.baseUrl].
 * [ApiClient] adds the `X-Api-Key` header.
 */
interface AppPulseApi {

    /**
     * Uploads crash reports that have not been synced.
     */
    @POST("api/v1/crashes")
    suspend fun uploadCrashes(@Body crashes: List<CrashDto>): Response<ResponseBody>

    /**
     * Uploads startup, screen, network, FPS, and ANR rows.
     */
    @POST("api/v1/performance")
    suspend fun uploadPerformance(@Body body: PerformanceDto): Response<ResponseBody>

    /**
     * Uploads heap samples, leak candidates, and bitmap measurements.
     */
    @POST("api/v1/memory")
    suspend fun uploadMemory(@Body body: MemoryDto): Response<ResponseBody>

    /**
     * Uploads security findings.
     */
    @POST("api/v1/security")
    suspend fun uploadSecurity(@Body body: SecurityDto): Response<ResponseBody>

    /**
     * Uploads health reports.
     */
    @POST("api/v1/health")
    suspend fun uploadHealth(@Body body: HealthDto): Response<ResponseBody>
}
