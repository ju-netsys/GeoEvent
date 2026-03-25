package fr.itii.geoevent_kotlin.p3

import android.content.Context
import android.view.MotionEvent
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import fr.itii.geoevent_kotlin.R
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Overlay

class OsmMapService(private val context: Context) : MapService {

    private lateinit var mapView: MapView
    private var mapClickListener: ((Double, Double, Float, Float) -> Unit)? = null
    private var markerClickListener: ((String, String, String, String, String, Float, Float) -> Unit)? = null

    private data class MarkerMeta(
        val description: String,
        val authorEmail: String,
        val userId: String
    )
    private val markerMeta = mutableMapOf<String, MarkerMeta>()

    init {
        Configuration.getInstance().userAgentValue = context.packageName
    }

    override fun showMap(container: ViewGroup) {
        mapView = MapView(context).apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setMultiTouchControls(true)
            controller.setZoom(10.0)
            controller.setCenter(GeoPoint(48.8566, 2.3522))
        }

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
            ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        )
    }

    override fun centerOn(lat: Double, lon: Double, zoom: Double) {
        mapView.controller.animateTo(GeoPoint(lat, lon))
        mapView.controller.setZoom(zoom)
    }

    override fun addMarker(
        lat: Double,
        lon: Double,
        title: String,
        description: String,
        authorEmail: String,
        eventId: String,
        userId: String
    ) {
        markerMeta[eventId] = MarkerMeta(description, authorEmail, userId)

        val icon = ContextCompat.getDrawable(context, R.drawable.ic_event_marker)
        val marker = Marker(mapView).apply {
            position = GeoPoint(lat, lon)
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            this.title = title
            this.icon = icon
            infoWindow = null
            id = eventId
        }

        marker.setOnMarkerClickListener { m, mv ->
            val point = mv.projection.toPixels(m.position, null)
            val meta = markerMeta[m.id] ?: MarkerMeta("", "", "")
            markerClickListener?.invoke(
                m.id ?: "",
                m.title ?: "",
                meta.description,
                meta.authorEmail,
                meta.userId,
                point.x.toFloat(),
                point.y.toFloat()
            )
            true
        }

        mapView.overlays.add(marker)
        mapView.invalidate()
    }

    override fun clearMarkers() {
        markerMeta.clear()
        mapView.overlays.removeAll { it is Marker }
        mapView.invalidate()
    }

    override fun setOnMapClickListener(listener: (Double, Double, Float, Float) -> Unit) {
        mapClickListener = listener
    }

    override fun setOnMarkerClickListener(
        listener: (String, String, String, String, String, Float, Float) -> Unit
    ) {
        markerClickListener = listener
    }

    override fun onResume() {
        if (::mapView.isInitialized) mapView.onResume()
    }

    override fun onPause() {
        if (::mapView.isInitialized) mapView.onPause()
    }

    override fun saveState(): MapState {
        if (!::mapView.isInitialized) return MapState()
        val center = mapView.mapCenter
        return MapState(center.latitude, center.longitude, mapView.zoomLevelDouble)
    }

    override fun restoreState(state: MapState) {
        if (::mapView.isInitialized) {
            mapView.controller.setCenter(GeoPoint(state.latitude, state.longitude))
            mapView.controller.setZoom(state.zoom)
        }
    }
}
