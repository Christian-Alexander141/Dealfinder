package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Verification status for ratings
 */
enum class VerificationStatus {
    PENDING,       // Noch nicht verifiziert
    VERIFIED,      // Verifiziert durch unabhängigen Tester
    REJECTED       // Abgelehnt
}

/**
 * Represents a verified rating by an independent tester
 */
@Entity(
    tableName = "verified_ratings",
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
        Index(value = ["verificationStatus"])
    ]
)
data class VerifiedRating(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val providerId: Long,
    val overallScore: Float, // 0.0 to 5.0
    val criteria: Map<String, Float>, // Specific criteria with their scores
    val testerReference: String, // Reference to the tester
    val testDate: Instant,
    val verificationStatus: VerificationStatus = VerificationStatus.VERIFIED,
    val notes: String? = null
)
