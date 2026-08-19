package com.sebaya.dm.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.sebaya.dm.data.ActivityRequest
import com.sebaya.dm.data.Article
import com.sebaya.dm.data.ArticleSummary
import com.sebaya.dm.data.Dashboard
import com.sebaya.dm.data.DietRequest
import com.sebaya.dm.data.GlucoseLog
import com.sebaya.dm.data.GlucoseRequest
import com.sebaya.dm.data.LeaderboardEntry
import com.sebaya.dm.data.MedicationRequest
import com.sebaya.dm.data.RegisterRequest
import com.sebaya.dm.data.Repository
import com.sebaya.dm.data.User
import kotlinx.coroutines.launch

class AppViewModel(val repo: Repository) : ViewModel() {

    var loggedIn by mutableStateOf(repo.isLoggedIn())
        private set
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)

    var dashboard by mutableStateOf<Dashboard?>(null)
        private set
    var me by mutableStateOf<User?>(null)
        private set
    var articles by mutableStateOf<List<ArticleSummary>>(emptyList())
        private set
    var currentArticle by mutableStateOf<Article?>(null)
        private set
    var leaderboard by mutableStateOf<List<LeaderboardEntry>>(emptyList())
        private set
    var glucose by mutableStateOf<List<GlucoseLog>>(emptyList())
        private set

    fun login(username: String, password: String, onDone: () -> Unit) = run {
        error = null; busy = true
        viewModelScope.launch {
            repo.login(username, password)
                .onSuccess { loggedIn = true; onDone() }
                .onFailure { error = it.message ?: "Login gagal." }
            busy = false
        }
    }

    fun register(req: RegisterRequest, onDone: () -> Unit) = run {
        error = null; busy = true
        viewModelScope.launch {
            repo.register(req)
                .onSuccess { loggedIn = true; onDone() }
                .onFailure { error = it.message ?: "Registrasi gagal." }
            busy = false
        }
    }

    fun logout(onDone: () -> Unit) = viewModelScope.launch {
        repo.logout(); loggedIn = false; onDone()
    }

    fun loadDashboard() = safe { dashboard = repo.api.dashboard().data }
    fun loadMe() = safe { me = repo.api.me().data }
    fun loadArticles() = safe { articles = repo.api.education().data ?: emptyList() }
    fun loadArticle(slug: String) = safe { currentArticle = repo.api.article(slug).data }
    fun loadLeaderboard() = safe { leaderboard = repo.api.leaderboard().data ?: emptyList() }
    fun loadGlucose() = safe { glucose = repo.api.glucose().data ?: emptyList() }

    fun submitMedication(complete: Boolean, onTime: Boolean) =
        safe { repo.api.logMedication(MedicationRequest(complete, onTime)); dashboard = repo.api.dashboard().data }

    fun submitActivity(did: Boolean, perAdvice: Boolean) =
        safe { repo.api.logActivity(ActivityRequest(did, perAdvice)); dashboard = repo.api.dashboard().data }

    fun submitDiet(perAdvice: Boolean, onSchedule: Boolean) =
        safe { repo.api.logDiet(DietRequest(perAdvice, onSchedule)); dashboard = repo.api.dashboard().data }

    fun submitGlucose(timing: String, value: Int, onDone: () -> Unit) = safe {
        repo.api.logGlucose(GlucoseRequest(timing, value))
        glucose = repo.api.glucose().data ?: emptyList()
        onDone()
    }

    private fun safe(block: suspend () -> Unit) {
        viewModelScope.launch {
            runCatching { block() }.onFailure { error = it.message }
        }
    }

    class Factory(private val repo: Repository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T = AppViewModel(repo) as T
    }
}
