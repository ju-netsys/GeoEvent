package fr.itii.geoevent_kotlin.p1

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class FirestoreDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("events")

    fun getEventsFlow(): Flow<List<Event>> = callbackFlow {
        val registration = eventsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    if (error.code == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                        close()
                    } else {
                        close(error)
                    }
                    return@addSnapshotListener
                }
                val events = snapshot?.toObjects(Event::class.java) ?: emptyList()
                trySend(events)
            }
        awaitClose { registration.remove() }
    }

    suspend fun addEvent(event: Event): Result<Unit> = runCatching {
        eventsCollection.add(event).await()
        Unit
    }

    suspend fun updateEvent(event: Event): Result<Unit> = runCatching {
        eventsCollection.document(event.id).set(event).await()
        Unit
    }

    suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        eventsCollection.document(eventId).delete().await()
        Unit
    }
}
