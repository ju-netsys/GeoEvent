package fr.itii.geoevent_kotlin.data.repository

import fr.itii.geoevent_kotlin.data.model.Event
import kotlinx.coroutines.flow.Flow

/**
 * Interface du dépôt d'événements.
 *
 * Les ViewModels dépendent exclusivement de cette interface, jamais de son
 * implémentation concrète [FirestoreEventRepository]. Cela garantit la
 * modularité requise par l'évaluation : on peut substituer Firestore par
 * n'importe quel autre backend sans modifier une seule ligne de ViewModel.
 */
interface EventRepository {

    /**
     * Retourne un flux réactif de la liste d'événements.
     * Le flux émet une nouvelle liste à chaque modification Firestore.
     */
    fun getEvents(): Flow<List<Event>>

    /**
     * Ajoute un événement dans le backend.
     * @return [Result.success] si l'écriture a réussi, [Result.failure] sinon.
     */
    suspend fun addEvent(event: Event): Result<Unit>

    /**
     * Supprime l'événement identifié par [eventId].
     * @return [Result.success] si la suppression a réussi, [Result.failure] sinon.
     */
    suspend fun deleteEvent(eventId: String): Result<Unit>
}
