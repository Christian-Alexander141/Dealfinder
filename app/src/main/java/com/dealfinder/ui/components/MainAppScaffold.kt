package com.dealfinder.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.dealfinder.navigation.AppNavHost

/**
 * Main scaffold for the Dealfinder app.
 *
 * Owns the single shared [androidx.navigation.NavHostController] instance so that
 * the bottom navigation bar and the [AppNavHost] always operate on the same
 * navigation state. The inner padding produced by the bottom bar is forwarded to
 * the nav host so that screen content is never hidden behind it.
 */
@Composable
fun MainAppScaffold() {
    val navController = rememberNavController()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            BottomNavigationBar(navController = navController)
        }
    ) { innerPadding ->
        AppNavHost(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
