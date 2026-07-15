package ca.uwaterloo.ece452.discoveruwaterloo.ui.events

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ca.uwaterloo.ece452.discoveruwaterloo.AppViewModel
import ca.uwaterloo.ece452.discoveruwaterloo.data.EventStatus
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EventDetailScreen(
    eventId: Int,
    viewModel: AppViewModel,
    onBack: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    val plannerEvents by viewModel.plannerEvents.collectAsState()
    val event = events.find { it.id == eventId }
    val isInPlanner = plannerEvents.any { it.id == eventId }

    if (event == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Event not found.")
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
        }

        Text(event.name, style = MaterialTheme.typography.headlineMedium)

        // Status chip
        val (statusLabel, statusColor) = when (event.status) {
            EventStatus.APPROVED -> "Approved" to Color(0xFF2E7D32)
            EventStatus.PENDING  -> "Pending Review" to Color(0xFFF57F17)
            EventStatus.REJECTED -> "Rejected" to MaterialTheme.colorScheme.error
        }
        SuggestionChip(
            onClick = {},
            label = { Text(statusLabel, color = Color.White) },
            colors = SuggestionChipDefaults.suggestionChipColors(containerColor = statusColor)
        )

        HorizontalDivider()

        // Location
        if (!event.location.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(event.location, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Date and Time
        if (!event.displayDateTime.isNullOrBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(event.displayDateTime ?: "", style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Duration
        if (event.duration != null) {
            val durationLabel = buildString {
                val h = event.duration / 60
                val m = event.duration % 60
                if (h > 0) append("${h}h ")
                if (m > 0) append("${m}m")
            }.trim()
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    Icons.Default.Schedule,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(durationLabel, style = MaterialTheme.typography.bodyLarge)
            }
        }

        // Description
        if (!event.description.isNullOrBlank()) {
            HorizontalDivider()
            Text(event.description, style = MaterialTheme.typography.bodyLarge)
        }

        // Tags
        if (event.tags.isNotEmpty()) {
            HorizontalDivider()
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                event.tags.forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag.name) })
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (isInPlanner) viewModel.removeFromPlanner(event) else viewModel.addToPlanner(event)
            },
            modifier = Modifier.fillMaxWidth(),
            colors = if (isInPlanner)
                ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
            else
                ButtonDefaults.buttonColors()
        ) {
            Icon(
                imageVector = if (isInPlanner) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(if (isInPlanner) "Remove from Planner" else "Add to Planner")
        }
    }
}
