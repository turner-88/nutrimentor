package com.nutrimentor.dm.net

import com.nutrimentor.dm.data.ActivityLog
import com.nutrimentor.dm.data.ActivityRequest
import com.nutrimentor.dm.data.ApiResponse
import com.nutrimentor.dm.data.Article
import com.nutrimentor.dm.data.ArticleSummary
import com.nutrimentor.dm.data.AuthData
import com.nutrimentor.dm.data.ChangePasswordRequest
import com.nutrimentor.dm.data.Dashboard
import com.nutrimentor.dm.data.DeviceTokenRequest
import com.nutrimentor.dm.data.DietLog
import com.nutrimentor.dm.data.DietRequest
import com.nutrimentor.dm.data.ForgotPasswordRequest
import com.nutrimentor.dm.data.GlucoseLog
import com.nutrimentor.dm.data.GlucoseRequest
import com.nutrimentor.dm.data.GlucoseResult
import com.nutrimentor.dm.data.LeaderboardEntry
import com.nutrimentor.dm.data.LoginRequest
import com.nutrimentor.dm.data.MedicationLog
import com.nutrimentor.dm.data.MedicationRequest
import com.nutrimentor.dm.data.RegisterRequest
import com.nutrimentor.dm.data.UpdateProfileRequest
import com.nutrimentor.dm.data.User
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

/** NutriMentor REST API surface consumed by the app. */
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
    suspend fun logGlucose(@Body body: GlucoseRequest): ApiResponse<GlucoseResult>

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

    @POST("education/{slug}/read")
    suspend fun markArticleRead(@Path("slug") slug: String): ApiResponse<Unit>

    @GET("leaderboard")
    suspend fun leaderboard(): ApiResponse<List<LeaderboardEntry>>

    @POST("me/device-token")
    suspend fun registerDeviceToken(@Body body: DeviceTokenRequest): ApiResponse<Unit>

    @HTTP(method = "DELETE", path = "me/device-token", hasBody = true)
    suspend fun unregisterDeviceToken(@Body body: DeviceTokenRequest): ApiResponse<Unit>
}
