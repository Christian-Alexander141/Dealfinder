package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Status of a booking
 */
enum class BookingStatus {
    CONFIRMED,     // Bestätigt
    PENDING,       // Ausstehend
    CANCELLED,     // Storniert
    COMPLETED      // Abgeschlossen
}

/**
 * Represents a booking made by a user
 */
@Entity(
    tableName = "bookings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Provider::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Service::class,
            parentColumns = ["id"],
            childColumns = ["serviceId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["providerId"]),
        Index(value = ["serviceId"]),
        Index(value = ["bookingDate"]),
        Index(value = ["status"])
    ]
)
data class Booking(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val providerId: Long,
    val serviceId: Long? = null,
    val bookingDate: Instant, // Date of the booking
    val timeSlot: String, // e.g., "14:00 - 15:00"
    val numberOfPeople: Int = 1,
    val status: BookingStatus = BookingStatus.CONFIRMED,
    val createdAt: Instant = Instant.now(),
    val notes: String? = null
)
