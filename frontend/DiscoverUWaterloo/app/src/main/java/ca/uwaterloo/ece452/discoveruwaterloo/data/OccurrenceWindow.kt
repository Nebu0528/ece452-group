package ca.uwaterloo.ece452.discoveruwaterloo.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val isoDateTimeFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

fun Event.nextOccurrenceStartCalendar(): Calendar? =
    nextOccurrenceStart?.let { runCatching { isoDateTimeFormat.parse(it) }.getOrNull() }
        ?.let { Calendar.getInstance().apply { time = it } }

// True if this event has a next occurrence and it starts on or before [deadline].
// The backend only ever returns a nextOccurrenceStart whose occurrence hasn't
// already ended, so checking the start against the deadline is sufficient for
// an overlap check against [now, deadline].
fun Event.occursBy(deadline: Calendar): Boolean =
    nextOccurrenceStartCalendar()?.let { !it.after(deadline) } ?: false

// The end of the current Monday-Sunday calendar week (the coming Sunday at 23:59:59).
fun endOfCurrentWeek(from: Calendar = Calendar.getInstance()): Calendar {
    val result = from.clone() as Calendar
    // Calendar.DAY_OF_WEEK: SUNDAY = 1 .. SATURDAY = 7
    val daysUntilSunday = (Calendar.SUNDAY - result.get(Calendar.DAY_OF_WEEK) + 7) % 7
    result.add(Calendar.DAY_OF_YEAR, daysUntilSunday)
    result.set(Calendar.HOUR_OF_DAY, 23)
    result.set(Calendar.MINUTE, 59)
    result.set(Calendar.SECOND, 59)
    result.set(Calendar.MILLISECOND, 999)
    return result
}
