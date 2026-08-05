package io.kudos.ability.log.audit.common.entity

import java.io.Serializable
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [SysMonitorMsgVo].
 *
 * Coverage:
 *  - All properties default to null and round-trip through setters/getters (including [SysMonitorMsgVo.environment],
 *    which the builder in MonitorMsgTool never populates — admin-side callers set it manually).
 *  - The VO is [Serializable] for MQ delivery.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysMonitorMsgVoTest {

    @Test
    fun allProperties_defaultToNull() {
        val vo = SysMonitorMsgVo()
        assertNull(vo.tenantId)
        assertNull(vo.exceptionType)
        assertNull(vo.applicationName)
        assertNull(vo.exceptionMsg)
        assertNull(vo.environment)
        assertNull(vo.callSource)
        assertNull(vo.createTime)
    }

    @Test
    fun allProperties_roundTrip() {
        val time = Date(0)
        val vo = SysMonitorMsgVo().apply {
            tenantId = "t-1"
            exceptionType = "PROVIDER_TIMEOUT"
            applicationName = "user-service"
            exceptionMsg = "boom\n\tat somewhere"
            environment = "prod"
            callSource = "(io.kudos.X, doWork)"
            createTime = time
        }

        assertEquals("t-1", vo.tenantId)
        assertEquals("PROVIDER_TIMEOUT", vo.exceptionType)
        assertEquals("user-service", vo.applicationName)
        assertEquals("boom\n\tat somewhere", vo.exceptionMsg)
        assertEquals("prod", vo.environment)
        assertEquals("(io.kudos.X, doWork)", vo.callSource)
        assertEquals(time, vo.createTime)
        assertTrue(vo is Serializable)
    }
}
