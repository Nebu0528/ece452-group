package ca.uwaterloo.ece452.discoveruwaterloo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.uwaterloo.ece452.discoveruwaterloo.data.*
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.RetrofitClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = EventRepository(
        RetrofitClient.api,
        EventDatabase.getInstance(app)
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private var _token: String? = null

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    val filteredEvents: StateFlow<List<Event>> = _events
        .map { list -> list.filter { it.status == EventStatus.APPROVED || _currentUser.value?.role == UserRole.ADMIN } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingEvents: StateFlow<List<Event>> = _events
        .map { list -> list.filter { it.status == EventStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _plannerEvents = MutableStateFlow<List<Event>>(emptyList())
    val plannerEvents: StateFlow<List<Event>> = _plannerEvents

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Observe local Room cache
        viewModelScope.launch {
            repository.getEvents().collect { _events.value = it }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val token = repository.login(email, password)
                _token = token
                // Decode user id from JWT sub claim
                val userId = decodeUserId(token)
                _currentUser.value = repository.getUser(userId, token)
                repository.refreshEvents()
            }.onSuccess { onSuccess() }
             .onFailure { onError(it.message ?: "Login failed") }
        }
    }

    fun register(name: String, email: String, password: String, role: UserRole, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val user = repository.register(name, email, password, role)
                // Auto-login after register
                login(email, password, onSuccess, onError)
                return@launch
            }.onFailure { onError(it.message ?: "Registration failed") }
        }
    }

    fun logout() {
        _currentUser.value = null
        _token = null
        _plannerEvents.value = emptyList()
    }

    fun createEvent(name: String, description: String?, location: String?, tagIds: List<Int>, onError: (String) -> Unit) {
        val token = _token ?: return onError("Not logged in")
        viewModelScope.launch {
            runCatching { repository.createEvent(name, description, location, tagIds, token) }
                .onSuccess { repository.refreshEvents() }
                .onFailure { onError(it.message ?: "Failed to create event") }
        }
    }

    fun approveEvent(eventId: Int, onError: (String) -> Unit) {
        val token = _token ?: return onError("Not logged in")
        viewModelScope.launch {
            runCatching { repository.reviewEvent(eventId, token) }
                .onSuccess { repository.refreshEvents() }
                .onFailure { onError(it.message ?: "Failed to approve event") }
        }
    }

    fun rejectEvent(eventId: Int, onError: (String) -> Unit) {
        val token = _token ?: return onError("Not logged in")
        viewModelScope.launch {
            runCatching { repository.deleteEvent(eventId, token) }
                .onSuccess { repository.refreshEvents() }
                .onFailure { onError(it.message ?: "Failed to reject event") }
        }
    }

    fun addToPlanner(event: Event) {
        if (_plannerEvents.value.none { it.id == event.id })
            _plannerEvents.value = _plannerEvents.value + event
    }

    fun removeFromPlanner(event: Event) {
        _plannerEvents.value = _plannerEvents.value.filter { it.id != event.id }
    }

    fun toggleTag(tagId: Int) { /* TODO Person 3: filter logic */ }

    // Decode user ID from JWT payload (no library needed — just base64 the middle segment)
    private fun decodeUserId(token: String): Int {
        val payload = token.split(".")[1]
        val decoded = String(android.util.Base64.decode(payload, android.util.Base64.URL_SAFE))
        val sub = Regex("\"sub\":\"(\\d+)\"").find(decoded)?.groupValues?.get(1)
        return sub?.toInt() ?: throw IllegalStateException("Invalid token")
    }
}
