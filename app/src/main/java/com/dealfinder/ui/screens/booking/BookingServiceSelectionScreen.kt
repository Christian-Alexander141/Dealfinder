package com.dealfinder.ui.screens.booking

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.data.models.BookingType
import com.dealfinder.data.models.Service
import com.dealfinder.ui.theme.DealfinderTheme
import com.dealfinder.viewmodel.BookingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale

/**
 * Schritt 1 des Buchungsflusses: Auswahl einer Dienstleistung des Anbieters.
 *
 * Der Screen besitzt eine eigene [BookingViewModel]-Instanz und befüllt sie
 * über [BookingViewModel.selectProvider] aus den Nav-Args.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingServiceSelectionScreen(
    providerId: Long,
    onServiceSelected: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: BookingViewModel = viewModel()

    val services by viewModel.services.collectAsState()
    val providerNameState by viewModel.providerName.collectAsState()

    var localProviderName by remember { mutableStateOf<String?>(null) }
    var servicesLoaded by remember { mutableStateOf(false) }

    LaunchedEffect(providerId) {
        viewModel.selectProvider(providerId)
        // Fallback, solange der StateFlow providerName nicht befüllt ist
        localProviderName = viewModel.getProviderName(providerId)
        // Room-Flow meldet sich asynchron – bei leerer Liste endet das Warten
        // nach kurzer Zeit im Leerzustand.
        withTimeoutOrNull(SERVICES_LOAD_TIMEOUT_MS) {
            viewModel.services.first { it.isNotEmpty() }
        }
        servicesLoaded = true
    }

    val displayName = providerNameState ?: localProviderName

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Dienstleistung wählen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
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
            // --- Kopfbereich ---
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                Text(
                    text = "Schritt 1 von 4",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayName ?: "Anbieter",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Wählen Sie die gewünschte Dienstleistung aus.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            when {
                !servicesLoaded -> {
                    LoadingState(text = "Dienstleistungen werden geladen …")
                }

                services.isEmpty() -> {
                    EmptyState(
                        text = "Für diesen Anbieter sind keine Dienstleistungen hinterlegt.",
                        onBack = onBack
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(services, key = { it.id }) { service ->
                            ServiceCard(
                                service = service,
                                onClick = { onServiceSelected(service.id) }
                            )
                        }
                        item {
                            Text(
                                text = "Demo-Prototyp – fiktive Anbieter- und Dienstleistungsdaten.",
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
    }
}

/** Klickbare Karte einer Dienstleistung inkl. Preis, Dauer und Buchungsart. */
@Composable
private fun ServiceCard(
    service: Service,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = service.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = service.price?.let { formatEuro(it) } ?: "Preis auf Anfrage",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!service.description.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = service.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Buchungsart mit eigenem Icon und Erklärtext
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = bookingTypeIcon(service.bookingType),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = bookingTypeLabel(service.bookingType),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = bookingTypeHint(service.bookingType) +
                            (service.maxParticipants?.let { " Max. $it Teilnehmer." } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Schedule,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = service.durationMinutes?.let { "$it Min." }
                        ?: "Dauer nicht angegeben",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LoadingState(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(text: String, onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Info,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onBack) {
            Text(text = "Zurück")
        }
    }
}

/** Eigenes Icon je Buchungsart. */
private fun bookingTypeIcon(type: BookingType): ImageVector = when (type) {
    BookingType.FIXED_DURATION -> Icons.Default.AccessTime
    BookingType.SEAT_RESERVATION -> Icons.Default.EventSeat
    BookingType.GROUP_BOOKING -> Icons.Default.Group
}

/** Deutsche Beschriftung der Buchungsart. */
private fun bookingTypeLabel(type: BookingType): String = when (type) {
    BookingType.FIXED_DURATION -> "Termin mit fester Dauer"
    BookingType.SEAT_RESERVATION -> "Sitzplatzreservierung"
    BookingType.GROUP_BOOKING -> "Gruppenbuchung"
}

/** Kurzer Erklärtext zur Buchungsart. */
private fun bookingTypeHint(type: BookingType): String = when (type) {
    BookingType.FIXED_DURATION -> "Zeitgebundener Termin – die Dauer ist festgelegt."
    BookingType.SEAT_RESERVATION -> "Reservierter Sitzplatz zur gewählten Zeit."
    BookingType.GROUP_BOOKING -> "Buchung für mehrere Personen in einer Gruppe."
}

/** Deutsche Preisformatierung: 12.5 -> "12,50 €" */
private fun formatEuro(value: Double): String =
    String.format(Locale.GERMAN, "%.2f €", value)

/** Zeitrahmen, bis eine (möglicherweise leere) Service-Liste als geladen gilt. */
private const val SERVICES_LOAD_TIMEOUT_MS = 2_000L

@Preview(showBackground = true)
@Composable
private fun BookingServiceSelectionScreenPreview() {
    DealfinderTheme {
        BookingServiceSelectionScreen(
            providerId = 1,
            onServiceSelected = {},
            onBack = {}
        )
    }
}
