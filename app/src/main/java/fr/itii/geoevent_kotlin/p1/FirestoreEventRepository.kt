package fr.itii.geoevent_kotlin.p1

import kotlinx.coroutines.flow.Flow

class FirestoreEventRepository(
    private val dataSource: FirestoreDataSource = FirestoreDataSource()
) : EventRepository {

    override fun getEvents(): Flow<List<Event>> = dataSource.getEventsFlow()

    override suspend fun addEvent(event: Event): Result<Unit> = dataSource.addEvent(event)

    override suspend fun updateEvent(event: Event): Result<Unit> = dataSource.updateEvent(event)

    override suspend fun deleteEvent(eventId: String): Result<Unit> = dataSource.deleteEvent(eventId)
}
