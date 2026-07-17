package ca.uwaterloo.ece452.discoveruwaterloo

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ca.uwaterloo.ece452.discoveruwaterloo.data.*
import ca.uwaterloo.ece452.discoveruwaterloo.data.api.RetrofitClient
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.Calendar

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val repository = EventRepository(
        RetrofitClient.api,
        EventDatabase.getInstance(app)
    )

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private var _token: String? = null

    private val _availableTags = MutableStateFlow<List<Tag>>(emptyList())
    val availableTags: StateFlow<List<Tag>> = _availableTags

    private val _selectedTags = MutableStateFlow<Set<Int>>(emptySet())
    val selectedTags: StateFlow<Set<Int>> = _selectedTags

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = combine(_events, _availableTags) { events, tags ->
        events.map { event ->
            if (event.tags.any { it.name.isEmpty() }) {
                event.copy(tags = event.tags.map { t -> tags.find { it.id == t.id } ?: t })
            } else event
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val filteredEvents: StateFlow<List<Event>> = combine(events, _currentUser, _selectedTags, _searchQuery) { events, user, selectedTags, query ->
        events.filter { event ->
            val matchesRole = event.status == EventStatus.APPROVED || user?.role == UserRole.ADMIN
            val matchesTags = selectedTags.isEmpty() || event.tags.any { it.id in selectedTags }
            val matchesQuery = query.isBlank() || event.name.contains(query, ignoreCase = true)
            matchesRole && matchesTags && matchesQuery
        }
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val pendingEvents: StateFlow<List<Event>> = _events
        .map { list -> list.filter { it.status == EventStatus.PENDING } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _plannerEvents = MutableStateFlow<List<Event>>(emptyList())
    val plannerEvents: StateFlow<List<Event>> = _plannerEvents

    private val _selectedPlannerDate = MutableStateFlow(Calendar.getInstance())
    val selectedPlannerDate: StateFlow<Calendar> = _selectedPlannerDate

    val plannerEventsForSelectedDay: StateFlow<List<Event>> =
        combine(_plannerEvents, _selectedPlannerDate) { events, day ->
            events.filter { it.startCalendar()?.isSameDay(day) == true }
                .sortedBy { it.startTime }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        // Observe local Room cache
        viewModelScope.launch {
            repository.getEvents().collect { _events.value = it }
        }
        fetchTags()
    }

    private fun fetchTags() {
        viewModelScope.launch {
            runCatching { repository.getTags() }
                .onSuccess { 
                    _availableTags.value = it
                    // Trigger a refresh of events to ensure tags are mapped
                    _events.value = _events.value.toList()
                }
        }
    }

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                val token = repository.login(email, password)
                _token = token
                val userId = decodeUserId(token)
                _currentUser.value = repository.getUser(userId, token)
                fetchTags()
                repository.refreshEvents()
            }.onSuccess { onSuccess() }
             .onFailure { onError(parseError(it, "Login failed")) }
        }
    }

    fun register(name: String, email: String, password: String, role: UserRole, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching {
                repository.register(name, email, password, role)
            }.onSuccess { onSuccess() }
             .onFailure { onError(parseError(it, "Registration failed")) }
        }
    }

    fun verifyEmail(email: String, code: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.verifyEmail(email, code) }
                .onSuccess { onSuccess() }
                .onFailure { onError(parseError(it, "Verification failed")) }
        }
    }

    fun resendCode(email: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit) {
        viewModelScope.launch {
            runCatching { repository.resendCode(email) }
                .onSuccess { onSuccess() }
                .onFailure { onError(parseError(it, "Failed to resend code")) }
        }
    }

    fun logout() {
        _currentUser.value = null
        _token = null
        _plannerEvents.value = emptyList()
    }

    fun createEvent(name: String, description: String?, location: String?, lat: Double?, lng: Double?, startTime: String, duration: Int, tagIds: List<Int>, onError: (String) -> Unit) {
        val token = _token ?: return onError("Not logged in")
        viewModelScope.launch {
            runCatching { repository.createEvent(name, description, location, lat, lng, startTime, duration, tagIds, token) }
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

    fun setPlannerDate(date: Calendar) {
        _selectedPlannerDate.value = date
    }

    fun stepPlannerDate(days: Int) {
        _selectedPlannerDate.value = (_selectedPlannerDate.value.clone() as Calendar)
            .apply { add(Calendar.DAY_OF_YEAR, days) }
    }

    fun toggleTag(tagId: Int) {
        if (_selectedTags.value.contains(tagId)) {
            _selectedTags.value = _selectedTags.value - tagId
        } else {
            _selectedTags.value = _selectedTags.value + tagId
        }
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    // Extract FastAPI's `detail` field from HTTP error responses
    private fun parseError(t: Throwable, fallback: String): String {
        return try {
            val body = (t as? retrofit2.HttpException)?.response()?.errorBody()?.string()
            val detail = body?.let { org.json.JSONObject(it).optString("detail") }
            if (!detail.isNullOrBlank()) detail else fallback
        } catch (e: Exception) { fallback }
    }

    // Decode user ID from JWT payload (no library needed — just base64 the middle segment)
    private fun decodeUserId(token: String): Int {
        val payload = token.split(".")[1]
        val decoded = String(android.util.Base64.decode(payload, android.util.Base64.URL_SAFE))
        val sub = Regex("\"sub\":\"(\\d+)\"").find(decoded)?.groupValues?.get(1)
        return sub?.toInt() ?: throw IllegalStateException("Invalid token")
    }
}
