package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Represents a user's favorite provider
 */
@Entity(
    tableName = "favorites",
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
        )
    ],
    indices = [
        Index(value = ["userId"]),
        Index(value = ["providerId"]),
        Index(value = ["userId", "providerId"], unique = true)
    ]
)
data class Favorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: Long,
    val providerId: Long,
    val createdAt: Instant = Instant.now()
)
