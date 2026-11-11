package com.dealfinder.navigation

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.dealfinder.di.AppModule
import com.dealfinder.ui.screens.about.AboutScreen
import com.dealfinder.ui.screens.auth.RegistrationScreen
import com.dealfinder.ui.screens.auth.UserTypeSelectionScreen
import com.dealfinder.ui.screens.booking.BookingConfirmationScreen
import com.dealfinder.ui.screens.booking.BookingDateSelectionScreen
import com.dealfinder.ui.screens.booking.BookingDetailScreen
import com.dealfinder.ui.screens.booking.BookingServiceSelectionScreen
import com.dealfinder.ui.screens.booking.BookingTimeSelectionScreen
import com.dealfinder.ui.screens.home.HomeScreen
import com.dealfinder.ui.screens.favorites.FavoritesScreen
import com.dealfinder.ui.screens.offers.MyOffersScreen
import com.dealfinder.ui.screens.offers.OffersScreen
import com.dealfinder.ui.screens.offers.OfferDetailScreen
import com.dealfinder.ui.screens.profile.ProfileScreen
import com.dealfinder.ui.screens.provider.ProviderDetailScreen
import com.dealfinder.ui.screens.search.SearchResultsScreen
import com.dealfinder.ui.screens.bookings.MyBookingsScreen
import com.dealfinder.ui.screens.settings.SettingsScreen
import com.dealfinder.ui.screens.welcome.WelcomeScreen
import com.dealfinder.viewmodel.AuthViewModel
import kotlinx.coroutines.flow.first

/**
 * Main navigation host for the Dealfinder app.
 *
 * Receives the shared [navController] from the hosting scaffold so that bottom
 * navigation and in-screen navigation act on one single back stack.
 */
