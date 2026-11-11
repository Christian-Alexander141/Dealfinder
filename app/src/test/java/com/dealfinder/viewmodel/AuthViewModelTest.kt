package com.dealfinder.viewmodel

import com.dealfinder.TestEnv
import com.dealfinder.awaitValue
import com.dealfinder.data.models.UserType
import com.dealfinder.di.AppModule
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests für die lokale Demo-Registrierung (keine Passwörter, keine Server).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AuthViewModelTest {

    private lateinit var viewModel: AuthViewModel

    @Before
    fun setup() {
        TestEnv.setUp()
        viewModel = AuthViewModel()
    }

    @After
    fun tearDown() {
        TestEnv.tearDown()
    }

    @Test
    fun `uiState startet im Leerzustand`() {
        assertTrue(viewModel.uiState.value is AuthViewModel.AuthUiState.Idle)
    }

    @Test
    fun `Registrierung legt Nutzer an und startet eine Sitzung`() = runBlocking {
        viewModel.registerUser("Anna Beispiel", "anna@example.de", UserType.PRIVATE)

        val state = viewModel.awaitValue { it is AuthViewModel.AuthUiState.Success }
        state as AuthViewModel.AuthUiState.Success
        assertEquals("Anna Beispiel", state.name)
        assertEquals("anna@example.de", state.email)
        assertEquals(UserType.PRIVATE, state.userType)

        // Sitzung dauerhaft in DataStore gespeichert
        val storedUserId = AppModule.provideAppPreferences().currentUserId
            .awaitValue { it != null }
        assertEquals(state.userId, storedUserId)

        // Nutzer existiert in der Datenbank
        assertNotNull(
            AppModule.provideUserRepository().getUserByEmail("anna@example.de")
        )
    }

    @Test
    fun `doppelte Registrierung mit gleicher E-Mail wird abgelehnt`() {
        viewModel.registerUser("Erster Nutzer", "doppelt@example.de", UserType.COMPANY)
        viewModel.awaitValue { it is AuthViewModel.AuthUiState.Success }

        viewModel.registerUser("Zweiter Nutzer", "doppelt@example.de", UserType.PRIVATE)
        val state = viewModel.awaitValue { it is AuthViewModel.AuthUiState.Error }
        state as AuthViewModel.AuthUiState.Error
        assertTrue(state.message.contains("bereits registriert"))
    }

    @Test
    fun `Unternehmen wird als Nutzerart gespeichert`() = runBlocking {
        viewModel.registerUser("Firma GmbH", "firma@example.de", UserType.COMPANY)

        val state = viewModel.awaitValue { it is AuthViewModel.AuthUiState.Success }
        state as AuthViewModel.AuthUiState.Success

        val storedType = AppModule.provideAppPreferences().currentUserType
            .awaitValue { it != null }
        assertEquals(UserType.COMPANY, storedType)
    }
}
