package io.kudos.ms.sys.common.platform.consts

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * test for SysConsts
 *
 * Covers the ATOMIC_SERVICE_NAME and DEFAULT_SUB_SYSTEM_CODE constant values.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysConstsTest {

    @Test
    fun constants() {
        assertEquals("sys", SysConsts.ATOMIC_SERVICE_NAME)
        assertEquals("default-sub-system", SysConsts.DEFAULT_SUB_SYSTEM_CODE)
    }

}
