package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.ServiceDao
import com.dealfinder.data.models.Service
import com.dealfinder.data.repository.ServiceRepository
import kotlinx.coroutines.flow.Flow

class RoomServiceRepository(
    private val serviceDao: ServiceDao
) : ServiceRepository {
    
    override suspend fun getServiceById(id: Long): Service? = serviceDao.getById(id)
    
    override fun getServicesByProvider(providerId: Long): Flow<List<Service>> {
        return serviceDao.getByProvider(providerId)
    }
}
