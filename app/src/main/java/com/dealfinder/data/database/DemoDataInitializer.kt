package com.dealfinder.data.database

import com.dealfinder.data.dao.BookingDao
import com.dealfinder.data.dao.CategoryDao
import com.dealfinder.data.dao.FavoriteDao
import com.dealfinder.data.dao.OfferDao
import com.dealfinder.data.dao.ProviderDao
import com.dealfinder.data.dao.ServiceDao
import com.dealfinder.data.dao.UserDao
import com.dealfinder.data.dao.VerifiedRatingDao
import kotlinx.coroutines.flow.first
import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.BookingStatus
import com.dealfinder.data.models.BookingType
import com.dealfinder.data.models.Category
import com.dealfinder.data.models.Favorite
import com.dealfinder.data.models.Offer
import com.dealfinder.data.models.Provider
import com.dealfinder.data.models.Service
import com.dealfinder.data.models.User
import com.dealfinder.data.models.UserType
import com.dealfinder.data.models.VerificationStatus
import com.dealfinder.data.models.VerifiedRating
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.temporal.ChronoUnit

/**
 * Initializes the database with demo data
 */
class DemoDataInitializer(
    private val userDao: UserDao,
    private val categoryDao: CategoryDao,
    private val providerDao: ProviderDao,
    private val serviceDao: ServiceDao,
    private val ratingDao: VerifiedRatingDao,
    private val offerDao: OfferDao,
    private val bookingDao: BookingDao,
    private val favoriteDao: FavoriteDao
) {
    companion object {
        /**
         * Objektweiter Sperr-Mutex: initialisiert und Zurücksetzen dürfen sich
         * nicht überlappen (z. B. App-Start-Seeding parallel zum "Demo-Daten
         * zurücksetzen"-Aktion aus dem Profil).
         */
        val lock = kotlinx.coroutines.sync.Mutex()
    }

    suspend fun initialize() = withContext(Dispatchers.IO) {
        lock.withLock {
        // Only initialize if database is empty
        if (userDao.getAll().first().isNotEmpty()) {
            return@withLock
        }
        
        // Clear existing data (just in case)
        favoriteDao.deleteAll()
        bookingDao.deleteAll()
        offerDao.deleteAll()
        ratingDao.deleteAll()
        serviceDao.deleteAll()
        providerDao.deleteAll()
        categoryDao.deleteAll()
        userDao.deleteAll()
        
        // Create categories
        val categories = createCategories()
        categoryDao.insertAll(categories)
        
        // Create providers
        val providers = createProviders(categories)
        providerDao.insertAll(providers)
        
        // Create services
        val services = createServices(providers)
        serviceDao.insertAll(services)
        
        // Create ratings
        val ratings = createRatings(providers)
        ratingDao.insertAll(ratings)
        
        // Create offers
        val offers = createOffers(providers)
        offerDao.insertAll(offers)
        
        // Create demo users
        val users = createUsers()
        users.forEach { userDao.insert(it) }
        
        // Create bookings for demo user
        val demoUser = users.first { it.userType == UserType.PRIVATE }
        val bookings = createBookings(demoUser, providers, services)
        bookings.forEach { bookingDao.insert(it) }
        
        // Create favorites for demo user
        val favorites = createFavorites(demoUser, providers)
        favorites.forEach { favoriteDao.insert(it) }
        }
    }
    
    private fun createCategories(): List<Category> {
        return listOf(
            Category(
                id = 1,
                name = "Restaurants & Gastronomie",
                description = "Restaurants, Cafés, Bars und andere gastronomische Betriebe",
                icon = "🍽️"
            ),
            Category(
                id = 2,
                name = "Freizeit & Unterhaltung",
                description = "Aktivitäten für Ihre Freizeitgestaltung",
                icon = "🎯"
            ),
            Category(
                id = 3,
                name = "Fitness & Sport",
                description = "Fitnessstudios, Sportkurse und Wellnessangebote",
                icon = "💪"
            ),
            Category(
                id = 4,
                name = "Beauty & Wellness",
                description = "Kosmetik, Friseur, Massage und Wellnessbehandlungen",
                icon = "💇"
            ),
            Category(
                id = 5,
                name = "Handwerk & Dienstleistungen",
                description = "Handwerksbetriebe und diverse Dienstleistungen",
                icon = "🔧"
            )
        )
    }
    
    private fun createProviders(categories: List<Category>): List<Provider> {
        val now = Instant.now()
        
        return listOf(
            // Restaurants & Gastronomie
            Provider(
                id = 1,
                name = "Gourmet Paradies",
                description = "Feine Küche in gemütlicher Atmosphäre mit regionalen Zutaten und kreativen Gerichten.",
                categoryId = categories[0].id,
                address = "Hauptstraße 1, 10115 Berlin",
                latitude = 52.5200,
                longitude = 13.4050,
                phone = "+49 30 1234567",
                email = "info@gourmet-paradies.de",
                website = "https://gourmet-paradies.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            Provider(
                id = 2,
                name = "Café Sonnenstrahl",
                description = "Kleines, familiengeführtes Café mit hausgemachten Kuchen und frischem Kaffee.",
                categoryId = categories[0].id,
                address = "Sonnenallee 45, 10965 Berlin",
                latitude = 52.4894,
                longitude = 13.4231,
                phone = "+49 30 9876543",
                email = "kontakt@cafe-sonnenstrahl.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            // Freizeit & Unterhaltung
            Provider(
                id = 3,
                name = "Kreativ Werkstatt",
                description = "Workshops und Kurse für kreative Köpfe. Von Malen über Töpfern bis hin zu Fotografie.",
                categoryId = categories[1].id,
                address = "Kreativstraße 12, 10787 Berlin",
                latitude = 52.4983,
                longitude = 13.3545,
                phone = "+49 30 4567890",
                email = "info@kreativ-werkstatt.de",
                website = "https://kreativ-werkstatt.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            Provider(
                id = 4,
                name = "Escape Room Abenteuer",
                description = "Spannende Rätsel in thematisch gestalteten Räumen. Perfekt für Gruppen bis zu 6 Personen.",
                categoryId = categories[1].id,
                address = "Abenteuerweg 22, 10829 Berlin",
                latitude = 52.4925,
                longitude = 13.3698,
                phone = "+49 30 1122334",
                email = "buchung@escape-room.de",
                website = "https://escape-room-abenteuer.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            // Fitness & Sport
            Provider(
                id = 5,
                name = "Vitality Fitness Studio",
                description = "Moderne Geräte, verschiedene Kurse und persönliches Coaching für Ihr Trainingsziel.",
                categoryId = categories[2].id,
                address = "Gesundheitsallee 33, 10369 Berlin",
                latitude = 52.5186,
                longitude = 13.4587,
                phone = "+49 30 5566778",
                email = "info@vitality-fitness.de",
                website = "https://vitality-fitness.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            Provider(
                id = 6,
                name = "Yoga Oase",
                description = "Yoga Kurse für alle Levels. Von Anfängern bis zu fortgeschrittenen Yogis.",
                categoryId = categories[2].id,
                address = "Ruheweg 7, 10439 Berlin",
                latitude = 52.5375,
                longitude = 13.4032,
                phone = "+49 30 6677889",
                email = "kontakt@yoga-oase.de",
                website = "https://yoga-oase-berlin.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            // Beauty & Wellness
            Provider(
                id = 7,
                name = "Beauty Lounge Berlin",
                description = "Professionelle Kosmetikbehandlungen, Nageldesign und Entspannungsmassagen.",
                categoryId = categories[3].id,
                address = "Schönheitsstraße 8, 10623 Berlin",
                latitude = 52.5000,
                longitude = 13.3400,
                phone = "+49 30 7788990",
                email = "termin@beauty-lounge-berlin.de",
                website = "https://beauty-lounge-berlin.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            Provider(
                id = 8,
                name = "Wellness Tempel",
                description = "Traditionelle Thai-Massagen und ganzheitliche Wellnessbehandlungen in authentischer Atmosphäre.",
                categoryId = categories[3].id,
                address = "Tempelweg 15, 10961 Berlin",
                latitude = 52.5100,
                longitude = 13.4100,
                phone = "+49 30 8899001",
                email = "info@wellness-tempel.de",
                website = "https://wellness-tempel.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            // Handwerk & Dienstleistungen
            Provider(
                id = 9,
                name = "Meister Handwerk",
                description = "Elektroinstallationen, Sanitärarbeiten und Heizungsbau von zertifizierten Fachkräften.",
                categoryId = categories[4].id,
                address = "Handwerkerstraße 40, 12047 Berlin",
                latitude = 52.4800,
                longitude = 13.3900,
                phone = "+49 30 2233445",
                email = "kontakt@meister-handwerk.de",
                website = "https://meister-handwerk.de",
                isDealfinderPartner = true,
                isActive = true
            ),
            Provider(
                id = 10,
                name = "Digital Service",
                description = "IT-Service, Computerreparatur und technische Unterstützung für Privat und Gewerbe.",
                categoryId = categories[4].id,
                address = "Digitalweg 5, 13359 Berlin",
                latitude = 52.5450,
                longitude = 13.3150,
                phone = "+49 30 3344556",
                email = "support@digital-service.de",
                website = "https://digital-service.de",
                isDealfinderPartner = true,
                isActive = true
            )
        )
    }
    
    private fun createServices(providers: List<Provider>): List<Service> {
        return listOf(
            // Gourmet Paradies
            Service(
                id = 1,
                providerId = 1,
                name = "3-Gänge Menü",
                description = "Vorspeise, Hauptgericht und Dessert nach Wahl des Kochs",
                price = 45.90,
                durationMinutes = 120,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 20
            ),
            Service(
                id = 2,
                providerId = 1,
                name = "À-la-carte Speisen",
                description = "Individuelle Gerichteauswahl von der Speisekarte",
                price = 25.00,
                durationMinutes = 90,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 4
            ),
            // Café Sonnenstrahl
            Service(
                id = 3,
                providerId = 2,
                name = "Frühstücksbuffet",
                description = "Vielfältiges Buffet mit frischen Brötchen, Aufschnitt, Käse, Obst und mehr",
                price = 12.50,
                durationMinutes = 60,
                bookingType = BookingType.SEAT_RESERVATION,
                maxParticipants = 30
            ),
            Service(
                id = 4,
                providerId = 2,
                name = "Kaffee & Kuchen",
                description = "Hausgemachter Kuchen mit einer Tasse Kaffee oder Tee",
                price = 7.50,
                durationMinutes = 30,
                bookingType = BookingType.SEAT_RESERVATION
            ),
            // Kreativ Werkstatt
            Service(
                id = 5,
                providerId = 3,
                name = "Malkurs für Anfänger",
                description = "Einführung in die Acrylmalerei mit allen Materialien",
                price = 35.00,
                durationMinutes = 180,
                bookingType = BookingType.GROUP_BOOKING,
                maxParticipants = 10
            ),
            Service(
                id = 6,
                providerId = 3,
                name = "Töpferkurs",
                description = "Erlernen der Grundtechniken der Töpferkunst",
                price = 45.00,
                durationMinutes = 240,
                bookingType = BookingType.GROUP_BOOKING,
                maxParticipants = 8
            ),
            // Escape Room Abenteuer
            Service(
                id = 7,
                providerId = 4,
                name = "Geheimnis des Pharao",
                description = "Ägypten-Themenraum mit kniffligen Rätseln. Dauer: 60 Minuten",
                price = 22.00,
                durationMinutes = 60,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 6
            ),
            Service(
                id = 8,
                providerId = 4,
                name = "Flucht aus Alcatraz",
                description = "Spannender Gefängnis-Ausbruchsraum. Dauer: 60 Minuten",
                price = 25.00,
                durationMinutes = 60,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 6
            ),
            // Vitality Fitness Studio
            Service(
                id = 9,
                providerId = 5,
                name = "Personales Training",
                description = "Individuelles Training mit zertifiziertem Trainer",
                price = 65.00,
                durationMinutes = 60,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 1
            ),
            Service(
                id = 10,
                providerId = 5,
                name = "Yoga Kurs",
                description = "Gruppen-Yogakurs für alle Levels",
                price = 15.00,
                durationMinutes = 60,
                bookingType = BookingType.GROUP_BOOKING,
                maxParticipants = 15
            ),
            // Yoga Oase
            Service(
                id = 11,
                providerId = 6,
                name = "Hatha Yoga",
                description = "Klassischer Hatha Yoga Kurs",
                price = 12.00,
                durationMinutes = 75,
                bookingType = BookingType.GROUP_BOOKING,
                maxParticipants = 12
            ),
            Service(
                id = 12,
                providerId = 6,
                name = "Meditation",
                description = "Geführte Meditation für innere Ruhe",
                price = 10.00,
                durationMinutes = 45,
                bookingType = BookingType.GROUP_BOOKING,
                maxParticipants = 10
            ),
            // Beauty Lounge Berlin
            Service(
                id = 13,
                providerId = 7,
                name = "Klassische Massage",
                description = "Ganzkörpermassage zur Entspannung der Muskeln",
                price = 55.00,
                durationMinutes = 60,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 1
            ),
            Service(
                id = 14,
                providerId = 7,
                name = "Gesichtsbehandlung",
                description = "Reinigung, Pflege und Maskenbehandlung für die Haut",
                price = 45.00,
                durationMinutes = 45,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 1
            ),
            // Wellness Tempel
            Service(
                id = 15,
                providerId = 8,
                name = "Thai-Massage",
                description = "Traditionelle Thai-Massage für mehr Beweglichkeit",
                price = 70.00,
                durationMinutes = 90,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 1
            ),
            Service(
                id = 16,
                providerId = 8,
                name = "Hot Stone Massage",
                description = "Massage mit warmen Basaltsteinen",
                price = 85.00,
                durationMinutes = 90,
                bookingType = BookingType.FIXED_DURATION,
                maxParticipants = 1
            ),
            // Meister Handwerk
            Service(
                id = 17,
                providerId = 9,
                name = "Elektroinstallation",
                description = "Fachgerechte Installation von Stromkreisen und Steckdosen",
                price = 75.00,
                durationMinutes = 120,
                bookingType = BookingType.FIXED_DURATION
            ),
            // Digital Service
            Service(
                id = 18,
                providerId = 10,
                name = "Computer Reparatur",
                description = "Diagnose und Reparatur von Hardware und Software Problemen",
                price = 50.00,
                durationMinutes = 60,
                bookingType = BookingType.FIXED_DURATION
            )
        )
    }
    
    private fun createRatings(providers: List<Provider>): List<VerifiedRating> {
        val now = Instant.now()
        
        return listOf(
            // Gourmet Paradies
            VerifiedRating(
                id = 1,
                providerId = 1,
                overallScore = 4.8f,
                criteria = mapOf(
                    "Qualität" to 5.0f,
                    "Service" to 4.8f,
                    "Preis-Leistung" to 4.5f,
                    "Atmosphäre" to 4.9f
                ),
                testerReference = "DEALFINDER-TEST-001",
                testDate = now.minus(30, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Hervorragende Küche mit kreativen Gerichten. Service war sehr aufmerksam."
            ),
            // Café Sonnenstrahl
            VerifiedRating(
                id = 2,
                providerId = 2,
                overallScore = 4.5f,
                criteria = mapOf(
                    "Qualität" to 4.6f,
                    "Service" to 4.3f,
                    "Preis-Leistung" to 4.7f,
                    "Atmosphäre" to 4.5f
                ),
                testerReference = "DEALFINDER-TEST-002",
                testDate = now.minus(25, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Gemütliches Café mit sehr gutem Kaffee. Kuchen sind hausgemacht und lecker."
            ),
            // Kreativ Werkstatt
            VerifiedRating(
                id = 3,
                providerId = 3,
                overallScore = 4.7f,
                criteria = mapOf(
                    "Qualität" to 4.8f,
                    "Service" to 4.7f,
                    "Preis-Leistung" to 4.5f,
                    "Atmosphäre" to 4.8f
                ),
                testerReference = "DEALFINDER-TEST-003",
                testDate = now.minus(45, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Tolle Workshops mit geduldigen Anleitern. Sehr gut für Anfänger geeignet."
            ),
            // Escape Room Abenteuer
            VerifiedRating(
                id = 4,
                providerId = 4,
                overallScore = 4.9f,
                criteria = mapOf(
                    "Spaßfaktor" to 5.0f,
                    "Service" to 4.8f,
                    "Preis-Leistung" to 4.7f,
                    "Rätselqualität" to 5.0f
                ),
                testerReference = "DEALFINDER-TEST-004",
                testDate = now.minus(15, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Sehr spannender Raum mit kreativen Rätseln. Perfekt für Gruppen."
            ),
            // Vitality Fitness Studio
            VerifiedRating(
                id = 5,
                providerId = 5,
                overallScore = 4.4f,
                criteria = mapOf(
                    "Ausrüstung" to 4.6f,
                    "Service" to 4.2f,
                    "Preis-Leistung" to 4.3f,
                    "Sauberkeit" to 4.5f
                ),
                testerReference = "DEALFINDER-TEST-005",
                testDate = now.minus(20, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Moderne Geräte und gute Kursauswahl. Etwas teurer, aber gute Qualität."
            ),
            // Yoga Oase
            VerifiedRating(
                id = 6,
                providerId = 6,
                overallScore = 4.6f,
                criteria = mapOf(
                    "Qualität" to 4.8f,
                    "Service" to 4.5f,
                    "Preis-Leistung" to 4.5f,
                    "Atmosphäre" to 4.7f
                ),
                testerReference = "DEALFINDER-TEST-006",
                testDate = now.minus(10, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Sehr entspannte Atmosphäre mit kompetenten Lehrern. Gut für Anfänger und Fortgeschrittene."
            ),
            // Beauty Lounge Berlin
            VerifiedRating(
                id = 7,
                providerId = 7,
                overallScore = 4.3f,
                criteria = mapOf(
                    "Qualität" to 4.4f,
                    "Service" to 4.2f,
                    "Preis-Leistung" to 4.3f,
                    "Atmosphäre" to 4.4f
                ),
                testerReference = "DEALFINDER-TEST-007",
                testDate = now.minus(35, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Gute Behandlungen, aber etwas teuer. Service könnte etwas freundlicher sein."
            ),
            // Wellness Tempel
            VerifiedRating(
                id = 8,
                providerId = 8,
                overallScore = 4.9f,
                criteria = mapOf(
                    "Qualität" to 5.0f,
                    "Service" to 4.8f,
                    "Preis-Leistung" to 4.7f,
                    "Atmosphäre" to 5.0f
                ),
                testerReference = "DEALFINDER-TEST-008",
                testDate = now.minus(5, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Hervorragende Massagen in authentischer Atmosphäre. Sehr zu empfehlen!"
            ),
            // Meister Handwerk
            VerifiedRating(
                id = 9,
                providerId = 9,
                overallScore = 4.7f,
                criteria = mapOf(
                    "Qualität" to 4.8f,
                    "Service" to 4.6f,
                    "Preis-Leistung" to 4.7f,
                    "Zuverlässigkeit" to 4.8f
                ),
                testerReference = "DEALFINDER-TEST-009",
                testDate = now.minus(50, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Zuverlässiges Handwerk mit guter Beratung. Pünktlich und saubere Arbeit."
            ),
            // Digital Service
            VerifiedRating(
                id = 10,
                providerId = 10,
                overallScore = 4.5f,
                criteria = mapOf(
                    "Qualität" to 4.6f,
                    "Service" to 4.4f,
                    "Preis-Leistung" to 4.4f,
                    "Schnelligkeit" to 4.6f
                ),
                testerReference = "DEALFINDER-TEST-010",
                testDate = now.minus(40, ChronoUnit.DAYS),
                verificationStatus = VerificationStatus.VERIFIED,
                notes = "Schnelle und kompetente Hilfe bei Computerproblemen. Preis ist fair."
            )
        )
    }
    
    private fun createOffers(providers: List<Provider>): List<Offer> {
        val now = Instant.now()
        
        return listOf(
            Offer(
                id = 1,
                providerId = 1,
                title = "Frühlingsmenü",
                description = "Genießen Sie unser spezielles Frühlingsmenü mit saisonalen Zutaten",
                discountValue = "20% Rabatt",
                validUntil = now.plus(60, ChronoUnit.DAYS),
                conditions = "Mindestens 2 Personen. Nur nach Reservierung.",
                isActive = true
            ),
            Offer(
                id = 2,
                providerId = 2,
                title = "Kaffee & Kuchen Spezial",
                description = "Eine Tasse Kaffee und ein Stück Kuchen Ihrer Wahl",
                discountValue = "3.50 €",
                validUntil = now.plus(30, ChronoUnit.DAYS),
                conditions = "Nur im Café einlösbar. Nicht mit anderen Rabatten kombinierbar.",
                isActive = true
            ),
            Offer(
                id = 3,
                providerId = 3,
                title = "Kurs-Paket",
                description = "10% Rabatt auf die Buchung von 5 Workshops",
                discountValue = "10% Rabatt",
                validUntil = now.plus(90, ChronoUnit.DAYS),
                conditions = "Buchung und Bezahlung im Voraus erforderlich.",
                isActive = true
            ),
            Offer(
                id = 4,
                providerId = 4,
                title = "Gruppenrabatt",
                description = "20% Rabatt für Gruppen ab 4 Personen",
                discountValue = "20% Rabatt",
                validUntil = now.plus(45, ChronoUnit.DAYS),
                conditions = "Nur für Escape Rooms. Vorabreservierung erforderlich.",
                isActive = true
            ),
            Offer(
                id = 5,
                providerId = 5,
                title = "Schnupperangebot",
                description = "1 Woche kostenlos trainieren",
                discountValue = "0.00 €",
                validUntil = now.plus(15, ChronoUnit.DAYS),
                conditions = "Nur für Neukunden. Anmeldung erforderlich.",
                isActive = true
            ),
            Offer(
                id = 6,
                providerId = 6,
                title = "Probestunde",
                description = "Kostenlose Probestunde für Neukunden",
                discountValue = "0.00 €",
                validUntil = now.plus(30, ChronoUnit.DAYS),
                conditions = "Nur für Neukunden. Vorabreservierung erforderlich.",
                isActive = true
            ),
            Offer(
                id = 7,
                providerId = 7,
                title = "Beauty-Paket",
                description = "Gesichtsbehandlung + Massage zum Sonderpreis",
                discountValue = "85.00 € statt 100.00 €",
                validUntil = now.plus(60, ChronoUnit.DAYS),
                conditions = "Termin nach Absprache. Nicht mit anderen Angeboten kombinierbar.",
                isActive = true
            ),
            Offer(
                id = 8,
                providerId = 8,
                title = "Wellness-Tag",
                description = "Thai-Massage + Hot Stone Massage + Teezeremonie",
                discountValue = "120.00 € statt 155.00 €",
                validUntil = now.plus(90, ChronoUnit.DAYS),
                conditions = "Termin nach Absprache. Mindestens 3 Stunden Aufenthalt.",
                isActive = true
            ),
            Offer(
                id = 9,
                providerId = 9,
                title = "Frühbucherrabatt",
                description = "10% Rabatt auf alle Dienstleistungen bei Buchung bis zum 30.06.2025",
                discountValue = "10% Rabatt",
                validUntil = now.plus(120, ChronoUnit.DAYS),
                conditions = "Nur für Termine bis zum 31.12.2025.",
                isActive = true
            ),
            Offer(
                id = 10,
                providerId = 10,
                title = "PC-Check",
                description = "Kostenlose Diagnose Ihres Computers",
                discountValue = "0.00 €",
                validUntil = now.plus(15, ChronoUnit.DAYS),
                conditions = "Nur für neue Kunden. Reparaturkosten werden separat berechnet.",
                isActive = true
            )
        )
    }
    
    private fun createUsers(): List<User> {
        return listOf(
            User(
                id = 1,
                name = "Max Mustermann",
                email = "max@mustermann.de",
                userType = UserType.PRIVATE,
                createdAt = Instant.now().minus(30, ChronoUnit.DAYS)
            ),
            User(
                id = 2,
                name = "Firma Muster GmbH",
                email = "kontakt@muster-gmbh.de",
                userType = UserType.COMPANY,
                createdAt = Instant.now().minus(20, ChronoUnit.DAYS)
            ),
            User(
                id = 3,
                name = "Anna Schmidt",
                email = "anna.schmidt@email.de",
                userType = UserType.PRIVATE,
                createdAt = Instant.now().minus(10, ChronoUnit.DAYS)
            )
        )
    }
    
    private fun createBookings(demoUser: User, providers: List<Provider>, services: List<Service>): List<Booking> {
        val now = Instant.now()
        
        return listOf(
            Booking(
                id = 1,
                userId = demoUser.id,
                providerId = 1,
                serviceId = 1,
                bookingDate = now.plus(5, ChronoUnit.DAYS),
                timeSlot = "19:00 - 21:00",
                numberOfPeople = 2,
                status = BookingStatus.CONFIRMED,
                createdAt = now.minus(2, ChronoUnit.DAYS),
                notes = "Allergie gegen Nüsse"
            ),
            Booking(
                id = 2,
                userId = demoUser.id,
                providerId = 2,
                serviceId = 3,
                bookingDate = now.plus(7, ChronoUnit.DAYS),
                timeSlot = "10:00 - 11:00",
                numberOfPeople = 1,
                status = BookingStatus.CONFIRMED,
                createdAt = now.minus(1, ChronoUnit.DAYS)
            ),
            Booking(
                id = 3,
                userId = demoUser.id,
                providerId = 6,
                serviceId = 11,
                bookingDate = now.plus(10, ChronoUnit.DAYS),
                timeSlot = "18:00 - 19:15",
                numberOfPeople = 1,
                status = BookingStatus.PENDING,
                createdAt = now.minus(3, ChronoUnit.DAYS)
            ),
            Booking(
                id = 4,
                userId = demoUser.id,
                providerId = 8,
                serviceId = 15,
                bookingDate = now.minus(5, ChronoUnit.DAYS),
                timeSlot = "15:00 - 16:30",
                numberOfPeople = 1,
                status = BookingStatus.COMPLETED,
                createdAt = now.minus(10, ChronoUnit.DAYS)
            ),
            Booking(
                id = 5,
                userId = demoUser.id,
                providerId = 4,
                serviceId = 7,
                bookingDate = now.minus(15, ChronoUnit.DAYS),
                timeSlot = "16:00 - 17:00",
                numberOfPeople = 4,
                status = BookingStatus.CANCELLED,
                createdAt = now.minus(20, ChronoUnit.DAYS),
                notes = "Storniert aufgrund von Krankheit"
            )
        )
    }
    
    private fun createFavorites(demoUser: User, providers: List<Provider>): List<Favorite> {
        return listOf(
            Favorite(
                id = 1,
                userId = demoUser.id,
                providerId = 1,
                createdAt = Instant.now().minus(10, ChronoUnit.DAYS)
            ),
            Favorite(
                id = 2,
                userId = demoUser.id,
                providerId = 3,
                createdAt = Instant.now().minus(5, ChronoUnit.DAYS)
            ),
            Favorite(
                id = 3,
                userId = demoUser.id,
                providerId = 8,
                createdAt = Instant.now().minus(2, ChronoUnit.DAYS)
            )
        )
    }
}
