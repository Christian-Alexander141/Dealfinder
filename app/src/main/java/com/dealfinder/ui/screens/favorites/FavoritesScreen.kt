package com.dealfinder.ui.screens.favorites

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.di.AppModule
import com.dealfinder.ui.components.ProviderCard
import com.dealfinder.viewmodel.FavoriteViewModel

/**
 * Favoriten-Übersicht ("Meine Favoriten").
 *
 * [FavoriteViewModel.favoriteItems] liefert reaktiv Anbieter inkl. unabhängiger
 * Demo-Bewertung und Angebotsstatus; das Entfernen eines Favoriten aktualisiert
 * die Liste automatisch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FavoritesScreen(
    onProviderClick: (Long) -> Unit
) {
    val favoriteViewModel: FavoriteViewModel = viewModel()
    val favoriteItems by favoriteViewModel.favoriteItems.collectAsState()
    val categories by AppModule.provideCategoryRepository()
        .getAllCategories()
        .collectAsState(initial = emptyList())

    val snackbarHostState = remember { SnackbarHostState() }
    val uiState by favoriteViewModel.uiState.collectAsState()

    // Fehler beim Favorisieren als Snackbar zeigen
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is FavoriteViewModel.FavoriteUiState.Error -> {
                snackbarHostState.showSnackbar(state.message)
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(text = "Meine Favoriten") }
            )
        }
    ) { innerPadding ->
        if (favoriteItems.isEmpty()) {
            // --- Leerzustand ---
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Noch keine Favoriten",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Tippe beim Durchstöbern auf das Herz eines Anbieters – " +
                        "er wird hier gespeichert und bleibt dauerhaft merkbar.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // --- Demo-Hinweis ---
                item {
                    Text(
                        text = "Bewertungen sind Demo-Bewertungen – keine echten Tests.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                items(favoriteItems, key = { it.provider.id }) { item ->
                    val categoryName = categories
                        .firstOrNull { it.id == item.provider.categoryId }
                        ?.name

                    ProviderCard(
                        provider = item.provider,
                        rating = item.averageRating,
                        isFavorite = true,
                        hasOffer = item.hasActiveOffer,
                        categoryName = categoryName,
                        onClick = { onProviderClick(item.provider.id) },
                        onFavoriteClick = {
                            // Entfernen → favoriteItems aktualisiert sich reaktiv
                            favoriteViewModel.toggleFavorite(item.provider.id)
                        }
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}
