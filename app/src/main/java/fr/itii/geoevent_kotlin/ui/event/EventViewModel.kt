package fr.itii.geoevent_kotlin.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.data.model.Event
import fr.itii.geoevent_kotlin.data.repository.EventRepository
import fr.itii.geoevent_kotlin.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel partagé par [AddEventActivity] et [EventDetailActivity].
 *
 * N'importe jamais Firebase directement pour les opérations sur les événements —
 * passe uniquement par [EventRepository].
 *
 * @param repository Dépôt injecté via [EventViewModelFactory].
 */
class EventViewModel(private val repository: EventRepository) : ViewModel() {

    private val _addState = MutableStateFlow<UiState<Unit>?>(null)
    /** Flux d'état pour l'ajout d'un événement. `null` = état initial. */
    val addState: StateFlow<UiState<Unit>?> = _addState

    private val _deleteState = MutableStateFlow<UiState<Unit>?>(null)
    /** Flux d'état pour la suppression d'un événement. `null` = état initial. */
    val deleteState: StateFlow<UiState<Unit>?> = _deleteState

    /**
     * Crée et persiste un nouvel événement dans Firestore.
     * L'UID de l'auteur est lu depuis [FirebaseAuth] directement ici
     * car c'est une donnée de session, pas une dépendance réseau.
     */
    fun addEvent(title: String, description: String, latitude: Double, longitude: Double) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

        val event = Event(
            title = title,
            description = description,
            latitude = latitude,
            longitude = longitude,
            userId = userId,
            createdAt = Timestamp.now()
        )

        viewModelScope.launch {
            _addState.value = UiState.Loading
            repository.addEvent(event)
                .onSuccess { _addState.value = UiState.Success(Unit) }
                .onFailure { e -> _addState.value = UiState.Error(e.message ?: "Erreur") }
        }
    }

    /**
     * Supprime l'événement identifié par [eventId] via [EventRepository].
     * Firestore vérifie côté serveur que l'utilisateur est le propriétaire.
     */
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            _deleteState.value = UiState.Loading
            repository.deleteEvent(eventId)
                .onSuccess { _deleteState.value = UiState.Success(Unit) }
                .onFailure { e -> _deleteState.value = UiState.Error(e.message ?: "Erreur") }
        }
    }
}
