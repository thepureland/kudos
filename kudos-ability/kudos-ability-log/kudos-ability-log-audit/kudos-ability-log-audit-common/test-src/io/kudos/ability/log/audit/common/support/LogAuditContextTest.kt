package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.entity.LogVo
import kotlin.concurrent.thread
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * ThreadLocal semantics tests for [LogAuditContext].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class LogAuditContextTest {

    @AfterTest
    fun tearDown() {
        LogAuditContext.clear()
    }

    @Test
    fun getOrNullDoesNotCreateContext() {
        LogAuditContext.clear()

        assertNull(LogAuditContext.getOrNull())
    }

    @Test
    fun getCreatesContextWhenMissing() {
        LogAuditContext.clear()

        val logVo = LogAuditContext.get()

        assertNotNull(logVo)
        assertSame(logVo, LogAuditContext.getOrNull())
    }

    @Test
    fun setAndClearControlCurrentThreadContext() {
        val logVo = LogVo()

        LogAuditContext.set(logVo)
        assertSame(logVo, LogAuditContext.getOrNull())

        LogAuditContext.clear()
        assertNull(LogAuditContext.getOrNull())
    }

    @Test
    fun childThreadDoesNotInheritParentContext() {
        LogAuditContext.set(LogVo())

        var childContext: LogVo? = null
        val child = thread(start = true) {
            childContext = LogAuditContext.getOrNull()
        }
        child.join()

        assertNull(childContext)
        assertNotNull(LogAuditContext.getOrNull())
    }

}
