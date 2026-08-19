package com.sebaya.dm.net

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sebaya.dm.BuildConfig
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory

private val Context.dataStore by preferencesDataStore(name = "sebaya_prefs")
private val TOKEN_KEY = stringPreferencesKey("auth_token")

/** Persists and caches the JWT; exposes an interceptor that attaches it. */
class TokenStore(private val context: Context) {
    @Volatile
    private var cached: String? = null

    fun load() {
        cached = runBlocking { context.dataStore.data.map { it[TOKEN_KEY] }.first() }
    }

    fun token(): String? = cached

    suspend fun save(token: String) {
        cached = token
        context.dataStore.edit { it[TOKEN_KEY] = token }
    }

    suspend fun clear() {
        cached = null
        context.dataStore.edit { it.remove(TOKEN_KEY) }
    }

    val authInterceptor = Interceptor { chain ->
        val req = chain.request()
        val t = cached
        val out = if (t.isNullOrEmpty()) req
        else req.newBuilder().addHeader("Authorization", "Bearer $t").build()
        chain.proceed(out)
    }
}

object Network {
    private val json = Json { ignoreUnknownKeys = true }

    fun buildApi(tokenStore: TokenStore): Api {
        val logging = HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
        val client = OkHttpClient.Builder()
            .addInterceptor(tokenStore.authInterceptor)
            .addInterceptor(logging)
            .build()
        return Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(Api::class.java)
    }
}
