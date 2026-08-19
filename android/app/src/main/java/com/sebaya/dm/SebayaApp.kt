package com.sebaya.dm

import android.app.Application
import com.sebaya.dm.data.Repository
import com.sebaya.dm.net.Network
import com.sebaya.dm.net.TokenStore

/** Application-scoped manual dependency container (no DI framework). */
class SebayaApp : Application() {
    lateinit var repository: Repository
        private set

    override fun onCreate() {
        super.onCreate()
        val tokenStore = TokenStore(this)
        tokenStore.load()
        val api = Network.buildApi(tokenStore)
        repository = Repository(api, tokenStore)
    }
}
