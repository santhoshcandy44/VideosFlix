package com.flix.videos.ui.utils

import java.text.CharacterIterator
import java.text.StringCharacterIterator
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

object FormatterUtils {
    fun formatTimeSeconds(seconds: Float): String {
        // Ensure seconds is non-negative to avoid negative values for hours, minutes, and seconds
        val positiveSeconds = max(seconds, 0f)

        // Calculate hours, minutes, and seconds based on the total seconds
        val hours = (positiveSeconds / 3600).toInt() // 3600 seconds in an hour
        val minutes = ((positiveSeconds % 3600) / 60).toInt() // Remaining minutes
        val remainingSeconds = (positiveSeconds % 60).toInt() // Remaining seconds

        // If hours > 0, show hours, minutes, and seconds in hh:mm:ss format
        return if (hours > 0) {
            String.format(Locale.ROOT, "%02d:%02d:%02d", hours, minutes, remainingSeconds)
        } else {
            // If hours == 0, show only minutes and seconds in mm:ss format
            String.format(Locale.ROOT, "%02d:%02d", minutes, remainingSeconds)
        }
    }

    fun formatHumanReadableBytesSize(bytes: Long): String {
        val absB = if (bytes == Long.MIN_VALUE) Long.MAX_VALUE else abs(bytes)
        if (absB < 1024) {
            return "${bytes}B"
        }
        var value = absB
        val ci: CharacterIterator = StringCharacterIterator("KMGTPE")
        var i = 40
        while (i >= 0 && absB > 0xfffccccccccccccL shr i) {
            value = value shr 10
            ci.next()
            i -= 10
        }
        value *= java.lang.Long.signum(bytes).toLong()
        return String.format(
            Locale.Builder()
                .setLanguage("en")
                .setRegion("IN")
                .build(),
            "%.2f %cB",
            value / 1024.0,
            ci.current()
        )
    }
}