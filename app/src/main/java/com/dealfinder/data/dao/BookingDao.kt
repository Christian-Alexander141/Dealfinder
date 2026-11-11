package com.dealfinder.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.BookingStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface BookingDao {
    
    @Insert
    suspend fun insert(booking: Booking): Long
    
    @Update
    suspend fun update(booking: Booking)
    
    @Query("SELECT * FROM bookings WHERE id = :id")
    suspend fun getById(id: Long): Booking?
    
    @Query("SELECT * FROM bookings WHERE userId = :userId")
    fun getByUser(userId: Long): Flow<List<Booking>>
    
    @Query("""
        SELECT * FROM bookings 
        WHERE userId = :userId 
        AND bookingDate >= :now
        AND status != 'CANCELLED'
        ORDER BY bookingDate ASC
        """)
    fun getUpcomingByUser(userId: Long, now: Instant): Flow<List<Booking>>
    
    @Query("""
        SELECT * FROM bookings 
        WHERE userId = :userId 
        AND (bookingDate < :now OR status = 'CANCELLED' OR status = 'COMPLETED')
        ORDER BY bookingDate DESC
        """)
    fun getPastByUser(userId: Long, now: Instant): Flow<List<Booking>>
    
    @Query("SELECT * FROM bookings WHERE providerId = :providerId")
    fun getByProvider(providerId: Long): Flow<List<Booking>>
    
    @Query("UPDATE bookings SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: Long, status: BookingStatus)
    
    @Query("DELETE FROM bookings WHERE id = :id")
    suspend fun delete(id: Long)
    
    @Query("DELETE FROM bookings")
    suspend fun deleteAll()
}
