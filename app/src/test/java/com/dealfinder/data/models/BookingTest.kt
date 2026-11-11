package com.dealfinder.data.models

import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Unit tests for Booking data model
 */
class BookingTest {

    @Test
    fun `test Booking creation`() {
        val now = Instant.now()
        val date = LocalDate.of(2025, 6, 15)
        val timeSlot = "10:00 - 11:00"
        
        val booking = Booking(
            id = 0,
            userId = 1L,
            providerId = 1L,
            serviceId = 1L,
            bookingDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
            timeSlot = timeSlot,
            numberOfPeople = 2,
            status = BookingStatus.CONFIRMED,
            createdAt = now,
            notes = "Test booking"
        )
        
        assert(booking.userId == 1L) { "User ID should be 1" }
        assert(booking.providerId == 1L) { "Provider ID should be 1" }
        assert(booking.serviceId == 1L) { "Service ID should be 1" }
        assert(booking.numberOfPeople == 2) { "Number of people should be 2" }
        assert(booking.status == BookingStatus.CONFIRMED) { "Status should be CONFIRMED" }
        assert(booking.notes == "Test booking") { "Notes should match" }
    }

    @Test
    fun `test Booking with nullable notes`() {
        val booking = Booking(
            id = 0,
            userId = 1L,
            providerId = 1L,
            serviceId = 1L,
            bookingDate = Instant.now(),
            timeSlot = "10:00 - 11:00",
            numberOfPeople = 1,
            status = BookingStatus.PENDING,
            createdAt = Instant.now(),
            notes = null
        )
        
        assert(booking.notes == null) { "Notes should be null" }
    }

    @Test
    fun `test Booking equality`() {
        val now = Instant.now()
        val booking1 = Booking(
            id = 1L,
            userId = 1L,
            providerId = 1L,
            serviceId = 1L,
            bookingDate = now,
            timeSlot = "10:00 - 11:00",
            numberOfPeople = 2,
            status = BookingStatus.CONFIRMED,
            createdAt = now,
            notes = "Test"
        )
        
        val booking2 = booking1.copy()
        
        assert(booking1 == booking2) { "Bookings should be equal" }
        assert(booking1.id == booking2.id) { "IDs should be equal" }
    }

    @Test
    fun `test Booking copy`() {
        val now = Instant.now()
        val original = Booking(
            id = 1L,
            userId = 1L,
            providerId = 1L,
            serviceId = 1L,
            bookingDate = now,
            timeSlot = "10:00 - 11:00",
            numberOfPeople = 2,
            status = BookingStatus.CONFIRMED,
            createdAt = now,
            notes = "Test"
        )
        
        val copy = original.copy(
            numberOfPeople = 3,
            status = BookingStatus.CANCELLED
        )
        
        assert(copy.id == original.id) { "ID should be same" }
        assert(copy.numberOfPeople == 3) { "Number of people should be updated" }
        assert(copy.status == BookingStatus.CANCELLED) { "Status should be updated" }
        assert(original.numberOfPeople == 2) { "Original should be unchanged" }
    }

    @Test
    fun `test BookingStatus values`() {
        val statuses = listOf(
            BookingStatus.PENDING,
            BookingStatus.CONFIRMED,
            BookingStatus.CANCELLED,
            BookingStatus.COMPLETED
        )
        
        assert(statuses.size == 4) { "Should have 4 booking statuses" }
        assert(statuses.contains(BookingStatus.PENDING)) { "Should contain PENDING" }
        assert(statuses.contains(BookingStatus.CONFIRMED)) { "Should contain CONFIRMED" }
    }
}
