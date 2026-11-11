package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.User
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    
    @Insert
    suspend fun insert(user: User): Long
    
    @Update
    suspend fun update(user: User)
    
    @Query("SELECT * FROM users WHERE id = :id")
    suspend fun getById(id: Long): User?
    
    @Query("SELECT * FROM users WHERE email = :email")
    suspend fun getByEmail(email: String): User?
    
    @Query("SELECT * FROM users")
    fun getAll(): Flow<List<User>>
    
    @Query("SELECT * FROM users WHERE userType = 'COMPANY'")
    fun getAllCompanies(): Flow<List<User>>
    
    @Query("DELETE FROM users WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM users")
    suspend fun deleteAll()
}
