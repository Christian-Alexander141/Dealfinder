package com.dealfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.BookingStatus
import com.dealfinder.data.models.BookingType
import com.dealfinder.data.models.Service
import com.dealfinder.di.AppModule
import com.dealfinder.util.BookingTimeSlots
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Anzeige-Buchung inkl. aufgelöster Namen (für Listen und Details).
 */
data class BookingDisplayItem(
    val booking: Booking,
    val providerName: String,
    val serviceName: String?
)

/**
 * ViewModel for booking functionality
 */
class BookingViewModel : ViewModel() {
    
    private val bookingRepository = AppModule.provideBookingRepository()
    private val serviceRepository = AppModule.provideServiceRepository()
    private val providerRepository = AppModule.provideProviderRepository()
    private val appPreferences = AppModule.provideAppPreferences()
    
    // State
    private val _selectedProviderId = MutableStateFlow<Long?>(null)
    val selectedProviderId: StateFlow<Long?> = _selectedProviderId.asStateFlow()
    
    private val _selectedServiceId = MutableStateFlow<Long?>(null)
    val selectedServiceId: StateFlow<Long?> = _selectedServiceId.asStateFlow()
    
    private val _selectedDate = MutableStateFlow<LocalDate?>(null)
    val selectedDate: StateFlow<LocalDate?> = _selectedDate.asStateFlow()
    
    private val _selectedTimeSlot = MutableStateFlow<String?>(null)
    val selectedTimeSlot: StateFlow<String?> = _selectedTimeSlot.asStateFlow()
    
    private val _numberOfPeople = MutableStateFlow(1)
    val numberOfPeople: StateFlow<Int> = _numberOfPeople.asStateFlow()
    
    private val _notes = MutableStateFlow("")
    val notes: StateFlow<String> = _notes.asStateFlow()
    
    // Provider details
    private val _providerName = MutableStateFlow<String?>(null)
    val providerName: StateFlow<String?> = _providerName.asStateFlow()
    
    // Service details
    private val _service = MutableStateFlow<Service?>(null)
    val service: StateFlow<Service?> = _service.asStateFlow()
    
    // Available services for provider
    val services = MutableStateFlow<List<Service>>(emptyList())
    
    // Available time slots
    val availableTimeSlots = MutableStateFlow<List<String>>(emptyList())
    
    // My bookings
    val myBookings = MutableStateFlow<List<Booking>>(emptyList())
    val upcomingBookings = MutableStateFlow<List<Booking>>(emptyList())
    val pastBookings = MutableStateFlow<List<Booking>>(emptyList())
    
    // UI State
    private val _uiState = MutableStateFlow<BookingUiState>(BookingUiState.Idle)
    val uiState: StateFlow<BookingUiState> = _uiState.asStateFlow()

    // Lade-Jobs für die Buchungsliste (verhindert doppelte parallele Collects)
    private var bookingsJob: kotlinx.coroutines.Job? = null
    private var servicesJob: kotlinx.coroutines.Job? = null
    
    sealed class BookingUiState {
        object Idle : BookingUiState()
        object Loading : BookingUiState()
        data class Success(val bookingId: Long) : BookingUiState()
        data class Error(val message: String) : BookingUiState()
    }
    
    init {
        // Load my bookings
        loadMyBookings()
    }
    
    /**
     * Select provider for booking
     */
    fun selectProvider(providerId: Long) {
        _selectedProviderId.value = providerId
        loadProviderServices(providerId)
    }
    
    /**
     * Select service for booking
     */
    fun selectService(serviceId: Long) {
        _selectedServiceId.value = serviceId
        _service.value = null // Alte Auswahl nicht einblenden
        viewModelScope.launch {
            _service.value = serviceRepository.getServiceById(serviceId)
        }
    }
    
