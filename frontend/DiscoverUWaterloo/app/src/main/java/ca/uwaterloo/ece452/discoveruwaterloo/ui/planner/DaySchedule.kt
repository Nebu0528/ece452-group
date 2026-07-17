package ca.uwaterloo.ece452.discoveruwaterloo.ui.planner

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ca.uwaterloo.ece452.discoveruwaterloo.data.Event
import ca.uwaterloo.ece452.discoveruwaterloo.data.formattedStartTime
import ca.uwaterloo.ece452.discoveruwaterloo.data.startCalendar
import java.util.Calendar

private val HOUR_HEIGHT = 64.dp
private val MIN_BLOCK_HEIGHT = 32.dp
private val HOUR_LABEL_WIDTH = 56.dp
private val VIEWPORT_HEIGHT = 480.dp
private const val MINUTES_PER_DAY = 24 * 60
private const val DEFAULT_DURATION_MINUTES = 30

private data class PositionedEvent(
    val event: Event,
    val top: Dp,
    val height: Dp,
    val columnIndex: Int,
    val columnCount: Int
)

private data class TimedEvent(val event: Event, val startMinute: Int, val endMinute: Int)

private fun hourLabel(hour: Int): String = when {
    hour == 0 -> "12 AM"
    hour < 12 -> "$hour AM"
    hour == 12 -> "12 PM"
    else -> "${hour - 12} PM"
}

// Sorts events by start time, groups transitively-overlapping events into clusters, then greedily
// packs each cluster into the fewest columns (reusing a column once its previous event has ended).
private fun layoutEvents(events: List<Event>): List<PositionedEvent> {
    val pixelsPerMinute = HOUR_HEIGHT / 60
    val totalHeight = pixelsPerMinute * MINUTES_PER_DAY

    val timedEvents = events.mapNotNull { event ->
        val cal = event.startCalendar() ?: return@mapNotNull null
        val startMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)
        val duration = event.duration?.takeIf { it > 0 } ?: DEFAULT_DURATION_MINUTES
        val endMinute = (startMinute + duration).coerceAtMost(MINUTES_PER_DAY)
        TimedEvent(event, startMinute, endMinute)
    }.sortedBy { it.startMinute }

    val positioned = mutableListOf<PositionedEvent>()
    var i = 0
    while (i < timedEvents.size) {
        var clusterEnd = timedEvents[i].endMinute
        var j = i + 1
        while (j < timedEvents.size && timedEvents[j].startMinute < clusterEnd) {
            clusterEnd = maxOf(clusterEnd, timedEvents[j].endMinute)
            j++
        }

        val columnEndTimes = mutableListOf<Int>()
        val columnAssignments = mutableListOf<Pair<TimedEvent, Int>>()
        for (timedEvent in timedEvents.subList(i, j)) {
            val freeColumn = columnEndTimes.indexOfFirst { it <= timedEvent.startMinute }
            val column = if (freeColumn >= 0) {
                columnEndTimes[freeColumn] = timedEvent.endMinute
                freeColumn
            } else {
                columnEndTimes.add(timedEvent.endMinute)
                columnEndTimes.lastIndex
            }
            columnAssignments.add(timedEvent to column)
        }

        val columnCount = columnEndTimes.size
        for ((timedEvent, column) in columnAssignments) {
            val top = pixelsPerMinute * timedEvent.startMinute
            val rawHeight = maxOf(pixelsPerMinute * (timedEvent.endMinute - timedEvent.startMinute), MIN_BLOCK_HEIGHT)
            val height = if (top + rawHeight > totalHeight) totalHeight - top else rawHeight
            positioned.add(PositionedEvent(timedEvent.event, top, height, column, columnCount))
        }
        i = j
    }
    return positioned
}

@Composable
fun DaySchedule(events: List<Event>, onEventClick: (Int) -> Unit) {
    val positioned = remember(events) { layoutEvents(events) }
    val totalHeight = HOUR_HEIGHT * 24

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(VIEWPORT_HEIGHT)
            .verticalScroll(rememberScrollState())
    ) {
        Column(modifier = Modifier.width(HOUR_LABEL_WIDTH)) {
            for (hour in 0 until 24) {
                Box(modifier = Modifier.height(HOUR_HEIGHT).fillMaxWidth()) {
                    Text(
                        text = hourLabel(hour),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(end = 4.dp, top = 2.dp)
                    )
                }
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .height(totalHeight)
        ) {
            for (hour in 0 until 24) {
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = HOUR_HEIGHT * hour)
                )
            }

            positioned.forEach { positionedEvent ->
                val columnWidth = maxWidth / positionedEvent.columnCount
                ScheduleEventBlock(
                    event = positionedEvent.event,
                    onClick = { onEventClick(positionedEvent.event.id) },
                    modifier = Modifier
                        .offset(x = columnWidth * positionedEvent.columnIndex, y = positionedEvent.top)
                        .width(columnWidth)
                        .height(positionedEvent.height)
                        .padding(1.dp)
                )
            }
        }
    }
}

@Composable
private fun ScheduleEventBlock(event: Event, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)) {
            Text(
                event.name,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            event.formattedStartTime()?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            if (!event.location.isNullOrBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        event.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
