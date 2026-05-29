package io.kudos.ability.log.audit.common.api

import io.kudos.ability.log.audit.common.entity.SysMonitorMsgVo
import java.util.Date
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Unit tests for [LoggingMonitorService] — the SLF4J fallback wired into
 * [io.kudos.ability.log.audit.common.starter.LogAuditCommonConfiguration] via `@ConditionalOnMissingBean`.
 *
 * The service is intentionally minimal (one ERROR-level log line + always returns `true`); these
 * tests pin the contract so a future refactor can't accidentally start returning `false` for
 * specific shapes (which would change downstream caller behavior — they treat `false` as "delivery
 * failed, consider retrying").
 */
internal class LoggingMonitorServiceTest {

    @Test
    fun submit_returnsTrueForPopulatedVo() {
        val service = LoggingMonitorService()
        val vo = SysMonitorMsgVo().apply {
            tenantId = "t-1"
            applicationName = "user-service"
            exceptionType = "PROVIDER_TIMEOUT"
            exceptionMsg = "Upstream call exceeded 30s"
            callSource = "(io.kudos.example.SomeService, doWork)"
            createTime = Date(0)
        }

        val out = service.submit(vo)

        // Returning true keeps MonitorMsgTool.pushErrMsg semantics aligned with MQ-backed impls:
        // "best-effort delivery completed". Returning false would prompt the caller to retry,
        // which has no meaning for an already-emitted log line.
        assertTrue(out, "fallback impl must report success — slf4j logging is best-effort and synchronous")
    }

    @Test
    fun submit_returnsTrueForVoWithAllNullsExceptCreateTime() {
        val service = LoggingMonitorService()
        val vo = SysMonitorMsgVo() // every field stays null

        val out = service.submit(vo)

        // Critical: even when every field is null, the fallback must not throw — error-flow callers
        // would otherwise turn one upstream error into two (their own exception plus an aborted
        // monitor report). The `orEmpty()` calls in LoggingMonitorService.submit are what protect
        // this; the test locks the invariant down so a future cleanup doesn't remove them.
        assertTrue(out, "must tolerate fully-null VO without throwing")
    }
}
