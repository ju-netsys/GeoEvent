package fr.itii.geoevent_kotlin.ui.map

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.view.Menu
import android.view.MenuItem
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
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.data.model.Event
import fr.itii.geoevent_kotlin.data.remote.FirestoreDataSource
import fr.itii.geoevent_kotlin.data.repository.FirestoreEventRepository
import fr.itii.geoevent_kotlin.databinding.ActivityMainBinding
import fr.itii.geoevent_kotlin.map.MapService
import fr.itii.geoevent_kotlin.map.MapState
import fr.itii.geoevent_kotlin.map.osm.OsmMapService
import fr.itii.geoevent_kotlin.ui.auth.LoginActivity
import fr.itii.geoevent_kotlin.ui.common.UiState
import fr.itii.geoevent_kotlin.ui.event.AddEventActivity
import kotlinx.coroutines.launch

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

    /** Interface cartographique — jamais osmdroid directement. */
    private lateinit var mapService: MapService

    /** Client GPS Play Services. */
    private lateinit var fusedClient: FusedLocationProviderClient

    private val viewModel: MapViewModel by viewModels {
        MapViewModelFactory(FirestoreEventRepository(FirestoreDataSource()))
    }

    /** Position et zoom sauvegardés lors d'une rotation/recréation. */
    private var savedMapState: MapState? = null

    // -------------------------------------------------------------------------
    // Callback GPS
    // -------------------------------------------------------------------------

    /**
     * Reçoit les mises à jour de position et centre la carte automatiquement.
     */
    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(result: LocationResult) {
            result.lastLocation?.let { location ->
                mapService.centerOn(location.latitude, location.longitude)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Gestion des permissions (ActivityResultContracts)
    // -------------------------------------------------------------------------

    /**
     * Lance la demande de permissions et gère les 3 cas :
     * accordé / refusé avec rationale / refusé définitivement.
     */
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
            else -> {
                // Refus définitif : rediriger vers les paramètres système
                showPermissionSettings()
            }
        }
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Rediriger vers Login si l'utilisateur n'est pas authentifié
        if (FirebaseAuth.getInstance().currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        fusedClient = LocationServices.getFusedLocationProviderClient(this)

        // Restaurer l'état de la carte après rotation
        savedMapState = savedInstanceState?.let {
            MapState(
                latitude = it.getDouble(KEY_LAT, 48.8566),
                longitude = it.getDouble(KEY_LON, 2.3522),
                zoom = it.getDouble(KEY_ZOOM, 10.0)
            )
        }

        initMap()
        observeEvents()

        binding.fabAddEvent.setOnClickListener {
            startActivity(Intent(this, AddEventActivity::class.java))
        }
    }

    /**
     * Initialise [OsmMapService] (via [MapService]) et restaure l'état si disponible.
     * Appelé une seule fois dans [onCreate].
     */
    private fun initMap() {
        mapService = OsmMapService(this)
        mapService.showMap(binding.mapContainer)
        savedMapState?.let { mapService.restoreState(it) }
    }

    /**
     * Collecte le [UiState] du ViewModel et met à jour les marqueurs de la carte.
     */
    private fun observeEvents() {
        lifecycleScope.launch {
            viewModel.eventsState.collect { state ->
                when (state) {
                    is UiState.Loading -> { /* Optionnel : afficher un indicateur de chargement */ }
                    is UiState.Success -> displayEvents(state.data)
                    is UiState.Error -> Toast.makeText(
                        this@MainActivity, state.message, Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Efface les anciens marqueurs et place un marqueur par événement.
     */
    private fun displayEvents(events: List<Event>) {
        mapService.clearMarkers()
        events.forEach { event ->
            mapService.addMarker(event.latitude, event.longitude, event.title)
        }
    }

    override fun onResume() {
        super.onResume()
        mapService.onResume()
        // Re-vérification nécessaire : les permissions peuvent être révoquées à chaud (Android 11+)
        checkAndRequestLocationPermission()
    }

    override fun onPause() {
        super.onPause()
        mapService.onPause()
        stopLocationUpdates()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        val state = mapService.saveState()
        outState.putDouble(KEY_LAT, state.latitude)
        outState.putDouble(KEY_LON, state.longitude)
        outState.putDouble(KEY_ZOOM, state.zoom)
    }

    // -------------------------------------------------------------------------
    // Localisation GPS
    // -------------------------------------------------------------------------

    /**
     * Vérifie si la permission est accordée ; sinon lance le [permissionLauncher].
     */
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

    /**
     * Démarre les mises à jour GPS toutes les 5 secondes.
     * La vérification de permission est obligatoire avant chaque appel.
     */
    private fun startLocationUpdates() {
        val fine = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (fine != PackageManager.PERMISSION_GRANTED && coarse != PackageManager.PERMISSION_GRANTED) return

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L).build()
        fusedClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    /**
     * Arrête les mises à jour GPS. Appelé dans [onPause] pour respecter le cycle de vie.
     */
    private fun stopLocationUpdates() {
        fusedClient.removeLocationUpdates(locationCallback)
    }

    // -------------------------------------------------------------------------
    // Dialogs de permission
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
    // Menu (déconnexion)
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
