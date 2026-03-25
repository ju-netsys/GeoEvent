package fr.itii.geoevent_kotlin.p3

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.itii.geoevent_kotlin.common.UiState
import fr.itii.geoevent_kotlin.p1.Event
import fr.itii.geoevent_kotlin.p1.EventRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class MapViewModel(private val repository: EventRepository) : ViewModel() {

    private val _eventsState = MutableStateFlow<UiState<List<Event>>>(UiState.Loading)
    val eventsState: StateFlow<UiState<List<Event>>> = _eventsState

    private val _deleteState = MutableStateFlow<UiState<Unit>?>(null)
    val deleteState: StateFlow<UiState<Unit>?> = _deleteState.asStateFlow()

    init {
        loadEvents()
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

    private fun loadEvents() {
        viewModelScope.launch {
            repository.getEvents()
                .catch { e -> _eventsState.value = UiState.Error(e.message ?: "Erreur inconnue") }
                .collect { events -> _eventsState.value = UiState.Success(events) }
        }
    }
}
