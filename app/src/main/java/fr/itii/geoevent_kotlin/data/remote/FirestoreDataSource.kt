package fr.itii.geoevent_kotlin.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import fr.itii.geoevent_kotlin.data.model.Event
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Source de données Firestore pour les événements.
 *
 * Cette classe est la seule à importer Firebase dans la couche data.
 * Elle est utilisée exclusivement par [fr.itii.geoevent_kotlin.data.repository.FirestoreEventRepository].
 */
class FirestoreDataSource {

    private val db = FirebaseFirestore.getInstance()
    private val eventsCollection = db.collection("events")

    /**
     * Ouvre un listener Firestore en temps réel et l'expose comme [Flow].
     *
     * Le listener est automatiquement supprimé quand le Flow est annulé
     * (via [awaitClose]), ce qui respecte le cycle de vie Android.
     */
    fun getEventsFlow(): Flow<List<Event>> = callbackFlow {
        val registration = eventsCollection
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val events = snapshot?.toObjects(Event::class.java) ?: emptyList()
                trySend(events)
            }
        awaitClose { registration.remove() }
    }

    /**
     * Ajoute un événement dans Firestore.
     * L'identifiant du document est généré automatiquement par Firestore.
     */
    suspend fun addEvent(event: Event): Result<Unit> = runCatching {
        eventsCollection.add(event).await()
        Unit
    }

    /**
     * Supprime l'événement identifié par [eventId] de Firestore.
     */
    suspend fun deleteEvent(eventId: String): Result<Unit> = runCatching {
        eventsCollection.document(eventId).delete().await()
        Unit
    }
}
