package io.kudos.base.time

import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals

internal class XLocalDateTest {

    @Test
    fun format() {
        val localDate = LocalDate.of(2021, 9, 10)
        assertEquals("2021-09-10", localDate.format(DateTimeFormatPattern.yyyy_MM_dd))
    }

}