package fr.itii.geoevent_kotlin.ui.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Factory générique pour créer des ViewModels avec dépendances.
 *
 * Remplace les classes `XxxViewModelFactory` individuelles et élimine
 * le code boilerplate répété à chaque écran.
 *
 * Usage :
 * ```kotlin
 * private val viewModel: MyViewModel by viewModels {
 *     ViewModelFactory { MyViewModel(ServiceLocator.eventRepository) }
 * }
 * ```
 *
 * @param creator Lambda qui instancie le ViewModel souhaité.
 */
class ViewModelFactory<T : ViewModel>(
    private val creator: () -> T
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
