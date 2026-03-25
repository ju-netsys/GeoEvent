package fr.itii.geoevent_kotlin.ui.map

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import fr.itii.geoevent_kotlin.data.repository.EventRepository

/**
 * Factory pour [MapViewModel].
 *
 * Injection manuelle du [EventRepository] — pas de Hilt/Dagger.
 * Permet de respecter la règle : les ViewModels ne connaissent pas
 * les implémentations concrètes de leurs dépendances.
 *
 * @param repository Instance d'[EventRepository] à injecter.
 */
class MapViewModelFactory(
    private val repository: EventRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MapViewModel::class.java)) {
            return MapViewModel(repository) as T
        }
        throw IllegalArgumentException("ViewModel inconnu : ${modelClass.name}")
    }
}
