package com.dealfinder.ui.screens.offers

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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.dealfinder.viewmodel.OfferViewModel

/**
 * "Meine Gutscheine" – alle vom Nutzer vorgemerkten Angebote.
 *
 * Die Liste ist reaktiv ([OfferViewModel.mySavedOffers]) und in Room pro
 * Nutzer persistiert; abgelaufene Gutscheine bleiben sichtbar und werden als
 * "Abgelaufen" gekennzeichnet.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyOffersScreen(
    onOfferClick: (Long) -> Unit,
    onBack: () -> Unit
) {
    val offerViewModel: OfferViewModel = viewModel()
    val savedOffers by offerViewModel.mySavedOffers.collectAsState()
    val savedOfferIds by offerViewModel.savedOfferIds.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Meine Gutscheine") },
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
        if (savedOffers.isEmpty()) {
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
                    imageVector = Icons.Default.BookmarkBorder,
                    contentDescription = null,
                    modifier = Modifier.size(56.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Noch keine Gutscheine vorgemerkt",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Finde auf der Startseite unter „Alle Angebote“ zahlreiche " +
                        "Demo-Angebote und tippe auf das Lesezeichen – dann tauchen " +
                        "sie hier automatisch auf.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) {
                    Text("Zurück")
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // --- Hinweis ---
                item {
                    Text(
                        text = "Vorgemerkte Demo-Gutscheine – lokal gespeichert, " +
                            "kein echter Einlösung/kauf.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    )
                }

                items(savedOffers, key = { it.id }) { offer ->
                    OfferListItem(
                        offer = offer,
                        isSaved = offer.id in savedOfferIds,
                        onClick = { onOfferClick(offer.id) },
                        // Bereits vorgemerkt → Toggle entfernt die Vormerkung
                        onToggleSave = { offerViewModel.toggleSaved(offer) }
                    )
                }

                item { Spacer(modifier = Modifier.height(8.dp)) }
            }
        }
    }
}
