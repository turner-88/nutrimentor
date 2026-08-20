package com.sebaya.dm.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebaya.dm.data.ActivityLog
import com.sebaya.dm.data.ActivityRequest
import com.sebaya.dm.data.Article
import com.sebaya.dm.data.ArticleSummary
import com.sebaya.dm.data.ChangePasswordRequest
import com.sebaya.dm.data.Dashboard
import com.sebaya.dm.data.DietLog
import com.sebaya.dm.data.DietRequest
import com.sebaya.dm.data.ForgotPasswordRequest
import com.sebaya.dm.data.GlucoseLog
import com.sebaya.dm.data.GlucoseRequest
import com.sebaya.dm.data.LeaderboardEntry
import com.sebaya.dm.data.MedicationLog
import com.sebaya.dm.data.MedicationRequest
import com.sebaya.dm.data.RegisterRequest
import com.sebaya.dm.data.Repository
import com.sebaya.dm.data.UpdateProfileRequest
import com.sebaya.dm.data.User
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException

/** Bundled 30-day self-history shown on the history screen. */
data class HistoryData(
    val medication: List<MedicationLog>,
    val activity: List<ActivityLog>,
    val diet: List<DietLog>,
    val glucose: List<GlucoseLog>,
) {
    val isEmpty: Boolean get() = medication.isEmpty() && activity.isEmpty() && diet.isEmpty() && glucose.isEmpty()
}

class AppViewModel(val repo: Repository) : ViewModel() {

    // ---- Auth ----------------------------------------------------------
    var loggedIn by mutableStateOf(repo.isLoggedIn())
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    // ---- Per-section screen state --------------------------------------
    var dashboardState by mutableStateOf<UiState<Dashboard>>(UiState.Loading)
        private set
    var latestGlucose by mutableStateOf<GlucoseLog?>(null)
        private set
    var refreshingDashboard by mutableStateOf(false)
        private set

    var leaderboardState by mutableStateOf<UiState<List<LeaderboardEntry>>>(UiState.Loading)
        private set
    var refreshingLeaderboard by mutableStateOf(false)
        private set

    var articlesState by mutableStateOf<UiState<List<ArticleSummary>>>(UiState.Loading)
        private set
    var refreshingArticles by mutableStateOf(false)
        private set

    var articleState by mutableStateOf<UiState<Article>>(UiState.Loading)
        private set

    var meState by mutableStateOf<UiState<User>>(UiState.Loading)
        private set

    var historyState by mutableStateOf<UiState<HistoryData>>(UiState.Loading)
        private set

    /** Non-null when the last glucose entry was out of range; shown as a warning. */
    var glucoseWarning by mutableStateOf<String?>(null)

    // ---- Auth actions --------------------------------------------------
    fun login(username: String, password: String, onDone: () -> Unit) {
        error = null; busy = true
        viewModelScope.launch {
            repo.login(username, password)
                .onSuccess { loggedIn = true; onDone(); repo.syncPushToken() }
                .onFailure { error = it.message ?: "Login gagal." }
            busy = false
        }
    }

    fun register(req: RegisterRequest, onDone: () -> Unit) {
        error = null; busy = true
        viewModelScope.launch {
            repo.register(req)
                .onSuccess { loggedIn = true; onDone(); repo.syncPushToken() }
                .onFailure { error = it.message ?: "Registrasi gagal." }
            busy = false
        }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        repo.clearPushToken()
        repo.logout(); loggedIn = false; onDone()
    }

    /** Registers the FCM token on app start when a session already exists. */
    fun syncPushTokenIfLoggedIn() {
        if (!loggedIn) return
        viewModelScope.launch { repo.syncPushToken() }
    }

    // ---- Account actions -----------------------------------------------
    fun forgotPassword(email: String, onDone: (String) -> Unit) {
        error = null; busy = true
        viewModelScope.launch {
            runCatching { repo.api.forgotPassword(ForgotPasswordRequest(email.trim())) }
                .onSuccess { onDone(it.message ?: "Jika email terdaftar, tautan reset telah dikirim.") }
                .onFailure { error = netMessage(it) }
            busy = false
        }
    }

    fun updateProfile(req: UpdateProfileRequest, onDone: () -> Unit) {
        error = null; busy = true
        viewModelScope.launch {
            runCatching { repo.api.updateProfile(req).data ?: error("Gagal menyimpan profil.") }
                .onSuccess { meState = UiState.Success(it); onDone() }
                .onFailure { error = netMessage(it) }
            busy = false
        }
    }

    fun changePassword(req: ChangePasswordRequest, onDone: () -> Unit) {
        error = null; busy = true
        viewModelScope.launch {
            runCatching { repo.api.changePassword(req) }
                .onSuccess { onDone() }
                .onFailure { error = netMessage(it) }
            busy = false
        }
    }

