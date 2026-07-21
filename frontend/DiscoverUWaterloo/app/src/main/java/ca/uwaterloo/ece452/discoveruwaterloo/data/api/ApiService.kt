package ca.uwaterloo.ece452.discoveruwaterloo.data.api

import ca.uwaterloo.ece452.discoveruwaterloo.BuildConfig
import com.google.gson.annotations.SerializedName
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

// Request/Response DTOs matching FastAPI schemas
data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, @SerializedName("invite_token") val inviteToken: String?)
data class InviteRequest(val email: String, val role: String)
data class VerifyEmailRequest(val email: String, val code: String)
data class MessageResponse(val message: String)
data class TokenResponse(@SerializedName("access_token") val accessToken: String)
data class UserResponse(val id: Int, val name: String, val email: String, val role: String)
data class TagResponse(val id: Int, val name: String, val description: String?)
data class EventResponse(
    val id: Int, val name: String, val description: String?,
    val location: String?, val lat: Double?, val lng: Double?,
    @SerializedName("user_id") val userId: Int,
    @SerializedName("reviewer_id") val reviewerId: Int?,
    @SerializedName("start_time") val startTime: String? = null,
    val duration: Int? = null,
    val tags: List<TagResponse> = emptyList(),
    @SerializedName("attendee_ids") val attendeeIds: List<Int> = emptyList()
)
data class EventCreateRequest(
    val name: String, val description: String?,
    val location: String?, val lat: Double?, val lng: Double?,
    @SerializedName("start_time") val startTime: String,
    val duration: Int,
    @SerializedName("tag_ids") val tagIds: List<Int> = emptyList()
)

interface ApiService {
    @POST("users/login")
    suspend fun login(@Body request: LoginRequest): TokenResponse

    @POST("users/register")
    suspend fun register(@Body request: RegisterRequest): UserResponse

    @POST("invites/")
    suspend fun sendInvite(@Body request: InviteRequest, @Header("Authorization") token: String): MessageResponse

    @POST("users/verify-email")
    suspend fun verifyEmail(@Body request: VerifyEmailRequest): MessageResponse

    @POST("users/resend-code")
    suspend fun resendCode(@Body request: VerifyEmailRequest): MessageResponse

    @GET("users/{id}")
    suspend fun getUser(@Path("id") id: Int, @Header("Authorization") token: String): UserResponse

    @GET("events/")
    suspend fun getEvents(): List<EventResponse>

    @GET("events/{id}")
    suspend fun getEvent(@Path("id") id: Int): EventResponse

    @POST("events/")
    suspend fun createEvent(@Body request: EventCreateRequest, @Header("Authorization") token: String): EventResponse

    @DELETE("events/{id}")
    suspend fun deleteEvent(@Path("id") id: Int, @Header("Authorization") token: String)

    @PATCH("events/{id}/review")
    suspend fun reviewEvent(@Path("id") id: Int, @Header("Authorization") token: String): EventResponse

    @POST("events/{id}/attend")
    suspend fun attendEvent(@Path("id") id: Int, @Header("Authorization") token: String): EventResponse

    @DELETE("events/{id}/attend")
    suspend fun unattendEvent(@Path("id") id: Int, @Header("Authorization") token: String): EventResponse

    @GET("tags/")
    suspend fun getTags(): List<TagResponse>
}

object RetrofitClient {
    val api: ApiService by lazy {
        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL + "/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
