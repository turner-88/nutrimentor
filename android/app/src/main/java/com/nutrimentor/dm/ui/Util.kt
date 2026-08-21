package com.nutrimentor.dm.ui

import com.nutrimentor.dm.BuildConfig
import java.time.LocalTime

/**
 * Resolves a possibly-relative cover-image path against the API origin.
 * API_BASE_URL is like `http://host:8081/api/`; images live under the same
 * origin (e.g. `/static/uploads/x.jpg`).
 */
fun imageUrl(path: String): String {
    if (path.isBlank()) return ""
    if (path.startsWith("http://") || path.startsWith("https://")) return path
    val base = BuildConfig.API_BASE_URL            // http://host:8081/api/
    val origin = base.removeSuffix("/").removeSuffix("/api")
    return origin + "/" + path.removePrefix("/")
}

/** Time-of-day greeting in Indonesian. */
fun greeting(now: LocalTime = LocalTime.now()): String = when (now.hour) {
    in 4..10 -> "Selamat pagi"
    in 11..14 -> "Selamat siang"
    in 15..18 -> "Selamat sore"
    else -> "Selamat malam"
}
