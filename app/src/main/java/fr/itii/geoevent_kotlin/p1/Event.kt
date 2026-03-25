package fr.itii.geoevent_kotlin.p1

import android.os.Parcelable
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import kotlinx.parcelize.RawValue

@Parcelize
data class Event(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val userId: String = "",
    val authorEmail: String = "",
    val createdAt: @RawValue Timestamp = Timestamp.now()
) : Parcelable
