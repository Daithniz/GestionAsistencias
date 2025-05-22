package com.example.gestion_asistencia.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Body
import retrofit2.http.Path
import retrofit2.http.DELETE
import retrofit2.http.PUT
import com.google.gson.annotations.SerializedName

data class ApiResponse(
    val success: Boolean,
    val body: MessageBody
)

data class MessageBody(
    val message: String,
    val error: String? = null
)

// Modelo para la solicitud de registro
data class RegisterRequest(
    val email: String,
    val pass: String,
    val displayName: String
)

// Modelo para la respuesta de registro
data class RegisterResponse(
    val success: Boolean,
    val body: RegisterBody
)

data class RegisterBody(
    val message: String,
    val user: UserData? = null
)

data class UserData(
    val id: String,
    val email: String,
    val displayName: String,
    val role: String? = null
)

// Modelo para la solicitud de login
data class LoginRequest(
    val email: String,
    val pass: String
)

// Modelo para la respuesta de login
data class LoginResponse(
    val success: Boolean,
    val body: LoginBody
)

data class LoginBody(
    val message: String,
    val error: String? = null,
    val user: UserData? = null,
    val token: String? = null
)

// Modelo para la respuesta de estado de asistencia
data class AttendanceStatusResponse(
    val success: Boolean,
    val body: AttendanceStatusBody
)

data class AttendanceStatusBody(
    val status: String,
    val lastRecord: String? = null
)

// Modelo para la respuesta de perfiles de usuario
data class UserProfilesResponse(
    val success: Boolean,
    val body: UserProfilesBody
)

data class UserProfilesBody(
    val message: String,
    val profiles: List<UserProfile>
)

data class UserProfile(
    @SerializedName("_id")
    val _id: String,
    val email: String,
    val displayName: String,
    val role: String
)

// Modelo para la solicitud de actualización de usuario
data class UpdateUserRequest(
    val _id: String,
    val email: String,
    val displayName: String,
)

// Modelo para la solicitud de eliminación de usuario
data class DeleteUserRequest(
    @SerializedName("_id")
    val _id: String
)

// Modelo para la solicitud de actualización de perfil de usuario
data class UpdateProfileRequest(
    val _id: String,
    val email: String,
    val displayName: String
)

// Modelo para la respuesta de asistencia del día
data class TodayAttendanceResponse(
    val success: Boolean,
    val body: TodayAttendanceBody
)

data class TodayAttendanceBody(
    val hasAttendance: Boolean,
    val attendance: AttendanceData?
)

data class AttendanceData(
    val checkIn: String,
    val checkOut: String?
)

// Modelo para la respuesta del historial de asistencia
data class UserAttendanceHistoryResponse(
    val success: Boolean,
    val body: UserAttendanceHistoryBody
)

data class UserAttendanceHistoryBody(
    val attendance: List<AttendanceRecord>
)

data class AttendanceRecord(
    @SerializedName("_id")
    val id: String,
    val userId: String,
    val date: String,
    val checkIn: String,
    val checkOut: String?,
    val createdAt: String,
    val updatedAt: String
)

// Modelo para la respuesta de historial de asistencia del administrador
data class AdminAttendanceHistoryResponse(
    val success: Boolean,
    val body: AdminAttendanceHistoryBody
)

data class AdminAttendanceHistoryBody(
    val history: List<AdminAttendanceRecord>
)

data class AdminAttendanceRecord(
    @SerializedName("_id")
    val id: String,
    val userId: String,
    val date: String,
    val checkIn: String,
    val checkOut: String?,
    val createdAt: String,
    val updatedAt: String,
    val user: UserProfile
)

interface ApiService {
    @GET("/api/")
    suspend fun testConnection(): Response<ApiResponse>

    @POST("/api/auth/register/")
    suspend fun register(@Body registerRequest: RegisterRequest): Response<RegisterResponse>

    @POST("/api/auth/login/")
    suspend fun login(@Body loginRequest: LoginRequest): Response<LoginResponse>

    @POST("/api/attendance/check-in")
    suspend fun registerCheckIn(): Response<ApiResponse>

    @POST("/api/attendance/check-out")
    suspend fun registerCheckOut(): Response<ApiResponse>

    @GET("/api/attendance/status")
    suspend fun getAttendanceStatus(): Response<AttendanceStatusResponse>

    @GET("/api/profile/all")
    suspend fun getAllProfiles(): Response<UserProfilesResponse>

    @DELETE("/api/profile/admin/delete/{userId}")
    suspend fun deleteUser(@Path("userId") userId: String): Response<ApiResponse>

    @PUT("/api/profile/admin/update")
    suspend fun updateUser(@Body updateRequest: UpdateUserRequest): Response<ApiResponse>

    @PUT("/api/profile/user/update")
    suspend fun updateUserProfile(@Body updateRequest: UpdateProfileRequest): Response<ApiResponse>

    @GET("/api/attendance/today/")
    suspend fun getTodayAttendance(): Response<TodayAttendanceResponse>

    @GET("/api/attendance/user/all/")
    suspend fun getUserAttendanceHistory(): Response<UserAttendanceHistoryResponse>

    @GET("/api/attendance/admin/history")
    suspend fun getAdminAttendanceHistory(): Response<AdminAttendanceHistoryResponse>

    companion object {
        const val BASE_URL = "https://apiasistenciajava.codevalcanos.com"
        
        fun create(): ApiService {
            return ApiConfig.retrofit.create(ApiService::class.java)
        }
    }
} 