package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.UserDao
import com.dealfinder.data.models.User
import com.dealfinder.data.models.UserType
import com.dealfinder.data.repository.UserRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoomUserRepository(
    private val userDao: UserDao
) : UserRepository {
    
    override fun getAllUsers(): Flow<List<User>> = userDao.getAll()
    
    override suspend fun getUserById(id: Long): User? = userDao.getById(id)
    
    override suspend fun getUserByEmail(email: String): User? = userDao.getByEmail(email)
    
    override suspend fun getCurrentUser(): User? {
        // For demo purposes, we'll just get the first user
        // In a real app, this would be stored in preferences
        val users = userDao.getAll().first()
        return users.firstOrNull()
    }
    
    override suspend fun registerUser(name: String, email: String, userType: UserType): User {
        val user = User(
            name = name,
            email = email,
            userType = userType
        )
        val id = userDao.insert(user)
        return user.copy(id = id)
    }
    
    override suspend fun updateUser(user: User) {
        userDao.update(user)
    }
    
    override suspend fun deleteUser(id: Long) {
        userDao.delete(id)
    }
    
    override suspend fun userExists(email: String): Boolean {
        return userDao.getByEmail(email) != null
    }
    
    override suspend fun setCurrentUser(user: User) {
        // For demo, we just ensure the user exists
        userDao.getById(user.id) ?: userDao.insert(user)
    }
    
    override suspend fun clearCurrentUser() {
        // For demo purposes, we don't actually clear anything
        // This would be handled by DataStore in a real app
    }
}
