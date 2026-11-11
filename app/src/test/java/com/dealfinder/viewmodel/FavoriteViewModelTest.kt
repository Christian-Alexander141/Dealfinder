package com.dealfinder.viewmodel

import com.dealfinder.TestEnv
import com.dealfinder.awaitValue
import com.dealfinder.di.AppModule
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
 * Tests für Favoriten-Logik: Hinzufügen, Entfernen und reaktive Anzeige.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FavoriteViewModelTest {

    private lateinit var viewModel: FavoriteViewModel

    @Before
    fun setup() {
        TestEnv.setUp()
        TestEnv.registerTestUser()
        viewModel = FavoriteViewModel()
    }

    @After
    fun tearDown() {
        TestEnv.tearDown()
    }

    @Test
    fun `Startzustand enthaelt keine Favoriten`() {
        val ids = viewModel.favoriteIds.awaitValue { true }
        assertTrue(ids.isEmpty())
    }

    @Test
    fun `Favorit wird hinzugefuegt und reaktiv angezeigt`() {
        viewModel.addFavorite(1L)

        val ids = viewModel.favoriteIds.awaitValue { it.isNotEmpty() }
        assertTrue(ids.contains(1L))

        val items = viewModel.favoriteItems.awaitValue { it.isNotEmpty() }
        assertEquals(listOf(1L), items.map { it.provider.id })
        // Anbieter erscheint nur mit korrekter Bewertung/Angebotsstatus
        assertTrue(items.first().averageRating != null)
    }

    @Test
    fun `toggleFavorite entfernt einen vorhandenen Favoriten wieder`() {
        viewModel.addFavorite(1L)
        viewModel.favoriteIds.awaitValue { it.contains(1L) }

        viewModel.toggleFavorite(1L)
        val ids = viewModel.favoriteIds.awaitValue { !it.contains(1L) }
        assertFalse(ids.contains(1L))
    }

    @Test
    fun `Favoriten sind nach App-Neustart weiterhin vorhanden`() {
        viewModel.addFavorite(2L)
        viewModel.favoriteIds.awaitValue { it.contains(2L) }

        // "Neustart": neue ViewModel-Instanz liest dieselben persistenten Daten
        val restarted = FavoriteViewModel()
        val ids = restarted.favoriteIds.awaitValue { it.isNotEmpty() }
        assertTrue(ids.contains(2L))
    }

    @Test
    fun `Favoriten werden pro Nutzer getrennt gespeichert`() {
        viewModel.addFavorite(1L)
        viewModel.favoriteIds.awaitValue { it.contains(1L) }

        val prefs = AppModule.provideAppPreferences()
        kotlinx.coroutines.runBlocking {
            prefs.clearAll()
            val other = AppModule.provideUserRepository()
                .registerUser("Anderer Nutzer", "other@dealfinder.local", com.dealfinder.data.models.UserType.PRIVATE)
            prefs.setCurrentUser(other.id, other.name, other.email, other.userType)
        }

        // "Neustart"-Sicht des zweiten Nutzers
        val otherViewModel = FavoriteViewModel()
        otherViewModel.addFavorite(3L)
        val ids = otherViewModel.favoriteIds.awaitValue { it.isNotEmpty() }

        assertTrue(ids.contains(3L))
        assertTrue(
            "Favorit des ersten Nutzers darf für den zweiten nicht sichtbar sein",
            !ids.contains(1L)
        )
    }
}
