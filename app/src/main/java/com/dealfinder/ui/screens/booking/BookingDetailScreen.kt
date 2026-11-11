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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Replay
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.data.models.Booking
import com.dealfinder.data.models.BookingStatus
import com.dealfinder.ui.theme.DealfinderTheme
import com.dealfinder.viewmodel.BookingViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Detailansicht einer (Demo-)Buchung mit Stornierungs- und Wiederbuchungs-Aktion.
 *
 * Die Anzeige wird suspend aus der lokalen DB geladen (kein DB-Zugriff im
 * Composable selbst, sondern über die ViewModel-API).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDetailScreen(
    bookingId: Long,
    onBack: () -> Unit,
    onCancelBooking: () -> Unit,
    onRebook: (Long) -> Unit
) {
    val viewModel: BookingViewModel = viewModel()

    var booking by remember { mutableStateOf<Booking?>(null) }
    var providerName by remember { mutableStateOf<String?>(null) }
    var serviceName by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf(false) }
    var notFound by remember { mutableStateOf(false) }
    var reloadKey by remember { mutableIntStateOf(0) }

    var showCancelDialog by remember { mutableStateOf(false) }
    var cancelRequested by remember { mutableStateOf(false) }
    var cancelError by remember { mutableStateOf<String?>(null) }

    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(bookingId, reloadKey) {
        loaded = false
        val found = viewModel.getBookingById(bookingId)
        if (found == null) {
            notFound = true
        } else {
            booking = found
            providerName = viewModel.getProviderName(found.providerId)
            serviceName = viewModel.getServiceName(found.serviceId)
            notFound = false
        }
        loaded = true
    }

    // Stornierung beobachten: erst weiter navigieren, wenn sie gespeichert ist.
    LaunchedEffect(cancelRequested, uiState) {
        if (!cancelRequested) return@LaunchedEffect
        when (val state = uiState) {
            is BookingViewModel.BookingUiState.Success -> {
                cancelRequested = false
                reloadKey++
                onCancelBooking()
            }

            is BookingViewModel.BookingUiState.Error -> {
                cancelRequested = false
                cancelError = state.message
            }

            else -> Unit
        }
    }

    // Stornierungs-Dialog
    if (showCancelDialog) {
        AlertDialog(
            onDismissRequest = { showCancelDialog = false },
            title = { Text(text = "Buchung stornieren?") },
            text = {
                Text(
                    text = "Möchten Sie diese Demo-Buchung wirklich stornieren? " +
                        "Die Stornierung wird nur lokal gespeichert und es erfolgt " +
                        "keine Benachrichtigung an den Anbieter."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCancelDialog = false
                        cancelError = null
                        viewModel.cancelBooking(bookingId)
                        cancelRequested = true
                    }
                ) {
                    Text(
                        text = "Stornieren",
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showCancelDialog = false }) {
                    Text(text = "Abbrechen")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Buchungsdetails") },
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
        when {
            !loaded -> {
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
                        text = "Buchung wird geladen …",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            notFound || booking == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
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
                        text = "Die Buchung wurde nicht gefunden.",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onBack) {
                        Text(text = "Zurück")
                    }
                }
            }

            else -> {
                val current = booking!!
                val canCancel = current.status != BookingStatus.CANCELLED &&
                    current.status != BookingStatus.COMPLETED

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- Statuskopf ---
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            StatusChip(
                                status = current.status,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            DemoBadge()
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = serviceName ?: "Dienstleistung",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = providerName ?: "Unbekannter Anbieter",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // --- Termindaten ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        ),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            DetailRow(
                                icon = Icons.Default.CalendarMonth,
                                label = "Datum",
                                value = current.bookingDate.toGermanDate()
                            )
                            DetailRow(
                                icon = Icons.Default.AccessTime,
                                label = "Zeitfenster",
                                value = "${current.timeSlot} (Demo)"
                            )
                            DetailRow(
                                icon = Icons.Default.Person,
                                label = "Personen",
                                value = if (current.numberOfPeople == 1) {
                                    "1 Person"
                                } else {
                                    "${current.numberOfPeople} Personen"
                                }
                            )
                            DetailRow(
                                icon = Icons.Default.Store,
                                label = "Anbieter",
                                value = providerName ?: "Unbekannter Anbieter"
                            )
                            DetailRow(
                                icon = null,
                                label = "Dienstleistung",
                                value = serviceName ?: "–"
                            )

                            if (!current.notes.isNullOrBlank()) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    text = "Notiz",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = current.notes,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                text = "Erstellt am ${current.createdAt.toGermanDateTime()}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Demo-Buchung – gespeichert nur lokal in dieser App. " +
                            "Es besteht keine echte Reservierung beim Anbieter.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        textAlign = TextAlign.Center
                    )

                    if (cancelError != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = cancelError ?: "",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
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
                        if (canCancel) {
                            OutlinedButton(
                                onClick = { showCancelDialog = true },
                                modifier = Modifier.weight(1f),
                                enabled = uiState !is BookingViewModel.BookingUiState.Loading
                            ) {
                                Text(
                                    text = "Stornieren",
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }

                        Button(
                            onClick = { onRebook(current.providerId) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Replay,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(text = "Erneut buchen")
                        }
                    }

                    if (!canCancel) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = when (current.status) {
                                BookingStatus.CANCELLED ->
                                    "Diese Buchung wurde storniert."

                                BookingStatus.COMPLETED ->
                                    "Diese Buchung wurde abgeschlossen und kann nicht " +
                                        "mehr storniert werden."

                                else -> ""
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            textAlign = TextAlign.Center
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}

/** Status-Chip mit deutscher Beschriftung und passenden Farben. */
@Composable
private fun StatusChip(status: BookingStatus, modifier: Modifier = Modifier) {
    val label: String
    val container: Color
    val onContainer: Color

    when (status) {
        BookingStatus.CONFIRMED -> {
            label = "Bestätigt"
            container = MaterialTheme.colorScheme.primaryContainer
            onContainer = MaterialTheme.colorScheme.onPrimaryContainer
        }

        BookingStatus.PENDING -> {
            label = "Ausstehend"
            container = MaterialTheme.colorScheme.surfaceVariant
            onContainer = MaterialTheme.colorScheme.onSurfaceVariant
        }

        BookingStatus.CANCELLED -> {
            label = "Storniert"
            container = MaterialTheme.colorScheme.errorContainer
            onContainer = MaterialTheme.colorScheme.onErrorContainer
        }

        BookingStatus.COMPLETED -> {
            label = "Abgeschlossen"
            container = MaterialTheme.colorScheme.tertiaryContainer
            onContainer = MaterialTheme.colorScheme.onTertiaryContainer
        }
    }

    AssistChip(
        onClick = {},
        label = { Text(text = label) },
        modifier = modifier,
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

/** Zeile einer Detailtabelle. */
@Composable
private fun DetailRow(
    icon: ImageVector?,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(130.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = FontWeight.SemiBold
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

/** Datum deutsch: „Mittwoch, 11. November 2026" */
private fun Instant.toGermanDate(): String =
    atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN))

/** Erstellungsdatum deutsch: „11.11.2026, 14:30" */
private fun Instant.toGermanDateTime(): String =
    atZone(ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm", Locale.GERMAN))

@Preview(showBackground = true)
@Composable
private fun BookingDetailScreenPreview() {
    DealfinderTheme {
        BookingDetailScreen(
            bookingId = 1,
            onBack = {},
            onCancelBooking = {},
            onRebook = {}
        )
    }
}