    /**
     * Select date for booking
     */
    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
        generateTimeSlots(date)
    }
    
    /**
     * Select time slot for booking
     */
    fun selectTimeSlot(timeSlot: String) {
        _selectedTimeSlot.value = timeSlot
    }
    
    /**
     * Update number of people
     */
    fun updateNumberOfPeople(count: Int) {
        _numberOfPeople.value = count.coerceAtLeast(1)
    }
    
    /**
     * Update notes
     */
    fun updateNotes(notes: String) {
        _notes.value = notes
    }
    
    /**
     * Load provider services.
     * Vorherige Kollektoren werden abgebrochen, damit keine Doppel-Collects
     * von Room-Flows entstehen (Screens rufen selectProvider mehrfach auf).
     */
    private fun loadProviderServices(providerId: Long) {
        servicesJob?.cancel()
        servicesJob = viewModelScope.launch {
            serviceRepository.getServicesByProvider(providerId)
                .collect { services ->
                    this@BookingViewModel.services.value = services
                }
        }
    }
    
    /**
     * Generate demo time slots for a date.
     *
     * Die Fenster richten sich nach der Dauer der gewählten Dienstleistung
     * (9–18 Uhr) und enthalten keine vergangenen Slots. Da noch kein
     * Anbieter-Kalender angebunden ist, sind sie lokale Demo-Daten und
     * werden in der UI entsprechend gekennzeichnet.
     */
    private fun generateTimeSlots(date: LocalDate) {
        viewModelScope.launch {
            val duration = _service.value?.durationMinutes?.takeIf { it > 0 }
                ?: _selectedServiceId.value
                    ?.let { id -> serviceRepository.getServiceById(id)?.durationMinutes }
                    ?.takeIf { it > 0 }
                ?: 60

            availableTimeSlots.value = BookingTimeSlots.generate(
                date = date,
                durationMinutes = duration
            )
        }
    }
    
    /**
     * Load my bookings.
     *
     * Room-Flows enden nie – die drei Listen werden daher parallel als
     * Kind-Jobs beobachtet, statt nacheinander zu sammeln (sonst würde der
     * erste Collect die anderen für immer blockieren).
     */
    private fun loadMyBookings() {
        bookingsJob?.cancel()
        bookingsJob = viewModelScope.launch {
            val userId = appPreferences.currentUserId.first()
            if (userId == null) {
                myBookings.value = emptyList()
                upcomingBookings.value = emptyList()
                pastBookings.value = emptyList()
                return@launch
            }

            launch {
                bookingRepository.getBookingsByUser(userId).collect { bookings ->
                    myBookings.value = bookings
                }
            }
            launch {
                bookingRepository.getUpcomingBookings(userId).collect { bookings ->
                    upcomingBookings.value = bookings
                }
            }
            launch {
                bookingRepository.getPastBookings(userId).collect { bookings ->
                    pastBookings.value = bookings
                }
            }
        }
    }
    
    /**
     * Create a new booking
     */
    fun createBooking(providerId: Long, serviceId: Long, date: LocalDate, timeSlot: String): Boolean {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading
            
            val userId = appPreferences.currentUserId.first()
            if (userId == null) {
                _uiState.value = BookingUiState.Error("Benutzer nicht angemeldet")
                return@launch
            }
            
            try {
                // Create booking
                val booking = Booking(
                    userId = userId,
                    providerId = providerId,
                    serviceId = serviceId,
                    bookingDate = date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                    timeSlot = timeSlot,
                    numberOfPeople = _numberOfPeople.value,
                    status = BookingStatus.CONFIRMED,
                    createdAt = Instant.now(),
                    notes = _notes.value.ifEmpty { null }
                )
                
                val bookingId = bookingRepository.createBooking(booking)
                
                // Reset selection
                _selectedProviderId.value = null
                _selectedServiceId.value = null
                _selectedDate.value = null
                _selectedTimeSlot.value = null
                _numberOfPeople.value = 1
                _notes.value = ""
                
                // Reload bookings
                loadMyBookings()
                
                _uiState.value = BookingUiState.Success(bookingId)
            } catch (e: Exception) {
                _uiState.value = BookingUiState.Error("Fehler beim Erstellen der Buchung: ${e.message}")
            }
        }
        
        return true
    }
    
    /**
     * Cancel booking
     */
    fun cancelBooking(bookingId: Long) {
        viewModelScope.launch {
            _uiState.value = BookingUiState.Loading
            
            try {
                bookingRepository.cancelBooking(bookingId)
                loadMyBookings()
                _uiState.value = BookingUiState.Success(bookingId)
            } catch (e: Exception) {
                _uiState.value = BookingUiState.Error("Fehler beim Stornieren der Buchung: ${e.message}")
            }
        }
    }
    
    /**
     * Get booking by ID
     */
    suspend fun getBookingById(id: Long): Booking? {
        return bookingRepository.getBookingById(id)
    }

    /** Anbieter-Name für eine Buchungsanzeige. */
    suspend fun getProviderName(providerId: Long): String? {
        return providerRepository.getProviderById(providerId)?.name
    }

    /** Dienstleistung-Name für eine Buchungsanzeige. */
    suspend fun getServiceName(serviceId: Long?): String? {
        if (serviceId == null) return null
        return serviceRepository.getServiceById(serviceId)?.name
    }

    /**
     * Liefert Buchungen angereichert mit Anbieter- und Leistungsnamen
     * (ein DB-Zugriff pro Buchung, für die Liste unkritisch).
     */
    suspend fun enrichBookings(bookings: List<Booking>): List<BookingDisplayItem> {
        return bookings.map { booking ->
            BookingDisplayItem(
                booking = booking,
                providerName = getProviderName(booking.providerId) ?: "Unbekannter Anbieter",
                serviceName = getServiceName(booking.serviceId)
            )
        }
    }

    /** Reset booking flow
     */
    fun resetBookingFlow() {
        _selectedProviderId.value = null
        _selectedServiceId.value = null
        _selectedDate.value = null
        _selectedTimeSlot.value = null
        _numberOfPeople.value = 1
        _notes.value = ""
        _uiState.value = BookingUiState.Idle
    }
}
