package com.dealfinder.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.dealfinder.data.database.AppDatabase
import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.BookingStatus
import com.dealfinder.data.models.Favorite
import com.dealfinder.data.models.Offer
import com.dealfinder.data.models.Provider
import com.dealfinder.data.models.SavedOffer
import com.dealfinder.data.models.Service
import com.dealfinder.data.models.User
import com.dealfinder.data.models.UserType
import com.dealfinder.data.models.VerificationStatus
import com.dealfinder.data.models.VerifiedRating
import com.dealfinder.data.repository.impl.RoomOfferRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Tests für Datenbankzugriffe (In-Memory-Room) und Repository-Logik:
 * Partnerprinzip, verifizierte Bewertungen, gespeicherte Gutscheine,
 * Buchungslisten und Favoriten.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class RoomDataLayerTest {

    private lateinit var database: AppDatabase

    private fun provider(
        id: Long,
        name: String = "Anbieter $id",
        partner: Boolean = true,
        active: Boolean = true
    ) = Provider(
        id = id,
        name = name,
        description = "Testbeschreibung $name",
        categoryId = null,
        address = "Teststraße $id, Berlin",
        isDealfinderPartner = partner,
        isActive = active
    )

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun `nur aktive Dealfinder-Partner werden aus der Datenbank geliefert`() = runBlocking {
        database.providerDao().insertAll(
            listOf(
                provider(1, "Aktiver Partner"),
                provider(2, "Kein Partner", partner = false),
                provider(3, "Inaktiver Partner", active = false)
            )
        )

        val partners = database.providerDao().getAllActivePartners().first()
        assertEquals(listOf(1L), partners.map { it.id })
    }

    @Test
    fun `Suche findet Partner ueber Name und Beschreibung`() = runBlocking {
        database.providerDao().insertAll(
            listOf(provider(1, "Yoga Oase"), provider(2, "Friseur Salon"))
        )

        val found = database.providerDao().searchActivePartners("%Yoga%").first()
        assertEquals(listOf(1L), found.map { it.id })
    }

    @Test
    fun `Durchschnittsbewertung zaehlt nur verifizierte Tests`() = runBlocking {
        database.providerDao().insert(provider(1))
        val now = Instant.now()

        database.verifiedRatingDao().insertAll(
            listOf(
                VerifiedRating(
                    providerId = 1,
                    overallScore = 4.0f,
                    criteria = mapOf("Service" to 4.0f),
                    testerReference = "T-1",
                    testDate = now.minus(10, ChronoUnit.DAYS),
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                VerifiedRating(
                    providerId = 1,
                    overallScore = 5.0f,
                    criteria = mapOf("Service" to 5.0f),
                    testerReference = "T-2",
                    testDate = now.minus(5, ChronoUnit.DAYS),
                    verificationStatus = VerificationStatus.VERIFIED
                ),
                VerifiedRating(
                    providerId = 1,
                    overallScore = 1.0f,
                    criteria = emptyMap(),
                    testerReference = "T-3",
                    testDate = now,
                    verificationStatus = VerificationStatus.PENDING
                )
            )
        )

        val average = database.verifiedRatingDao().getAverageScore(1).first()
        assertEquals(4.5f, average!!, 0.01f)

        // getAllVerified liefern ausschließlich verifizierte Einträge
        val verified = database.verifiedRatingDao().getAllVerified().first()
        assertEquals(2, verified.size)
        assertTrue(verified.all { it.verificationStatus == VerificationStatus.VERIFIED })
    }

    @Test
    fun `Gutscheine lassen sich vormerken und wieder entfernen`() = runBlocking {
        val userId = database.userDao().insert(
            User(name = "Test", email = "t@e.de", userType = UserType.PRIVATE)
        )
        database.providerDao().insert(provider(1))
        val offerId = database.offerDao().insert(
            Offer(
                providerId = 1,
                title = "10 % Rabatt",
                description = "Auf alles",
                discountValue = "10%",
                validUntil = Instant.now().plus(30, ChronoUnit.DAYS)
            )
        )

        val repository = RoomOfferRepository(database.offerDao(), database.savedOfferDao())

        assertFalse(repository.isOfferSaved(userId, offerId))

        repository.saveOffer(userId, offerId)
        assertTrue(repository.isOfferSaved(userId, offerId))

        // Persistenz: vollständige Offer-Zeile über Join verfügbar
        val saved = repository.getSavedOffers(userId).first()
        assertEquals(listOf(offerId), saved.map { it.id })
        assertEquals("10 % Rabatt", saved.first().title)

        // Zweimaliges Vormerken ist idempotent (Unique-Index)
        repository.saveOffer(userId, offerId)
        assertEquals(1, database.savedOfferDao().countSaved(userId, offerId))

        repository.removeSavedOffer(userId, offerId)
        assertFalse(repository.isOfferSaved(userId, offerId))
        assertTrue(repository.getSavedOffers(userId).first().isEmpty())
    }

    @Test
    fun `kommende und vergangene Buchungen werden korrekt getrennt`() = runBlocking {
        val userId = database.userDao().insert(
            User(name = "Test", email = "b@e.de", userType = UserType.PRIVATE)
        )
        database.providerDao().insert(provider(1))
        database.serviceDao().insert(
            Service(id = 1, providerId = 1, name = "Termin")
        )

        val now = Instant.now()
        val upcomingId = database.bookingDao().insert(
            Booking(
                userId = userId,
                providerId = 1,
                serviceId = 1,
                bookingDate = now.plus(3, ChronoUnit.DAYS),
                timeSlot = "10:00 - 11:00",
                numberOfPeople = 1,
                status = BookingStatus.CONFIRMED,
                createdAt = now
            )
        )
        val pastId = database.bookingDao().insert(
            Booking(
                userId = userId,
                providerId = 1,
                serviceId = 1,
                bookingDate = now.minus(3, ChronoUnit.DAYS),
                timeSlot = "14:00 - 15:00",
                numberOfPeople = 1,
                status = BookingStatus.CONFIRMED,
                createdAt = now.minus(10, ChronoUnit.DAYS)
            )
        )

        val upcoming = database.bookingDao().getUpcomingByUser(userId, now).first()
        val past = database.bookingDao().getPastByUser(userId, now).first()

        assertEquals(listOf(upcomingId), upcoming.map { it.id })
        assertEquals(listOf(pastId), past.map { it.id })
    }

    @Test
    fun `stornierte Buchung gilt als vergangen`() = runBlocking {
        val userId = database.userDao().insert(
            User(name = "Test", email = "c@e.de", userType = UserType.PRIVATE)
        )
        database.providerDao().insert(provider(1))
        database.serviceDao().insert(Service(id = 1, providerId = 1, name = "Termin"))

        val now = Instant.now()
        val bookingId = database.bookingDao().insert(
            Booking(
                userId = userId,
                providerId = 1,
                serviceId = 1,
                bookingDate = now.plus(5, ChronoUnit.DAYS),
                timeSlot = "09:00 - 10:00",
                numberOfPeople = 1,
                status = BookingStatus.CONFIRMED,
                createdAt = now
            )
        )

        database.bookingDao().updateStatus(bookingId, BookingStatus.CANCELLED)

        assertTrue(database.bookingDao().getUpcomingByUser(userId, now).first().isEmpty())
        assertEquals(
            listOf(bookingId),
            database.bookingDao().getPastByUser(userId, now).first().map { it.id }
        )
    }

    @Test
    fun `Favoriten werden pro Nutzer gespeichert und entfernt`() = runBlocking {
        val userA = database.userDao().insert(
            User(name = "A", email = "a@e.de", userType = UserType.PRIVATE)
        )
        val userB = database.userDao().insert(
            User(name = "B", email = "b2@e.de", userType = UserType.PRIVATE)
        )
        database.providerDao().insertAll(listOf(provider(1), provider(2)))

        database.favoriteDao().insert(Favorite(userId = userA, providerId = 1))
        database.favoriteDao().insert(Favorite(userId = userB, providerId = 2))

        val favoritesA = database.favoriteDao().getByUser(userA).first()
        assertEquals(listOf(1L), favoritesA.map { it.providerId })

        // Favorit des anderen Nutzers ist nicht sichtbar
        assertFalse(favoritesA.any { it.providerId == 2L })

        database.favoriteDao().deleteByUserAndProvider(userA, 1)
        assertTrue(database.favoriteDao().getByUser(userA).first().isEmpty())
        // User B bleibt unberührt
        assertEquals(1, database.favoriteDao().getByUser(userB).first().size)
    }

    @Test
    fun `Demo-Daten-Initialisierung laeuft nur einmal`() = runBlocking {
        com.dealfinder.di.AppModule.initialize(
            ApplicationProvider.getApplicationContext()
        )
        com.dealfinder.di.AppModule.clearAllData()
        com.dealfinder.di.AppModule.initializeDemoData()
        val firstCount = com.dealfinder.di.AppModule.provideProviderRepository()
            .getAllActivePartners().first().size

        // Zweiter Aufruf darf keine Duplikate erzeugen
        com.dealfinder.di.AppModule.initializeDemoData()
        val secondCount = com.dealfinder.di.AppModule.provideProviderRepository()
            .getAllActivePartners().first().size

        assertTrue("Mindestens 8 Demo-Partner erwartet", firstCount >= 8)
        assertEquals(firstCount, secondCount)
    }

    @Test
    fun `SavedOffer-Eintraege referenzieren Nutzer und Angebot`() = runBlocking {
        val userId = database.userDao().insert(
            User(name = "Test", email = "s@e.de", userType = UserType.PRIVATE)
        )
        database.providerDao().insert(provider(1))
        val offerId = database.offerDao().insert(
            Offer(
                providerId = 1,
                title = "Gutschein",
                description = "Demo",
                discountValue = "5 €",
                validUntil = Instant.now().plus(10, ChronoUnit.DAYS)
            )
        )

        val id = database.savedOfferDao().insert(SavedOffer(userId = userId, offerId = offerId))
        assertTrue(id > 0)
        assertEquals(1, database.savedOfferDao().countSaved(userId, offerId))

        // Löschen des Nutzers entfernt die Vormerkung (FK CASCADE)
        database.userDao().delete(userId)
        assertEquals(0, database.savedOfferDao().countSaved(userId, offerId))
    }
}
