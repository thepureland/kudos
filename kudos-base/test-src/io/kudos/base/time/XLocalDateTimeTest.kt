package io.kudos.base.time

import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for XLocalDateTime.kt
 *
 * @author K
 * @since 1.0.0
 */
internal class XLocalDateTimeTest {

    private val localDateTime = LocalDateTime.of(2021, 9, 10, 17, 15, 1)

    @Test
    fun format() {
        assertEquals("2021-09-10 17:15:01", localDateTime.format(DateTimeFormatPattern.yyyy_MM_dd_HH_mm_ss))
    }

    @Test
    fun toCronExp() {
        assertEquals("1 15 17 10 9 ? 2021", localDateTime.toCronExp())
    }

    @Test
    fun toDate() {
        val date = localDateTime.toDate()
        assertEquals(localDateTime.toInstant(OffsetDateTime.now(ZoneId.systemDefault()).offset), date.toInstant())
    }

}