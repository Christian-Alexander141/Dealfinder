package com.dealfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealfinder.data.models.Provider
import com.dealfinder.data.models.Service
import com.dealfinder.data.models.VerifiedRating
import com.dealfinder.data.search.ProviderSearch
import com.dealfinder.data.search.ProviderSearchItem
import com.dealfinder.data.search.SearchFilters
import com.dealfinder.data.search.SortOrder
import com.dealfinder.di.AppModule
import com.dealfinder.util.GeoDistance
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for provider details and search.
 *
 * Reihenfolge der Zustände:
 * - Suchfilter (Query, Kategorie, Bewertung, Entfernung, Angebote, Sortierung)
 * - [searchState] kombiniert Anbieter, verifizierte Bewertungen, Angebote,
 *   Standort und Filter über die reine Logik in [ProviderSearch].
 * - Detailzustand (Anbieter, Services, Bewertungen, Favoriten, Angebote)
 */
class ProviderViewModel : ViewModel() {

    private val providerRepository = AppModule.provideProviderRepository()
    private val serviceRepository = AppModule.provideServiceRepository()
    private val ratingRepository = AppModule.provideRatingRepository()
    private val favoriteRepository = AppModule.provideFavoriteRepository()
    private val offerRepository = AppModule.provideOfferRepository()
    private val categoryRepository = AppModule.provideCategoryRepository()
    private val appPreferences = AppModule.provideAppPreferences()

    // --- Suchfilter (einzeln, damit die UI sie direkt beobachten kann) ---

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _locationQuery = MutableStateFlow("")
    val locationQuery: StateFlow<String> = _locationQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<Long?>(null)
    val selectedCategoryId: StateFlow<Long?> = _selectedCategoryId.asStateFlow()

    private val _minRating = MutableStateFlow<Float?>(null)
    val minRating: StateFlow<Float?> = _minRating.asStateFlow()

    private val _maxDistanceKm = MutableStateFlow<Double?>(null)
    val maxDistanceKm: StateFlow<Double?> = _maxDistanceKm.asStateFlow()

