package io.kudos.base.time

import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Defines extension functions for java.time.LocalDate.
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
fun LocalDate.format(pattern: String): String = this.format(DateTimeFormatter.ofPattern(pattern))

