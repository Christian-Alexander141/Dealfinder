package com.dealfinder.navigation

/**
 * Navigation routes for the Dealfinder app
 */
object NavRoutes {
    // Bottom Navigation Routes
    const val HOME = "home"
    const val SEARCH = "search"
    const val FAVORITES = "favorites"
    const val BOOKINGS = "bookings"
    const val PROFILE = "profile"
    
    // Authentication Routes
    const val WELCOME = "welcome"
    const val USER_TYPE_SELECTION = "user_type_selection"
    const val REGISTRATION = "registration"
    
    // Main App Routes
    const val SEARCH_RESULTS = "search_results"
    const val PROVIDER_DETAIL = "provider_detail/{providerId}"
    const val PROVIDER_DETAIL_WITH_ID = "provider_detail"
    const val BOOKING_FLOW = "booking_flow"
    const val BOOKING_SERVICE_SELECTION = "booking_service_selection/{providerId}"
    const val BOOKING_DATE_SELECTION = "booking_date_selection/{providerId}/{serviceId}"
    const val BOOKING_TIME_SELECTION = "booking_time_selection/{providerId}/{serviceId}/{date}"
    // Query-Parameter werden an die ConfirmationScreen weitergereicht (Argumente
    // müssen im Routen-Muster deklariert sein, sonst greifen sie nicht).
    const val BOOKING_CONFIRMATION =
        "booking_confirmation?providerId={providerId}&serviceId={serviceId}&date={date}&timeSlot={timeSlot}"
    const val BOOKING_DETAIL = "booking_detail/{bookingId}"
    const val OFFERS = "offers"
    const val OFFER_DETAIL = "offer_detail/{offerId}"
    const val MY_OFFERS = "my_offers"
    
    // Settings Routes
    const val SETTINGS = "settings"
    const val ABOUT = "about"
}

/**
 * Arguments for navigation
 */
object NavArgs {
    const val PROVIDER_ID = "providerId"
    const val SERVICE_ID = "serviceId"
    const val DATE = "date"
    const val TIME_SLOT = "timeSlot"
    const val BOOKING_ID = "bookingId"
    const val OFFER_ID = "offerId"
}
