package com.dealfinder.ui.screens.provider

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.data.models.BookingType
import com.dealfinder.data.models.Offer
import com.dealfinder.data.models.Service
import com.dealfinder.data.models.VerificationStatus
import com.dealfinder.data.models.VerifiedRating
import com.dealfinder.ui.theme.DealfinderTheme
import com.dealfinder.viewmodel.ProviderViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Vollständige Detailansicht eines Anbieters.
 *
 * Enthält Beschreibung, Adresse, Kontakt, die unabhängige (Demo-)Bewertung mit
 * Kriterien/Testdatum/Verifizierungsstatus, Dienstleistungen, Sonderangebote,
 * Favoriten-Schalter, Buchungs- und Navigations-Button (Geo-Intent).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProviderDetailScreen(
    providerId: Long,
    onBack: () -> Unit,
    onBookClick: () -> Unit,
    onOfferClick: (Long) -> Unit
) {
    val providerViewModel: ProviderViewModel = viewModel()
    val context = LocalContext.current

    LaunchedEffect(providerId) {
        providerViewModel.selectProvider(providerId)
    }

    val provider by providerViewModel.selectedProvider.collectAsState()
    val services by providerViewModel.services.collectAsState()
    val ratings by providerViewModel.ratings.collectAsState()
    val averageScore by providerViewModel.averageScore.collectAsState()
    val offers by providerViewModel.offers.collectAsState()
    val isFavorite by providerViewModel.isFavorite.collectAsState()
    val uiState by providerViewModel.uiState.collectAsState()

    // Kategorie-Name nachladen (einmaliger Aufruf)
    var categoryName by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(provider?.categoryId) {
        categoryName = providerViewModel.getCategoryName(provider?.categoryId)
    }

    val germanDate = remember { DateTimeFormatter.ofPattern("dd. MMMM yyyy", Locale.GERMAN) }

    // Geo-Intent für externe Kartenanwendung (kein API-Schlüssel nötig)
    fun openNavigation() {
        val current = provider ?: return
        val geoUri = if (current.latitude != null && current.longitude != null) {
            "geo:${current.latitude},${current.longitude}?q=${Uri.encode(current.address)}"
        } else {
            "geo:0,0?q=${Uri.encode(current.address)}"
        }
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(geoUri)))
        } catch (e: ActivityNotFoundException) {
            // Fallback: Web-Suche nach der Adresse
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.google.com/maps/search/?api=1&query=${Uri.encode(current.address)}")
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = provider?.name ?: "Anbieter") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                },
                actions = {
                    if (provider != null) {
                        IconButton(
                            onClick = { providerViewModel.toggleFavorite(providerId) }
                        ) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isFavorite) "Aus Favoriten entfernen" else "Zu Favoriten hinzufügen",
                                tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        when {
            uiState is ProviderViewModel.ProviderUiState.Error -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = (uiState as ProviderViewModel.ProviderUiState.Error).message,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onBack) { Text("Zurück") }
                }
            }

            provider == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Anbieter wird geladen …",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            else -> {
                val current = provider!!
                // Nur VERIFIZIERTE Testbewertungen gelten als unabhängiges Testergebnis
                val verifiedRatings = ratings.filter { it.verificationStatus == VerificationStatus.VERIFIED }
                val unverifiedRatings = ratings.filter { it.verificationStatus != VerificationStatus.VERIFIED }
                val latestVerified = verifiedRatings.maxByOrNull { it.testDate }
                val activeOffers = offers.filter { it.isActive && it.validUntil.isAfter(Instant.now()) }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // --- Kopfbereich ---
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = current.name,
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            AssistChip(
                                onClick = {},
                                label = { Text("Dealfinder Partner") }
                            )
                            if (categoryName != null) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(categoryName!!) }
                                )
                            }
                            if (current.isActive) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text("Aktiv") }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = current.description,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = current.address,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // --- Bewertung ---
                    RatingSection(
                        averageScore = averageScore,
                        latestVerified = latestVerified,
                        unverifiedRatings = unverifiedRatings,
                        germanDate = germanDate
                    )

                    // --- Kontakt ---
                    ContactCard(
                        phone = current.phone,
                        email = current.email,
                        website = current.website
                    )

                    // --- Dienstleistungen ---
                    ServicesSection(
                        services = services,
                        onBookClick = onBookClick
                    )

                    // --- Sonderangebote ---
                    if (activeOffers.isNotEmpty()) {
                        OffersSection(
                            offers = activeOffers,
                            onOfferClick = onOfferClick
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Aktionen ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onBookClick,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Termin buchen")
                        }
                        OutlinedButton(
                            onClick = { openNavigation() },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Navigation,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Navigieren")
                        }
                    }

                    Text(
                        text = "Demo-Prototyp – fiktive Anbieter- und Testdaten.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Bewertungsblock: Gesamtbewertung, Kriterien, Testdatum und Verifizierungsstatus.
 * Nicht verifizierte Bewertungen werden NICHT als unabhängiges Testergebnis dargestellt.
 */
@Composable
private fun RatingSection(
    averageScore: Float?,
    latestVerified: VerifiedRating?,
    unverifiedRatings: List<VerifiedRating>,
    germanDate: DateTimeFormatter
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Unabhängige Bewertung",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.weight(1f)
                )
                AssistChip(
                    onClick = {},
                    label = { Text("Demo-Bewertung") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (averageScore != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = String.format(Locale.GERMAN, "%.1f", averageScore),
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = " / 5,0",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Unabhängig geprüft – Durchschnitt verifizierter Tests",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Noch keine verifizierte Bewertung vorhanden.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (latestVerified != null) {
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Verifizierungsstatus
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (latestVerified.verificationStatus) {
                            VerificationStatus.VERIFIED -> "Status: Unabhängige Prüfung verifiziert"
                            VerificationStatus.PENDING -> "Status: Prüfung ausstehend"
                            VerificationStatus.REJECTED -> "Status: Prüfung abgelehnt"
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Testdatum: ${latestVerified.testDate.atZone(ZoneId.systemDefault()).format(germanDate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Prüf-Referenz: ${latestVerified.testerReference}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Einzelne Kriterien
                if (latestVerified.criteria.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Geprüfte Kriterien",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    latestVerified.criteria.forEach { (criterion, score) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = criterion,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.width(120.dp)
                            )
                            LinearProgressIndicator(
                                progress = { (score / 5f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = String.format(Locale.GERMAN, "%.1f", score),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }
                }

                if (!latestVerified.notes.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Tester-Notiz: ${latestVerified.notes}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Alle Bewertungen dieser App sind ausdrücklich als Demodaten gekennzeichnet.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Nicht verifizierte Bewertungen klar abgrenzen
            if (unverifiedRatings.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Nicht verifizierte Einträge (${unverifiedRatings.size}) werden nicht als unabhängige Testergebnisse angezeigt.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Kontaktdaten mit Intents (Telefon, E-Mail, Website). */
@Composable
private fun ContactCard(phone: String?, email: String?, website: String?) {
    val context = LocalContext.current

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Kontakt",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!phone.isNullOrBlank()) {
                ContactRow(icon = { Icon(Icons.Default.Call, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }, label = phone) {
                    try {
                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                    } catch (e: Exception) {
                        // Kein Telefon-Client verfügbar – nichts zu tun
                    }
                }
            }
            if (!email.isNullOrBlank()) {
                ContactRow(icon = { Icon(Icons.Default.Email, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }, label = email) {
                    val intent = Intent(Intent.ACTION_SENDTO).apply {
                        data = Uri.parse("mailto:$email")
                    }
                    try {
                        context.startActivity(intent)
                    } catch (e: ActivityNotFoundException) {
                        // Kein Mail-Client – ignorieren
                    }
                }
            }
            if (!website.isNullOrBlank()) {
                ContactRow(icon = { Icon(Icons.Default.Language, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp)) }, label = website) {
                    val url = if (website.startsWith("http")) website else "https://$website"
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                    } catch (e: ActivityNotFoundException) {
                        // Kein Browser – ignorieren
                    }
                }
            }

            if (phone.isNullOrBlank() && email.isNullOrBlank() && website.isNullOrBlank()) {
                Text(
                    text = "Keine Kontaktdaten hinterlegt.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContactRow(icon: @Composable () -> Unit, label: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            icon()
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

/** Angebotsübersicht der Dienstleistungen inkl. Buchungsart. */
@Composable
private fun ServicesSection(services: List<Service>, onBookClick: () -> Unit) {
    if (services.isEmpty()) return

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Dienstleistungen",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))

            services.forEachIndexed { index, service ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = service.name,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
                        )
                        if (!service.description.isNullOrBlank()) {
                            Text(
                                text = service.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = bookingTypeLabel(service.bookingType) +
                                (service.durationMinutes?.let { " · $it Min." } ?: "") +
                                (service.maxParticipants?.let { " · max. $it Teilnehmer" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = service.price?.let { formatEuro(it) } ?: "Preis auf Anfrage",
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                        )
                        TextButton(onClick = onBookClick) {
                            Text("Buchen")
                        }
                    }
                }
                if (index < services.lastIndex) HorizontalDivider()
            }
        }
    }
}

/** Sonderangebote des Anbieters. */
@Composable
private fun OffersSection(offers: List<Offer>, onOfferClick: (Long) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Sonderangebote",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.weight(1f)
                )
                AssistChip(onClick = {}, label = { Text("Demo-Angebote") })
            }
            Spacer(modifier = Modifier.height(4.dp))

            offers.forEach { offer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = offer.title,
                            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = offer.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Rabatt: ${offer.discountValue}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    TextButton(onClick = { onOfferClick(offer.id) }) {
                        Text("Details")
                    }
                }
            }
        }
    }
}

/** Deutsche Beschriftung der Buchungsart. */
fun bookingTypeLabel(type: BookingType): String = when (type) {
    BookingType.FIXED_DURATION -> "Termin mit fester Dauer"
    BookingType.SEAT_RESERVATION -> "Sitzplatzreservierung"
    BookingType.GROUP_BOOKING -> "Gruppenbuchung"
}

/** Deutsche Preisformatierung: 12.5 -> "12,50 €" */
fun formatEuro(value: Double): String =
    String.format(Locale.GERMAN, "%.2f €", value)

@androidx.compose.ui.tooling.preview.Preview(showBackground = true)
@Composable
private fun ProviderDetailPreview() {
    DealfinderTheme {
        ProviderDetailScreen(
            providerId = 1,
            onBack = {},
            onBookClick = {},
            onOfferClick = {}
        )
    }
}
