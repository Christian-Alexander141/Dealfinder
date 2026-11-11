package com.dealfinder.data.search

import com.dealfinder.data.models.Provider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit-Tests für die reine Such-, Filter- und Sortierlogik.
 *
 * Es wird bewusst geprüft, dass ausschließlich aktive Dealfinder-Partner
 * in den Ergebnissen erscheinen (MVP-Partnerprinzip).
 */
class ProviderSearchTest {

    private fun provider(
        id: Long,
        name: String,
        description: String = "Beschreibung",
        categoryId: Long? = 1,
        address: String = "Hauptstraße 1, 10115 Berlin",
        partner: Boolean = true,
        active: Boolean = true,
        lat: Double? = null,
        lon: Double? = null
    ) = Provider(
        id = id,
        name = name,
        description = description,
        categoryId = categoryId,
        address = address,
        latitude = lat,
        longitude = lon,
        isDealfinderPartner = partner,
        isActive = active
    )

    private val items = listOf(
        ProviderSearchItem(provider = provider(1, "Gourmet Paradies", description = "Feines Restaurant"), averageRating = 4.8f, hasActiveOffer = true),
        ProviderSearchItem(provider = provider(2, "Café Sonnenstrahl", description = "Kuchen und Kaffee"), averageRating = 4.2f, hasActiveOffer = false),
        ProviderSearchItem(provider = provider(3, "Kreativ Werkstatt", categoryId = 2), averageRating = 4.5f, hasActiveOffer = true),
        ProviderSearchItem(provider = provider(4, "Nicht Partner", partner = false), averageRating = 5.0f, hasActiveOffer = true),
        ProviderSearchItem(provider = provider(5, "Inaktiv", active = false), averageRating = 5.0f, hasActiveOffer = true),
        ProviderSearchItem(provider = provider(6, "Ohne Bewertung"), averageRating = null, hasActiveOffer = false)
    )

    @Test
    fun `nur aktive Dealfinder-Partner werden angezeigt`() {
        val result = ProviderSearch.build(items, SearchFilters())

        assertTrue(result.none { it.provider.id == 4L }) { "Nicht-Partner darf nicht erscheinen" }
        assertTrue(result.none { it.provider.id == 5L }) { "Inaktiver Anbieter darf nicht erscheinen" }
        assertEquals(4, result.size)
    }

