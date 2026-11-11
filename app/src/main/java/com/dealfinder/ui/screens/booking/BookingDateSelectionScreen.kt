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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Schritt 2 des Buchungsflusses: Auswahl eines Datums (nächste 14 Tage).
 *
 * Vergangene Tage werden nicht angeboten – die Liste beginnt immer bei heute.
 * Das Ergebnis wird im ISO-Format (yyyy-MM-dd) an [onDateSelected] übergeben.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookingDateSelectionScreen(
    providerId: Long,
    serviceId: Long,
    onDateSelected: (String) -> Unit,
    onBack: () -> Unit
) {
    val viewModel: BookingViewModel = viewModel()

    val service by viewModel.service.collectAsState()

    var localProviderName by remember { mutableStateOf<String?>(null) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }

    LaunchedEffect(providerId, serviceId) {
        viewModel.selectProvider(providerId)
        viewModel.selectService(serviceId)
        localProviderName = viewModel.getProviderName(providerId)
    }

    // Nächste 14 Tage ab heute – vergangene Tage sind gar nicht erst enthalten.
    val dates = remember {
        (0 until DATE_RANGE_DAYS).map { offset -> LocalDate.now().plusDays(offset.toLong()) }
    }
    val shortFormatter = remember {
        DateTimeFormatter.ofPattern("EEE, dd. MMM", Locale.GERMAN)
    }
    val fullFormatter = remember {
        DateTimeFormatter.ofPattern("EEEE, dd. MMMM yyyy", Locale.GERMAN)
    }

    val contextLabel = listOfNotNull(
        localProviderName,
        service?.name ?: "Dienstleistung"
    ).joinToString(" · ")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Datum wählen") },
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
                    text = "Schritt 2 von 4",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Datum wählen",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontWeight = FontWeight.Bold
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = contextLabel,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Wählen Sie ein Datum aus den nächsten $DATE_RANGE_DAYS Tagen. " +
                        "Vergangene Tage sind nicht wählbar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // --- Datumsauswahl ---
            if (localProviderName == null && service == null) {
                // Kurzer Ladezustand, bis die Kontextdaten stehen (Liste bleibt nutzbar)
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Ausgewählter Termin wird vorbereitet …",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(dates, key = { it.toString() }) { date ->
                    val isSelected = date == selectedDate
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedDate = date },
                        label = {
                            Text(
                                text = shortFormatter.format(date),
                                style = MaterialTheme.typography.labelMedium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            labelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Gewähltes Datum ---
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (selectedDate != null) {
                        MaterialTheme.colorScheme.secondaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = null,
                        tint = if (selectedDate != null) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(12.dp))
                    Column {
                        Text(
                            text = "Gewähltes Datum",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (selectedDate != null) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                        Text(
                            text = selectedDate?.format(fullFormatter) ?: "Noch kein Datum gewählt",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (selectedDate != null) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    val date = selectedDate ?: return@Button
                    onDateSelected(date.toString())
                },
                enabled = selectedDate != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(56.dp),
                shape = MaterialTheme.shapes.medium
            ) {
                Text(text = "Weiter zu den Zeitfenstern")
            }

            Text(
                text = "Demo-Prototyp – die Terminvergabe erfolgt über lokale Demodaten.",
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

/** Anzahl der angebotenen Tage (ab heute). */
private const val DATE_RANGE_DAYS = 14

@Preview(showBackground = true)
@Composable
private fun BookingDateSelectionScreenPreview() {
    DealfinderTheme {
        BookingDateSelectionScreen(
            providerId = 1,
            serviceId = 1,
            onDateSelected = {},
            onBack = {}
        )
    }
}
