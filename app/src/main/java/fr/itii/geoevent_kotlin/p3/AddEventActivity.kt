package fr.itii.geoevent_kotlin.p3

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.common.UiState
import fr.itii.geoevent_kotlin.common.ViewModelFactory
import fr.itii.geoevent_kotlin.databinding.ActivityAddEventBinding
import fr.itii.geoevent_kotlin.p1.Event
import fr.itii.geoevent_kotlin.p1.ServiceLocator
import kotlinx.coroutines.launch

class AddEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEventBinding
    private var eventToEdit: Event? = null

    private val viewModel: EventViewModel by viewModels {
        ViewModelFactory { EventViewModel(ServiceLocator.eventRepository) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        @Suppress("DEPRECATION")
        eventToEdit = intent.getParcelableExtra(EXTRA_EVENT)

        if (eventToEdit != null) {
            supportActionBar?.title = getString(R.string.edit_event_title)
            prefillForm(eventToEdit!!)
        } else {
            supportActionBar?.title = getString(R.string.add_event_title)
            prefillCoordinates()
        }

        binding.btnSave.setOnClickListener { onSaveClicked() }

        observeAddState()
    }

    private fun prefillForm(event: Event) {
        binding.etTitle.setText(event.title)
        binding.etDescription.setText(event.description)
        binding.etLatitude.setText(event.latitude.toString())
        binding.etLongitude.setText(event.longitude.toString())
        binding.btnSave.text = getString(R.string.save_changes)
    }

    private fun prefillCoordinates() {
        val lat = intent.getDoubleExtra(EXTRA_LAT, Double.NaN)
        if (!lat.isNaN()) {
            binding.etLatitude.setText(lat.toString())
        }
        val lon = intent.getDoubleExtra(EXTRA_LON, Double.NaN)
        if (!lon.isNaN()) {
            binding.etLongitude.setText(lon.toString())
        }
    }

    private fun onSaveClicked() {
        val title = binding.etTitle.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val latText = binding.etLatitude.text.toString().trim()
        val lonText = binding.etLongitude.text.toString().trim()

        if (!validateInput(title, latText, lonText)) return

        val lat = latText.toDouble()
        val lon = lonText.toDouble()

        if (eventToEdit != null) {
            viewModel.updateEvent(eventToEdit!!, title, description, lat, lon)
        } else {
            viewModel.addEvent(title, description, lat, lon)
        }
    }

    private fun observeAddState() {
        lifecycleScope.launch {
            viewModel.addState.collect { state ->
                when (state) {
                    is UiState.Loading -> setLoading(true)
                    is UiState.Success -> {
                        setLoading(false)
                        val message = if (eventToEdit != null) R.string.event_updated else R.string.event_added
                        Toast.makeText(this@AddEventActivity, message, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is UiState.Error -> {
                        setLoading(false)
                        Toast.makeText(this@AddEventActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    null -> {}
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }

    private fun validateInput(title: String, latText: String, lonText: String): Boolean {
        var valid = true

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.error_title_required)
            valid = false
        } else {
            binding.tilTitle.error = null
        }

        val lat = latText.toDoubleOrNull()
        if (lat == null || lat < -90 || lat > 90) {
            binding.tilLatitude.error = getString(R.string.error_invalid_latitude)
            valid = false
        } else {
            binding.tilLatitude.error = null
        }

        val lon = lonText.toDoubleOrNull()
        if (lon == null || lon < -180 || lon > 180) {
            binding.tilLongitude.error = getString(R.string.error_invalid_longitude)
            valid = false
        } else {
            binding.tilLongitude.error = null
        }

        return valid
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        const val EXTRA_LAT = "extra_lat"
        const val EXTRA_LON = "extra_lon"
        const val EXTRA_EVENT = "extra_event"
    }
}
