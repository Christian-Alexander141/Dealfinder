package com.dealfinder.data.repository.impl

import com.dealfinder.data.dao.BookingDao
import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.BookingStatus
import com.dealfinder.data.repository.BookingRepository
import kotlinx.coroutines.flow.Flow
import java.time.Instant

class RoomBookingRepository(
    private val bookingDao: BookingDao
) : BookingRepository {
    
    override fun getBookingsByUser(userId: Long): Flow<List<Booking>> {
        return bookingDao.getByUser(userId)
    }
    
    override fun getUpcomingBookings(userId: Long): Flow<List<Booking>> {
        return bookingDao.getUpcomingByUser(userId, Instant.now())
    }
    
    override fun getPastBookings(userId: Long): Flow<List<Booking>> {
        return bookingDao.getPastByUser(userId, Instant.now())
    }
    
    override suspend fun getBookingById(id: Long): Booking? = bookingDao.getById(id)
    
    override suspend fun createBooking(booking: Booking): Long {
        return bookingDao.insert(booking)
    }
    
    override suspend fun updateBooking(booking: Booking) {
        bookingDao.update(booking)
    }
    
    override suspend fun cancelBooking(bookingId: Long) {
        bookingDao.updateStatus(bookingId, BookingStatus.CANCELLED)
    }
    
    override suspend fun deleteBooking(bookingId: Long) {
        bookingDao.delete(bookingId)
    }
}
