package com.dealfinder.data.repository

import com.dealfinder.data.models.Service
import kotlinx.coroutines.flow.Flow

interface ServiceRepository {
    
    /**
     * Get service by ID
     */
    suspend fun getServiceById(id: Long): Service?
    
    /**
     * Get all services by provider
     */
    fun getServicesByProvider(providerId: Long): Flow<List<Service>>
}
