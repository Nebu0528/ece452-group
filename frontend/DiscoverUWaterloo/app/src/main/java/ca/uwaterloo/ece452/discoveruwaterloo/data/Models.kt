package ca.uwaterloo.ece452.discoveruwaterloo.data

enum class UserRole { BASIC, ORGANIZER, ADMIN }

enum class EventStatus { PENDING, APPROVED, REJECTED }

data class Tag(val id: Int, val name: String)

data class EventLocation(val lat: Double, val lng: Double)

data class Event(
    val id: Int,
    val name: String,
    val description: String?,
    val location: String?,
    val locationCoords: EventLocation? = null,
    val date: String?,
    val userId: Int,
    val reviewerId: Int?,
    val status: EventStatus = EventStatus.PENDING,
    val tags: List<Tag> = emptyList()
)

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: UserRole
)
