package com.dealfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealfinder.data.models.Favorite
import com.dealfinder.data.models.Provider
import com.dealfinder.data.search.ProviderSearchItem
import com.dealfinder.di.AppModule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for favorites.
 *
 * Favoriten werden pro Nutzer in Room gespeichert und reaktiv beobachtet,
 * inkl. verifizierter Bewertung und Sonderangebot für die Anzeige.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FavoriteViewModel : ViewModel() {

    private val favoriteRepository = AppModule.provideFavoriteRepository()
    private val providerRepository = AppModule.provideProviderRepository()
    private val ratingRepository = AppModule.provideRatingRepository()
    private val offerRepository = AppModule.provideOfferRepository()
    private val appPreferences = AppModule.provideAppPreferences()

    /** Favoriten-IDs des Nutzers (reaktiv). */
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

    /** Favoriten des Nutzers (reaktiv). */
    val allFavorites: StateFlow<List<Favorite>> = appPreferences.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList()) else favoriteRepository.getFavoritesByUser(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /**
     * Favoriten mit Anbieter, verifizierter Gesamtbewertung und Angebotsstatus –
     * die Anzeige-Grundlage der Favoritenliste.
     */
    val favoriteItems: StateFlow<List<ProviderSearchItem>> = combine(
        allFavorites,
        providerRepository.getAllActivePartners(),
        ratingRepository.getAllVerifiedRatings(),
        offerRepository.getAllActiveOffers()
    ) { favorites, providers, ratings, offers ->
        val providersById = providers.associateBy { it.id }
        val ratingsByProvider = ratings
            .groupBy { it.providerId }
            .mapNotNull { (id, list) -> id to list.map { it.overallScore }.average().toFloat() }
            .toMap()
        val providersWithOffer = offers.map { it.providerId }.toSet()

        favorites.mapNotNull { favorite ->
            val provider = providersById[favorite.providerId] ?: return@mapNotNull null
            ProviderSearchItem(
                provider = provider,
                averageRating = ratingsByProvider[provider.id],
                hasActiveOffer = provider.id in providersWithOffer
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Favoriten-Paare (Favorite, Provider) – kompatibel zur bisherigen API. */
    val favoritesWithProviders: StateFlow<List<Pair<Favorite, Provider>>> =
        favoriteItems.map { items ->
            items.map { item -> Favorite(providerId = item.provider.id, userId = 0) to item.provider }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // UI State
    private val _uiState = MutableStateFlow<FavoriteUiState>(FavoriteUiState.Idle)
    val uiState: StateFlow<FavoriteUiState> = _uiState.asStateFlow()

    sealed class FavoriteUiState {
        object Idle : FavoriteUiState()
        object Loading : FavoriteUiState()
        data class Success(val message: String) : FavoriteUiState()
        data class Error(val message: String) : FavoriteUiState()
    }

    /**
     * Add to favorites
     */
    fun addFavorite(providerId: Long) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            try {
                favoriteRepository.addFavorite(userId, providerId)
                _uiState.value = FavoriteUiState.Success("Zu Favoriten hinzugefügt")
            } catch (e: Exception) {
                _uiState.value = FavoriteUiState.Error("Fehler: ${e.message}")
            }
        }
    }

    /**
     * Remove from favorites
     */
    fun removeFavorite(providerId: Long) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            try {
                favoriteRepository.removeFavorite(userId, providerId)
                _uiState.value = FavoriteUiState.Success("Aus Favoriten entfernt")
            } catch (e: Exception) {
                _uiState.value = FavoriteUiState.Error("Fehler: ${e.message}")
            }
        }
    }

    /**
     * Toggle favorite
     */
    fun toggleFavorite(providerId: Long) {
        if (favoriteIds.value.contains(providerId)) {
            removeFavorite(providerId)
        } else {
            addFavorite(providerId)
        }
    }

    /**
     * Check if provider is favorite
     */
    suspend fun isFavorite(providerId: Long): Boolean {
        return favoriteIds.value.contains(providerId)
    }
}
