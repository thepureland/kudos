package io.kudos.base.time

import java.time.LocalTime
import java.time.format.DateTimeFormatter


/**
 * Defines extension functions for java.time.LocalTime.
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
fun LocalTime.format(pattern: String): String = this.format(DateTimeFormatter.ofPattern(pattern))