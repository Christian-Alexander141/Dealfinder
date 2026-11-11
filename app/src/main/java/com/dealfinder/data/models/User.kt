package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * Represents a user in the Dealfinder system
 * Can be either a private user or a business/company
 */
enum class UserType {
    PRIVATE,       // Privatperson
    COMPANY        // Unternehmen
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val email: String,
    val userType: UserType,
    val createdAt: Instant = Instant.now()
)