@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    val authViewModel: AuthViewModel = viewModel()
    val appPreferences = AppModule.provideAppPreferences()
    val scope = rememberCoroutineScope()

    // Start-Destination erst bestimmen, wenn die DataStore-Werte tatsächlich
    // vorliegen (sonst startet die App bei jedem Neustart auf dem Willkommensbild).
    val startDestination by produceState<String?>(initialValue = null) {
        val onboarded = appPreferences.isOnboarded.first()
        val userId = appPreferences.currentUserId.first()
        value = if (onboarded && userId != null) NavRoutes.HOME else NavRoutes.WELCOME
    }

    if (startDestination == null) {
        // Kurzer Ladezustand, bis die gespeicherte Session gelesen wurde
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = androidx.compose.ui.Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    Log.d("AppNavHost", "Starting with destination: $startDestination")

    NavHost(
        navController = navController,
        startDestination = startDestination!!,
        modifier = modifier
    ) {
        // Authentication Flow
        composable(NavRoutes.WELCOME) {
            WelcomeScreen(
                onGetStarted = {
                    scope.launch { appPreferences.markAsOnboarded() }
                    navController.navigate(NavRoutes.USER_TYPE_SELECTION)
                },
                onSkip = {
                    scope.launch { appPreferences.markAsOnboarded() }
                    navController.navigate(NavRoutes.HOME)
                }
            )
        }
        
        composable(NavRoutes.USER_TYPE_SELECTION) {
            UserTypeSelectionScreen(
                onUserTypeSelected = { userType ->
                    navController.navigate("${NavRoutes.REGISTRATION}/$userType")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = "${NavRoutes.REGISTRATION}/{userType}",
            arguments = listOf(navArgument("userType") { defaultValue = "PRIVATE" })
        ) { backStackEntry ->
            val userType = backStackEntry.arguments?.getString("userType") ?: "PRIVATE"
            RegistrationScreen(
                userType = userType,
                onRegistrationComplete = { userId, name, email, userType ->
                    // Set user and navigate
                    authViewModel.setCurrentUser(userId, name, email, userType)
                    scope.launch { appPreferences.markAsOnboarded() }
                    navController.navigate(NavRoutes.HOME) {
                        popUpTo(NavRoutes.WELCOME) { inclusive = true }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Main App Bottom Navigation
        composable(NavRoutes.HOME) {
            HomeScreen(
                onSearchClick = { query, location, categoryId ->
                    val encodedQuery = android.net.Uri.encode(query)
                    val encodedLocation = android.net.Uri.encode(location)
                    val categoryPart = categoryId?.let { "&categoryId=$it" } ?: ""
                    navController.navigate("search?query=$encodedQuery&location=$encodedLocation$categoryPart")
                },
                onProviderClick = { providerId ->
                    navController.navigate("${NavRoutes.PROVIDER_DETAIL}/$providerId")
                },
                onOfferClick = { offerId ->
                    navController.navigate("${NavRoutes.OFFER_DETAIL}/$offerId")
                },
                onViewAllOffers = {
                    navController.navigate(NavRoutes.OFFERS)
                },
                onFavoritesClick = {
                    navController.navigate(NavRoutes.FAVORITES)
                }
            )
        }
        
        composable(
            route = "search?query={query}&location={location}&categoryId={categoryId}",
            arguments = listOf(
                navArgument("query") { defaultValue = "" },
                navArgument("location") { defaultValue = "" },
                navArgument("categoryId") {
                    type = NavType.LongType
                    defaultValue = -1L
                }
            )
        ) { backStackEntry ->
            val initialQuery = backStackEntry.arguments?.getString("query") ?: ""
            val initialLocation = backStackEntry.arguments?.getString("location") ?: ""
            val categoryId = backStackEntry.arguments?.getLong("categoryId") ?: -1L
            SearchResultsScreen(
                initialQuery = initialQuery,
                initialLocation = initialLocation,
                initialCategoryId = categoryId.takeIf { it > 0 },
                onProviderClick = { providerId ->
                    navController.navigate("${NavRoutes.PROVIDER_DETAIL}/$providerId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(NavRoutes.FAVORITES) {
            FavoritesScreen(
                onProviderClick = { providerId ->
                    navController.navigate("${NavRoutes.PROVIDER_DETAIL}/$providerId")
                }
            )
        }
        
        composable(NavRoutes.BOOKINGS) {
            MyBookingsScreen(
                onBookingClick = { bookingId ->
                    navController.navigate("${NavRoutes.BOOKING_DETAIL}/$bookingId")
                },
                onBookAgain = { providerId ->
                    navController.navigate("${NavRoutes.PROVIDER_DETAIL}/$providerId")
                }
            )
        }
        
        composable(NavRoutes.PROFILE) {
            ProfileScreen(
                onEditProfile = {},
                onViewFavorites = { navController.navigate(NavRoutes.FAVORITES) },
                onViewBookings = { navController.navigate(NavRoutes.BOOKINGS) },
                onViewMyOffers = { navController.navigate(NavRoutes.MY_OFFERS) },
                onSettings = { navController.navigate(NavRoutes.SETTINGS) },
                onAbout = { navController.navigate(NavRoutes.ABOUT) },
                onLogout = {
                    scope.launch {
                        AppModule.provideAppPreferences().clearCurrentUser()
                        navController.navigate(NavRoutes.WELCOME) {
                            popUpTo(NavRoutes.HOME) { inclusive = true }
                        }
                    }
                },
                onResetDemoData = {
                    scope.launch {
                        AppModule.clearAllData()
                        AppModule.initializeDemoData()
                        navController.navigate(NavRoutes.HOME) {
                            popUpTo(NavRoutes.PROFILE) { inclusive = false }
                        }
                    }
                }
            )
        }
        
        // Search Results
        composable(NavRoutes.SEARCH_RESULTS) {
            SearchResultsScreen(
                onProviderClick = { providerId ->
                    navController.navigate("${NavRoutes.PROVIDER_DETAIL}/$providerId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Provider Detail
        composable(
            route = NavRoutes.PROVIDER_DETAIL,
            arguments = listOf(
                navArgument(NavArgs.PROVIDER_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getLong(NavArgs.PROVIDER_ID) ?: 0L
            ProviderDetailScreen(
                providerId = providerId,
                onBack = { navController.popBackStack() },
                onBookClick = {
                    navController.navigate("${NavRoutes.BOOKING_SERVICE_SELECTION}/$providerId")
                },
                onOfferClick = { offerId ->
                    navController.navigate("${NavRoutes.OFFER_DETAIL}/$offerId")
                }
            )
        }
        
        // Booking Flow
        composable(
            route = NavRoutes.BOOKING_SERVICE_SELECTION,
            arguments = listOf(
                navArgument(NavArgs.PROVIDER_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getLong(NavArgs.PROVIDER_ID) ?: 0L
            BookingServiceSelectionScreen(
                providerId = providerId,
                onServiceSelected = { serviceId ->
                    navController.navigate("${NavRoutes.BOOKING_DATE_SELECTION}/$providerId/$serviceId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = NavRoutes.BOOKING_DATE_SELECTION,
            arguments = listOf(
                navArgument(NavArgs.PROVIDER_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument(NavArgs.SERVICE_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getLong(NavArgs.PROVIDER_ID) ?: 0L
            val serviceId = backStackEntry.arguments?.getLong(NavArgs.SERVICE_ID) ?: 0L
            BookingDateSelectionScreen(
                providerId = providerId,
                serviceId = serviceId,
                onDateSelected = { date ->
                    navController.navigate("${NavRoutes.BOOKING_TIME_SELECTION}/$providerId/$serviceId/$date")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = NavRoutes.BOOKING_TIME_SELECTION,
            arguments = listOf(
                navArgument(NavArgs.PROVIDER_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument(NavArgs.SERVICE_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument(NavArgs.DATE) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getLong(NavArgs.PROVIDER_ID) ?: 0L
            val serviceId = backStackEntry.arguments?.getLong(NavArgs.SERVICE_ID) ?: 0L
            val date = backStackEntry.arguments?.getString(NavArgs.DATE) ?: ""
            BookingTimeSelectionScreen(
                providerId = providerId,
                serviceId = serviceId,
                date = date,
                onTimeSelected = { timeSlot ->
                    val encodedSlot = android.net.Uri.encode(timeSlot)
                    navController.navigate(
                        "${NavRoutes.BOOKING_CONFIRMATION}?" +
                            "${NavArgs.PROVIDER_ID}=$providerId&" +
                            "${NavArgs.SERVICE_ID}=$serviceId&" +
                            "${NavArgs.DATE}=$date&" +
                            "timeSlot=$encodedSlot"
                    )
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = NavRoutes.BOOKING_CONFIRMATION,
            arguments = listOf(
                navArgument(NavArgs.PROVIDER_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument(NavArgs.SERVICE_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                },
                navArgument(NavArgs.DATE) {
                    type = NavType.StringType
                    defaultValue = ""
                },
                navArgument(NavArgs.TIME_SLOT) {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val providerId = backStackEntry.arguments?.getLong(NavArgs.PROVIDER_ID) ?: 0L
            val serviceId = backStackEntry.arguments?.getLong(NavArgs.SERVICE_ID) ?: 0L
            val date = backStackEntry.arguments?.getString(NavArgs.DATE) ?: ""
            val timeSlot = backStackEntry.arguments?.getString(NavArgs.TIME_SLOT) ?: ""
            BookingConfirmationScreen(
                providerId = providerId,
                serviceId = serviceId,
                date = date,
                timeSlot = timeSlot,
                onBookingConfirmed = { bookingId ->
                    navController.navigate(NavRoutes.BOOKINGS) {
                        popUpTo(NavRoutes.HOME) { inclusive = false }
                    }
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = NavRoutes.BOOKING_DETAIL,
            arguments = listOf(
                navArgument(NavArgs.BOOKING_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val bookingId = backStackEntry.arguments?.getLong(NavArgs.BOOKING_ID) ?: 0L
            BookingDetailScreen(
                bookingId = bookingId,
                onBack = { navController.popBackStack() },
                onCancelBooking = {
                    navController.popBackStack()
                },
                onRebook = { providerId ->
                    navController.navigate("${NavRoutes.PROVIDER_DETAIL}/$providerId") {
                        popUpTo(NavRoutes.BOOKINGS) { inclusive = false }
                    }
                }
            )
        }
        
        // Offers
        composable(NavRoutes.OFFERS) {
            OffersScreen(
                onOfferClick = { offerId ->
                    navController.navigate("${NavRoutes.OFFER_DETAIL}/$offerId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        composable(
            route = NavRoutes.OFFER_DETAIL,
            arguments = listOf(
                navArgument(NavArgs.OFFER_ID) {
                    type = NavType.LongType
                    defaultValue = 0L
                }
            )
        ) { backStackEntry ->
            val offerId = backStackEntry.arguments?.getLong(NavArgs.OFFER_ID) ?: 0L
            OfferDetailScreen(
                offerId = offerId,
                onBack = { navController.popBackStack() },
                onProviderClick = { providerId ->
                    navController.navigate("${NavRoutes.PROVIDER_DETAIL}/$providerId")
                },
                onSaveOffer = {
                    // Vormerkung erfolgt direkt in der Screen-Logik (OfferViewModel)
                }
            )
        }
        
        composable(NavRoutes.MY_OFFERS) {
            MyOffersScreen(
                onOfferClick = { offerId ->
                    navController.navigate("${NavRoutes.OFFER_DETAIL}/$offerId")
                },
                onBack = { navController.popBackStack() }
            )
        }
        
        // Settings
        composable(NavRoutes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAbout = { navController.navigate(NavRoutes.ABOUT) }
            )
        }
        
        composable(NavRoutes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
