package com.dealfinder.ui.screens.booking

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.ui.theme.DealfinderTheme
import com.dealfinder.viewmodel.BookingViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schritt 3 des Buchungsflusses: Auswahl eines Zeitfensters.
 *
 * WICHTIG: Die Fenster sind lokale Demo-Daten – es gibt noch keinen
 * angebundenen Anbieter-Kalender. Der Screen kennzeichnet das oben
 * deutlich, damit nichts als reale Verfügbarkeit missverstanden wird.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingTimeSelectionScreen(
    providerId: Long,
    serviceId: Long,
    date: String,
    onTimeSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: BookingViewModel = viewModel()

    val timeSlots by viewModel.availableTimeSlots.collectAsState()
    val service by viewModel.service.collectAsState()

    var localProviderName by remember { mutableStateOf<String?>(null) }
    var slotsLoaded by remember { mutableStateOf(false) }
    var invalidDate by remember { mutableStateOf(false) }

    val bookingDate = remember(date) {
        runCatching { LocalDate.parse(date) }.getOrNull()
    }

    LaunchedEffect(providerId, serviceId, date) {
        viewModel.selectProvider(providerId)
        viewModel.selectService(serviceId)

        if (bookingDate == null) {
            invalidDate = true
            slotsLoaded = true
        } else {
            invalidDate = false
            viewModel.selectDate(bookingDate)
            localProviderName = viewModel.getProviderName(providerId)
            // Generierung im ViewModel ist asynchron – bis zu [SLOTS_LOAD_TIMEOUT_MS]
            // auf die erste nicht-leere Liste warten, sonst Leerzustand.
            withTimeoutOrNull(SLOTS_LOAD_TIMEOUT_MS) {
                viewModel.availableTimeSlots.first { it.isNotEmpty() }
            }
            slotsLoaded = true
        }
    }

    val fullFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Zeitfenster wählen") },
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
                    text = "Schritt 3 von 4",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = bookingDate?.format(fullFormatter) ?: "Ungültiges Datum",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = listOfNotNull(
                        localProviderName,
                        service?.name ?: "Dienstleistung"
                    ).joinToString(" · "),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- DEUTLICHER DEMO-HINWEIS (oben) ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    AssistChip(
                        onClick = {},
                        label = { Text(text = "Demo-Zeitfenster") },
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
                        text = "Keine reale Verfügbarkeit – es ist noch kein " +
                            "Anbieter-Kalender angebunden. Die angezeigten Zeiten sind " +
                            "lokale Demo-Zeitfenster und keine verfügbaren Termine.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                invalidDate -> {
                    MessageState(
                        icon = Icons.Default.Info,
                        title = "Ungültiges Datum",
                        text = "Das ausgewählte Datum konnte nicht gelesen werden. " +
                            "Bitte gehen Sie zurück und wählen Sie erneut ein Datum."
                    )
                }

                !slotsLoaded -> {
                    MessageState(
                        icon = null,
                        title = "Zeitfenster werden geladen …",
                        text = "Demo-Zeitfenster für den gewählten Tag werden erzeugt."
                    )
                }

                timeSlots.isEmpty() -> {
                    MessageState(
                        icon = Icons.Default.Event,
                        title = "Keine Zeitfenster verfügbar",
                        text = "Für diesen Tag sind keine Demo-Zeitfenster mehr " +
                            "verfügbar (vergangener Tag oder Tageszeit bereits vorbei)."
                    )
                }

                else -> {
                    Text(
                        text = "Verfügbare Demo-Zeitfenster",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold
                        ),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = 104.dp),
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(timeSlots, key = { it }) { slot ->
                            FilterChip(
                                selected = false,
                                onClick = {
                                    viewModel.selectTimeSlot(slot)
                                    onTimeSelected(slot)
                                },
                                label = {
                                    Text(
                                        text = slot,
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        }
                        item {
                            Text(
                                text = "Demo – keine reale Verfügbarkeit.",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Zentrierter Lade-/Leer-/Hinweiszustand. */
@Composable
private fun MessageState(
    icon: androidx.compose.ui.graphics.vector.ImageVector?,
    title: String,
    text: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

/** Zeitrahmen, bis die (möglicherweise leere) Slot-Liste als geladen gilt. */
private const val SLOTS_LOAD_TIMEOUT_MS = 2_000L

@Preview(showBackground = true)
@Composable
private fun BookingTimeSelectionScreenPreview() {
    DealfinderTheme {
        BookingTimeSelectionScreen(
            providerId = 1,
            serviceId = 1,
            date = java.time.LocalDate.now().toString(),
            onTimeSelected = {},
            onBack = {}
        )
    }
}
