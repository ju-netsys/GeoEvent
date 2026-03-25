package fr.itii.geoevent_kotlin.p2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<UiState<Unit>?>(null)
    val authState: StateFlow<UiState<Unit>?> = _authState

    val isLoggedIn: Boolean get() = auth.currentUser != null

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                _authState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _authState.value = UiState.Error(e.message ?: "Erreur de connexion")
            }
        }
    }

    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                _authState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _authState.value = UiState.Error(e.message ?: "Erreur d'inscription")
            }
        }
    }

    fun logout() {
        auth.signOut()
    }
}
