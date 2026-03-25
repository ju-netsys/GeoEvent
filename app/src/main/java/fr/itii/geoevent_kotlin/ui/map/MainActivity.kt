package fr.itii.geoevent_kotlin.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.data.model.Event
import fr.itii.geoevent_kotlin.databinding.ActivityMainBinding
import fr.itii.geoevent_kotlin.di.ServiceLocator
import fr.itii.geoevent_kotlin.map.MapService
import fr.itii.geoevent_kotlin.map.MapState
import fr.itii.geoevent_kotlin.map.osm.OsmMapService
import fr.itii.geoevent_kotlin.ui.auth.LoginActivity
import fr.itii.geoevent_kotlin.ui.common.UiState
import fr.itii.geoevent_kotlin.ui.common.ViewModelFactory
import fr.itii.geoevent_kotlin.ui.event.AddEventActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

/**
 * Activité principale de l'application : affiche la carte OSM,
 * gère les permissions de localisation et affiche les événements Firestore.
 *
 * Architecture MVVM :
 * - [MapViewModel] expose [UiState] via [kotlinx.coroutines.flow.StateFlow]
 * - [MapService] (interface) masque toute dépendance envers osmdroid
 * - [FusedLocationProviderClient] fournit la position GPS
 *
 * Cycle de vie :
 * - onResume  : reprend la carte OSM + relance les MAJ GPS
 * - onPause   : suspend la carte OSM + arrête les MAJ GPS
 * - onSaveInstanceState : sauvegarde centre/zoom de la carte
 */
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapService: MapService
    private lateinit var fusedClient: FusedLocationProviderClient
    private var mapPopup: PopupWindow? = null
    private var markerPopup: PopupWindow? = null

    private val viewModel: MapViewModel by viewModels {
        ViewModelFactory { MapViewModel(ServiceLocator.eventRepository) }
    }

    private var savedMapState: MapState? = null

    /**
     * Vrai dès que la carte a été centrée une première fois sur la position GPS.
     * Évite que chaque mise à jour GPS (toutes les 5s) recentre la carte pendant
     * que l'utilisateur navigue.
     * Remis à false dans [onPause] pour recentrer à la prochaine ouverture.
     */
    private var hasCenteredOnUser = false

    // -------------------------------------------------------------------------
    // Callback GPS
    // -------------------------------------------------------------------------

    /**
     * Reçoit les mises à jour de position.
     * Ne centre la carte que lors du **premier** fix GPS de la session.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                if (!hasCenteredOnUser) {
                    mapService.centerOn(location.latitude, location.longitude)
                    hasCenteredOnUser = true
                }
            }
        }
    }

    // -------------------------------------------------------------------------
    // Permissions
    // -------------------------------------------------------------------------

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                    permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                startLocationUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                showPermissionRationale()
            }
            else -> showPermissionSettings()
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        savedMapState = savedInstanceState?.let {
            MapState(
                latitude = it.getDouble(KEY_LAT, 48.8566),
                longitude = it.getDouble(KEY_LON, 2.3522),
                zoom = it.getDouble(KEY_ZOOM, 10.0)
            )
        }

        initMap()
        observeEvents()
        observeDeleteState()
        initSearch()

        binding.fabAddEvent.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }
    }

    private fun initMap() {
        mapService = OsmMapService(this)
        mapService.showMap(binding.mapContainer)
        savedMapState?.let { mapService.restoreState(it) }

        mapService.setOnMapClickListener { lat, lon, viewX, viewY ->
            showMapPopup(lat, lon, viewX, viewY)
        }

        mapService.setOnMarkerClickListener { eventId, title, userId, viewX, viewY ->
            showMarkerPopup(eventId, title, userId, viewX, viewY)
        }
    }

    // -------------------------------------------------------------------------
    // Bubble popup au tap sur la carte
    // -------------------------------------------------------------------------

    /**
     * Affiche une bulle contextuelle avec les coordonnées du tap
     * et un bouton "+" pour créer un événement à cet endroit.
     *
     * @param viewX / viewY : coordonnées relatives à la vue carte.
     */
    private fun showMapPopup(lat: Double, lon: Double, viewX: Float, viewY: Float) {
        mapPopup?.dismiss()

        val popupView = layoutInflater.inflate(R.layout.popup_map_click, null)
        popupView.findViewById<TextView>(R.id.tvCoordinates).text =
            getString(R.string.coordinates_format, lat, lon)
        popupView.findViewById<MaterialButton>(R.id.btnAddHere).setOnClickListener {
            mapPopup?.dismiss()
            startActivity(Intent(this, AddEventActivity::class.java).apply {
                putExtra(AddEventActivity.EXTRA_LAT, lat)
                putExtra(AddEventActivity.EXTRA_LON, lon)
            })
        }

        // Mesure la bulle pour centrer horizontalement et la placer au-dessus du tap
        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = popupView.measuredWidth
        val popupH = popupView.measuredHeight

        // Convertit les coordonnées de vue en coordonnées fenêtre
        val loc = IntArray(2)
        binding.mapContainer.getLocationInWindow(loc)
        val winX = loc[0] + viewX.toInt() - popupW / 2
        val winY = loc[1] + viewY.toInt() - popupH - 12

        mapPopup = PopupWindow(
            popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            showAtLocation(binding.root, Gravity.NO_GRAVITY, winX, winY)
        }
    }

    // -------------------------------------------------------------------------
    // Bubble popup au tap sur un marqueur existant
    // -------------------------------------------------------------------------

    /**
     * Affiche une bulle avec le titre de l'événement et un bouton de suppression.
     * Le bouton est masqué si l'utilisateur courant n'est pas le propriétaire.
     *
     * @param viewX / viewY : coordonnées écran du marqueur (depuis la projection osmdroid).
     */
    private fun showMarkerPopup(eventId: String, title: String, userId: String, viewX: Float, viewY: Float) {
        markerPopup?.dismiss()
        mapPopup?.dismiss()

        val isOwner = FirebaseAuth.getInstance().currentUser?.uid == userId

        val popupView = layoutInflater.inflate(R.layout.popup_marker_click, null)
        popupView.findViewById<TextView>(R.id.tvMarkerTitle).text = title

        val btnDelete = popupView.findViewById<MaterialButton>(R.id.btnDeleteMarker)
        if (isOwner) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                markerPopup?.dismiss()
                viewModel.deleteEvent(eventId)
            }
        } else {
            btnDelete.visibility = View.GONE
        }

        popupView.measure(
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        )
        val popupW = popupView.measuredWidth
        val popupH = popupView.measuredHeight

        val loc = IntArray(2)
        binding.mapContainer.getLocationInWindow(loc)
        val winX = loc[0] + viewX.toInt() - popupW / 2
        val winY = loc[1] + viewY.toInt() - popupH - 12

        markerPopup = PopupWindow(
            popupView,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            showAtLocation(binding.root, Gravity.NO_GRAVITY, winX, winY)
        }
    }

    private fun observeDeleteState() {
        lifecycleScope.launch {
            viewModel.deleteState.collect { state ->
                when (state) {
                    is UiState.Success -> Toast.makeText(
                        this@MainActivity, R.string.event_deleted, Toast.LENGTH_SHORT
                    ).show()
                    is UiState.Error -> Toast.makeText(
                        this@MainActivity, state.message, Toast.LENGTH_SHORT
                    ).show()
                    else -> {}
                }
            }
        }
    }

    private fun observeEvents() {
        lifecycleScope.launch {
            viewModel.eventsState.collect { state ->
                when (state) {
                    is UiState.Loading -> {}
                    is UiState.Success -> displayEvents(state.data)
                    is UiState.Error -> Toast.makeText(
                        this@MainActivity, state.message, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun displayEvents(events: List<Event>) {
        mapService.clearMarkers()
        events.forEach { event ->
            mapService.addMarker(event.latitude, event.longitude, event.title, event.id, event.userId)
        }
    }

    override fun onResume() {
        super.onResume()
        mapService.onResume()
        checkAndRequestLocationPermission()
    }

    override fun onPause() {
        super.onPause()
        mapService.onPause()
        stopLocationUpdates()
        // Permet de recentrer à la prochaine reprise
        hasCenteredOnUser = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = mapService.saveState()
        outState.putDouble(KEY_LAT, state.latitude)
        outState.putDouble(KEY_LON, state.longitude)
        outState.putDouble(KEY_ZOOM, state.zoom)
    }

    // -------------------------------------------------------------------------
    // GPS
    // -------------------------------------------------------------------------

    private fun checkAndRequestLocationPermission() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine == PackageManager.PERMISSION_GRANTED || coarse == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun startLocationUpdates() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    private fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    // -------------------------------------------------------------------------
    // Recherche de lieux (Nominatim / OpenStreetMap)
    // -------------------------------------------------------------------------

    /**
     * Configure la barre de recherche.
     * Déclenche [searchPlace] au clic sur le bouton ou à l'action "Search" du clavier.
     */
    private fun initSearch() {
        binding.btnSearch.setOnClickListener {
            val query = binding.etSearch.text.toString().trim()
            if (query.isNotEmpty()) searchPlace(query)
        }

        binding.etSearch.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                val query = binding.etSearch.text.toString().trim()
                if (query.isNotEmpty()) searchPlace(query)
                true
            } else false
        }
    }

    /**
     * Géocode un code postal via l'API zippopotam.us et centre la carte sur le résultat.
     *
     * Format accepté :
     * - "75001"       → pays par défaut : France (fr)
     * - "us/10001"    → pays explicite avant le "/"
     *
     * L'appel réseau s'effectue sur [Dispatchers.IO] pour ne pas bloquer le main thread.
     */
    private fun searchPlace(query: String) {
        hideKeyboard()
        lifecycleScope.launch {
            try {
                val (country, zip) = if (query.contains("/")) {
                    val parts = query.split("/", limit = 2)
                    parts[0].trim() to parts[1].trim()
                } else {
                    "fr" to query.trim()
                }

                val result = withContext(Dispatchers.IO) {
                    val url = URL("https://api.zippopotam.us/$country/$zip")
                    val connection = url.openConnection() as HttpURLConnection
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                        val response = connection.inputStream.bufferedReader().readText()
                        connection.disconnect()
                        response
                    } else {
                        connection.disconnect()
                        null
                    }
                }

                if (result != null) {
                    val json = org.json.JSONObject(result)
                    val places = json.getJSONArray("places")
                    val place = places.getJSONObject(0)
                    val lat = place.getString("latitude").toDouble()
                    val lon = place.getString("longitude").toDouble()
                    val placeName = place.getString("place name")
                    mapService.centerOn(lat, lon, 13.0)
                    Toast.makeText(this@MainActivity, placeName, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this@MainActivity, R.string.search_no_result, Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, R.string.search_error, Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hideKeyboard() {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(binding.etSearch.windowToken, 0)
    }

    // -------------------------------------------------------------------------
    // Dialogs permission
    // -------------------------------------------------------------------------

    private fun showPermissionRationale() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_required_message)
            .setPositiveButton(R.string.grant) { _, _ ->
                permissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showPermissionSettings() {
        AlertDialog.Builder(this)
            .setTitle(R.string.permission_required_title)
            .setMessage(R.string.permission_settings_message)
            .setPositiveButton(R.string.settings) { _, _ ->
                startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.fromParts("package", packageName, null)
                    }
                )
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    // -------------------------------------------------------------------------
    // Menu
    // -------------------------------------------------------------------------

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean = when (item.itemId) {
        R.id.action_logout -> {
            FirebaseAuth.getInstance().signOut()
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            true
        }
        else -> super.onOptionsItemSelected(item)
    }

    companion object {
        private const val KEY_LAT = "map_lat"
        private const val KEY_LON = "map_lon"
        private const val KEY_ZOOM = "map_zoom"
    }
}