    // ---- Loaders -------------------------------------------------------
    fun loadDashboard(refresh: Boolean = false) {
        if (refresh) refreshingDashboard = true else if (dashboardState !is UiState.Success) dashboardState = UiState.Loading
        viewModelScope.launch {
            runCatching {
                val dash = repo.api.dashboard().data ?: error("Data tidak tersedia.")
                latestGlucose = runCatching { repo.api.glucose().data?.firstOrNull() }.getOrNull()
                dash
            }.onSuccess { dashboardState = UiState.Success(it) }
                .onFailure { dashboardState = UiState.Error(netMessage(it)) }
            refreshingDashboard = false
        }
    }

    fun loadLeaderboard(refresh: Boolean = false) {
        if (refresh) refreshingLeaderboard = true else if (leaderboardState !is UiState.Success) leaderboardState = UiState.Loading
        viewModelScope.launch {
            runCatching { repo.api.leaderboard().data ?: emptyList() }
                .onSuccess { leaderboardState = if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
                .onFailure { leaderboardState = UiState.Error(netMessage(it)) }
            refreshingLeaderboard = false
        }
    }

    fun loadArticles(refresh: Boolean = false) {
        if (refresh) refreshingArticles = true else if (articlesState !is UiState.Success) articlesState = UiState.Loading
        viewModelScope.launch {
            runCatching { repo.api.education().data ?: emptyList() }
                .onSuccess { articlesState = if (it.isEmpty()) UiState.Empty else UiState.Success(it) }
                .onFailure { articlesState = UiState.Error(netMessage(it)) }
            refreshingArticles = false
        }
    }

    fun loadArticle(slug: String) {
        articleState = UiState.Loading
        viewModelScope.launch {
            runCatching { repo.api.article(slug).data ?: error("Artikel tidak ditemukan.") }
                .onSuccess {
                    articleState = UiState.Success(it)
                    // Fire-and-forget: record reading progress for reminders.
                    runCatching { repo.api.markArticleRead(slug) }
                }
                .onFailure { articleState = UiState.Error(netMessage(it)) }
        }
    }

    fun loadMe(refresh: Boolean = false) {
        if (!refresh && meState !is UiState.Success) meState = UiState.Loading
        viewModelScope.launch {
            runCatching { repo.api.me().data ?: error("Profil tidak tersedia.") }
                .onSuccess { meState = UiState.Success(it) }
                .onFailure { meState = UiState.Error(netMessage(it)) }
        }
    }

    fun loadHistory(refresh: Boolean = false) {
        if (!refresh && historyState !is UiState.Success) historyState = UiState.Loading
        viewModelScope.launch {
            runCatching {
                HistoryData(
                    medication = repo.api.medicationHistory().data.orEmpty(),
                    activity = repo.api.activityHistory().data.orEmpty(),
                    diet = repo.api.dietHistory().data.orEmpty(),
                    glucose = repo.api.glucose().data.orEmpty(),
                )
            }
                .onSuccess { historyState = if (it.isEmpty) UiState.Empty else UiState.Success(it) }
                .onFailure { historyState = UiState.Error(netMessage(it)) }
        }
    }

    // ---- Mutations (refresh dashboard afterwards) ----------------------
    fun submitMedication(complete: Boolean, onTime: Boolean) = mutate {
        repo.api.logMedication(MedicationRequest(complete, onTime))
    }

    fun submitActivity(did: Boolean, perAdvice: Boolean, days: Int) = mutate {
        repo.api.logActivity(ActivityRequest(did, perAdvice, days))
    }

    fun submitDiet(perAdvice: Boolean, onSchedule: Boolean, limit: Boolean) = mutate {
        repo.api.logDiet(DietRequest(perAdvice, onSchedule, limit))
    }

    fun submitGlucose(timing: String, value: Int) = mutate {
        val res = repo.api.logGlucose(GlucoseRequest(timing, value)).data
        glucoseWarning = res?.takeIf { it.outOfRange }?.warning?.takeIf { it.isNotBlank() }
    }

    /** Dismisses the out-of-range glucose warning after the user acknowledges it. */
    fun clearGlucoseWarning() { glucoseWarning = null }

    private fun mutate(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching {
                block()
                latestGlucose = runCatching { repo.api.glucose().data?.firstOrNull() }.getOrNull()
                repo.api.dashboard().data ?: error("Data tidak tersedia.")
            }.onSuccess { dashboardState = UiState.Success(it) }
                .onFailure { error = netMessage(it) }
        }
    }

    private fun netMessage(t: Throwable): String {
        if (t is HttpException) {
            val body = runCatching { t.response()?.errorBody()?.string() }.getOrNull()
            if (!body.isNullOrBlank()) {
                val msg = runCatching {
                    val obj = Json.parseToJsonElement(body).jsonObject
                    (obj["error"] ?: obj["message"])?.jsonPrimitive?.contentOrNull
                }.getOrNull()
                if (!msg.isNullOrBlank()) return msg
            }
        }
        return t.message?.takeIf { it.isNotBlank() } ?: "Tidak dapat terhubung ke server."
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repo) as T
    }
}
