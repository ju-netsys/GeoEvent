package fr.itii.geoevent_kotlin.ui.event

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.itii.geoevent_kotlin.data.repository.EventRepository

/**
 * Factory pour [EventViewModel].
 *
 * Injection manuelle de [EventRepository] — pas de Hilt/Dagger.
 *
 * @param repository Instance d'[EventRepository] à injecter.
 */
class EventViewModelFactory(
    private val repository: EventRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EventViewModel::class.java)) {
            return EventViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
    }
}
