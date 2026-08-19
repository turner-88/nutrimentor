package com.sebaya.dm

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
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

        createNotificationChannel()
    }

    /** Registers the default channel used for all FCM display notifications. */
    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            NOTIFICATION_CHANNEL_ID,
            "Pengingat SebayaDM",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Notifikasi pengingat harian dan artikel edukasi baru."
        }
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    companion object {
        const val NOTIFICATION_CHANNEL_ID = "sebaya_default"
    }
}
