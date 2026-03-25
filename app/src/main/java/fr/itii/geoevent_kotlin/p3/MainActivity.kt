package fr.itii.geoevent_kotlin.p3

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
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.common.UiState
import fr.itii.geoevent_kotlin.common.ViewModelFactory
import fr.itii.geoevent_kotlin.databinding.ActivityMainBinding
import fr.itii.geoevent_kotlin.p1.Event
import fr.itii.geoevent_kotlin.p1.ServiceLocator
import fr.itii.geoevent_kotlin.p2.LoginActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var mapService: MapService
    private lateinit var fusedClient: FusedLocationProviderClient
    private lateinit var eventAdapter: EventAdapter
    private var mapPopup: PopupWindow? = null
    private var markerPopup: PopupWindow? = null
    private var currentEvents: List<Event> = emptyList()
    private var hasCenteredOnUser = false
    private var savedMapState: MapState? = null

    private val viewModel: MapViewModel by viewModels {
        ViewModelFactory { MapViewModel(ServiceLocator.eventRepository) }
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            val location = result.lastLocation ?: return
            mapService.updateMyLocation(location.latitude, location.longitude)
            if (!hasCenteredOnUser) {
                mapService.centerOn(location.latitude, location.longitude)
                hasCenteredOnUser = true
            }
        }
    }

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
        initRecyclerView()
        initBottomNav()
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

        mapService.setOnMarkerClickListener { eventId, title, description, authorEmail, userId, viewX, viewY ->
            showMarkerPopup(eventId, title, description, authorEmail, userId, viewX, viewY)
        }
    }

    private fun initRecyclerView() {
        eventAdapter = EventAdapter { event ->
            binding.bottomNav.selectedItemId = R.id.nav_map
            mapService.centerOn(event.latitude, event.longitude, 15.0)
        }
        binding.rvEvents.layoutManager = LinearLayoutManager(this)
        binding.rvEvents.adapter = eventAdapter
    }

    private fun initBottomNav() {
        binding.bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_map -> {
                    binding.mapContainer.visibility = View.VISIBLE
                    binding.searchCard.visibility = View.VISIBLE
                    binding.fabAddEvent.visibility = View.VISIBLE
                    binding.rvEvents.visibility = View.GONE
                    markerPopup?.dismiss()
                    mapPopup?.dismiss()
                    true
                }
                R.id.nav_list -> {
                    binding.mapContainer.visibility = View.GONE
                    binding.searchCard.visibility = View.GONE
                    binding.fabAddEvent.visibility = View.GONE
                    binding.rvEvents.visibility = View.VISIBLE
                    markerPopup?.dismiss()
                    mapPopup?.dismiss()
                    true
                }
                else -> false
            }
        }
    }

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

    private fun showMarkerPopup(
        eventId: String,
        title: String,
        description: String,
        authorEmail: String,
        userId: String,
        viewX: Float,
        viewY: Float
    ) {
        markerPopup?.dismiss()
        mapPopup?.dismiss()

        val isOwner = FirebaseAuth.getInstance().currentUser?.uid == userId

        val popupView = layoutInflater.inflate(R.layout.popup_marker_click, null)
        popupView.findViewById<TextView>(R.id.tvMarkerTitle).text = title

        val tvDescription = popupView.findViewById<TextView>(R.id.tvMarkerDescription)
        if (description.isNotEmpty()) {
            tvDescription.text = description
            tvDescription.visibility = View.VISIBLE
        }

        val tvAuthor = popupView.findViewById<TextView>(R.id.tvMarkerAuthor)
        if (authorEmail.isNotEmpty()) {
            tvAuthor.text = "${getString(R.string.author)} : $authorEmail"
            tvAuthor.visibility = View.VISIBLE
        }

        val btnEdit = popupView.findViewById<MaterialButton>(R.id.btnEditMarker)
        val btnDelete = popupView.findViewById<MaterialButton>(R.id.btnDeleteMarker)

        if (isOwner) {
            btnEdit.visibility = View.VISIBLE
            btnEdit.setOnClickListener {
                markerPopup?.dismiss()
                val event = currentEvents.find { it.id == eventId }
                if (event != null) {
                    startActivity(Intent(this, AddEventActivity::class.java).apply {
                        putExtra(AddEventActivity.EXTRA_EVENT, event)
                    })
                }
            }

            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                markerPopup?.dismiss()
                viewModel.deleteEvent(eventId)
            }
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
        currentEvents = events
        mapService.clearMarkers()
        events.forEach { event ->
            mapService.addMarker(
                event.latitude,
                event.longitude,
                event.title,
                event.description,
                event.authorEmail,
                event.id,
                event.userId
            )
        }
        eventAdapter.submitList(events)
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
        hasCenteredOnUser = false
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = mapService.saveState()
        outState.putDouble(KEY_LAT, state.latitude)
        outState.putDouble(KEY_LON, state.longitude)
        outState.putDouble(KEY_ZOOM, state.zoom)
    }

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
