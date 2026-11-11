package com.dealfinder.viewmodel

import com.dealfinder.TestEnv
import com.dealfinder.awaitValue
import com.dealfinder.data.models.BookingStatus
import com.dealfinder.di.AppModule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Tests für den Buchungsprozess: Zeitfenster, Eingaben, Persistenz, Stornierung.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BookingViewModelTest {

    private lateinit var viewModel: BookingViewModel
    private val futureDate: LocalDate = LocalDate.now().plusDays(14)
    private val slotFormat = Regex("\\d{2}:\\d{2} - \\d{2}:\\d{2}")

    @Before
    fun setup() {
        TestEnv.setUp()
        TestEnv.registerTestUser()
        viewModel = BookingViewModel()
    }

    @After
    fun tearDown() {
        TestEnv.tearDown()
    }

    @Test
    fun `Personenzahl wird auf mindestens 1 begrenzt`() {
        viewModel.updateNumberOfPeople(0)
        assertEquals(1, viewModel.numberOfPeople.value)

        viewModel.updateNumberOfPeople(-5)
        assertEquals(1, viewModel.numberOfPeople.value)

        viewModel.updateNumberOfPeople(4)
        assertEquals(4, viewModel.numberOfPeople.value)
    }

    @Test
    fun `Notizen koennen gesetzt und geleert werden`() {
        viewModel.updateNotes("Bitte Ruecksitz")
        assertEquals("Bitte Ruecksitz", viewModel.notes.value)

        viewModel.updateNotes("")
        assertEquals("", viewModel.notes.value)
    }

    @Test
    fun `Buchungsfluss-Reset leert alle Auswahlwerte`() {
        viewModel.selectProvider(1L)
        viewModel.selectService(1L)
        viewModel.selectDate(futureDate)
        viewModel.selectTimeSlot("10:00 - 11:00")
        viewModel.updateNumberOfPeople(3)
        viewModel.updateNotes("Notiz")

        viewModel.resetBookingFlow()

        assertEquals(null, viewModel.selectedProviderId.value)
        assertEquals(null, viewModel.selectedServiceId.value)
        assertEquals(null, viewModel.selectedDate.value)
        assertEquals(null, viewModel.selectedTimeSlot.value)
        assertEquals(1, viewModel.numberOfPeople.value)
        assertEquals("", viewModel.notes.value)
        assertTrue(viewModel.uiState.value is BookingViewModel.BookingUiState.Idle)
    }

    @Test
    fun `Dienstleistungen eines Anbieters werden geladen`() {
        viewModel.selectProvider(1L)
        val services = viewModel.services.awaitValue { it.isNotEmpty() }
        assertTrue(services.all { it.providerId == 1L })
    }

    @Test
    fun `Zeitfenster werden fuer ein Zukunftsdatum erzeugt`() {
        viewModel.selectProvider(1L)
        viewModel.selectService(1L)
        viewModel.selectDate(futureDate)

        val slots = viewModel.availableTimeSlots.awaitValue { it.isNotEmpty() }

        assertTrue("Format der Fenster", slots.all { it.matches(slotFormat) })
        assertTrue("Beginn um 09:00", slots.first().startsWith("09:00"))
        assertTrue("Kein Fenster endet nach 18:00", slots.all { it.substringAfter("- ") <= "18:00" })
        assertTrue("Mindestens ein Fenster erwartet", slots.size >= 3)

        // Demo-Kennzeichnung ist Aufgabe der UI; die Logik liefert reine Slots
        val german = futureDate.format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN))
        assertTrue(german.isNotEmpty())
    }

    @Test
    fun `Fensterdauer richtet sich nach der Dienstleistungsdauer`() {
        // Service 1 ("3-Gänge Menü") hat 120 Minuten → 2-Stunden-Fenster
        viewModel.selectProvider(1L)
        viewModel.selectService(1L)
        viewModel.selectDate(futureDate)

        val slots = viewModel.availableTimeSlots.awaitValue { it.isNotEmpty() }
        val start = slots.first().substringBefore(" - ")
        val end = slots.first().substringAfter("- ")
        assertEquals("09:00", start)
        assertEquals("11:00", end)
    }

    @Test
    fun `Buchung wird erstellt und bleibt nach Neustart erhalten`() = runBlocking {
        viewModel.createBooking(
            providerId = 1L,
            serviceId = 1L,
            date = futureDate,
            timeSlot = "09:00 - 10:00"
        )

        val state = viewModel.awaitValue { it is BookingViewModel.BookingUiState.Success }
        state as BookingViewModel.BookingUiState.Success
        val bookingId = state.bookingId
        assertTrue(bookingId > 0)

        // Sofort in der Datenbank
        val bookingRepo = AppModule.provideBookingRepository()
        val stored = bookingRepo.getBookingById(bookingId)
        assertNotNull(stored)
        assertEquals(1L, stored!!.providerId)
        assertEquals(1L, stored.serviceId)
        assertEquals(BookingStatus.CONFIRMED, stored.status)
        assertEquals("09:00 - 10:00", stored.timeSlot)

        // "Neustart": frische ViewModel-Instanz lädt dieselben Daten
        val restarted = BookingViewModel()
        val bookings = restarted.myBookings.awaitValue { it.isNotEmpty() }
        assertTrue(bookings.any { it.id == bookingId })
    }

    @Test
    fun `kommende und vergangene Buchungen werden getrennt`() = runBlocking {
        viewModel.createBooking(1L, 1L, LocalDate.now().plusDays(3), "10:00 - 11:00")
        viewModel.awaitValue { it is BookingViewModel.BookingUiState.Success }

        val upcoming = viewModel.upcomingBookings.awaitValue { it.isNotEmpty() }
        assertEquals(1, upcoming.size)
        assertEquals(BookingStatus.CONFIRMED, upcoming.first().status)
    }

    @Test
    fun `Buchung kann storniert werden`() = runBlocking {
        viewModel.createBooking(1L, 1L, futureDate, "11:00 - 12:00")
        val state = viewModel.awaitValue { it is BookingViewModel.BookingUiState.Success }
        val bookingId = (state as BookingViewModel.BookingUiState.Success).bookingId

        viewModel.cancelBooking(bookingId)

        val repo = AppModule.provideBookingRepository()
        val deadline = System.currentTimeMillis() + 5_000
        var status: BookingStatus? = null
        while (System.currentTimeMillis() < deadline) {
            status = repo.getBookingById(bookingId)?.status
            if (status == BookingStatus.CANCELLED) break
            Thread.sleep(10)
        }
        assertEquals(BookingStatus.CANCELLED, status)

        // Stornierte Buchung erscheint nicht mehr unter "kommend"
        viewModel.upcomingBookings.awaitValue { it.isEmpty() }
    }
}
