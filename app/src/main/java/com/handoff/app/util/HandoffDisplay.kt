package com.handoff.app.util

import com.handoff.app.data.repo.HandoffEntry
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Display helpers for handoff list rows: title fallback, meta time, date grouping. */
object HandoffDisplay {

    /** Uses the first user message when the saved title is just the platform name. */
    fun titleFor(entry: HandoffEntry): String {
        val saved = entry.title.trim()
        val platformOnly = saved.isEmpty() ||
            saved.equals(entry.platform.displayName, ignoreCase = true) ||
            saved.equals(entry.platform.name, ignoreCase = true)
        val snippet = entry.snippet?.trim().orEmpty()
        return when {
            !platformOnly -> saved
            snippet.isNotEmpty() -> if (snippet.length > 60) snippet.take(60).trimEnd() + "…" else snippet
            else -> entry.platform.displayName
        }
    }

    private val timeFormat = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    fun time(entry: HandoffEntry): String = timeFormat.format(Date(entry.createdAt))

    enum class DayBucket { TODAY, YESTERDAY, EARLIER }

    fun bucket(entry: HandoffEntry, now: Long = System.currentTimeMillis()): DayBucket {
        val days = midnightDaysBetween(entry.createdAt, now)
        return when {
            days <= 0 -> DayBucket.TODAY
            days == 1 -> DayBucket.YESTERDAY
            else -> DayBucket.EARLIER
        }
    }

    private fun midnightDaysBetween(then: Long, now: Long): Int {
        val a = Calendar.getInstance().apply { timeInMillis = then }
        val b = Calendar.getInstance().apply { timeInMillis = now }
        val dayMillis = 24L * 60 * 60 * 1000
        val ay = a.get(Calendar.YEAR)
        val ad = a.get(Calendar.DAY_OF_YEAR)
        val by = b.get(Calendar.YEAR)
        val bd = b.get(Calendar.DAY_OF_YEAR)
        if (ay == by) return bd - ad
        // Spanning years: compare normalized midnight stamps.
        a.set(Calendar.HOUR_OF_DAY, 0); a.set(Calendar.MINUTE, 0)
        a.set(Calendar.SECOND, 0); a.set(Calendar.MILLISECOND, 0)
        b.set(Calendar.HOUR_OF_DAY, 0); b.set(Calendar.MINUTE, 0)
        b.set(Calendar.SECOND, 0); b.set(Calendar.MILLISECOND, 0)
        return ((b.timeInMillis - a.timeInMillis) / dayMillis).toInt()
    }
}
