package com.dealfinder.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.dealfinder.ui.theme.DealfinderTheme

/**
 * Category chip for filtering
 */
@Composable
fun CategoryChip(
    categoryName: String,
    icon: String? = null,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                if (icon != null) {
                    Text(
                        text = icon,
                        modifier = Modifier.padding(end = 4.dp)
                    )
                }
                Text(
                    text = categoryName,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        },
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
            labelColor = if (isSelected) {
                MaterialTheme.colorScheme.onPrimaryContainer
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        ),
        border = if (isSelected) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
        } else {
            null
        }
    )
}

/**
 * Rating chip
 */
@Composable
fun RatingChip(
    score: Float,
    reviewCount: Int = 0
) {
    AssistChip(
        onClick = {},
        label = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Icon(
                    imageVector = Icons.Default.Star,
                    contentDescription = "Bewertung",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier
                        .width(14.dp)
                        .padding(end = 2.dp)
                )
                Text(
                    text = String.format("%.1f", score),
                    style = MaterialTheme.typography.labelSmall
                )
                if (reviewCount > 0) {
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "($reviewCount)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            labelColor = MaterialTheme.colorScheme.onSecondaryContainer
        )
    )
}

@Preview(showBackground = true)
@Composable
fun CategoryChipPreview() {
    DealfinderTheme {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CategoryChip(
                categoryName = "Restaurants",
                icon = "🍽️",
                isSelected = true,
                onClick = {}
            )
            CategoryChip(
                categoryName = "Fitness",
                icon = "💪",
                isSelected = false,
                onClick = {}
            )
        }
    }
}
