package com.dealfinder.data.repository

import com.dealfinder.data.models.Category
import kotlinx.coroutines.flow.Flow

interface CategoryRepository {
    
    /**
     * Get all categories
     */
    fun getAllCategories(): Flow<List<Category>>
    
    /**
     * Get category by ID
     */
    suspend fun getCategoryById(id: Long): Category?
}
