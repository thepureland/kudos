package io.kudos.base.time

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date


/**
 * Defines extension functions for java.time.LocalDateTime.
 *
 * @author K
 * @since 1.0.0
 */


/**
 * Formats the value.
 *
 * @param pattern Format pattern string; common patterns are available as constants in the DateTimeFormatPattern class
 * @return The formatted string
 * @author K
 * @since 1.0.0
 */
fun LocalDateTime.format(pattern: String): String = this.format(DateTimeFormatter.ofPattern(pattern))

/**
 * Returns the Quartz cron expression corresponding to the current time.
 *
 * @return Quartz cron expression
 * @author K
 * @since 1.0.0
 */
fun LocalDateTime.toCronExp(): String =
    with(this) {
        "$second $minute $hour $dayOfMonth ${month.value} ? $year"
    }

/**
 * Converts to a Date.
 *
 * @param zoneId Time zone ID; defaults to the system default time zone
 * @return Date
 * @author K
 * @since 1.0.0
 */
fun LocalDateTime.toDate(zoneId: ZoneId = ZoneId.systemDefault()): Date = Date.from(this.atZone(zoneId).toInstant())
