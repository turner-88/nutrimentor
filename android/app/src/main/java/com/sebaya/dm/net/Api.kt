package com.sebaya.dm.net

import com.sebaya.dm.data.ActivityRequest
import com.sebaya.dm.data.ApiResponse
import com.sebaya.dm.data.Article
import com.sebaya.dm.data.ArticleSummary
import com.sebaya.dm.data.AuthData
import com.sebaya.dm.data.Dashboard
import com.sebaya.dm.data.DietRequest
import com.sebaya.dm.data.GlucoseLog
import com.sebaya.dm.data.GlucoseRequest
import com.sebaya.dm.data.LeaderboardEntry
import com.sebaya.dm.data.LoginRequest
import com.sebaya.dm.data.MedicationRequest
import com.sebaya.dm.data.RegisterRequest
import com.sebaya.dm.data.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path

/** SebayaDM REST API surface consumed by the app. */
interface Api {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthData>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthData>

    @GET("me")
    suspend fun me(): ApiResponse<User>

    @GET("dashboard")
    suspend fun dashboard(): ApiResponse<Dashboard>

    @POST("logs/medication")
    suspend fun logMedication(@Body body: MedicationRequest): ApiResponse<Unit>

    @POST("logs/activity")
    suspend fun logActivity(@Body body: ActivityRequest): ApiResponse<Unit>

    @POST("logs/diet")
    suspend fun logDiet(@Body body: DietRequest): ApiResponse<Unit>

    @POST("logs/glucose")
    suspend fun logGlucose(@Body body: GlucoseRequest): ApiResponse<Unit>

    @GET("logs/glucose")
    suspend fun glucose(): ApiResponse<List<GlucoseLog>>

    @GET("education")
    suspend fun education(): ApiResponse<List<ArticleSummary>>

    @GET("education/{slug}")
    suspend fun article(@Path("slug") slug: String): ApiResponse<Article>

    @GET("leaderboard")
    suspend fun leaderboard(): ApiResponse<List<LeaderboardEntry>>
}
