package com.dealfinder.ui.screens.booking

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.EventSeat
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.data.models.BookingType
import com.dealfinder.data.models.Service
import com.dealfinder.ui.theme.DealfinderTheme
import com.dealfinder.viewmodel.BookingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schritt 4 des Buchungsflusses: Zusammenfassung und Demo-Buchung.
 *
 * Der Screen befüllt seine eigene [BookingViewModel]-Instanz aus den Nav-Args
 * und zeigt nach dem Speichern eine große Bestätigungsansicht. Die Buchung ist
 * ausschließlich eine lokale Demo-Buchung.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingConfirmationScreen(
    providerId: Long,
    serviceId: Long,
    date: String,
    timeSlot: String,
    onBookingConfirmed: (Long) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: BookingViewModel = viewModel()

    val providerNameState by viewModel.providerName.collectAsState()
    val service by viewModel.service.collectAsState()
    val numberOfPeople by viewModel.numberOfPeople.collectAsState()
    val notes by viewModel.notes.collectAsState()
    val uiState by viewModel.uiState.collectAsState()

    var localProviderName by remember { mutableStateOf<String?>(null) }
    var serviceLoaded by remember { mutableStateOf(false) }
    var bookingCreatedId by remember { mutableStateOf<Long?>(null) }
    var demoHintChecked by remember { mutableStateOf(false) }
    var snackbarMessage by remember { mutableStateOf<String?>(null) }

    val bookingDate = remember(date) {
        runCatching { LocalDate.parse(date) }.getOrNull()
    }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(providerId, serviceId, date, timeSlot) {
        viewModel.selectProvider(providerId)
        viewModel.selectService(serviceId)
        bookingDate?.let { viewModel.selectDate(it) }
        viewModel.selectTimeSlot(timeSlot)
        localProviderName = viewModel.getProviderName(providerId)
        withTimeoutOrNull(SERVICE_LOAD_TIMEOUT_MS) {
            viewModel.service.first { it != null }
        }
        serviceLoaded = true
    }

    // Ergebnis der Buchung beobachten
    LaunchedEffect(uiState) {
        when (val state = uiState) {
            is BookingViewModel.BookingUiState.Success -> {
                if (bookingCreatedId == null) {
                    bookingCreatedId = state.bookingId
                }
            }

            is BookingViewModel.BookingUiState.Error -> {
                snackbarMessage = state.message
            }

            else -> Unit
        }
    }

    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            snackbarMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Demo-Buchung bestätigen") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Zurück"
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { innerPadding ->
        val createdId = bookingCreatedId
        if (createdId != null) {
            // --- Bestätigungsansicht ---
            SuccessView(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                bookingId = createdId,
                onOpenBookings = { onBookingConfirmed(createdId) },
                onBack = onBack
            )
            return@Scaffold
        }

        when {
            !serviceLoaded -> {
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
                        text = "Zusammenfassung wird geladen …",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            service == null || bookingDate == null -> {
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
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (service == null) {
                            "Die Dienstleistung konnte nicht geladen werden."
                        } else {
                            "Das ausgewählte Datum ist ungültig."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.error
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = onBack) {
                        Text(text = "Zurück")
                    }
                }
            }

            else -> {
                val currentService = service!!
                val isSubmitting = uiState is BookingViewModel.BookingUiState.Loading
                val errorMessage = (uiState as? BookingViewModel.BookingUiState.Error)?.message
                val displayName = providerNameState ?: localProviderName ?: "Anbieter"
                val fullFormatter = remember {
                    DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                ) {
                    // --- Kopf ---
                    Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text(
                            text = "Schritt 4 von 4",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Zusammenfassung",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    }

                    // --- Demo-Hinweis ---
                    DemoNoticeCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // --- Zusammenfassung ---
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
                            SummaryRow(
                                icon = Icons.Default.Person,
                                label = "Anbieter",
                                value = displayName
                            )
                            SummaryRow(
                                icon = bookingTypeIcon(currentService.bookingType),
                                label = "Dienstleistung",
                                value = currentService.name + " · " +
                                    bookingTypeLabel(currentService.bookingType)
                            )
                            SummaryRow(
                                icon = Icons.Default.Event,
                                label = "Datum",
                                value = bookingDate!!.format(fullFormatter)
                            )
                            SummaryRow(
                                icon = Icons.Default.AccessTime,
                                label = "Zeitfenster",
                                value = "$timeSlot (Demo)"
                            )
                            if (currentService.price != null) {
                                SummaryRow(
                                    icon = null,
                                    label = "Preis",
                                    value = formatEuro(currentService.price!!)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Personenanzahl (je nach Buchungsart) ---
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = when (currentService.bookingType) {
                                    BookingType.GROUP_BOOKING -> "Teilnehmer"
                                    BookingType.SEAT_RESERVATION -> "Personen / Sitzplätze"
                                    BookingType.FIXED_DURATION -> "Personen"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                            Spacer(modifier = Modifier.height(4.dp))

                            when (currentService.bookingType) {
                                BookingType.FIXED_DURATION -> {
                                    Text(
                                        text = "1 Person",
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "Termingebunden – dieser Demo-Termin ist für " +
                                            "genau eine Person gebucht.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                else -> {
                                    val maxPeople = currentService.maxParticipants ?: 10
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        IconButton(
                                            onClick = {
                                                viewModel.updateNumberOfPeople(numberOfPeople - 1)
                                            },
                                            enabled = numberOfPeople > 1
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Remove,
                                                contentDescription = "Eine Person weniger"
                                            )
                                        }
                                        Text(
                                            text = numberOfPeople.toString(),
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontWeight = FontWeight.Bold
                                            ),
                                            textAlign = TextAlign.Center,
                                            modifier = Modifier.width(48.dp)
                                        )
                                        IconButton(
                                            onClick = {
                                                viewModel.updateNumberOfPeople(numberOfPeople + 1)
                                            },
                                            enabled = numberOfPeople < maxPeople
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Eine Person mehr"
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (numberOfPeople == 1) "Person" else "Personen",
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Text(
                                        text = (currentService.maxParticipants?.let {
                                            "Max. $it Teilnehmer."
                                        } ?: "Max. $maxPeople Personen.")
                                            + " Personenanzahl und Termin sind Demo-Daten.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Notiz ---
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { viewModel.updateNotes(it) },
                        label = { Text("Notiz (optional)") },
                        placeholder = { Text("Wünsche oder Hinweise zum Termin") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        maxLines = 4
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // --- Demo-Bestätigung ---
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = demoHintChecked,
                            onCheckedChange = { demoHintChecked = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Demo-Buchung – es erfolgt keine echte Reservierung/" +
                                "Bestätigung durch den Anbieter.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            val selectedDate = bookingDate ?: return@Button
                            viewModel.createBooking(
                                providerId,
                                serviceId,
                                selectedDate,
                                timeSlot
                            )
                        },
                        enabled = demoHintChecked && !isSubmitting,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .height(56.dp),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(text = "Demo-Buchung bestätigen")
                        }
                    }

                    Text(
                        text = "Hinweis: Diese Buchung wird nur lokal in der App gespeichert " +
                            "und ist ein Teil des Demos.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/** Große Bestätigungsansicht nach dem Speichern der Demo-Buchung. */
@Composable
private fun SuccessView(
    modifier: Modifier = Modifier,
    bookingId: Long,
    onOpenBookings: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(96.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(56.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Demo-Buchung gespeichert",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Buchungsnummer: $bookingId",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Die Buchung ist nur lokal gespeichert. Es erfolgt keine echte " +
                "Reservierung oder Bestätigung durch den Anbieter.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onOpenBookings,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(text = "Meine Buchungen")
        }

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onBack) {
            Text(text = "Zurück")
        }
    }
}

/** Demo-Hinweiskarte mit Badge. */
@Composable
private fun DemoNoticeCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            AssistChip(
                onClick = {},
                label = { Text(text = "Demo-Buchung") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                },
                colors = AssistChipDefaults.assistChipColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    labelColor = MaterialTheme.colorScheme.onSurface
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Diese Buchung ist eine Demo-Buchung. Es erfolgt keine echte " +
                    "Reservierung und keine Bestätigung durch den Anbieter – die " +
                    "Daten bleiben ausschließlich lokal in der App.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/** Einzelne Zeile der Zusammenfassung. */
@Composable
private fun SummaryRow(
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
            modifier = Modifier.width(120.dp)
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

/** Deutsche Preisformatierung: 12.5 -> "12,50 €" */
private fun formatEuro(value: Double): String =
    String.format(Locale.GERMAN, "%.2f €", value)

/** Zeitrahmen, bis die Dienstleistung als (nicht) geladen gilt. */
private const val SERVICE_LOAD_TIMEOUT_MS = 2_000L

@Preview(showBackground = true)
@Composable
private fun BookingConfirmationScreenPreview() {
    DealfinderTheme {
        BookingConfirmationScreen(
            providerId = 1,
            serviceId = 1,
            date = java.time.LocalDate.now().toString(),
            timeSlot = "09:00 - 10:00",
            onBookingConfirmed = {},
            onBack = {}
        )
    }
}
