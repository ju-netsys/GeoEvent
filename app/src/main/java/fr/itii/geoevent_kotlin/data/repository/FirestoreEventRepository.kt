package fr.itii.geoevent_kotlin.data.repository

import fr.itii.geoevent_kotlin.data.model.Event
import fr.itii.geoevent_kotlin.data.remote.FirestoreDataSource
import kotlinx.coroutines.flow.Flow

/**
 * Implémentation Firestore de [EventRepository].
 *
 * Injectée manuellement dans les ViewModels via une ViewModelFactory — pas de Hilt/Dagger.
 * Cette séparation garantit que les ViewModels n'importent jamais Firebase directement.
 *
 * @param dataSource Source de données Firestore sous-jacente.
 */
class FirestoreEventRepository(
    private val dataSource: FirestoreDataSource = FirestoreDataSource()
) : EventRepository {

    override fun getEvents(): Flow<List<Event>> = dataSource.getEventsFlow()

    override suspend fun addEvent(event: Event): Result<Unit> = dataSource.addEvent(event)

    override suspend fun deleteEvent(eventId: String): Result<Unit> = dataSource.deleteEvent(eventId)
}
