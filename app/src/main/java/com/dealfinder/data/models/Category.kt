package com.dealfinder.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Categories for providers/services
 */
@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String? = null,
    val icon: String? = null
)
