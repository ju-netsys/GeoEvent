package fr.itii.geoevent_kotlin.data.model

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize

/**
 * Modèle de données représentant un événement géolocalisé.
 *
 * L'annotation [@DocumentId] indique à Firestore d'injecter automatiquement
 * l'identifiant du document dans le champ [id] lors de la désérialisation.
 *
 * @property id       Identifiant unique Firestore (injecté automatiquement).
 * @property title    Titre de l'événement.
 * @property description Description optionnelle.
 * @property latitude Latitude géographique (WGS 84).
 * @property longitude Longitude géographique (WGS 84).
 * @property userId   UID Firebase de l'auteur.
 * @property createdAt Horodatage de création.
 */
@Parcelize
data class Event(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val userId: String = "",
    val createdAt: @RawValue Timestamp = Timestamp.now()
) : Parcelable
