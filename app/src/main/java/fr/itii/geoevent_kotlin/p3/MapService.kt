package fr.itii.geoevent_kotlin.p3

import android.view.ViewGroup

interface MapService {

    fun showMap(container: ViewGroup)

    fun centerOn(lat: Double, lon: Double, zoom: Double = 15.0)

    fun addMarker(
        lat: Double,
        lon: Double,
        title: String,
        description: String,
        authorEmail: String,
        eventId: String,
        userId: String
    )

    fun clearMarkers()

    fun onResume()

    fun onPause()

    fun saveState(): MapState

    fun restoreState(state: MapState)

    fun setOnMapClickListener(listener: (lat: Double, lon: Double, x: Float, y: Float) -> Unit)

    fun setOnMarkerClickListener(
        listener: (eventId: String, title: String, description: String, authorEmail: String, userId: String, x: Float, y: Float) -> Unit
    )

    fun updateMyLocation(lat: Double, lon: Double)
}

data class MapState(
    val latitude: Double = 48.8566,
    val longitude: Double = 2.3522,
    val zoom: Double = 10.0
)
