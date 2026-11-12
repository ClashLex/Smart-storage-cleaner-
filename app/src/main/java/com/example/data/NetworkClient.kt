package com.example.network

import com.example.BuildConfig
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class NetworkClient(private val userPreferencesRepository: UserPreferencesRepository) {
    private val baseUrl = BuildConfig.API_BASE_URL.let {
        if (it.endsWith('/')) it else "$it/"
    }

    private val authInterceptor = Interceptor { chain ->
        val token = runBlocking {
            userPreferencesRepository.userSession.firstOrNull()?.token
        }
        val request = chain.request().newBuilder().apply {
            if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
        }.build()
        chain.proceed(request)
    }

    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        // Never log authorization headers, request bodies, or purchase tokens.
        redactHeader("Authorization")
        level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
        else HttpLoggingInterceptor.Level.NONE
    }

    private val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(authInterceptor)
        .addInterceptor(loggingInterceptor)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(okHttpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    val authApi: AuthApi = retrofit.create(AuthApi::class.java)
}
