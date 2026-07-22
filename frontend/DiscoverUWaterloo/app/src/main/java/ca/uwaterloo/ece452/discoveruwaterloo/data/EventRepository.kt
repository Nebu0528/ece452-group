package ca.uwaterloo.ece452.discoveruwaterloo.data

import ca.uwaterloo.ece452.discoveruwaterloo.data.api.ApiService
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.EventCreateRequest
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.EventResponse
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.InviteRequest
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.LoginRequest
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.RegisterRequest
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.VerifyEmailRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(private val api: ApiService, private val db: EventDatabase) {

    // Auth
    suspend fun login(email: String, password: String): String =
        api.login(LoginRequest(email, password)).accessToken

    suspend fun register(name: String, email: String, password: String, inviteToken: String?): User {
        val resp = api.register(RegisterRequest(name, email, password, inviteToken))
        return User(resp.id, resp.name, resp.email, UserRole.valueOf(resp.role.uppercase()))
    }

    suspend fun sendInvite(email: String, role: String, token: String): String =
        api.sendInvite(InviteRequest(email, role), "Bearer $token").message

    suspend fun verifyEmail(email: String, code: String): String =
        api.verifyEmail(VerifyEmailRequest(email, code)).message

    suspend fun resendCode(email: String): String =
        api.resendCode(VerifyEmailRequest(email, "")).message

    suspend fun getUser(id: Int, token: String): User {
        val resp = api.getUser(id, "Bearer $token")
        return User(resp.id, resp.name, resp.email, UserRole.valueOf(resp.role.uppercase()))
    }

    // Events — fetches from API and caches locally
    suspend fun refreshEvents(token: String? = null) {
        val remote = api.getEvents(token?.let { "Bearer $it" })
        db.eventDao().clearAll()
        db.eventDao().upsertAll(remote.map { it.toEntity() })
    }

    fun getEvents(): Flow<List<Event>> =
        db.eventDao().getAllEvents().map { list -> list.map { it.toEvent() } }

    suspend fun createEvent(name: String, description: String?, location: String?, lat: Double?, lng: Double?, startTime: String, duration: Int, tagIds: List<Int>, token: String): Event =
        api.createEvent(EventCreateRequest(name, description, location, lat, lng, startTime, duration, tagIds), "Bearer $token").toEvent()

    suspend fun deleteEvent(id: Int, token: String) = api.deleteEvent(id, "Bearer $token")

    suspend fun reviewEvent(id: Int, token: String): Event = api.reviewEvent(id, "Bearer $token").toEvent()

    suspend fun rejectEvent(id: Int, token: String): Event = api.rejectEvent(id, "Bearer $token").toEvent()

    suspend fun attendEvent(id: Int, token: String): Event = api.attendEvent(id, "Bearer $token").toEvent()

    suspend fun unattendEvent(id: Int, token: String): Event = api.unattendEvent(id, "Bearer $token").toEvent()

    suspend fun getTags(): List<Tag> = api.getTags().map { Tag(it.id, it.name) }
}

// Mapping helpers
private fun EventResponse.toEntity() = EventEntity(
    id = id, name = name, description = description, location = location,
    lat = lat, lng = lng, date = null, startTime = startTime,
    duration = duration,
    userId = userId, reviewerId = reviewerId,
    status = status.uppercase(),
    tagIds = tags.joinToString(",") { it.id.toString() },
    attendeeIds = attendeeIds.joinToString(",")
)

private fun EventEntity.toEvent() = Event(
    id = id, name = name, description = description, location = location,
    locationCoords = if (lat != null && lng != null) EventLocation(lat, lng) else null,
    date = date, startTime = startTime, duration = duration,
    userId = userId, reviewerId = reviewerId,
    status = runCatching { EventStatus.valueOf(status) }.getOrDefault(EventStatus.PENDING),
    tags = tagIds.split(",").filter { it.isNotBlank() }.map { Tag(it.trim().toInt(), "") },
    attendeeIds = attendeeIds.split(",").filter { it.isNotBlank() }.map { it.trim().toInt() }
)

private fun EventResponse.toEvent() = Event(
    id = id, name = name, description = description, location = location,
    locationCoords = if (lat != null && lng != null) EventLocation(lat, lng) else null,
    date = null, startTime = startTime,
    duration = duration,
    userId = userId, reviewerId = reviewerId,
    status = runCatching { EventStatus.valueOf(status.uppercase()) }.getOrDefault(EventStatus.PENDING),
    tags = tags.map { Tag(it.id, it.name) },
    attendeeIds = attendeeIds
)