    @Test
    fun `Suchbegriff filtert case-insensitive ueber Name und Beschreibung`() {
        val byName = ProviderSearch.build(items, SearchFilters(query = "gourmet"))
        assertEquals(listOf(1L), byName.map { it.provider.id })

        val byDescription = ProviderSearch.build(items, SearchFilters(query = "KUCHEN"))
        assertEquals(listOf(2L), byDescription.map { it.provider.id })

        val noMatch = ProviderSearch.build(items, SearchFilters(query = "Friseur"))
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `Standorttext filtert ueber die Adresse`() {
        // Alle Partner bekommen eine Berliner Adresse – die Partner-Filterung
        // reduziert das Ergebnis trotzdem auf die 4 zulässigen Anbieter.
        val withBerlin = items.map {
            it.copy(provider = it.provider.copy(address = "Berlin, Mitte 5"))
        }
        val result = ProviderSearch.build(withBerlin, SearchFilters(locationQuery = "Berlin"))
        assertEquals(4, result.size)

        val noMatch = ProviderSearch.build(items, SearchFilters(locationQuery = "München"))
        assertTrue(noMatch.isEmpty())
    }

    @Test
    fun `Kategorienfilter grenzt korrekt ein`() {
        val result = ProviderSearch.build(items, SearchFilters(categoryId = 2))
        assertEquals(listOf(3L), result.map { it.provider.id })
    }

    @Test
    fun `Mindestbewertung beruecksichtigt nur Anbieter mit verifizierter Bewertung`() {
        val result = ProviderSearch.build(items, SearchFilters(minRating = 4.5f))
        val ids = result.map { it.provider.id }

        assertTrue(1L in ids && 3L in ids)
        assertTrue(2L !in ids) { "4.2 unter Mindestbewertung 4.5" }
        assertTrue(6L !in ids) { "Anbieter ohne Bewertung fällt bei Mindestbewertung raus" }
    }

    @Test
    fun `Angebotsfilter zeigt nur Anbieter mit aktiven Angeboten`() {
        val result = ProviderSearch.build(items, SearchFilters(onlyWithOffers = true))
        val ids = result.map { it.provider.id }

        assertTrue(1L in ids && 3L in ids)
        assertTrue(2L !in ids && 6L !in ids)
    }

    @Test
    fun `Angebote aus separater Menge werden ergaenzt`() {
        val result = ProviderSearch.build(
            items = items,
            filters = SearchFilters(onlyWithOffers = true),
            providerIdsWithOffer = setOf(2L)
        )
        assertTrue(2L in result.map { it.provider.id })
    }

    @Test
    fun `Distanzfilter greift nur bei vorhandenem Standort`() {
        val withDistance = items.map { it.copy(distanceKm = 3.0) }

        // Mit Standort: 3 km > 2 km → alles gefiltert
        val filtered = ProviderSearch.build(
            withDistance,
            SearchFilters(maxDistanceKm = 2.0, userLat = 52.5, userLon = 13.4)
        )
        assertTrue(filtered.isEmpty())

        // Ohne Standort wird der Distanzfilter ignoriert
        val withoutLocation = ProviderSearch.build(
            withDistance,
            SearchFilters(maxDistanceKm = 2.0)
        )
        assertEquals(4, withoutLocation.size)
    }

    @Test
    fun `Sortierung nach Bewertung absteigend`() {
        val result = ProviderSearch.build(items, SearchFilters(sortOrder = SortOrder.RATING))
        val ratings = result.map { it.averageRating ?: -1f }
        assertEquals(ratings.sortedDescending(), ratings)
    }

    @Test
    fun `Sortierung nach Name aufsteigend`() {
        val result = ProviderSearch.build(items, SearchFilters(sortOrder = SortOrder.NAME))
        val names = result.map { it.provider.name.lowercase() }
        assertEquals(names.sorted(), names)
    }

    @Test
    fun `Sortierung nach Entfernung bringt naechste Anbieter nach vorn`() {
        val distances = listOf(5.0, 1.0, 3.0, 8.0)
        val itemsWithDistance = listOf(
            ProviderSearchItem(provider = provider(1, "A"), distanceKm = distances[0]),
            ProviderSearchItem(provider = provider(2, "B"), distanceKm = distances[1]),
            ProviderSearchItem(provider = provider(3, "C"), distanceKm = distances[2]),
            ProviderSearchItem(provider = provider(4, "D"), distanceKm = distances[3])
        )

        val result = ProviderSearch.build(
            itemsWithDistance,
            SearchFilters(sortOrder = SortOrder.DISTANCE, userLat = 52.5, userLon = 13.4)
        )
        assertEquals(listOf(2L, 3L, 1L, 4L), result.map { it.provider.id })
    }

    @Test
    fun `Kombination aus Query, Kategorie und Angebot`() {
        val result = ProviderSearch.build(
            items,
            SearchFilters(query = "Werkstatt", categoryId = 2, onlyWithOffers = true)
        )
        assertEquals(listOf(3L), result.map { it.provider.id })
    }

    @Test
    fun `Filterstatus wird korrekt erkannt`() {
        assertTrue(SearchFilters().isAnyFilterActive.not())
        assertTrue(SearchFilters(categoryId = 1).isAnyFilterActive)
        assertTrue(SearchFilters(minRating = 4f).isAnyFilterActive)
        assertTrue(SearchFilters(maxDistanceKm = 5.0).isAnyFilterActive)
        assertTrue(SearchFilters(onlyWithOffers = true).isAnyFilterActive)
    }
}
