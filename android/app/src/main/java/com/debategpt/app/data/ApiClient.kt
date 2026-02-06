package com.debategpt.app.data

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object ApiClient {

    private const val DEFAULT_BASE_URL = "http://10.0.2.2:8000/"

    private var _baseUrl: String = DEFAULT_BASE_URL
    private var _retrofit: Retrofit? = null
    private var _api: ApiService? = null

    fun setServerUrl(url: String) {
        val newUrl = url.trimEnd('/') + "/"
        if (newUrl != _baseUrl) {
            _baseUrl = newUrl
            _retrofit = null
            _api = null
        }
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    val api: ApiService
        get() {
            _api?.let { return it }
            val retrofit = _retrofit ?: Retrofit.Builder()
                .baseUrl(_baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build().also { _retrofit = it }
            return retrofit.create(ApiService::class.java).also { _api = it }
        }
}
