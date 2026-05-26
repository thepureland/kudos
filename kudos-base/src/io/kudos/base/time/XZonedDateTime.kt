package io.kudos.base.time

import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/**
 * Defines extension functions for java.time.ZonedDateTime.
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
fun ZonedDateTime.format(pattern: String): String = this.format(DateTimeFormatter.ofPattern(pattern))



/**
 * Returns the Quartz cron expression corresponding to the current time.
 *
 * @return Quartz cron expression
 * @author K
 * @since 1.0.0
 */
fun ZonedDateTime.toCronExp(): String =
    with(this) {
        "$second $minute $hour $dayOfMonth ${month.value} ? $year"
    }

