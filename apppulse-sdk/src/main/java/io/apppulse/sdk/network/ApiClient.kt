package io.apppulse.sdk.network

import io.apppulse.sdk.config.AppPulseOptions
import io.apppulse.sdk.core.AppPulseLogger
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Builds the Retrofit client used by remote sync.
 *
 * Connect, read, and write timeouts are 30 seconds. The logging interceptor
 * writes through [AppPulseLogger] and redacts `X-Api-Key`.
 *
 * @param options Runtime API key, base URL, and debug flag.
 */
class ApiClient(
    private val options: AppPulseOptions
) {

    @Volatile
    private var api: AppPulseApi? = null

    /**
     * Returns the shared [AppPulseApi], creating it on the first call.
     *
     * @throws IllegalArgumentException when [AppPulseOptions.baseUrl] is not an absolute URL.
     */
    fun api(): AppPulseApi {
        api?.let { return it }
        return synchronized(this) {
            api ?: build().also { created -> api = created }
        }
    }

    private fun build(): AppPulseApi {
        val logging = HttpLoggingInterceptor { message ->
            AppPulseLogger.d(TAG, message)
        }.apply {
            level = if (options.debugMode) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.BASIC
            }
            redactHeader(API_KEY_HEADER)
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(apiKeyInterceptor())
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(options.baseUrl)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(AppPulseApi::class.java)
    }

    private fun apiKeyInterceptor(): Interceptor {
        return Interceptor { chain ->
            val request = chain.request().newBuilder()
                .header(API_KEY_HEADER, options.apiKey)
                .header("Accept", "application/json")
                .build()
            chain.proceed(request)
        }
    }

    private companion object {
        const val TAG = "AppPulseHttp"
        const val API_KEY_HEADER = "X-Api-Key"
        const val TIMEOUT_SECONDS = 30L
    }
}
