package io.kudos.base.time

import java.time.LocalTime
import kotlin.test.Test
import kotlin.test.assertEquals

internal class XLocalTimeTest {

    private val localTime = LocalTime.of(17, 15, 1)

    @Test
    fun format() {
        assertEquals("17:15:01", localTime.format(DateTimeFormatPattern.HH_mm_ss))
    }

}