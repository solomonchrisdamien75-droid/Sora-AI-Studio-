package com.example.network.huggingface

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

/**
 * Factory & Singleton provider for Hugging Face Retrofit client.
 */
object HuggingFaceRetrofitClient {

    private const val BASE_URL = "https://huggingface.co/"
    private const val DEFAULT_USER_AGENT = "SoraAIStudio-Android/3.5 (Linux; Android 14; Mobile AI Engine)"

    @Volatile
    private var customApiKey: String? = null

    /**
     * Set an optional Hugging Face User Access Token (Bearer Token) for private models or higher rate limits.
     */
    fun setApiToken(token: String?) {
        customApiKey = token?.trim()?.ifBlank { null }
    }

    private val userAgentInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("User-Agent", DEFAULT_USER_AGENT)
            .header("Accept", "application/json, application/octet-stream, */*")

        customApiKey?.let { token ->
            requestBuilder.header("Authorization", "Bearer $token")
        }

        chain.proceed(requestBuilder.build())
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.HEADERS
    }

    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(userAgentInterceptor)
            .addInterceptor(loggingInterceptor)
            .followRedirects(true)
            .followSslRedirects(true)
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }

    val apiService: HuggingFaceApiService by lazy {
        retrofit.create(HuggingFaceApiService::class.java)
    }

    /**
     * Creates a custom configured Retrofit instance for custom endpoints or proxies.
     */
    fun createCustomService(baseUrl: String = BASE_URL, token: String? = null): HuggingFaceApiService {
        val clientBuilder = okHttpClient.newBuilder()
        if (!token.isNullOrBlank()) {
            clientBuilder.addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .header("Authorization", "Bearer $token")
                    .build()
                chain.proceed(req)
            }
        }
        return Retrofit.Builder()
            .baseUrl(if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/")
            .client(clientBuilder.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(HuggingFaceApiService::class.java)
    }
}
