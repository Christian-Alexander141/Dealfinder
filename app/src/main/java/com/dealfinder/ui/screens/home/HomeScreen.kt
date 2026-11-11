package com.dealfinder.ui.screens.home

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.di.AppModule
import com.dealfinder.ui.components.CategoryChip
import com.dealfinder.ui.components.ProviderCard
import com.dealfinder.util.LocationHelper
import com.dealfinder.viewmodel.OfferViewModel
import com.dealfinder.viewmodel.ProviderViewModel
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Startseite: Suchfunktion (Aktivität + Standort), Kategorien, Empfehlungen,
 * aktuelle Sonderangebote und Zugang zu den Favoriten.
 *
 * Die Standortberechtigung wird ausschließlich auf Nutzerwunsch angefordert
 * (on-demand). Eine Ablehnung blockiert die Nutzung nicht – die manuelle
 * Ortseingabe bleibt jederzeit möglich.
 */
@Composable
fun HomeScreen(
    onSearchClick: (query: String, location: String, categoryId: Long?) -> Unit,
    onProviderClick: (Long) -> Unit,
    onOfferClick: (Long) -> Unit,
    onViewAllOffers: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    val providerViewModel: ProviderViewModel = viewModel()
    val offerViewModel: OfferViewModel = viewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val appPreferences = AppModule.provideAppPreferences()

    var activityQuery by remember { mutableStateOf("") }
    var locationQuery by remember { mutableStateOf("") }
    var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
    var locationStatus by remember { mutableStateOf<String?>(null) }

    val searchState by providerViewModel.searchState.collectAsState()
    val categories by providerViewModel.categories.collectAsState(emptyList())
    val offers by offerViewModel.allOffers.collectAsState()
    val favoriteIds by providerViewModel.favoriteIds.collectAsState()
    val userLocation by providerViewModel.userLocation.collectAsState()

    // Empfehlungen: nach unabhängiger Bewertung sortiert (nur aktive Partner)
    val recommendations = remember(searchState.items) {
        searchState.items
            .filter { it.provider.isActive && it.provider.isDealfinderPartner }
            .filter { selectedCategoryId == null || it.provider.categoryId == selectedCategoryId }
            .sortedByDescending { it.averageRating ?: 0f }
            .take(4)
    }

    val activeOffers = remember(offers) {
        offers.filter { it.validUntil.isAfter(Instant.now()) }.take(3)
    }

    // Standort nur auf expliziten Nutzerwunsch anfordern
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val granted = results[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            results[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (granted) {
            scope.launch {
                val location: Location? = LocationHelper.getCurrentLocation(context)
                if (location != null) {
                    appPreferences.setLastLocation(location.latitude, location.longitude)
                    locationStatus = "Eigener Standort verwendet – Entfernungen werden berechnet."
                } else {
                    locationStatus = "Standort konnte nicht ermittelt werden. Bitte manuell eingeben."
                }
            }
        } else {
            locationStatus = "Standortfreigabe abgelehnt – Sie können den Ort weiterhin manuell eingeben."
        }
    }

    fun requestLocation() {
        val hasPermission =
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED

        if (hasPermission) {
            scope.launch {
                val location: Location? = LocationHelper.getCurrentLocation(context)
                if (location != null) {
                    appPreferences.setLastLocation(location.latitude, location.longitude)
                    locationStatus = "Eigener Standort verwendet – Entfernungen werden berechnet."
                } else {
                    locationStatus = "Standort konnte nicht ermittelt werden. Bitte manuell eingeben."
                }
            }
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // --- Marke ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
        ) {
            Text(
                text = "Dealfinder",
                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Finden. Vertrauen. Buchen. – unabhängig geprüfte Dienstleister in Ihrer Nähe",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // --- Suchkarte ---
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = activityQuery,
                    onValueChange = { activityQuery = it },
                    label = { Text("Aktivität oder Dienstleistung") },
                    placeholder = { Text("z. B. Friseur, Restaurant, Yoga") },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search)
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = locationQuery,
                    onValueChange = { locationQuery = it },
                    label = { Text("Standort") },
                    placeholder = { Text("PLZ oder Stadtteil") },
                    leadingIcon = {
                        Icon(Icons.Default.LocationOn, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButtonLocation(onClick = { requestLocation() })
                }

                if (locationStatus != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = locationStatus!!,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (userLocation != null) {
                    Text(
                        text = "Eigener Standort aktiv – Ergebnisse werden nach Entfernung sortiert.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        providerViewModel.onLocationQueryChanged(locationQuery)
                        onSearchClick(activityQuery, locationQuery, selectedCategoryId)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Anbieter suchen")
                }
            }
        }

        // --- Favoriten-Zugang ---
        Card(
            onClick = onFavoritesClick,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Meine Favoriten",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Text(
                        text = if (favoriteIds.isEmpty()) "Noch keine Favoriten gespeichert"
                        else "${favoriteIds.size} gespeicherte Anbieter ansehen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                Text(
                    text = "›",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer
                )
            }
        }

        // --- Kategorien ---
        SectionTitle(title = "Kategorien")

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                categoryName = "Alle",
                isSelected = selectedCategoryId == null,
                onClick = { selectedCategoryId = null }
            )
            categories.forEach { category ->
                CategoryChip(
                    categoryName = category.name,
                    icon = category.icon,
                    isSelected = selectedCategoryId == category.id,
                    onClick = {
                        selectedCategoryId = if (selectedCategoryId == category.id) null else category.id
                    }
                )
            }
        }

        // --- Empfehlungen ---
        SectionTitle(title = "Empfehlungen")

        if (recommendations.isEmpty()) {
            EmptyHint(text = "Noch keine Empfehlungen – Demodaten werden geladen.")
        } else {
            recommendations.forEach { item ->
                ProviderCard(
                    provider = item.provider,
                    rating = item.averageRating,
                    isFavorite = favoriteIds.contains(item.provider.id),
                    hasOffer = item.hasActiveOffer,
                    distanceKm = item.distanceKm,
                    onClick = { onProviderClick(item.provider.id) },
                    onFavoriteClick = { providerViewModel.toggleFavorite(item.provider.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // --- Sonderangebote ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Aktuelle Sonderangebote",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(onClick = onViewAllOffers) {
                Text("Alle")
            }
        }

        if (activeOffers.isEmpty()) {
            EmptyHint(text = "Zurzeit keine aktiven Demo-Angebote.")
        } else {
            activeOffers.forEach { offer ->
                Card(
                    onClick = { onOfferClick(offer.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = offer.title,
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = offer.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Rabatt: ${offer.discountValue} · Gültig bis " +
                                    offer.validUntil.atZone(ZoneId.systemDefault())
                                        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.GERMAN)),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Text(
                            text = "Demo",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Hinweis: Dies ist ein Prototyp mit vollständig fiktiven Demodaten. " +
                "Bewertungen, Angebote und Buchungen sind Beispieldaten.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun EmptyHint(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun TextButtonLocation(onClick: () -> Unit) {
    androidx.compose.material3.TextButton(onClick = onClick) {
        Icon(
            imageVector = Icons.Default.LocationOn,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text("Eigenen Standort verwenden", style = MaterialTheme.typography.labelMedium)
    }
}
