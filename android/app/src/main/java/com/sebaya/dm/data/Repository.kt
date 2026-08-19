package com.sebaya.dm.data

import com.sebaya.dm.net.Api
import com.sebaya.dm.net.TokenStore

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

    /** Runs [block], mapping the envelope's error to a failed [Result]. */
    private inline fun <T> call(block: () -> T): Result<T> =
        runCatching { block() }
}
