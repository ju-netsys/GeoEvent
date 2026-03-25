package fr.itii.geoevent_kotlin.p3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.common.UiState
import fr.itii.geoevent_kotlin.p1.Event
import fr.itii.geoevent_kotlin.p1.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventViewModel(private val repository: EventRepository) : ViewModel() {

    private val _addState = MutableStateFlow<UiState<Unit>?>(null)
    val addState: StateFlow<UiState<Unit>?> = _addState

    private val _deleteState = MutableStateFlow<UiState<Unit>?>(null)
    val deleteState: StateFlow<UiState<Unit>?> = _deleteState

    fun addEvent(title: String, description: String, latitude: Double, longitude: Double) {
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) return

        val event = Event(
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            userId = currentUser.uid,
            authorEmail = currentUser.email ?: "",
            createdAt = Timestamp.now()
        )

        viewModelScope.launch {
            _addState.value = UiState.Loading
            val result = repository.addEvent(event)
            if (result.isSuccess) {
                _addState.value = UiState.Success(Unit)
            } else {
                _addState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Erreur")
            }
        }
    }

    fun updateEvent(original: Event, title: String, description: String, latitude: Double, longitude: Double) {
        val updatedEvent = original.copy(
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude
        )

        viewModelScope.launch {
            _addState.value = UiState.Loading
            val result = repository.updateEvent(updatedEvent)
            if (result.isSuccess) {
                _addState.value = UiState.Success(Unit)
            } else {
                _addState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Erreur")
            }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            val result = repository.deleteEvent(eventId)
            if (result.isSuccess) {
                _deleteState.value = UiState.Success(Unit)
            } else {
                _deleteState.value = UiState.Error(result.exceptionOrNull()?.message ?: "Erreur")
            }
        }
    }
}
