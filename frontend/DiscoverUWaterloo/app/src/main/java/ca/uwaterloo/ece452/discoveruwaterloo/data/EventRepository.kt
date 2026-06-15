package ca.uwaterloo.ece452.discoveruwaterloo.data

import ca.uwaterloo.ece452.discoveruwaterloo.data.api.ApiService
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.EventCreateRequest
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.EventResponse
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.LoginRequest
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.RegisterRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class EventRepository(private val api: ApiService, private val db: EventDatabase) {

    // Auth
    suspend fun login(email: String, password: String): String =
        api.login(LoginRequest(email, password)).accessToken

    suspend fun register(name: String, email: String, password: String, role: UserRole): User {
        val resp = api.register(RegisterRequest(name, email, password, role.name.lowercase()))
        return User(resp.id, resp.name, resp.email, UserRole.valueOf(resp.role.uppercase()))
    }

    suspend fun getUser(id: Int, token: String): User {
        val resp = api.getUser(id, "Bearer $token")
        return User(resp.id, resp.name, resp.email, UserRole.valueOf(resp.role.uppercase()))
    }

    // Events — fetches from API and caches locally
    suspend fun refreshEvents() {
        val remote = api.getEvents()
        db.eventDao().clearAll()
        db.eventDao().upsertAll(remote.map { it.toEntity() })
    }

    fun getEvents(): Flow<List<Event>> =
        db.eventDao().getAllEvents().map { list -> list.map { it.toEvent() } }

    suspend fun createEvent(name: String, description: String?, location: String?, lat: Double?, lng: Double?, tagIds: List<Int>, token: String): Event =
        api.createEvent(EventCreateRequest(name, description, location, lat, lng, tagIds), "Bearer $token").toEvent()

    suspend fun deleteEvent(id: Int, token: String) = api.deleteEvent(id, "Bearer $token")

    suspend fun reviewEvent(id: Int, token: String): Event = api.reviewEvent(id, "Bearer $token").toEvent()

    suspend fun getTags(): List<Tag> = api.getTags().map { Tag(it.id, it.name) }
}

// Mapping helpers
private fun EventResponse.toEntity() = EventEntity(
    id = id, name = name, description = description, location = location,
    lat = lat, lng = lng, date = null, userId = userId, reviewerId = reviewerId,
    status = if (reviewerId != null) EventStatus.APPROVED.name else EventStatus.PENDING.name,
    tagIds = tags.joinToString(",") { it.id.toString() }
)

private fun EventEntity.toEvent() = Event(
    id = id, name = name, description = description, location = location,
    locationCoords = if (lat != null && lng != null) EventLocation(lat, lng) else null,
    date = date, userId = userId, reviewerId = reviewerId,
    status = EventStatus.valueOf(status),
    tags = tagIds.split(",").filter { it.isNotEmpty() }.map { Tag(it.toInt(), "") }
)

private fun EventResponse.toEvent() = Event(
    id = id, name = name, description = description, location = location,
    locationCoords = if (lat != null && lng != null) EventLocation(lat, lng) else null,
    date = null, userId = userId, reviewerId = reviewerId,
    status = if (reviewerId != null) EventStatus.APPROVED else EventStatus.PENDING,
    tags = tags.map { Tag(it.id, it.name) }
)
