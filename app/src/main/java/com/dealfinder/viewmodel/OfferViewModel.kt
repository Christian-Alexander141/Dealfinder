package com.dealfinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.dealfinder.data.models.Offer
import com.dealfinder.di.AppModule
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * ViewModel for offers and vouchers.
 *
 * Saved vouchers are persisted in Room (per user), so they survive a restart.
 */
class OfferViewModel : ViewModel() {
    
    private val offerRepository = AppModule.provideOfferRepository()
    private val appPreferences = AppModule.provideAppPreferences()
    
    // All active offers (reactive)
    val allOffers: StateFlow<List<Offer>> = offerRepository.getAllActiveOffers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    // Offers sorted by expiry (reactive)
    val offersByExpiry: StateFlow<List<Offer>> = offerRepository.getActiveOffersSortedByExpiry()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    // Offers saved ("vorgemerkt") by the current user – persisted in Room.
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val mySavedOffers: StateFlow<List<Offer>> = appPreferences.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList()) else offerRepository.getSavedOffers(userId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())
    
    // IDs of saved offers for cheap "is saved" checks in lists
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val savedOfferIds: StateFlow<Set<Long>> = appPreferences.currentUserId
        .flatMapLatest { userId ->
            if (userId == null) flowOf(emptyList<Offer>()) else offerRepository.getSavedOffers(userId)
        }
        .map { offers -> offers.map { it.id }.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())
    
    // Selected offer
    private val _selectedOfferId = MutableStateFlow<Long?>(null)
    val selectedOfferId: StateFlow<Long?> = _selectedOfferId.asStateFlow()
    
    private val _selectedOffer = MutableStateFlow<Offer?>(null)
    val selectedOffer: StateFlow<Offer?> = _selectedOffer.asStateFlow()
    
    // UI State
    private val _uiState = MutableStateFlow<OfferUiState>(OfferUiState.Idle)
    val uiState: StateFlow<OfferUiState> = _uiState.asStateFlow()
    
    sealed class OfferUiState {
        object Idle : OfferUiState()
        object Loading : OfferUiState()
        data class Success(val message: String) : OfferUiState()
        data class Error(val message: String) : OfferUiState()
    }
    
    /**
     * Load offer by ID for the detail screen.
     */
    fun selectOffer(offerId: Long) {
        _selectedOfferId.value = offerId
        viewModelScope.launch {
            _uiState.value = OfferUiState.Loading
            try {
                _selectedOffer.value = offerRepository.getOfferById(offerId)
                _uiState.value = OfferUiState.Success("Angebot geladen")
            } catch (e: Exception) {
                _uiState.value = OfferUiState.Error("Fehler beim Laden: ${e.message}")
            }
        }
    }
    
    /**
     * Get offer by ID (one-shot, e.g. for previews/screens without selection state).
     */
    suspend fun getOfferById(id: Long): Offer? {
        return offerRepository.getOfferById(id)
    }
    
    /**
     * Get offers by provider
     */
    suspend fun getOffersByProvider(providerId: Long): List<Offer> {
        return offerRepository.getActiveOffersByProvider(providerId)
    }
    
    /**
     * Save ("vormerken") an offer for the current user. Persisted locally.
     */
    fun saveOffer(offer: Offer) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first()
            if (userId == null) {
                _uiState.value = OfferUiState.Error("Bitte zuerst ein Benutzerkonto anlegen")
                return@launch
            }
            
            try {
                offerRepository.saveOffer(userId, offer.id)
                _uiState.value = OfferUiState.Success("Gutschein vorgemerkt")
            } catch (e: Exception) {
                _uiState.value = OfferUiState.Error("Fehler beim Speichern: ${e.message}")
            }
        }
    }
    
    /**
     * Remove a saved offer again.
     */
    fun removeSavedOffer(offer: Offer) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first()
            if (userId == null) return@launch
            
            try {
                offerRepository.removeSavedOffer(userId, offer.id)
                _uiState.value = OfferUiState.Success("Vormerkung entfernt")
            } catch (e: Exception) {
                _uiState.value = OfferUiState.Error("Fehler: ${e.message}")
            }
        }
    }
    
    /**
     * Toggle the saved state of an offer.
     */
    fun toggleSaved(offer: Offer) {
        viewModelScope.launch {
            val userId = appPreferences.currentUserId.first() ?: return@launch
            if (offerRepository.isOfferSaved(userId, offer.id)) {
                offerRepository.removeSavedOffer(userId, offer.id)
                _uiState.value = OfferUiState.Success("Vormerkung entfernt")
            } else {
                offerRepository.saveOffer(userId, offer.id)
                _uiState.value = OfferUiState.Success("Gutschein vorgemerkt")
            }
        }
    }
    
    /**
     * Check if an offer is currently saved by the current user.
     */
    suspend fun isOfferSaved(offerId: Long): Boolean {
        val userId = appPreferences.currentUserId.first() ?: return false
        return offerRepository.isOfferSaved(userId, offerId)
    }
    
    /**
     * Reset transient UI state.
     */
    fun resetState() {
        _selectedOfferId.value = null
        _selectedOffer.value = null
        _uiState.value = OfferUiState.Idle
    }
}
