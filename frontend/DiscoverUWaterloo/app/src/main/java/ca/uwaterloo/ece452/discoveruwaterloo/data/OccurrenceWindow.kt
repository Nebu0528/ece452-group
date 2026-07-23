package ca.uwaterloo.ece452.discoveruwaterloo.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val isoDateTimeFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

private val cronDateFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

private val nextOccurrenceDisplayFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())

private val timeOnlyDisplayFormat: SimpleDateFormat
    get() = SimpleDateFormat("h:mm a", Locale.getDefault())

private fun atStartOfDay(calendar: Calendar): Calendar =
    (calendar.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

// Whether this event occurs on [date], and if so, the (start, end) of that specific
// occurrence. Only understands the 3 cron shapes this app itself generates via
// cronForRepeatOption (daily/weekly/monthly wildcards) plus one-time (null schedule) —
// not a general cron parser, since those are the only shapes ever written.
fun Event.occurrenceOn(date: Calendar): Pair<Calendar, Calendar>? {
    val anchor = startCalendar() ?: return null
    val durationMinutes = duration ?: return null
    val dayStart = atStartOfDay(date)
    if (dayStart.before(atStartOfDay(anchor))) return null

    val minute: Int
    val hour: Int

    if (schedule == null) {
        if (!anchor.isSameDay(date)) return null
        minute = anchor.get(Calendar.MINUTE)
        hour = anchor.get(Calendar.HOUR_OF_DAY)
    } else {
        val fields = schedule.trim().split(Regex("\\s+"))
        if (fields.size != 5) return null
        minute = fields[0].toIntOrNull() ?: return null
        hour = fields[1].toIntOrNull() ?: return null
        val dayOfMonth = fields[2]
        val dayOfWeek = fields[4]

        frequencyEnd?.let { fe ->
            val feCal = runCatching { cronDateFormat.parse(fe) }.getOrNull()
                ?.let { Calendar.getInstance().apply { time = it } }
                ?: return null
            if (dayStart.after(atStartOfDay(feCal))) return null
        }

        val matchesDay = when {
            dayOfMonth != "*" -> date.get(Calendar.DAY_OF_MONTH) == dayOfMonth.toIntOrNull()
            dayOfWeek != "*" -> cronDayOfWeek(date) == dayOfWeek.toIntOrNull()
            else -> true
        }
        if (!matchesDay) return null
    }

    val occurrenceStart = (date.clone() as Calendar).apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val occurrenceEnd = (occurrenceStart.clone() as Calendar).apply { add(Calendar.MINUTE, durationMinutes) }
    return occurrenceStart to occurrenceEnd
}

// "Happening now" if [now] falls within (start, end); otherwise the formatted start time.
// [fullDate] controls whether the date is spelled out (for lists spanning multiple days)
// or just the time-of-day (for a single day's schedule).
fun occurrenceDisplay(start: Calendar, end: Calendar, fullDate: Boolean, now: Calendar = Calendar.getInstance()): String {
    if (!now.before(start) && !now.after(end)) return "Happening now"
    val format = if (fullDate) nextOccurrenceDisplayFormat else timeOnlyDisplayFormat
    return format.format(start.time)
}

fun Event.nextOccurrenceStartCalendar(): Calendar? =
    nextOccurrenceStart?.let { runCatching { isoDateTimeFormat.parse(it) }.getOrNull() }
        ?.let { Calendar.getInstance().apply { time = it } }

fun Event.nextOccurrenceEndCalendar(): Calendar? =
    nextOccurrenceEnd?.let { runCatching { isoDateTimeFormat.parse(it) }.getOrNull() }
        ?.let { Calendar.getInstance().apply { time = it } }

// True if [now] falls within this event's next occurrence window.
fun Event.isHappeningNow(now: Calendar = Calendar.getInstance()): Boolean {
    val start = nextOccurrenceStartCalendar() ?: return false
    val end = nextOccurrenceEndCalendar() ?: return false
    return !now.before(start) && !now.after(end)
}

// "Happening now" while the event's next occurrence is in progress; otherwise
// the formatted next occurrence start. Falls back to the legacy displayDateTime
// (based on the original start_time) if there's no next occurrence at all.
fun Event.nextOccurrenceDisplay(): String? {
    if (isHappeningNow()) return "Happening now"
    val start = nextOccurrenceStartCalendar() ?: return displayDateTime
    return nextOccurrenceDisplayFormat.format(start.time)
}

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
