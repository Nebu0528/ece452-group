package ca.uwaterloo.ece452.discoveruwaterloo

import androidx.lifecycle.ViewModel
import ca.uwaterloo.ece452.discoveruwaterloo.data.Event
import ca.uwaterloo.ece452.discoveruwaterloo.data.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppViewModel : ViewModel() {
    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private val _plannerEvents = MutableStateFlow<List<Event>>(emptyList())
    val plannerEvents: StateFlow<List<Event>> = _plannerEvents

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    // TODO Person 1: implement with real API calls
    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        onError("Not implemented yet")
    }

    fun register(name: String, email: String, password: String, role: ca.uwaterloo.ece452.discoveruwaterloo.data.UserRole, onSuccess: () -> Unit, onError: (String) -> Unit) {
        onError("Not implemented yet")
    }

    fun logout() { _currentUser.value = null }

    fun addToPlanner(event: Event) {
        if (_plannerEvents.value.none { it.id == event.id })
            _plannerEvents.value = _plannerEvents.value + event
    }

    fun removeFromPlanner(event: Event) {
        _plannerEvents.value = _plannerEvents.value.filter { it.id != event.id }
    }
}
