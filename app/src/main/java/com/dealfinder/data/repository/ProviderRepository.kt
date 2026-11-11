package com.dealfinder.data.repository

import com.dealfinder.data.models.Provider
import kotlinx.coroutines.flow.Flow

interface ProviderRepository {
    
    /**
     * Get all active Dealfinder partners
     */
    fun getAllActivePartners(): Flow<List<Provider>>
    
    /**
     * Get provider by ID
     */
    suspend fun getProviderById(id: Long): Provider?
    
    /**
     * Search providers by query
     */
    fun searchProviders(query: String): Flow<List<Provider>>
    
    /**
     * Get providers by category
     */
    fun getProvidersByCategory(categoryId: Long): Flow<List<Provider>>
    
    /**
     * Search providers by category and query
     */
    fun searchProvidersByCategory(query: String, categoryId: Long): Flow<List<Provider>>
    
    /**
     * Get provider by name (exact match for demo)
     */
    suspend fun getProviderByName(name: String): Provider?
}
