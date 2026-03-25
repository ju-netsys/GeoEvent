package fr.itii.geoevent_kotlin.ui.common

/**
 * Représente les états possibles d'une opération asynchrone.
 *
 * Utilisé par tous les ViewModels pour exposer l'état de chaque requête réseau
 * (Firestore, Auth) au travers de [kotlinx.coroutines.flow.StateFlow].
 *
 * @param T Le type de données retourné en cas de succès.
 */
sealed class UiState<out T> {

    /** L'opération est en cours. */
    object Loading : UiState<Nothing>()

    /**
     * L'opération s'est terminée avec succès.
     * @property data Les données résultantes.
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * L'opération a échoué.
     * @property message Le message d'erreur lisible.
     */
    data class Error(val message: String) : UiState<Nothing>()
}
