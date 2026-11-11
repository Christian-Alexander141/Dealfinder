package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.Category
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    
    @Insert
    suspend fun insert(category: Category): Long
    
    @Insert
    suspend fun insertAll(categories: List<Category>)
    
    @Update
    suspend fun update(category: Category)
    
    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getById(id: Long): Category?
    
    @Query("SELECT * FROM categories")
    fun getAll(): Flow<List<Category>>
    
    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM categories")
    suspend fun deleteAll()
}
