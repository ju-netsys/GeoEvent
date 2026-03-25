package fr.itii.geoevent_kotlin.map.osm

import android.content.Context
import android.view.ViewGroup
import fr.itii.geoevent_kotlin.map.MapService
import fr.itii.geoevent_kotlin.map.MapState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

/**
 * Implémentation de [MapService] basée sur la bibliothèque osmdroid (OpenStreetMap).
 *
 * Peut être remplacée par une implémentation Google Maps sans modifier
 * [fr.itii.geoevent_kotlin.ui.map.MainActivity].
 *
 * @param context Contexte Android nécessaire à osmdroid et à son cache de tuiles.
 */
class OsmMapService(private val context: Context) : MapService {

    private lateinit var mapView: MapView

    init {
        // osmdroid exige un User-Agent valide pour télécharger les tuiles OSM.
        Configuration.getInstance().userAgentValue = context.packageName
    }

    /**
     * Crée le [MapView], configure les sources de tuiles et l'ajoute dans [container].
     * Appelé une seule fois depuis [fr.itii.geoevent_kotlin.ui.map.MainActivity.initMap].
     */
    override fun showMap(container: ViewGroup) {
        mapView = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(48.8566, 2.3522)) // Paris par défaut
        }
        container.removeAllViews()
        container.addView(
            mapView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    /**
     * Anime le déplacement de la caméra vers les coordonnées données.
     */
    override fun centerOn(lat: Double, lon: Double, zoom: Double) {
        mapView.controller.animateTo(GeoPoint(lat, lon))
        mapView.controller.setZoom(zoom)
    }

    /**
     * Ajoute un [Marker] osmdroid sur la carte et rafraîchit l'affichage.
     */
    override fun addMarker(lat: Double, lon: Double, title: String) {
        val marker = Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    /**
     * Supprime tous les [Marker] des overlays et rafraîchit la vue.
     */
    override fun clearMarkers() {
        mapView.overlays.removeAll { it is Marker }
        mapView.invalidate()
    }

    /** Délègue à [MapView.onResume] pour reprendre le chargement des tuiles. */
    override fun onResume() {
        if (::mapView.isInitialized) mapView.onResume()
    }

    /** Délègue à [MapView.onPause] pour libérer les ressources réseau. */
    override fun onPause() {
        if (::mapView.isInitialized) mapView.onPause()
    }

    /** Capture la position et le zoom actuels. */
    override fun saveState(): MapState = if (::mapView.isInitialized) {
        val center = mapView.mapCenter
        MapState(center.latitude, center.longitude, mapView.zoomLevelDouble)
    } else MapState()

    /** Restaure la position et le zoom précédemment capturés. */
    override fun restoreState(state: MapState) {
        if (::mapView.isInitialized) {
            mapView.controller.setCenter(GeoPoint(state.latitude, state.longitude))
            mapView.controller.setZoom(state.zoom)
        }
    }
}
