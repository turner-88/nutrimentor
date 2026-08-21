package com.nutrimentor.dm.data

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.nutrimentor.dm.net.Api
import com.nutrimentor.dm.net.TokenStore
import kotlinx.coroutines.tasks.await

/** Thin wrapper that adds token persistence around the raw [Api]. */
class Repository(
    val api: Api,
    private val tokenStore: TokenStore,
) {
    fun isLoggedIn(): Boolean = !tokenStore.token().isNullOrEmpty()

    suspend fun login(username: String, password: String): Result<User> = call {
        val res = api.login(LoginRequest(username, password))
        val data = res.data ?: error(res.error ?: "Login gagal.")
        tokenStore.save(data.token)
        data.user
    }

    suspend fun register(req: RegisterRequest): Result<User> = call {
        val res = api.register(req)
        val data = res.data ?: error(res.error ?: "Registrasi gagal.")
        tokenStore.save(data.token)
        data.user
    }

    suspend fun logout() = tokenStore.clear()

    /**
     * Registers this device's current FCM token with the backend so it can
     * receive push notifications. Requires a logged-in session (the auth
     * interceptor attaches the JWT). Failures are swallowed — push is
     * best-effort and must never block auth flows.
     */
    suspend fun syncPushToken() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            registerPushToken(token)
        }.onFailure { Log.w(TAG, "syncPushToken failed", it) }
    }

    /** Uploads a specific FCM token (used by the messaging service's onNewToken). */
    suspend fun registerPushToken(token: String) {
        api.registerDeviceToken(DeviceTokenRequest(token))
    }

    /**
     * Removes this device's FCM token from the backend and deletes it locally.
     * Call before clearing the session on logout.
     */
    suspend fun clearPushToken() {
        runCatching {
            val token = FirebaseMessaging.getInstance().token.await()
            api.unregisterDeviceToken(DeviceTokenRequest(token))
            FirebaseMessaging.getInstance().deleteToken().await()
        }.onFailure { Log.w(TAG, "clearPushToken failed", it) }
    }

    private companion object {
        const val TAG = "Repository"
    }

    /** Runs [block], mapping the envelope's error to a failed [Result]. */
    private inline fun <T> call(block: () -> T): Result<T> =
        runCatching { block() }
}
