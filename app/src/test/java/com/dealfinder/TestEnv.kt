package com.dealfinder

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.dealfinder.data.models.User
import com.dealfinder.data.models.UserType
import com.dealfinder.di.AppModule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * Gemeinsame Test-Hilfen.
 *
 * Alle Robolectric-Tests starten mit leerer Session und frischen Demodaten.
 * Die ViewModels nutzen Dispatchers.Main – im Test wird ein unconfined
 * Test-Dispatcher gesetzt, damit Zustandsänderungen sofort sichtbar sind.
 */
object TestEnv {

    /** Setzt die Testumgebung zurück und lädt die Demodaten. */
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        runBlocking {
            val context = ApplicationProvider.getApplicationContext<Context>()
            AppModule.initialize(context)
            AppModule.clearAllData()
            AppModule.initializeDemoData()
            AppModule.provideAppPreferences().clearAll()
        }
    }

    /** Räumt die Testumgebung ab. */
    fun tearDown() {
        runBlocking {
            AppModule.clearAllData()
            AppModule.provideAppPreferences().clearAll()
        }
        Dispatchers.resetMain()
    }

    /** Legt einen Test-Nutzer an und meldet ihn als Session an. */
    fun registerTestUser(
        name: String = "Test Nutzer",
        email: String = "test@dealfinder.local"
    ): User = runBlocking {
        val user = AppModule.provideUserRepository()
            .registerUser(name, email, UserType.PRIVATE)
        AppModule.provideAppPreferences()
            .setCurrentUser(user.id, user.name, user.email, user.userType)
        user
    }
}

/**
 * Wartet, bis der Flow einen Zustand erreicht, der [condition] erfüllt.
 *
 * Der Flow wird dabei wirklich abonnient (wichtig für StateFlows mit
 * SharingStarted.WhileSubscribed) und der Empfang mit Timeout abgesichert.
 */
fun <T> Flow<T>.awaitValue(
    timeoutMs: Long = 5_000,
    condition: (T) -> Boolean
): T {
    val result = kotlinx.coroutines.runBlocking {
        kotlinx.coroutines.withTimeoutOrNull(timeoutMs) {
            first { condition(it) }
        }
    }
    return result ?: throw AssertionError(
        "Timeout nach ${timeoutMs}ms – Bedingung wurde nicht erfüllt"
    )
}
