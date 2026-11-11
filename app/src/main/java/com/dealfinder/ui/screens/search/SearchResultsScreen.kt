package com.dealfinder.ui.screens.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.data.search.SortOrder
import com.dealfinder.ui.components.CategoryChip
import com.dealfinder.ui.components.ProviderCard
import com.dealfinder.viewmodel.ProviderViewModel

/**
 * Suchergebnisse mit Filtern (Kategorie, Bewertung, Entfernung, Sonderangebote)
 * und Sortierung (Bewertung, Entfernung, Name).
 *
 * Angezeigt werden ausschließlich aktive Dealfinder-Partner; die Filterung und
 * Sortierung erfolgt reaktiv über den [ProviderViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchResultsScreen(
    initialQuery: String = "",
    initialLocation: String = "",
    initialCategoryId: Long? = null,
    onProviderClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val providerViewModel: ProviderViewModel = viewModel()

    var localQuery by remember { mutableStateOf(initialQuery) }
    var showFilters by remember { mutableStateOf(false) }

    val searchState by providerViewModel.searchState.collectAsState()
    val filters by providerViewModel.searchFilters.collectAsState()
    val categories by providerViewModel.categories.collectAsState(emptyList())
    val favoriteIds by providerViewModel.favoriteIds.collectAsState()
    val userLocation by providerViewModel.userLocation.collectAsState()
    val minRating by providerViewModel.minRating.collectAsState()
    val maxDistanceKm by providerViewModel.maxDistanceKm.collectAsState()
    val onlyWithOffers by providerViewModel.onlyWithOffers.collectAsState()
    val sortOrder by providerViewModel.sortOrder.collectAsState()

    // Nav-Argumente einmalig auf den ViewModel übertragen
    LaunchedEffect(Unit) {
        providerViewModel.onSearchQueryChanged(initialQuery)
        providerViewModel.onLocationQueryChanged(initialLocation)
        providerViewModel.onCategorySelected(initialCategoryId)
    }

    val categoryNames = remember(categories) { categories.associate { it.id to it.name } }
    val filtersActive = filters.isAnyFilterActive

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = localQuery,
                        onValueChange = {
                            localQuery = it
                            providerViewModel.onSearchQueryChanged(it)
                        },
                        placeholder = { Text("Anbieter oder Dienstleistung suchen") },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Suche",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        },
                        trailingIcon = {
                            if (localQuery.isNotEmpty()) {
                                IconButton(onClick = {
                                    localQuery = ""
                                    providerViewModel.onSearchQueryChanged("")
                                }) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Löschen",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showFilters = !showFilters }) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (showFilters || filtersActive) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            }
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Filterbereich ---
            if (showFilters) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Filter & Sortierung",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Kategorie
                    Text(
                        text = "Kategorie",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        CategoryChip(
                            categoryName = "Alle",
                            isSelected = filters.categoryId == null,
                            onClick = { providerViewModel.onCategorySelected(null) }
                        )
                        categories.forEach { category ->
                            CategoryChip(
                                categoryName = category.name,
                                icon = category.icon,
                                isSelected = filters.categoryId == category.id,
                                onClick = {
                                    providerViewModel.onCategorySelected(
                                        if (filters.categoryId == category.id) null else category.id
                                    )
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Bewertung
                    Text(
                        text = "Mindestbewertung (unabhängig geprüft)",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = minRating == null,
                            onClick = { providerViewModel.onMinRatingChanged(null) },
                            label = { Text("Alle") }
                        )
                        listOf(3.5f, 4.0f, 4.5f).forEach { value ->
                            FilterChip(
                                selected = minRating == value,
                                onClick = {
                                    providerViewModel.onMinRatingChanged(if (minRating == value) null else value)
                                },
                                label = { Text("${String.format(java.util.Locale.GERMAN, "%.1f", value)}+") }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Entfernung (nur mit aktivem Standort sinnvoll)
                    if (userLocation != null) {
                        Text(
                            text = "Maximale Entfernung",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = maxDistanceKm == null,
                                onClick = { providerViewModel.onMaxDistanceChanged(null) },
                                label = { Text("Alle") }
                            )
                            listOf(2.0, 5.0, 10.0, 25.0).forEach { value ->
                                FilterChip(
                                    selected = maxDistanceKm == value,
                                    onClick = {
                                        providerViewModel.onMaxDistanceChanged(if (maxDistanceKm == value) null else value)
                                    },
                                    label = { Text("${value.toInt()} km") }
                                )
                            }
                        }
                    } else {
                        Text(
                            text = "Tipp: Auf der Startseite den eigenen Standort aktivieren, um nach Entfernung zu filtern.",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Nur mit Sonderangebot
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Nur Anbieter mit Sonderangebot",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Switch(
                            checked = onlyWithOffers,
                            onCheckedChange = { providerViewModel.onOnlyWithOffersChanged(it) }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Sortierung
                    Text(
                        text = "Sortierung",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = sortOrder == SortOrder.RELEVANCE,
                            onClick = { providerViewModel.onSortOrderChanged(SortOrder.RELEVANCE) },
                            label = { Text("Empfohlen") }
                        )
                        FilterChip(
                            selected = sortOrder == SortOrder.RATING,
                            onClick = { providerViewModel.onSortOrderChanged(SortOrder.RATING) },
                            label = { Text("Bewertung") }
                        )
                        FilterChip(
                            selected = sortOrder == SortOrder.DISTANCE,
                            onClick = { providerViewModel.onSortOrderChanged(SortOrder.DISTANCE) },
                            label = { Text("Entfernung") },
                            enabled = userLocation != null
                        )
                        FilterChip(
                            selected = sortOrder == SortOrder.NAME,
                            onClick = { providerViewModel.onSortOrderChanged(SortOrder.NAME) },
                            label = { Text("A–Z") }
                        )
                    }

                    if (filtersActive) {
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.TextButton(
                            onClick = { providerViewModel.clearFilters() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Filter zurücksetzen", color = MaterialTheme.colorScheme.primary)
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                }
            }

            // --- Ergebniszähler ---
            Text(
                text = if (searchState.isLoading) "Wird geladen …"
                else "${searchState.items.size} Anbieter gefunden (nur aktive Dealfinder-Partner)",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            )

            when {
                // --- Ladezustand ---
                searchState.isLoading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Suche läuft …",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // --- Fehlerzustand ---
                searchState.error != null -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = searchState.error!!,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // --- Ergebnisse ---
                searchState.items.isNotEmpty() -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        )
                    ) {
                        items(searchState.items, key = { it.provider.id }) { item ->
                            ProviderCard(
                                provider = item.provider,
                                rating = item.averageRating,
                                isFavorite = favoriteIds.contains(item.provider.id),
                                hasOffer = item.hasActiveOffer,
                                categoryName = categoryNames[item.provider.categoryId],
                                distanceKm = item.distanceKm,
                                onClick = { onProviderClick(item.provider.id) },
                                onFavoriteClick = { providerViewModel.toggleFavorite(item.provider.id) }
                            )
                        }
                    }
                }

                // --- Leerzustand ---
                else -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Keine Anbieter gefunden",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Passen Sie Suchbegriff oder Filter an – es werden ausschließlich aktive Dealfinder-Partner angezeigt.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
