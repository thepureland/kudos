package io.kudos.base.time

import java.time.*
import java.util.Date


/**
 * Defines extension functions for java.util.Date.
 *
 * @author K
 * @since 1.0.0
 */

/**
 * Converts to a LocalDateTime.
 *
 * @param zoneId Time zone ID; defaults to the system default time zone
 * @return LocalDateTime
 * @author K
 * @since 1.0.0
 */
fun Date.toLocalDateTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalDateTime =
    toZonedDateTime(this, zoneId).toLocalDateTime()

/**
 * Converts to a LocalDate.
 *
 * @param zoneId Time zone ID; defaults to the system default time zone
 * @return LocalDate
 * @author K
 * @since 1.0.0
 */
fun Date.toLocalDate(zoneId: ZoneId = ZoneId.systemDefault()): LocalDate = toZonedDateTime(this, zoneId).toLocalDate()

/**
 * Converts to a LocalTime.
 *
 * @param zoneId Time zone ID; defaults to the system default time zone
 * @return LocalTime
 * @author K
 * @since 1.0.0
 */
fun Date.toLocalTime(zoneId: ZoneId = ZoneId.systemDefault()): LocalTime = toZonedDateTime(this, zoneId).toLocalTime()

private fun toZonedDateTime(date: Date, zoneId: ZoneId): ZonedDateTime {
    val instant = date.toInstant()
    return instant.atZone(zoneId)
}