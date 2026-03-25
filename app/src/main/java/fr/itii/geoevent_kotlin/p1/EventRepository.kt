package fr.itii.geoevent_kotlin.p1

import kotlinx.coroutines.flow.Flow

interface EventRepository {
    fun getEvents(): Flow<List<Event>>
    suspend fun addEvent(event: Event): Result<Unit>
    suspend fun updateEvent(event: Event): Result<Unit>
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
