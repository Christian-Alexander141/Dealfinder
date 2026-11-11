package com.dealfinder.viewmodel

import com.dealfinder.TestEnv
import com.dealfinder.awaitValue
import com.dealfinder.data.search.SortOrder
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests für Suche, Filter, Sortierung und Favoriten im ProviderViewModel.
 *
 * Es wird u. a. geprüft, dass ausschließlich aktive Dealfinder-Partner
 * angezeigt werden (Partnerprinzip).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProviderViewModelTest {

    private lateinit var viewModel: ProviderViewModel

    @Before
    fun setup() {
        TestEnv.setUp()
        TestEnv.registerTestUser()
        viewModel = ProviderViewModel()
    }

    @After
    fun tearDown() {
        TestEnv.tearDown()
    }

    private fun loadedItems() =
        viewModel.searchState.awaitValue { !it.isLoading }.items

    @Test
    fun `Suche startet im Leerzustand`() {
        assertEquals("", viewModel.searchQuery.value)
        assertEquals("", viewModel.locationQuery.value)
        assertEquals(SortOrder.RELEVANCE, viewModel.sortOrder.value)
    }

    @Test
    fun `Demodaten enthalten mindestens 8 aktive Dealfinder-Partner`() {
        val items = loadedItems()

        assertTrue("Mindestens 8 Partner erwartet, waren ${items.size}", items.size >= 8)
        assertTrue("Nur aktive Partner", items.all { it.provider.isActive })
        assertTrue("Nur Dealfinder-Partner", items.all { it.provider.isDealfinderPartner })
    }

    @Test
    fun `Suchbegriff veraendert die Ergebnisse`() {
        val total = loadedItems().size

        viewModel.onSearchQueryChanged("Yoga")
        val filtered = viewModel.searchState.awaitValue {
            !it.isLoading && it.items.size < total
        }.items

        assertTrue(filtered.isNotEmpty())
        assertTrue(
            filtered.all {
                "${it.provider.name} ${it.provider.description}"
                    .contains("Yoga", ignoreCase = true)
            }
        )
    }

    @Test
    fun `Kategoriefilter wirkt auf die Ergebnisse`() {
        val total = loadedItems().size

        viewModel.onCategorySelected(3L) // Fitness & Sport
        val filtered = viewModel.searchState.awaitValue {
            !it.isLoading && it.items.size < total
        }.items

        assertTrue(filtered.isNotEmpty())
        assertTrue(filtered.all { it.provider.categoryId == 3L })
    }

    @Test
    fun `Mindestbewertung filtert Anbieter mit niedrigerem Schnitt heraus`() {
        viewModel.onMinRatingChanged(4.5f)
        val filtered = viewModel.searchState.awaitValue { state ->
            !state.isLoading &&
                state.items.isNotEmpty() &&
                state.items.all { (it.averageRating ?: 0f) >= 4.5f }
        }.items

        assertTrue(filtered.isNotEmpty())
    }

    @Test
    fun `Angebotsfilter zeigt nur Anbieter mit aktiven Sonderangeboten`() {
        viewModel.onOnlyWithOffersChanged(true)
        val filtered = viewModel.searchState.awaitValue { state ->
            !state.isLoading &&
                state.items.isNotEmpty() &&
                state.items.all { it.hasActiveOffer }
        }.items

        assertTrue(filtered.isNotEmpty())
    }

    @Test
    fun `Sortierung nach Bewertung ist absteigend`() {
        viewModel.onSortOrderChanged(SortOrder.RATING)
        val ratings = viewModel.searchState.awaitValue { state ->
            val values = state.items.map { it.averageRating ?: -1f }
            !state.isLoading && values == values.sortedDescending()
        }.items.map { it.averageRating ?: -1f }

        assertEquals(ratings.sortedDescending(), ratings)
    }

    @Test
    fun `Sortierung nach Name ist aufsteigend`() {
        viewModel.onSortOrderChanged(SortOrder.NAME)
        val names = viewModel.searchState.awaitValue { state ->
            val values = state.items.map { it.provider.name.lowercase() }
            !state.isLoading && values.isNotEmpty() && values == values.sorted()
        }.items.map { it.provider.name.lowercase() }

        assertEquals(names.sorted(), names)
    }

    @Test
    fun `clearFilters stellt alle Treffer wieder her`() {
        val total = loadedItems().size

        viewModel.onSearchQueryChanged("Yoga")
        viewModel.onCategorySelected(3L)
        viewModel.onMinRatingChanged(4.5f)
        viewModel.onOnlyWithOffersChanged(true)
        viewModel.searchState.awaitValue { it.items.size < total }

        viewModel.clearFilters()
        val restored = viewModel.searchState.awaitValue {
            !it.isLoading && it.items.size >= total
        }.items

        assertEquals(total, restored.size)
        assertEquals("", viewModel.searchQuery.value)
    }

    @Test
    fun `Favoriten-Status eines Anbieters kann umgeschaltet werden`() {
        viewModel.toggleFavorite(1L)
        val added = viewModel.favoriteIds.awaitValue { it.isNotEmpty() }
        assertTrue(added.contains(1L))

        viewModel.toggleFavorite(1L)
        viewModel.favoriteIds.awaitValue { !it.contains(1L) }
        assertFalse(viewModel.favoriteIds.value.contains(1L))
    }

    @Test
    fun `ohne Anmeldung wird kein Favorit gespeichert`() {
        kotlinx.coroutines.runBlocking {
            com.dealfinder.di.AppModule.provideAppPreferences().clearAll()
        }
        viewModel.toggleFavorite(1L)
        Thread.sleep(150)
        assertTrue(viewModel.favoriteIds.value.isEmpty())
    }

    @Test
    fun `Detaildaten eines Anbieters werden geladen`() {
        viewModel.selectProvider(1L)

        val provider = viewModel.selectedProvider.awaitValue { it != null }
        assertEquals(1L, provider!!.id)

        val services = viewModel.services.awaitValue { it.isNotEmpty() }
        assertTrue(services.all { it.providerId == 1L })

        val ratings = viewModel.ratings.awaitValue { it.isNotEmpty() }
        assertTrue(ratings.all { it.providerId == 1L })

        viewModel.uiState.awaitValue { it is ProviderViewModel.ProviderUiState.Success }
    }
}
