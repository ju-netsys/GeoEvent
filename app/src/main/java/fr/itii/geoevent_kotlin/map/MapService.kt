package fr.itii.geoevent_kotlin.map

import android.view.ViewGroup

/**
 * Interface abstraite pour le service cartographique.
 *
 * [fr.itii.geoevent_kotlin.ui.map.MainActivity] n'importe jamais osmdroid directement —
 * elle passe uniquement par cette interface. Cela permet de remplacer OSM par
 * Google Maps (ou toute autre lib) sans toucher au reste du code.
 */
interface MapService {

    /**
     * Initialise et affiche la carte dans le [container] fourni.
     * Doit être appelé une seule fois, après vérification des permissions.
     */
    fun showMap(container: ViewGroup)

    /**
     * Centre la carte sur les coordonnées données.
     * @param lat Latitude (degrés décimaux).
     * @param lon Longitude (degrés décimaux).
     * @param zoom Niveau de zoom (défaut : 15).
     */
    fun centerOn(lat: Double, lon: Double, zoom: Double = 15.0)

    /**
     * Ajoute un marqueur sur la carte.
     * @param lat     Latitude du marqueur.
     * @param lon     Longitude du marqueur.
     * @param title   Libellé de l'événement.
     * @param eventId Identifiant Firestore de l'événement.
     * @param userId  UID du propriétaire de l'événement.
     */
    fun addMarker(lat: Double, lon: Double, title: String, eventId: String, userId: String)

    /** Supprime tous les marqueurs de la carte. */
    fun clearMarkers()

    /** À appeler dans [android.app.Activity.onResume] pour reprendre les tuiles. */
    fun onResume()

    /** À appeler dans [android.app.Activity.onPause] pour libérer les ressources. */
    fun onPause()

    /** Sauvegarde le centre et le zoom actuels pour [restoreState]. */
    fun saveState(): MapState

    /** Restaure une position et un zoom précédemment sauvegardés. */
    fun restoreState(state: MapState)

    /**
     * Enregistre un callback appelé quand l'utilisateur tape sur la carte (zone vide).
     * @param listener reçoit (latitude, longitude, x, y) en coordonnées relatives à la vue carte.
     */
    fun setOnMapClickListener(listener: (lat: Double, lon: Double, x: Float, y: Float) -> Unit)

    /**
     * Enregistre un callback appelé quand l'utilisateur tape sur un marqueur existant.
     * @param listener reçoit (eventId, title, userId, x, y) — x/y en coordonnées fenêtre.
     */
    fun setOnMarkerClickListener(listener: (eventId: String, title: String, userId: String, x: Float, y: Float) -> Unit)
}

/**
 * Données de sauvegarde de l'état de la carte.
 *
 * Persiste le centre (lat/lon) et le niveau de zoom, utilisés dans
 * [android.app.Activity.onSaveInstanceState] / [android.app.Activity.onCreate].
 */
data class MapState(
    val latitude: Double = 48.8566,   // Paris par défaut
    val longitude: Double = 2.3522,
    val zoom: Double = 10.0
)
