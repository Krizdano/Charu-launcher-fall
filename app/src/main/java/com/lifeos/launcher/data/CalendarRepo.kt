package com.lifeos.launcher.data

import android.Manifest
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset

data class CalEvent(
    val id: Long,
    val title: String,
    val begin: Long,
    val end: Long,
    val allDay: Boolean,
    val location: String?,
)

/** Every local date this event touches (all-day events are stored in UTC). */
fun CalEvent.dateRange(zone: ZoneId): List<LocalDate> {
    val z: ZoneId = if (allDay) ZoneOffset.UTC else zone
    val s = Instant.ofEpochMilli(begin).atZone(z).toLocalDate()
    val e = Instant.ofEpochMilli(maxOf(end - 1, begin)).atZone(z).toLocalDate()
    return generateSequence(s) { it.plusDays(1) }.takeWhile { !it.isAfter(e) }.take(62).toList()
}

object CalendarRepo {
    fun hasPermission(c: Context): Boolean =
        ContextCompat.checkSelfPermission(c, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

    fun query(c: Context, fromMs: Long, toMs: Long): List<CalEvent> {
        if (!hasPermission(c)) return emptyList()
        val builder = CalendarContract.Instances.CONTENT_URI.buildUpon()
        ContentUris.appendId(builder, fromMs)
        ContentUris.appendId(builder, toMs)
        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.END,
            CalendarContract.Instances.ALL_DAY,
            CalendarContract.Instances.EVENT_LOCATION,
        )
        val out = mutableListOf<CalEvent>()
        try {
            c.contentResolver.query(
                builder.build(), projection, null, null,
                "${CalendarContract.Instances.BEGIN} ASC",
            )?.use { cur ->
                while (cur.moveToNext()) {
                    out += CalEvent(
                        id = cur.getLong(0),
                        title = cur.getString(1)?.takeIf { it.isNotBlank() } ?: "(No title)",
                        begin = cur.getLong(2),
                        end = cur.getLong(3),
                        allDay = cur.getInt(4) == 1,
                        location = cur.getString(5),
                    )
                }
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun groupByDay(events: List<CalEvent>, zone: ZoneId): Map<LocalDate, List<CalEvent>> {
        val map = HashMap<LocalDate, MutableList<CalEvent>>()
        for (e in events) for (d in e.dateRange(zone)) map.getOrPut(d) { mutableListOf() }.add(e)
        return map
    }

    fun addEventIntent(date: LocalDate, title: String? = null): Intent {
        val start = date.atTime(9, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return Intent(Intent.ACTION_INSERT).apply {
            data = CalendarContract.Events.CONTENT_URI
            putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, start)
            putExtra(CalendarContract.EXTRA_EVENT_END_TIME, start + 3_600_000L)
            if (!title.isNullOrBlank()) putExtra(CalendarContract.Events.TITLE, title)
        }
    }

    fun viewEventIntent(id: Long): Intent =
        Intent(Intent.ACTION_VIEW, ContentUris.withAppendedId(CalendarContract.Events.CONTENT_URI, id))
}
