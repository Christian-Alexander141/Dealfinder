package com.dealfinder.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Unit-Tests für die Demo-Zeitfenster-Logik (kein echter Kalender).
 */
class BookingTimeSlotsTest {

    private val format = Regex("\\d{2}:\\d{2} - \\d{2}:\\d{2}")

    @Test
    fun `vergangene Tage liefern keine Zeitfenster`() {
        val past = LocalDate.of(2020, 6, 15)
        assertTrue(BookingTimeSlots.generate(past, 60, now = LocalDateTime.of(2026, 6, 1, 8, 0)).isEmpty())
    }

    @Test
    fun `Zukunftstag mit 60 Minuten Dauer liefert 9 Fenster von 9 bis 18 Uhr`() {
        val date = LocalDate.of(2030, 6, 15)
        val slots = BookingTimeSlots.generate(date, 60, now = LocalDateTime.of(2026, 1, 1, 8, 0))

        assertEquals(9, slots.size)
        assertEquals("09:00 - 10:00", slots.first())
        assertEquals("17:00 - 18:00", slots.last())
        slots.forEach { assertTrue("Format: $it", it.matches(format)) }
    }

    @Test
    fun `laengere Dauer erzeugt groessere Fenster`() {
        val date = LocalDate.of(2030, 6, 15)
        val slots = BookingTimeSlots.generate(date, 90, now = LocalDateTime.of(2026, 1, 1, 8, 0))

        assertEquals(6, slots.size)
        assertEquals("09:00 - 10:30", slots.first())
        assertEquals("16:30 - 18:00", slots.last())
    }

    @Test
    fun `heutige Fenster in der Vergangenheit werden ausgelassen`() {
        val date = LocalDate.of(2030, 6, 15)
        val now = LocalDateTime.of(2030, 6, 15, 14, 0)
        val slots = BookingTimeSlots.generate(date, 60, now = now)

        assertEquals(listOf("15:00 - 16:00", "16:00 - 17:00", "17:00 - 18:00"), slots)
    }

    @Test
    fun `Dauer wird auf sinnvollen Bereich begrenzt`() {
        val date = LocalDate.of(2030, 6, 15)
        // 0 Minuten werden auf 15 Minuten begrenzt
        val slots = BookingTimeSlots.generate(date, 0, now = LocalDateTime.of(2026, 1, 1, 8, 0))
        assertTrue(slots.isNotEmpty())
        assertTrue(slots.size <= BookingTimeSlots.generate(date, 15).size)
    }
}
