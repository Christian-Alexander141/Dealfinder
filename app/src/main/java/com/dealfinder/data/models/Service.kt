package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Booking types for services
 */
enum class BookingType {
    FIXED_DURATION,      // Termin mit fester Dauer (z.B. Friseur)
    SEAT_RESERVATION,    // Sitzplatzreservierung (z.B. Restaurant)
    GROUP_BOOKING        // Gruppenbuchung (z.B. Kurs)
}

/**
 * Represents a service offered by a provider
 */
@Entity(
    tableName = "services",
    foreignKeys = [
        ForeignKey(
            entity = Provider::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["providerId"])
    ]
)
data class Service(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: Long,
    val name: String,
    val description: String? = null,
    val price: Double? = null,
    val durationMinutes: Int? = null,
    val bookingType: BookingType = BookingType.FIXED_DURATION,
    val maxParticipants: Int? = null
)
