package fr.itii.geoevent_kotlin.ui.event

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.data.remote.FirestoreDataSource
import fr.itii.geoevent_kotlin.data.repository.FirestoreEventRepository
import fr.itii.geoevent_kotlin.databinding.ActivityAddEventBinding
import fr.itii.geoevent_kotlin.ui.common.UiState
import kotlinx.coroutines.launch

/**
 * Écran de création d'un événement.
 *
 * L'utilisateur saisit titre, description et coordonnées GPS (latitude/longitude).
 * La validation côté client vérifie les plages [-90,90] et [-180,180].
 * L'écriture Firestore est déléguée à [EventViewModel.addEvent].
 */
class AddEventActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddEventBinding

    private val viewModel: EventViewModel by viewModels {
        EventViewModelFactory(FirestoreEventRepository(FirestoreDataSource()))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAddEventBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.add_event_title)
        }

        binding.btnSave.setOnClickListener {
            val title = binding.etTitle.text.toString().trim()
            val description = binding.etDescription.text.toString().trim()
            val latStr = binding.etLatitude.text.toString().trim()
            val lonStr = binding.etLongitude.text.toString().trim()
            if (validateInput(title, latStr, lonStr)) {
                viewModel.addEvent(title, description, latStr.toDouble(), lonStr.toDouble())
            }
        }

        observeAddState()
    }

    private fun observeAddState() {
        lifecycleScope.launch {
            viewModel.addState.collect { state ->
                when (state) {
                    is UiState.Loading -> setLoading(true)
                    is UiState.Success -> {
                        setLoading(false)
                        Toast.makeText(this@AddEventActivity, R.string.event_added, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is UiState.Error -> {
                        setLoading(false)
                        Toast.makeText(this@AddEventActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    null -> { /* État initial : rien à faire */ }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnSave.isEnabled = !loading
    }

    private fun validateInput(title: String, latStr: String, lonStr: String): Boolean {
        var valid = true

        if (title.isEmpty()) {
            binding.tilTitle.error = getString(R.string.error_title_required)
            valid = false
        } else binding.tilTitle.error = null

        val lat = latStr.toDoubleOrNull()
        if (lat == null || lat < -90 || lat > 90) {
            binding.tilLatitude.error = getString(R.string.error_invalid_latitude)
            valid = false
        } else binding.tilLatitude.error = null

        val lon = lonStr.toDoubleOrNull()
        if (lon == null || lon < -180 || lon > 180) {
            binding.tilLongitude.error = getString(R.string.error_invalid_longitude)
            valid = false
        } else binding.tilLongitude.error = null

        return valid
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
