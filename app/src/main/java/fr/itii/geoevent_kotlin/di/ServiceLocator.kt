package fr.itii.geoevent_kotlin.di

import fr.itii.geoevent_kotlin.data.remote.FirestoreDataSource
import fr.itii.geoevent_kotlin.data.repository.EventRepository
import fr.itii.geoevent_kotlin.data.repository.FirestoreEventRepository

/**
 * Conteneur de dépendances léger (injection manuelle, sans Hilt/Dagger).
 *
 * Toutes les dépendances sont instanciées en lazy et partagées dans toute
 * l'application : une seule instance de [FirestoreDataSource] → un seul
 * listener Firestore → pas de doublons de snapshots.
 *
 * Usage dans les Activities / Fragments :
 * ```kotlin
 * private val viewModel: MapViewModel by viewModels {
 *     ViewModelFactory { MapViewModel(ServiceLocator.eventRepository) }
 * }
 * ```
 */
object ServiceLocator {

    private val firestoreDataSource: FirestoreDataSource by lazy {
        FirestoreDataSource()
    }

    /** Dépôt d'événements partagé — utiliser ce singleton dans tous les écrans. */
    val eventRepository: EventRepository by lazy {
        FirestoreEventRepository(firestoreDataSource)
    }
}
