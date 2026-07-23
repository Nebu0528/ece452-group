package ca.uwaterloo.ece452.discoveruwaterloo.data

import java.text.SimpleDateFormat
import java.util.Locale

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
    val startTime: String? = null,
    val duration: Int? = null,
    val userId: Int,
    val reviewerId: Int?,
    val status: EventStatus = EventStatus.PENDING,
    val organizerName: String? = null,
    val tags: List<Tag> = emptyList(),
    val attendeeIds: List<Int> = emptyList()
) {
    val displayDateTime: String?
        get() = startTime?.let {
            runCatching {
                val input = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
                val output = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
                output.format(input.parse(it)!!)
            }.getOrNull()
        } ?: date
}

data class User(
    val id: Int,
    val name: String,
    val email: String,
    val role: UserRole
)
