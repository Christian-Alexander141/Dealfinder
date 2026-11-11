package com.dealfinder.data.repository

import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.BookingStatus
import kotlinx.coroutines.flow.Flow

interface BookingRepository {
    
    /**
     * Get all bookings for a user
     */
    fun getBookingsByUser(userId: Long): Flow<List<Booking>>
    
    /**
     * Get upcoming bookings for a user
     */
    fun getUpcomingBookings(userId: Long): Flow<List<Booking>>
    
    /**
     * Get past bookings for a user
     */
    fun getPastBookings(userId: Long): Flow<List<Booking>>
    
    /**
     * Get booking by ID
     */
    suspend fun getBookingById(id: Long): Booking?
    
    /**
     * Create a new booking
     */
    suspend fun createBooking(booking: Booking): Long
    
    /**
     * Update booking
     */
    suspend fun updateBooking(booking: Booking)
    
    /**
     * Cancel booking
     */
    suspend fun cancelBooking(bookingId: Long)
    
    /**
     * Delete booking
     */
    suspend fun deleteBooking(bookingId: Long)
}
