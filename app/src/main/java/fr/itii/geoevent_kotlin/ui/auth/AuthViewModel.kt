package fr.itii.geoevent_kotlin.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import fr.itii.geoevent_kotlin.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

/**
 * ViewModel partagé par [LoginActivity] et [RegisterActivity].
 *
 * Interagit directement avec [FirebaseAuth] — c'est le seul ViewModel
 * autorisé à le faire car il n'y a pas de couche Repository pour l'auth
 * dans ce projet (pas requis par l'évaluation).
 */
class AuthViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()

    private val _authState = MutableStateFlow<UiState<Unit>?>(null)

    /**
     * Flux d'état exposé aux activités.
     * `null` = état initial (aucune action en cours).
     * Émet [UiState.Loading] pendant la requête, [UiState.Success] ou [UiState.Error].
     */
    val authState: StateFlow<UiState<Unit>?> = _authState

    /** Vrai si un utilisateur Firebase est déjà connecté. */
    val isLoggedIn: Boolean get() = auth.currentUser != null

    /**
     * Connecte l'utilisateur avec email et mot de passe.
     * Le résultat est émis dans [authState].
     */
    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            runCatching {
                auth.signInWithEmailAndPassword(email, password).await()
            }.onSuccess {
                _authState.value = UiState.Success(Unit)
            }.onFailure { e ->
                _authState.value = UiState.Error(e.message ?: "Erreur de connexion")
            }
        }
    }

    /**
     * Crée un nouveau compte Firebase avec email et mot de passe.
     * Le résultat est émis dans [authState].
     */
    fun register(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            runCatching {
                auth.createUserWithEmailAndPassword(email, password).await()
            }.onSuccess {
                _authState.value = UiState.Success(Unit)
            }.onFailure { e ->
                _authState.value = UiState.Error(e.message ?: "Erreur d'inscription")
            }
        }
    }

    /** Déconnecte l'utilisateur courant de Firebase. */
    fun logout() {
        auth.signOut()
    }
}
