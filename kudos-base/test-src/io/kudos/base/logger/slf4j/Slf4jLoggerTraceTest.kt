package io.kudos.base.logger.slf4j

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.LoggerContext
import org.slf4j.LoggerFactory
import kotlin.test.*

/**
 * Covers the trace/debug "enabled" fast-path bodies of [Slf4jLogger], which are skipped under the
 * default INFO/DEBUG test configuration. We raise a dedicated logger to TRACE via the logback API so
 * the guarded `logger.log(...)` body actually executes, then restore the level.
 *
 * @author K
 * @since 1.0.0
 */
internal class Slf4jLoggerTraceTest {

    private val loggerName = "io.kudos.test.trace." + System.nanoTime()

    @Test
    fun trace_and_debug_bodiesExecuteWhenEnabled() {
        val ctx = LoggerFactory.getILoggerFactory() as LoggerContext
        val lbLogger = ctx.getLogger(loggerName)
        val original = lbLogger.level
        try {
            lbLogger.level = Level.TRACE
            val log = Slf4jLogger(LoggerFactory.getLogger(loggerName))

            assertTrue(log.isTraceEnabled())
            assertTrue(log.isDebugEnabled())

            // these now enter the guarded body (previously skipped)
            log.trace("trace enabled {0}", "x")
            log.trace("trace no args")
            log.debug("debug enabled {0}", "y")
        } finally {
            lbLogger.level = original
        }
    }
}
