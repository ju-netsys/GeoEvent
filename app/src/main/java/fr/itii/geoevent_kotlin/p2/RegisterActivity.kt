package fr.itii.geoevent_kotlin.p2

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import fr.itii.geoevent_kotlin.R
import fr.itii.geoevent_kotlin.common.UiState
import fr.itii.geoevent_kotlin.databinding.ActivityRegisterBinding
import fr.itii.geoevent_kotlin.p3.MainActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.etPassword.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                updatePasswordStrengthIndicators(s?.toString() ?: "")
            }
        })

        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirm = binding.etConfirmPassword.text.toString().trim()
            if (validateInput(email, password, confirm)) {
                viewModel.register(email, password)
            }
        }

        binding.tvLogin.setOnClickListener { finish() }

        observeAuthState()
    }

    private fun updatePasswordStrengthIndicators(password: String) {
        setIndicator(binding.tvReqLength, password.length >= 8)
        setIndicator(binding.tvReqUpper, password.any { it.isUpperCase() })
        setIndicator(binding.tvReqLower, password.any { it.isLowerCase() })
        setIndicator(binding.tvReqDigit, password.any { it.isDigit() })
        setIndicator(binding.tvReqSpecial, password.any { !it.isLetterOrDigit() })
    }

    private fun setIndicator(view: TextView, satisfied: Boolean) {
        if (satisfied) {
            view.setTextColor(Color.parseColor("#2E7D32"))
        } else {
            view.setTextColor(Color.parseColor("#C62828"))
        }
    }

    private fun isPasswordValid(password: String): Boolean {
        if (password.length < 8) return false
        if (!password.any { it.isUpperCase() }) return false
        if (!password.any { it.isLowerCase() }) return false
        if (!password.any { it.isDigit() }) return false
        if (!password.any { !it.isLetterOrDigit() }) return false
        return true
    }

    private fun observeAuthState() {
        lifecycleScope.launch {
            viewModel.authState.collect { state ->
                when (state) {
                    is UiState.Loading -> setLoading(true)
                    is UiState.Success -> {
                        setLoading(false)
                        startActivity(Intent(this@RegisterActivity, MainActivity::class.java))
                        finish()
                    }
                    is UiState.Error -> {
                        setLoading(false)
                        Toast.makeText(this@RegisterActivity, state.message, Toast.LENGTH_LONG).show()
                    }
                    null -> setLoading(false)
                }
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.progressBar.visibility = if (loading) View.VISIBLE else View.GONE
        binding.btnRegister.isEnabled = !loading
    }

    private fun validateInput(email: String, password: String, confirm: String): Boolean {
        var valid = true

        if (email.isEmpty()) {
            binding.tilEmail.error = getString(R.string.error_email_required)
            valid = false
        } else {
            binding.tilEmail.error = null
        }

        if (!isPasswordValid(password)) {
            binding.tilPassword.error = getString(R.string.error_password_policy)
            valid = false
        } else {
            binding.tilPassword.error = null
        }

        if (password != confirm) {
            binding.tilConfirmPassword.error = getString(R.string.error_passwords_not_match)
            valid = false
        } else {
            binding.tilConfirmPassword.error = null
        }

        return valid
    }
}
