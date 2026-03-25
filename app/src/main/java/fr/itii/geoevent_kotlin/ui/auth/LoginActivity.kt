package fr.itii.geoevent_kotlin.ui.auth

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.databinding.ActivityLoginBinding
import fr.itii.geoevent_kotlin.ui.common.UiState
import fr.itii.geoevent_kotlin.ui.map.MainActivity
import kotlinx.coroutines.launch

/**
 * Écran de connexion.
 *
 * Redirige automatiquement vers [MainActivity] si l'utilisateur est déjà connecté.
 * Valide les champs avant d'appeler [AuthViewModel.login].
 */
class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si déjà connecté, sauter l'écran de login
        if (viewModel.isLoggedIn) {
            navigateToMap()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            if (validateInput(email, password)) {
                viewModel.login(email, password)
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        observeAuthState()
    }

    /**
     * Collecte le [UiState] et met à jour l'interface en conséquence.
     */
    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is UiState.Loading -> setLoading(true)
                    is UiState.Success -> {
                        setLoading(false)
                        navigateToMap()
                    }
                    is UiState.Error -> {
                        setLoading(false)
                        Toast.makeText(this@LoginActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnLogin.isEnabled = !loading
    }

    private fun validateInput(email: String, password: String): Boolean {
        var valid = true
        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            valid = false
        } else binding.tilEmail.error = null

        if (password.isEmpty()) {
            binding.tilPassword.error = getString(R.string.error_password_required)
            valid = false
        } else binding.tilPassword.error = null

        return valid
    }

    private fun navigateToMap() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
