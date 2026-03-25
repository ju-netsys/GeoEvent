package fr.itii.geoevent_kotlin.ui.event

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.data.model.Event
import fr.itii.geoevent_kotlin.data.remote.FirestoreDataSource
import fr.itii.geoevent_kotlin.data.repository.FirestoreEventRepository
import fr.itii.geoevent_kotlin.databinding.ActivityEventDetailBinding
import fr.itii.geoevent_kotlin.ui.common.UiState
import kotlinx.coroutines.launch

/**
 * Écran de détail d'un événement.
 *
 * Reçoit un objet [Event] via [EXTRA_EVENT] (Parcelable).
 * Le bouton de suppression n'est visible que pour l'auteur de l'événement.
 */
class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding

    private val viewModel: EventViewModel by viewModels {
        EventViewModelFactory(FirestoreEventRepository(FirestoreDataSource()))
    }

    private var event: Event? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        @Suppress("DEPRECATION")
        event = intent.getParcelableExtra(EXTRA_EVENT)
        event?.let { displayEvent(it) }

        binding.btnDelete.setOnClickListener { confirmDelete() }

        observeDeleteState()
    }

    /**
     * Affiche les données de l'événement et masque le bouton supprimer
     * si l'utilisateur courant n'est pas le propriétaire.
     */
    private fun displayEvent(event: Event) {
        supportActionBar?.title = event.title
        binding.tvTitle.text = event.title
        binding.tvDescription.text = event.description.ifEmpty { getString(R.string.no_description) }
        binding.tvCoordinates.text = getString(R.string.coordinates_format, event.latitude, event.longitude)

        val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
        binding.btnDelete.visibility = if (event.userId == currentUserId) View.VISIBLE else View.GONE
    }

    private fun confirmDelete() {
        AlertDialog.Builder(this)
            .setTitle(R.string.delete_event_title)
            .setMessage(R.string.delete_event_confirm)
            .setPositiveButton(R.string.delete) { _, _ ->
                event?.id?.let { viewModel.deleteEvent(it) }
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun observeDeleteState() {
        lifecycleScope.launch {
            viewModel.deleteState.collect { state ->
                when (state) {
                    is UiState.Loading -> binding.progressBar.visibility = View.VISIBLE
                    is UiState.Success -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@EventDetailActivity, R.string.event_deleted, Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    is UiState.Error -> {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this@EventDetailActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    null -> { /* État initial */ }
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    companion object {
        /** Clé Intent pour passer un objet [Event] Parcelable. */
        const val EXTRA_EVENT = "extra_event"
    }
}
