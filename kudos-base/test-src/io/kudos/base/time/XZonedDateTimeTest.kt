package io.kudos.base.time

import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for XZonedDateTime.kt
 *
 * @author K
 * @since 1.0.0
 */
internal class XZonedDateTimeTest {

    private val zonedDateTime = ZonedDateTime.of(2021, 9, 10, 17, 15, 1, 0, ZoneId.systemDefault())

    @Test
    fun format() {
        assertEquals("2021-09-10 17:15:01", zonedDateTime.format(DateTimeFormatPattern.yyyy_MM_dd_HH_mm_ss))
    }

    @Test
    fun toCronExp() {
        assertEquals("1 15 17 10 9 ? 2021", zonedDateTime.toCronExp())
    }

}