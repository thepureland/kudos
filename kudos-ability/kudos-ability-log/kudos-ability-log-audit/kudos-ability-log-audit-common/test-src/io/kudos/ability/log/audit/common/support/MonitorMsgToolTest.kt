package io.kudos.ability.log.audit.common.support

import io.kudos.ability.log.audit.common.api.IMonitorService
import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo
import io.kudos.context.core.KudosContextHolder
import io.kudos.context.kit.SpringKit
import org.springframework.context.support.StaticApplicationContext
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [MonitorMsgTool].
 *
 * Coverage:
 *  - [MonitorMsgTool.pushErrMsg] builds the VO and submits it through the [IMonitorService] looked
 *    up from Spring (a recording fake registered into a [StaticApplicationContext]).
 *  - [MonitorMsgTool.createSysMonitorMsgVo] builds without submitting; tenant/application are taken
 *    from [KudosContextHolder]; createTime is stamped.
 *  - Call-source capture via StackWalker points at the real caller (this test class).
 *  - Stack-trace summary: truncated to 10 frames for deep stacks; tolerates a zero-frame throwable;
 *    no stack section at all when throwable is null.
 *
 * @author K
 * @since 1.0.0
 */
internal class MonitorMsgToolTest {

    private lateinit var ctx: StaticApplicationContext
    private val recorder = RecordingMonitorService()

    @BeforeTest
    fun setup() {
        ctx = StaticApplicationContext().apply { refresh() }
        ctx.beanFactory.registerSingleton("monitorService", recorder)
        SpringKit.applicationContext = ctx
        KudosContextHolder.clear()
        KudosContextHolder.get().apply {
            tenantId = "t-7"
            atomicServiceCode = "svc-a"
        }
    }

    @AfterTest
    fun teardown() {
        KudosContextHolder.clear()
        ctx.close()
    }

    @Test
    fun pushErrMsg_buildsVoAndSubmitsThroughSpringService() {
        MonitorMsgTool.pushErrMsg("query failed", "DB_ERROR", IllegalStateException("conn reset"))

        assertEquals(1, recorder.captured.size, "pushErrMsg must submit exactly once")
        val vo = recorder.captured.single()
        assertEquals("t-7", vo.tenantId)
        assertEquals("svc-a", vo.applicationName)
        assertEquals("DB_ERROR", vo.exceptionType)
        assertNotNull(vo.createTime)
        val msg = assertNotNull(vo.exceptionMsg)
        assertTrue(msg.startsWith("query failed"), "original message must lead: $msg")
        assertTrue("java.lang.IllegalStateException: conn reset" in msg, "exception type+message must be appended: $msg")
        assertTrue("\tat " in msg, "stack frames must be appended: $msg")
        val callSource = assertNotNull(vo.callSource)
        assertTrue("MonitorMsgToolTest" in callSource && "pushErrMsg_buildsVoAndSubmitsThroughSpringService" in callSource,
            "StackWalker must capture this test method as the caller: $callSource")
    }

    @Test
    fun createSysMonitorMsgVo_withoutThrowable_keepsMessageVerbatim() {
        val vo = MonitorMsgTool.createSysMonitorMsgVo("plain warning", "BIZ_WARN", null)

        assertEquals(0, recorder.captured.size, "create-only API must not submit")
        assertEquals("plain warning", vo.exceptionMsg, "no throwable -> no stack section appended")
        assertEquals("BIZ_WARN", vo.exceptionType)
        assertEquals("t-7", vo.tenantId)
        assertEquals("svc-a", vo.applicationName)
        assertNotNull(vo.createTime)
        val callSource = assertNotNull(vo.callSource)
        assertTrue("createSysMonitorMsgVo_withoutThrowable_keepsMessageVerbatim" in callSource,
            "caller capture must work for the create-only entry point too: $callSource")
    }

    @Test
    fun deepStack_isTruncatedToTenFrames() {
        val deep = buildDeepThrowable(depth = 30)
        val vo = MonitorMsgTool.createSysMonitorMsgVo("deep", "DEEP", deep)

        val msg = assertNotNull(vo.exceptionMsg)
        val frameCount = Regex("""\tat """).findAll(msg).count()
        assertEquals(10, frameCount, "stack summary must be capped at 10 frames: $msg")
    }

    @Test
    fun zeroFrameThrowable_appendsOnlyHeaderLine() {
        val bare = RuntimeException("no-frames").apply { stackTrace = arrayOf() }
        val vo = MonitorMsgTool.createSysMonitorMsgVo("hdr", "BARE", bare)

        val msg = assertNotNull(vo.exceptionMsg)
        assertTrue("java.lang.RuntimeException: no-frames" in msg, "header line must still appear: $msg")
        assertTrue("\tat " !in msg, "no frames -> no 'at' lines: $msg")
    }

    private fun buildDeepThrowable(depth: Int): Throwable =
        if (depth <= 0) RuntimeException("bottom") else try {
            recurseThenThrow(depth)
            error("unreachable")
        } catch (e: RuntimeException) {
            e
        }

    private fun recurseThenThrow(depth: Int) {
        if (depth <= 0) throw RuntimeException("bottom")
        recurseThenThrow(depth - 1)
    }

    /** Records every submitted VO for assertions. */
    private class RecordingMonitorService : IMonitorService {
        val captured = mutableListOf<SysMonitorMsgVo>()
        override fun submit(monitorMsgVo: SysMonitorMsgVo): Boolean {
            captured.add(monitorMsgVo)
            return true
        }
    }
}
