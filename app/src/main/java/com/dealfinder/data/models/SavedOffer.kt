package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A voucher/offer the user has saved ("vorgemerkt") for later use.
 *
 * Persisted in Room so that saved vouchers survive an app restart.
 * There is no payment/redemption flow in this MVP – saving is a local bookmark.
 */
@Entity(
    tableName = "saved_offers",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Offer::class,
            parentColumns = ["id"],
            childColumns = ["offerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId", "offerId"], unique = true),
        Index(value = ["userId"])
    ]
)
data class SavedOffer(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val offerId: Long,
    val savedAt: Instant = Instant.now()
)
