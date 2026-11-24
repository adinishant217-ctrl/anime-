package com.anistream.app.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun formatClock(positionMs: Long): String {
    val totalSeconds = (positionMs.coerceAtLeast(0L) / 1000L)
    val hours = totalSeconds / 3600L
    val minutes = (totalSeconds % 3600L) / 60L
    val seconds = totalSeconds % 60L
    return if (hours > 0L) {
        String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.US, "%d:%02d", minutes, seconds)
    }
}

fun formatReleaseDate(timestampMs: Long): String =
    SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(timestampMs))

fun formatRelativeTime(timestampMs: Long): String {
    val diff = System.currentTimeMillis() - timestampMs
    if (diff < 0L) return "Just now"
    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)
    return when {
        minutes < 1 -> "Just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        days < 7 -> "${days}d ago"
        else -> formatReleaseDate(timestampMs)
    }
}

fun episodeLabel(number: Int): String = if (number > 0) "Episode $number" else "Episode"

fun formatPlaybackProgress(positionMs: Long, durationMs: Long): String {
    if (durationMs <= 0L) return formatClock(positionMs)
    val remaining = (durationMs - positionMs).coerceAtLeast(0L)
    return "${formatClock(positionMs)} / ${formatClock(durationMs)} · ${formatClock(remaining)} left"
}
