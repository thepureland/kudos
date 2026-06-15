package io.kudos.ms.sys.common.datasource.consts

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for SysDataSourceConsts
 *
 * Covers the PASSWORD_MASK constant value.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysDataSourceConstsTest {

    @Test
    fun passwordMask() {
        assertEquals("******", SysDataSourceConsts.PASSWORD_MASK)
    }

}
