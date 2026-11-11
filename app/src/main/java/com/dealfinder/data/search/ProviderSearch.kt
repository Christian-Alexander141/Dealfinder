package com.dealfinder.data.search

import com.dealfinder.data.models.Provider

/** Sortieroptionen der Suchergebnisliste. */
enum class SortOrder {
    /** Standard: Bewertung absteigend, dann Name. */
    RELEVANCE,
    /** Entfernung aufsteigend (nur mit aktivem Standort). */
    DISTANCE,
    /** Unabhängige Gesamtbewertung absteigend. */
    RATING,
    /** Anbietername aufsteigend (A–Z). */
    NAME
}

/**
 * Alle Filter- und Sortieroptionen der Suchergebnisseite.
 * Reine Datenklasse – die eigentliche Filterung steckt in [ProviderSearch].
 */
data class SearchFilters(
    val query: String = "",
    val locationQuery: String = "",
    val categoryId: Long? = null,
    /** Mindestens diese unabhängige Gesamtbewertung (0–5). */
    val minRating: Float? = null,
    /** Maximale Entfernung in km – nur wirksam, wenn ein Standort vorliegt. */
    val maxDistanceKm: Double? = null,
    /** Nur Anbieter mit aktuellem Sonderangebot. */
    val onlyWithOffers: Boolean = false,
    val sortOrder: SortOrder = SortOrder.RELEVANCE,
    /** Aktueller Nutzerstandort (GPS) – optional. */
    val userLat: Double? = null,
    val userLon: Double? = null
) {
    val hasUserLocation: Boolean get() = userLat != null && userLon != null

    /** Ist irgendein Filter aktiv? (für die Filter-Button-Darstellung) */
    val isAnyFilterActive: Boolean
        get() = categoryId != null || minRating != null || maxDistanceKm != null || onlyWithOffers
}

/**
 * Ein Suchergebnis-Element mit den für die Anzeige benötigten Derivativen.
 *
 * @param averageRating unabhängige, verifizierte Gesamtbewertung (null = keine)
 * @param hasActiveOffer besitzt aktuell ein aktives Sonderangebot
 * @param distanceKm Entfernung zum Nutzerstandort (null = kein Standort)
 */
data class ProviderSearchItem(
    val provider: Provider,
    val averageRating: Float? = null,
    val hasActiveOffer: Boolean = false,
    val distanceKm: Double? = null
)

/**
 * Reine Such-, Filter- und Sortierlogik für die Ergebnisliste.
 *
 * Statt die Regeln in einer Composable oder einem ViewModel zu verstecken,
 * ist diese Objekt-Logik direkt unit-testbar. Die UI reicht nur die
 * Rohdaten (Anbieter, Bewertungen, Angebote, Standort) ein.
 *
 * Sicherheitsregel des MVP: Es erscheinen ausschließlich aktive
 * Dealfinder-Partner – nicht freigegebene Betriebe werden hier aussortiert.
 */
object ProviderSearch {

    /**
     * Wendet Filter und Sortierung auf die Rohdaten an.
     *
     * @param items Rohdaten aus der Datenbank (alle aktiven Partner)
     * @param filters aktive Filter/Sortierung
     * @param verifiedRatingsByProvider nur VERIFIZIERTE Testbewertungen je Anbieter
     * @param providerIdsWithOffer Anbieter-IDs mit aktiven Sonderangeboten
     */
    fun build(
        items: List<ProviderSearchItem>,
        filters: SearchFilters,
        verifiedRatingsByProvider: Map<Long, Float> = emptyMap(),
        providerIdsWithOffer: Set<Long> = emptySet()
    ): List<ProviderSearchItem> {
        // Basis-Derivate ergänzen
        val enriched = items.map { item ->
            item.copy(
                averageRating = verifiedRatingsByProvider[item.provider.id] ?: item.averageRating,
                hasActiveOffer = item.provider.id in providerIdsWithOffer ||
                    item.hasActiveOffer
            )
        }

        val query = filters.query.trim()
        val locationQuery = filters.locationQuery.trim()

        val filtered = enriched.filter { item ->
            val provider = item.provider

            // Nur aktive Dealfinder-Partner dürfen erscheinen
            if (!provider.isDealfinderPartner || !provider.isActive) return@filter false

            // Freitextsuche über Name und Beschreibung (case-insensitive)
            if (query.isNotEmpty()) {
                val haystack = "${provider.name} ${provider.description}".lowercase()
                if (!haystack.contains(query.lowercase())) return@filter false
            }

            // Standort-Textfilter über die Adresse (manuelle Ortseingabe)
            if (locationQuery.isNotEmpty()) {
                if (!provider.address.contains(locationQuery, ignoreCase = true)) {
                    return@filter false
                }
            }

            // Kategorie
            if (filters.categoryId != null && provider.categoryId != filters.categoryId) {
                return@filter false
            }

            // Mindestbewertung (nur verifizierte Bewertungen zählen)
            if (filters.minRating != null) {
                val rating = item.averageRating ?: return@filter false
                if (rating < filters.minRating) return@filter false
            }

            // Angebotsfilter
            if (filters.onlyWithOffers && !item.hasActiveOffer) return@filter false

            // Nur mit aktivem Standort anwendbarer Distanzfilter
            if (filters.maxDistanceKm != null && filters.hasUserLocation) {
                val distance = item.distanceKm ?: return@filter false
                if (distance > filters.maxDistanceKm) return@filter false
            }

            true
        }

        return when (filters.sortOrder) {
            SortOrder.RELEVANCE -> filtered.sortedWith(
                compareByDescending<ProviderSearchItem> { it.averageRating ?: 0f }
                    .thenBy { it.provider.name }
            )
            SortOrder.RATING -> filtered.sortedByDescending { it.averageRating ?: -1f }
            SortOrder.NAME -> filtered.sortedBy { it.provider.name.lowercase() }
            SortOrder.DISTANCE -> filtered.sortedWith(
                compareBy<ProviderSearchItem> { it.distanceKm ?: Double.MAX_VALUE }
                    .thenByDescending { it.averageRating ?: 0f }
            )
        }
    }
}
