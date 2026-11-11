package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Represents a special offer or voucher from a provider
 */
@Entity(
    tableName = "offers",
    foreignKeys = [
        ForeignKey(
            entity = Provider::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["providerId"]),
        Index(value = ["validUntil"])
    ]
)
data class Offer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: Long,
    val title: String,
    val description: String,
    val discountValue: String, // e.g., "10%", "5.00 €", "2 for 1"
    val validUntil: Instant,
    val conditions: String? = null,
    val isActive: Boolean = true
)
