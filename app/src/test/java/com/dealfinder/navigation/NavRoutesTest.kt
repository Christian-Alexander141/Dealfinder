package com.dealfinder.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Navigationstests: Routen-Muster, Argumentdeklarationen und Konsistenz
 * zwischen Route-Platzhaltern und [NavArgs].
 *
 * Die Platzhalter müssen exakt den in AppNavHost deklarierten Argumenten
 * entsprechen – sonst greifen die Nav-Argumente zur Laufzeit nicht.
 */
class NavRoutesTest {

    private val allRoutes = listOf(
        NavRoutes.HOME to NavRoutes.HOME,
        NavRoutes.SEARCH to NavRoutes.SEARCH,
        NavRoutes.FAVORITES to NavRoutes.FAVORITES,
        NavRoutes.BOOKINGS to NavRoutes.BOOKINGS,
        NavRoutes.PROFILE to NavRoutes.PROFILE,
        NavRoutes.WELCOME to NavRoutes.WELCOME,
        NavRoutes.USER_TYPE_SELECTION to NavRoutes.USER_TYPE_SELECTION,
        NavRoutes.REGISTRATION to NavRoutes.REGISTRATION,
        NavRoutes.SEARCH_RESULTS to NavRoutes.SEARCH_RESULTS,
        NavRoutes.PROVIDER_DETAIL to NavRoutes.PROVIDER_DETAIL,
        NavRoutes.BOOKING_SERVICE_SELECTION to NavRoutes.BOOKING_SERVICE_SELECTION,
        NavRoutes.BOOKING_DATE_SELECTION to NavRoutes.BOOKING_DATE_SELECTION,
        NavRoutes.BOOKING_TIME_SELECTION to NavRoutes.BOOKING_TIME_SELECTION,
        NavRoutes.BOOKING_CONFIRMATION to NavRoutes.BOOKING_CONFIRMATION,
        NavRoutes.BOOKING_DETAIL to NavRoutes.BOOKING_DETAIL,
        NavRoutes.OFFERS to NavRoutes.OFFERS,
        NavRoutes.OFFER_DETAIL to NavRoutes.OFFER_DETAIL,
        NavRoutes.MY_OFFERS to NavRoutes.MY_OFFERS,
        NavRoutes.SETTINGS to NavRoutes.SETTINGS,
        NavRoutes.ABOUT to NavRoutes.ABOUT
    )

    private fun placeholdersOf(route: String): List<String> =
        Regex("\\{([^}]+)}").findAll(route).map { it.groupValues[1] }.toList()

    @Test
    fun `Routen sind eindeutig`() {
        val routes = allRoutes.map { it.first }
        assertEquals(
            "Doppelte Routen gefunden: ${routes.groupingBy { it }.eachCount().filter { it.value > 1 }}",
            routes.size,
            routes.toSet().size
        )
    }

    @Test
    fun `jeder Routenplatzhalter ist in NavArgs deklariert`() {
        val knownArgs = setOf(
            NavArgs.PROVIDER_ID,
            NavArgs.SERVICE_ID,
            NavArgs.DATE,
            NavArgs.TIME_SLOT,
            NavArgs.BOOKING_ID,
            NavArgs.OFFER_ID
        )
        allRoutes.forEach { (_, route) ->
            placeholdersOf(route).forEach { placeholder ->
                assertTrue(
                    "Platzhalter {$placeholder} in '$route' fehlt in NavArgs",
                    placeholder in knownArgs || placeholder in setOf("userType", "query", "location", "categoryId")
                )
            }
        }
    }

    @Test
    fun `Buchungs-Route enthaelt alle Argumente des Flusses`() {
        assertEquals(
            listOf(NavArgs.PROVIDER_ID, NavArgs.SERVICE_ID),
            placeholdersOf(NavRoutes.BOOKING_DATE_SELECTION)
        )
        assertEquals(
            listOf(NavArgs.PROVIDER_ID, NavArgs.SERVICE_ID, NavArgs.DATE),
            placeholdersOf(NavRoutes.BOOKING_TIME_SELECTION)
        )
        assertEquals(
            listOf(NavArgs.PROVIDER_ID, NavArgs.SERVICE_ID, NavArgs.DATE, NavArgs.TIME_SLOT),
            placeholdersOf(NavRoutes.BOOKING_CONFIRMATION)
        )
    }

    @Test
    fun `Detail-Routen tragen die erwarteten IDs`() {
        assertEquals(listOf(NavArgs.PROVIDER_ID), placeholdersOf(NavRoutes.PROVIDER_DETAIL))
        assertEquals(listOf(NavArgs.BOOKING_ID), placeholdersOf(NavRoutes.BOOKING_DETAIL))
        assertEquals(listOf(NavArgs.OFFER_ID), placeholdersOf(NavRoutes.OFFER_DETAIL))
        assertEquals(listOf(NavArgs.PROVIDER_ID), placeholdersOf(NavRoutes.BOOKING_SERVICE_SELECTION))
    }

    @Test
    fun `Suchroute laesst optionale Query-Parameter mit Standardwerten zu`() {
        val searchRoute = "search?query={query}&location={location}&categoryId={categoryId}"
        val placeholders = placeholdersOf(searchRoute)
        assertEquals(listOf("query", "location", "categoryId"), placeholders)
    }
}
