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
     * @param lat   Latitude du marqueur.
     * @param lon   Longitude du marqueur.
     * @param title Libellé affiché au tap.
     */
    fun addMarker(lat: Double, lon: Double, title: String)

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
