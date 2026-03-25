package fr.itii.geoevent_kotlin.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fr.itii.geoevent_kotlin.data.model.Event
import fr.itii.geoevent_kotlin.data.repository.EventRepository
import fr.itii.geoevent_kotlin.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

/**
 * ViewModel de la carte principale.
 *
 * Observe la liste d'événements depuis [EventRepository] et expose l'état
 * via [eventsState]. N'importe jamais Firebase directement.
 *
 * @param repository Dépôt injecté via [fr.itii.geoevent_kotlin.ui.common.ViewModelFactory].
 */
class MapViewModel(private val repository: EventRepository) : ViewModel() {

    private val _eventsState = MutableStateFlow<UiState<List<Event>>>(UiState.Loading)

    /**
     * Flux d'état exposé à [MainActivity].
     * Émet [UiState.Loading], [UiState.Success] ou [UiState.Error].
     */
    val eventsState: StateFlow<UiState<List<Event>>> = _eventsState

    private val _deleteState = MutableStateFlow<UiState<Unit>?>(null)
    /** Flux d'état pour la suppression d'un événement depuis la carte. */
    val deleteState: StateFlow<UiState<Unit>?> = _deleteState.asStateFlow()

    init {
        loadEvents()
    }

    /**
     * Supprime un événement par son identifiant Firestore.
     * Accessible depuis [MainActivity] via la bulle de marqueur.
     */
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            repository.deleteEvent(eventId)
                .onSuccess { _deleteState.value = UiState.Success(Unit) }
                .onFailure { e -> _deleteState.value = UiState.Error(e.message ?: "Erreur") }
        }
    }

    /**
     * Souscrit au flux Firestore en temps réel.
     * Toute modification de la collection `events` déclenche une nouvelle émission.
     */
    private fun loadEvents() {
        viewModelScope.launch {
            repository.getEvents()
                .catch { e ->
                    _eventsState.value = UiState.Error(e.message ?: "Erreur inconnue")
                }
                .collect { events ->
                    _eventsState.value = UiState.Success(events)
                }
        }
    }
}
