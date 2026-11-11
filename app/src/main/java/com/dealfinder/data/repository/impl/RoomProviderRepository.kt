package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.ProviderDao
import com.dealfinder.data.models.Provider
import com.dealfinder.data.repository.ProviderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class RoomProviderRepository(
    private val providerDao: ProviderDao
) : ProviderRepository {
    
    override fun getAllActivePartners(): Flow<List<Provider>> = providerDao.getAllActivePartners()
    
    override suspend fun getProviderById(id: Long): Provider? = providerDao.getById(id)
    
    override fun searchProviders(query: String): Flow<List<Provider>> {
        val searchPattern = "%$query%"
        return providerDao.searchActivePartners(searchPattern)
    }
    
    override fun getProvidersByCategory(categoryId: Long): Flow<List<Provider>> {
        return providerDao.getByCategory(categoryId)
    }
    
    override fun searchProvidersByCategory(query: String, categoryId: Long): Flow<List<Provider>> {
        val searchPattern = "%$query%"
        return providerDao.searchByCategory(searchPattern, categoryId)
    }
    
    override suspend fun getProviderByName(name: String): Provider? {
        val allProviders = providerDao.getAllActivePartners().first()
        return allProviders.firstOrNull { it.name == name }
    }
}
