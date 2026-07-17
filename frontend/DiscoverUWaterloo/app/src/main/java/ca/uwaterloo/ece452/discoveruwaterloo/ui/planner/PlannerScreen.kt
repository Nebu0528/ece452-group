package ca.uwaterloo.ece452.discoveruwaterloo.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ca.uwaterloo.ece452.discoveruwaterloo.AppViewModel
import ca.uwaterloo.ece452.discoveruwaterloo.data.Event
import ca.uwaterloo.ece452.discoveruwaterloo.data.dayLabel
import ca.uwaterloo.ece452.discoveruwaterloo.data.formattedStartTime
import java.util.Calendar
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlannerScreen(
    viewModel: AppViewModel,
    onEventClick: (Int) -> Unit
) {
    val plannerEvents by viewModel.plannerEvents.collectAsState()
    val selectedDate by viewModel.selectedPlannerDate.collectAsState()
    val dayEvents by viewModel.plannerEventsForSelectedDay.collectAsState()

    if (plannerEvents.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.CalendarToday,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(12.dp))
                Text("No events saved yet.", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Browse events and tap 'Add to Planner'.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            DaySelector(
                selectedDate = selectedDate,
                onPrevDay = { viewModel.stepPlannerDate(-1) },
                onNextDay = { viewModel.stepPlannerDate(1) },
                onDatePicked = { viewModel.setPlannerDate(it) }
            )
        }
        item {
            Text(
                "Events on ${selectedDate.dayLabel()}",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
        item {
            if (dayEvents.isEmpty()) {
                Text(
                    "No planner events on this day.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                DaySchedule(events = dayEvents, onEventClick = onEventClick)
            }
        }
        item {
            Text(
                "My Planner (${plannerEvents.size})",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
            )
        }
        items(plannerEvents, key = { it.id }) { event ->
            PlannerEventCard(
                event = event,
                onClick = { onEventClick(event.id) },
                onRemove = { viewModel.removeFromPlanner(event) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaySelector(
    selectedDate: Calendar,
    onPrevDay: () -> Unit,
    onNextDay: () -> Unit,
    onDatePicked: (Calendar) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = selectedDate.timeInMillis
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    showDatePicker = false
                    val millis = datePickerState.selectedDateMillis
                    if (millis != null) {
                        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
                        utcCal.timeInMillis = millis
                        val picked = Calendar.getInstance().apply {
                            set(
                                utcCal.get(Calendar.YEAR),
                                utcCal.get(Calendar.MONTH),
                                utcCal.get(Calendar.DAY_OF_MONTH)
                            )
                        }
                        onDatePicked(picked)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onPrevDay) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous day")
        }
        TextButton(onClick = { showDatePicker = true }) {
            Text(selectedDate.dayLabel(), style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onNextDay) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun PlannerEventCard(
    event: Event,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(event.name, style = MaterialTheme.typography.titleMedium)

                if (!event.location.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            event.location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                event.formattedStartTime()?.let { formatted ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            formatted,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Default.DeleteOutline,
                    contentDescription = "Remove from planner",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
