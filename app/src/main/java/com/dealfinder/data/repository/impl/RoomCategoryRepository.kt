package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.CategoryDao
import com.dealfinder.data.models.Category
import com.dealfinder.data.repository.CategoryRepository
import kotlinx.coroutines.flow.Flow

class RoomCategoryRepository(
    private val categoryDao: CategoryDao
) : CategoryRepository {
    
    override fun getAllCategories(): Flow<List<Category>> = categoryDao.getAll()
    
    override suspend fun getCategoryById(id: Long): Category? = categoryDao.getById(id)
}
