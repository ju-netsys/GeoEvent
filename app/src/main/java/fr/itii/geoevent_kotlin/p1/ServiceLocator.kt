package fr.itii.geoevent_kotlin.p1

object ServiceLocator {

    private val firestoreDataSource: FirestoreDataSource by lazy {
        FirestoreDataSource()
    }

    val eventRepository: EventRepository by lazy {
        FirestoreEventRepository(firestoreDataSource)
    }
}
