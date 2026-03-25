package fr.itii.geoevent_kotlin.map.osm

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.map.MapService
import fr.itii.geoevent_kotlin.map.MapState
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

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
    private var mapClickListener: ((Double, Double, Float, Float) -> Unit)? = null
    private var markerClickListener: ((String, String, String, Float, Float) -> Unit)? = null

    init {
        // osmdroid exige un User-Agent valide pour télécharger les tuiles OSM.
        Configuration.getInstance().userAgentValue = context.packageName
    }

    /**
     * Crée le [MapView], configure les sources de tuiles et l'ajoute dans [container].
     * Ajoute également un overlay de clic pour détecter les taps sur la carte vide.
     */
    override fun showMap(container: ViewGroup) {
        mapView = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(48.8566, 2.3522)) // Paris par défaut
        }

        // Overlay ajouté en premier : sera traité en dernier dans la chaîne d'événements
        // (osmdroid traite les overlays en ordre inverse). Les marqueurs ajoutés plus tard
        // auront la priorité ; ce callback n'est déclenché que sur la carte vide.
        mapView.overlays.add(object : Overlay() {
            override fun onSingleTapConfirmed(e: MotionEvent, mapView: MapView): Boolean {
                val geo = mapView.projection.fromPixels(e.x.toInt(), e.y.toInt())
                mapClickListener?.invoke(geo.latitude, geo.longitude, e.x, e.y)
                return true
            }
        })

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
     * Ajoute un [Marker] osmdroid avec l'icône personnalisée [R.drawable.ic_event_marker].
     * La fenêtre d'info par défaut (qui affichait le "doigt") est désactivée.
     * Au tap sur le marqueur, [markerClickListener] est déclenché avec l'eventId et userId.
     */
    override fun addMarker(lat: Double, lon: Double, title: String, eventId: String, userId: String) {
        val icon = ContextCompat.getDrawable(context, R.drawable.ic_event_marker)
        val marker = Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            this.icon = icon
            infoWindow = null   // désactive le popup "doigt" par défaut d'osmdroid
            id = eventId
            snippet = userId    // snippet stocke userId (non affiché, infoWindow = null)
        }
        marker.setOnMarkerClickListener { m, mv ->
            val point = mv.projection.toPixels(m.position, null)
            markerClickListener?.invoke(
                m.id ?: "", m.title ?: "", m.snippet ?: "",
                point.x.toFloat(), point.y.toFloat()
            )
            true
        }
        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    /**
     * Supprime tous les [Marker] des overlays et rafraîchit la vue.
     * L'overlay de clic (non-Marker) est conservé.
     */
    override fun clearMarkers() {
        mapView.overlays.removeAll { it is Marker }
        mapView.invalidate()
    }

    override fun setOnMapClickListener(listener: (lat: Double, lon: Double, x: Float, y: Float) -> Unit) {
        mapClickListener = listener
    }

    override fun setOnMarkerClickListener(listener: (eventId: String, title: String, userId: String, x: Float, y: Float) -> Unit) {
        markerClickListener = listener
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
