package com.dealfinder.ui.screens.bookings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventBusy
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
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
import com.dealfinder.data.models.Booking
import com.dealfinder.viewmodel.BookingDisplayItem
import com.dealfinder.data.models.BookingStatus
import com.dealfinder.ui.theme.DealfinderTheme
import com.dealfinder.viewmodel.BookingViewModel
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Übersicht der eigenen Buchungen mit „Kommend"/„Vergangen"-Filter.
 *
 * Die Listen aus dem ViewModel enthalten rohe Buchungen – die Namen werden
 * über [BookingViewModel.enrichBookings] in eigenen Remember-States
 * aufgelöst, damit Anbieter- und Leistungsnamen in den Karten stehen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyBookingsScreen(
    onBookingClick: (Long) -> Unit,
    onBookAgain: (Long) -> Unit
) {
    val viewModel: BookingViewModel = viewModel()

    val upcomingBookings by viewModel.upcomingBookings.collectAsState()
    val pastBookings by viewModel.pastBookings.collectAsState()

    var showPast by remember { mutableStateOf(false) }
    var upcomingItems by remember { mutableStateOf<List<BookingDisplayItem>?>(null) }
    var pastItems by remember { mutableStateOf<List<BookingDisplayItem>?>(null) }

    // Namen je Liste auflösen (eigener State, damit nichts im Rendering passiert)
    LaunchedEffect(upcomingBookings) {
        upcomingItems = viewModel.enrichBookings(upcomingBookings)
    }
    LaunchedEffect(pastBookings) {
        pastItems = viewModel.enrichBookings(pastBookings)
    }

    val visibleItems = if (showPast) pastItems else upcomingItems

    val dateFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, dd. MMM yyyy", Locale.GERMAN)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Meine Buchungen") }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Demo-Hinweis ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Alle Buchungen sind Demo-Buchungen – nur lokal gespeichert.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- Filter ---
            Row(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = !showPast,
                    onClick = { showPast = false },
                    label = { Text("Kommend (${upcomingBookings.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
                FilterChip(
                    selected = showPast,
                    onClick = { showPast = true },
                    label = { Text("Vergangen (${pastBookings.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                visibleItems == null -> {
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
                            text = "Buchungen werden geladen …",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                visibleItems.isEmpty() -> {
                    EmptyState(
                        isPast = showPast,
                        onBookAgain = null
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(
                            horizontal = 16.dp,
                            vertical = 8.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(visibleItems, key = { it.booking.id }) { item ->
                            BookingCard(
                                item = item,
                                dateFormatter = dateFormatter,
                                onClick = { onBookingClick(item.booking.id) },
                                onRebook = { onBookAgain(item.booking.providerId) }
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Karte einer Buchung inkl. Status, Demo-Badge und Wiederbuchungs-Aktion. */
@Composable
private fun BookingCard(
    item: BookingDisplayItem,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit,
    onRebook: () -> Unit
) {
    val booking = item.booking

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.providerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = item.serviceName ?: "Dienstleistung",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                StatusChip(status = booking.status)
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Event,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = booking.bookingDate
                        .atZone(ZoneId.systemDefault())
                        .toLocalDate()
                        .format(dateFormatter),
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.width(12.dp))
                Icon(
                    imageVector = Icons.Default.AccessTime,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = booking.timeSlot,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (booking.numberOfPeople > 1) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${booking.numberOfPeople} Personen",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                DemoBadge()
                Spacer(modifier = Modifier.weight(1f))
                TextButton(onClick = onRebook) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Erneut buchen",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

/** Status-Chip mit deutscher Beschriftung. */
@Composable
private fun StatusChip(status: BookingStatus) {
    val label: String
    val container = when (status) {
        BookingStatus.CONFIRMED -> {
            label = "Bestätigt"
            MaterialTheme.colorScheme.primaryContainer
        }

        BookingStatus.PENDING -> {
            label = "Ausstehend"
            MaterialTheme.colorScheme.surfaceVariant
        }

        BookingStatus.CANCELLED -> {
            label = "Storniert"
            MaterialTheme.colorScheme.errorContainer
        }

        BookingStatus.COMPLETED -> {
            label = "Abgeschlossen"
            MaterialTheme.colorScheme.tertiaryContainer
        }
    }
    val onContainer = when (status) {
        BookingStatus.CONFIRMED -> MaterialTheme.colorScheme.onPrimaryContainer
        BookingStatus.PENDING -> MaterialTheme.colorScheme.onSurfaceVariant
        BookingStatus.CANCELLED -> MaterialTheme.colorScheme.onErrorContainer
        BookingStatus.COMPLETED -> MaterialTheme.colorScheme.onTertiaryContainer
    }

    AssistChip(
        onClick = {},
        label = { Text(text = label) },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = container,
            labelColor = onContainer
        )
    )
}

/** Kleines „Demo-Buchung"-Badge. */
@Composable
private fun DemoBadge() {
    AssistChip(
        onClick = {},
        label = { Text(text = "Demo-Buchung") },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            labelColor = MaterialTheme.colorScheme.onTertiaryContainer
        )
    )
}

/** Leerezustand je Filter. */
@Composable
private fun EmptyState(isPast: Boolean, onBookAgain: (() -> Unit)?) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = if (isPast) Icons.Default.EventBusy else Icons.Default.Event,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = if (isPast) "Keine vergangenen Buchungen" else "Keine kommenden Buchungen",
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isPast) {
                "Abgeschlossene und stornierte Demo-Buchungen erscheinen hier."
            } else {
                "Sobald Sie einen Demo-Termin buchen, finden Sie ihn hier."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onBookAgain != null) {
            Spacer(modifier = Modifier.height(16.dp))
            TextButton(onClick = onBookAgain) {
                Text(text = "Anbieter entdecken")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MyBookingsScreenPreview() {
    DealfinderTheme {
        MyBookingsScreen(
            onBookingClick = {},
            onBookAgain = {}
        )
    }
}
