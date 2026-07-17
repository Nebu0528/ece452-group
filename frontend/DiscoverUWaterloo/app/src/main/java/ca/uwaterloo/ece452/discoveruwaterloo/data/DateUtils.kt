package ca.uwaterloo.ece452.discoveruwaterloo.data

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private val isoFormat: SimpleDateFormat
    get() = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())

private val displayFormat: SimpleDateFormat
    get() = SimpleDateFormat("MMM d, yyyy · h:mm a", Locale.getDefault())

private val dayLabelFormat: SimpleDateFormat
    get() = SimpleDateFormat("EEE, MMM d, yyyy", Locale.getDefault())

fun Event.startCalendar(): Calendar? =
    startTime?.let { runCatching { isoFormat.parse(it) }.getOrNull() }
        ?.let { Calendar.getInstance().apply { time = it } }

fun Event.formattedStartTime(): String? =
    startTime?.let { runCatching { displayFormat.format(isoFormat.parse(it)!!) }.getOrNull() }

fun Calendar.isSameDay(other: Calendar): Boolean =
    get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
        get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)

fun Calendar.dayLabel(): String = dayLabelFormat.format(time)
