package com.sebaya.dm.net

import com.sebaya.dm.data.ActivityLog
import com.sebaya.dm.data.ActivityRequest
import com.sebaya.dm.data.ApiResponse
import com.sebaya.dm.data.Article
import com.sebaya.dm.data.ArticleSummary
import com.sebaya.dm.data.AuthData
import com.sebaya.dm.data.ChangePasswordRequest
import com.sebaya.dm.data.Dashboard
import com.sebaya.dm.data.DeviceTokenRequest
import com.sebaya.dm.data.DietLog
import com.sebaya.dm.data.DietRequest
import com.sebaya.dm.data.ForgotPasswordRequest
import com.sebaya.dm.data.GlucoseLog
import com.sebaya.dm.data.GlucoseRequest
import com.sebaya.dm.data.LeaderboardEntry
import com.sebaya.dm.data.LoginRequest
import com.sebaya.dm.data.MedicationLog
import com.sebaya.dm.data.MedicationRequest
import com.sebaya.dm.data.RegisterRequest
import com.sebaya.dm.data.UpdateProfileRequest
import com.sebaya.dm.data.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** SebayaDM REST API surface consumed by the app. */
interface Api {
    @POST("auth/register")
    suspend fun register(@Body body: RegisterRequest): ApiResponse<AuthData>

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequest): ApiResponse<AuthData>

    @POST("auth/forgot-password")
    suspend fun forgotPassword(@Body body: ForgotPasswordRequest): ApiResponse<Unit>

    @GET("me")
    suspend fun me(): ApiResponse<User>

    @PUT("me")
    suspend fun updateProfile(@Body body: UpdateProfileRequest): ApiResponse<User>

    @POST("me/password")
    suspend fun changePassword(@Body body: ChangePasswordRequest): ApiResponse<Unit>

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

    @GET("logs/medication")
    suspend fun medicationHistory(): ApiResponse<List<MedicationLog>>

    @GET("logs/activity")
    suspend fun activityHistory(): ApiResponse<List<ActivityLog>>

    @GET("logs/diet")
    suspend fun dietHistory(): ApiResponse<List<DietLog>>

    @GET("education")
    suspend fun education(): ApiResponse<List<ArticleSummary>>

    @GET("education/{slug}")
    suspend fun article(@Path("slug") slug: String): ApiResponse<Article>

    @GET("leaderboard")
    suspend fun leaderboard(): ApiResponse<List<LeaderboardEntry>>

    @POST("me/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest): ApiResponse<Unit>

    @HTTP(method = "DELETE", path = "me/device-token", hasBody = true)
    suspend fun unregisterDeviceToken(@Body body: DeviceTokenRequest): ApiResponse<Unit>
}
