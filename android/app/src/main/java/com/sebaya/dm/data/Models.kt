package com.sebaya.dm.data

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Standard API envelope returned by the SebayaDM backend. */
@Serializable
data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null,
    val error: String? = null,
)

@Serializable
data class AuthData(
    val token: String,
    val user: User,
)

@Serializable
data class User(
    val id: Int,
    val role: String = "patient",
    @SerialName("nama_lengkap") val namaLengkap: String = "",
    val usia: Int = 0,
    @SerialName("jenis_kelamin") val jenisKelamin: String = "L",
    val pendidikan: String = "",
    val pekerjaan: String = "",
    val username: String = "",
    val email: String = "",
    @SerialName("group_id") val groupId: Int? = null,
    @SerialName("study_arm") val studyArm: String = "",
)

@Serializable
data class RegisterRequest(
    @SerialName("nama_lengkap") val namaLengkap: String,
    val usia: Int,
    @SerialName("jenis_kelamin") val jenisKelamin: String,
    val pendidikan: String,
    val pekerjaan: String,
    val username: String,
    val password: String,
    val email: String = "",
)

@Serializable
data class LoginRequest(val username: String, val password: String)

@Serializable
data class ForgotPasswordRequest(val email: String)

@Serializable
data class DeviceTokenRequest(
    val token: String,
    val platform: String = "android",
)

@Serializable
data class UpdateProfileRequest(
    @SerialName("nama_lengkap") val namaLengkap: String,
    val usia: Int,
    @SerialName("jenis_kelamin") val jenisKelamin: String,
    val pendidikan: String,
    val pekerjaan: String,
    val email: String = "",
)

@Serializable
data class ChangePasswordRequest(
    @SerialName("current_password") val currentPassword: String,
    @SerialName("new_password") val newPassword: String,
)

@Serializable
data class PillarState(
    val logged: Boolean = false,
    @SerialName("taken_complete") val takenComplete: Boolean = false,
    @SerialName("taken_on_time") val takenOnTime: Boolean = false,
    @SerialName("did_activity") val didActivity: Boolean = false,
    @SerialName("per_doctor_advice") val perDoctorAdvice: Boolean = false,
    @SerialName("on_schedule") val onSchedule: Boolean = false,
)

@Serializable
data class Dashboard(
    val date: String = "",
    val medication: PillarState = PillarState(),
    val activity: PillarState = PillarState(),
    val diet: PillarState = PillarState(),
)

@Serializable
data class MedicationRequest(
    @SerialName("taken_complete") val takenComplete: Boolean,
    @SerialName("taken_on_time") val takenOnTime: Boolean,
)

@Serializable
data class ActivityRequest(
    @SerialName("did_activity") val didActivity: Boolean,
    @SerialName("per_doctor_advice") val perDoctorAdvice: Boolean,
)

@Serializable
data class DietRequest(
    @SerialName("per_doctor_advice") val perDoctorAdvice: Boolean,
    @SerialName("on_schedule") val onSchedule: Boolean,
)

@Serializable
data class GlucoseRequest(
    val timing: String,
    @SerialName("value_mgdl") val valueMgdl: Int,
)

@Serializable
data class GlucoseLog(
    val id: Int = 0,
    @SerialName("measured_at") val measuredAt: String = "",
    val timing: String = "",
    @SerialName("value_mgdl") val valueMgdl: Int = 0,
)

@Serializable
data class MedicationLog(
    val id: Int = 0,
    @SerialName("log_date") val logDate: String = "",
    @SerialName("taken_complete") val takenComplete: Boolean = false,
    @SerialName("taken_on_time") val takenOnTime: Boolean = false,
)

@Serializable
data class ActivityLog(
    val id: Int = 0,
    @SerialName("log_date") val logDate: String = "",
    @SerialName("did_activity") val didActivity: Boolean = false,
    @SerialName("per_doctor_advice") val perDoctorAdvice: Boolean = false,
)

@Serializable
data class DietLog(
    val id: Int = 0,
    @SerialName("log_date") val logDate: String = "",
    @SerialName("per_doctor_advice") val perDoctorAdvice: Boolean = false,
    @SerialName("on_schedule") val onSchedule: Boolean = false,
)

@Serializable
data class ArticleSummary(
    val id: Int,
    val title: String,
    val slug: String,
    val category: String = "",
    @SerialName("cover_image_path") val coverImagePath: String = "",
    val excerpt: String = "",
)

@Serializable
data class Article(
    val id: Int,
    val title: String,
    val slug: String,
    val category: String = "",
    @SerialName("cover_image_path") val coverImagePath: String = "",
    @SerialName("cover_width") val coverWidth: Int = 0,
    @SerialName("cover_height") val coverHeight: Int = 0,
    @SerialName("body_html") val bodyHtml: String = "",
)

@Serializable
data class LeaderboardEntry(
    @SerialName("user_id") val userId: Int,
    @SerialName("nama_lengkap") val namaLengkap: String,
    @SerialName("compliant_days") val compliantDays: Int,
    @SerialName("score_percent") val scorePercent: Int,
    val rank: Int,
    @SerialName("is_me") val isMe: Boolean = false,
)