    private val _onlyWithOffers = MutableStateFlow(false)
    val onlyWithOffers: StateFlow<Boolean> = _onlyWithOffers.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.RELEVANCE)
    val sortOrder: StateFlow<SortOrder> = _sortOrder.asStateFlow()

    private val _selectedProviderId = MutableStateFlow<Long?>(null)
    val selectedProviderId: StateFlow<Long?> = _selectedProviderId.asStateFlow()

    // --- Alle aktiven Partner (Basis der Suche) ---
    val allProviders = providerRepository.getAllActivePartners()

    // --- Kategorien (für Filter-Chips) ---
    val categories = categoryRepository.getAllCategories()

    // --- Aktueller Nutzerstandort (DataStore; null, wenn nie ermittelt) ---
    val userLocation: StateFlow<Pair<Double, Double>?> = combine(
        appPreferences.lastLocationLat,
        appPreferences.lastLocationLon
    ) { lat, lon ->
        if (lat != null && lon != null) lat to lon else null
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    // --- Favoriten des Nutzers (reaktiv aus Room) ---
    @OptIn(ExperimentalCoroutinesApi::class)
    val favoriteIds: StateFlow<Set<Long>> = appPreferences.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) {
                flowOf(emptySet())
            } else {
                favoriteRepository.getFavoritesByUser(userId)
                    .map { favorites -> favorites.map { it.providerId }.toSet() }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** Kombinierter Suchfilter für die Ergebnisliste. */
    private val filtersFlow: StateFlow<SearchFilters> = combine(
        _searchQuery,
        _locationQuery,
        _selectedCategoryId,
        _minRating,
        _maxDistanceKm
    ) { query, location, categoryId, minRating, maxDistance ->
        SearchFilters(
            query = query,
            locationQuery = location,
            categoryId = categoryId,
            minRating = minRating,
            maxDistanceKm = maxDistance
        )
    }.combine(_onlyWithOffers) { filters, onlyOffers ->
        filters.copy(onlyWithOffers = onlyOffers)
    }.combine(_sortOrder) { filters, sortOrder ->
        filters.copy(sortOrder = sortOrder)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchFilters())

    /** Vollständiger Filterzustand (inkl. Standort) – für die UI. */
    val searchFilters: StateFlow<SearchFilters> = combine(
        filtersFlow, userLocation
    ) { filters, location ->
        filters.copy(userLat = location?.first, userLon = location?.second)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchFilters())

    /**
     * Suchergebnisse: reaktiver Zustand mit gefilterten/sortierten Anbietern,
     * inkl. verifizierter Bewertung, Angebot und Entfernung.
     */
    val searchState: StateFlow<SearchState> = combine(
        providerRepository.getAllActivePartners(),
        ratingRepository.getAllVerifiedRatings(),
        offerRepository.getAllActiveOffers(),
        searchFilters
    ) { providers, ratings, offers, filters ->
        // Nur VERIFIZIERTE Testbewertungen fließen in die Gesamtbewertung ein
        val ratingsByProvider = ratings
            .groupBy { it.providerId }
            .mapNotNull { (providerId, list) ->
                providerId to list.map { it.overallScore }.average().toFloat()
            }
            .toMap()

        val providerIdsWithOffer = offers.map { it.providerId }.toSet()

        val items = providers.map { provider ->
            val distance = if (filters.hasUserLocation &&
                provider.latitude != null && provider.longitude != null
            ) {
                GeoDistance.betweenKm(
                    filters.userLat!!, filters.userLon!!,
                    provider.latitude!!, provider.longitude!!
                )
            } else {
                null
            }
            ProviderSearchItem(provider = provider, distanceKm = distance)
        }

        SearchState(
            isLoading = false,
            items = ProviderSearch.build(
                items = items,
                filters = filters,
                verifiedRatingsByProvider = ratingsByProvider,
                providerIdsWithOffer = providerIdsWithOffer
            )
        )
    }.catch { error ->
        // Fehlerzustand abfangen, damit die UI keinen Absturz sieht
        emit(SearchState(isLoading = false, error = "Fehler bei der Suche: ${error.message}"))
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SearchState())

    // --- Detailzustand ---

    private val _selectedProvider = MutableStateFlow<Provider?>(null)
    val selectedProvider: StateFlow<Provider?> = _selectedProvider.asStateFlow()

    private val _services = MutableStateFlow<List<Service>>(emptyList())
    val services: StateFlow<List<Service>> = _services.asStateFlow()

    private val _ratings = MutableStateFlow<List<VerifiedRating>>(emptyList())
    val ratings: StateFlow<List<VerifiedRating>> = _ratings.asStateFlow()

    private val _averageScore = MutableStateFlow<Float?>(null)
    val averageScore: StateFlow<Float?> = _averageScore.asStateFlow()

    /** Ob der aktuell geöffnete Anbieter ein Favorit des Nutzers ist. */
    val isFavorite: StateFlow<Boolean> = combine(favoriteIds, _selectedProviderId) { ids, providerId ->
        providerId != null && ids.contains(providerId)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _offers = MutableStateFlow<List<com.dealfinder.data.models.Offer>>(emptyList())
    val offers: StateFlow<List<com.dealfinder.data.models.Offer>> = _offers.asStateFlow()

    private val _uiState = MutableStateFlow<ProviderUiState>(ProviderUiState.Idle)
    val uiState: StateFlow<ProviderUiState> = _uiState.asStateFlow()

    sealed class ProviderUiState {
        object Idle : ProviderUiState()
        object Loading : ProviderUiState()
        data class Error(val message: String) : ProviderUiState()
        object Success : ProviderUiState()
    }

    // Laufender Job für den Detail-Ladevorgang (wird beim Wechsel des Anbieters abgebrochen)
    private var detailJob: kotlinx.coroutines.Job? = null

    init {
        // Observe selected provider and load related data
        viewModelScope.launch {
            _selectedProviderId.collect { providerId ->
                if (providerId != null) {
                    loadProviderDetails(providerId)
                }
            }
        }
    }

    // --- Filter-Setter ---

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun onLocationQueryChanged(location: String) {
        _locationQuery.value = location
    }

    fun onCategorySelected(categoryId: Long?) {
        _selectedCategoryId.value = categoryId
    }

    fun onMinRatingChanged(minRating: Float?) {
        _minRating.value = minRating
    }

    fun onMaxDistanceChanged(maxDistanceKm: Double?) {
        _maxDistanceKm.value = maxDistanceKm
    }

    fun onOnlyWithOffersChanged(onlyWithOffers: Boolean) {
        _onlyWithOffers.value = onlyWithOffers
    }

    fun onSortOrderChanged(sortOrder: SortOrder) {
        _sortOrder.value = sortOrder
    }

    /** Alle Filter einschließlich Suchbegriff zurücksetzen. */
    fun clearFilters() {
        _searchQuery.value = ""
        _locationQuery.value = ""
        _selectedCategoryId.value = null
        _minRating.value = null
        _maxDistanceKm.value = null
        _onlyWithOffers.value = false
        _sortOrder.value = SortOrder.RELEVANCE
    }

    // --- Favoriten ---

    fun toggleFavorite(providerId: Long) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            try {
                if (favoriteIds.value.contains(providerId)) {
                    favoriteRepository.removeFavorite(userId, providerId)
                } else {
                    favoriteRepository.addFavorite(userId, providerId)
                }
            } catch (e: Exception) {
                _uiState.value = ProviderUiState.Error("Fehler beim Aktualisieren der Favoriten: ${e.message}")
            }
        }
    }

    // --- Detail ---

    fun selectProvider(providerId: Long) {
        _selectedProviderId.value = providerId
    }

    /**
     * Load provider details.
     *
     * Room-Flows enden nie, daher dürfen die Einzel-Flows nicht sequenziell
     * mit `.collect` abgefragt werden (der erste Collect würde die nachfolgenden
     * blockieren). Stattdessen laufen sie parallel als Kind-Jobs des Lade-Jobs.
     */
    private fun loadProviderDetails(providerId: Long) {
        detailJob?.cancel()
        // Alte Daten eines vorherigen Anbieters nicht einblenden
        _services.value = emptyList()
        _ratings.value = emptyList()
        _averageScore.value = null
        _offers.value = emptyList()

        detailJob = viewModelScope.launch {
            _uiState.value = ProviderUiState.Loading

            try {
                // Load provider (one-shot)
                _selectedProvider.value = providerRepository.getProviderById(providerId)

                // Reactive Daten parallel beobachten
                launch {
                    serviceRepository.getServicesByProvider(providerId).collect { services ->
                        _services.value = services
                    }
                }
                launch {
                    ratingRepository.getRatingsByProvider(providerId).collect { ratings ->
                        _ratings.value = ratings
                    }
                }
                launch {
                    ratingRepository.getAverageScore(providerId).collect { score ->
                        _averageScore.value = score
                    }
                }
                launch {
                    offerRepository.getOffersByProvider(providerId).collect { offers ->
                        _offers.value = offers
                    }
                }

                _uiState.value = ProviderUiState.Success
            } catch (e: Exception) {
                _uiState.value = ProviderUiState.Error("Fehler beim Laden der Anbieterdaten: ${e.message}")
            }
        }
    }

    /** Get provider by ID (one-shot). */
    suspend fun getProviderById(id: Long): Provider? {
        return providerRepository.getProviderById(id)
    }

    /** Kategorie-Name für einen Anbieter (für die Ergebnisdarstellung). */
    suspend fun getCategoryName(categoryId: Long?): String? {
        if (categoryId == null) return null
        return categoryRepository.getCategoryById(categoryId)?.name
    }
}

/**
 * UI-Zustand der Suchergebnisliste.
 */
data class SearchState(
    val isLoading: Boolean = true,
    val items: List<ProviderSearchItem> = emptyList(),
    val error: String? = null
)
