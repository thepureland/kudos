package io.kudos.base.logger.slf4j

import io.kudos.base.logger.ILog
import org.slf4j.LoggerFactory
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Slf4jLogger测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class Slf4jLoggerTest {

    private val logger: ILog = Slf4jLogger(LoggerFactory.getLogger(Slf4jLoggerTest::class.java))

    @Test
    fun testConstructor() {
        val slf4jLogger = LoggerFactory.getLogger("test")
        val logger = Slf4jLogger(slf4jLogger)
        assertNotNull(logger)
    }

    @Test
    fun testTrace() {
        // 只是验证不会抛出异常
        logger.trace("trace message")
        logger.trace("trace message with args: {0}", "arg1")
    }

    @Test
    fun testDebug() {
        logger.debug("debug message")
        logger.debug("debug message with args: {0}", "arg1")
    }

    @Test
    fun testInfo() {
        logger.info("info message")
        logger.info("info message with args: {0}", "arg1")
    }

    @Test
    fun testWarn() {
        logger.warn("warn message")
        logger.warn("warn message with args: {0}", "arg1")
    }

    @Test
    fun testError() {
        logger.error("error message")
        logger.error("error message with args: {0}", "arg1")
    }

    @Test
    fun testErrorWithThrowable() {
        val exception = RuntimeException("test exception")
        logger.error(exception, "error message")
        logger.error(exception, "error message with args: {0}", "arg1")
    }

    @Test
    fun testErrorWithThrowableOnly() {
        val exception = RuntimeException("test exception")
        logger.error(exception)
    }

    @Test
    fun testIsTraceEnabled() {
        // 验证方法不会抛出异常
        val enabled = logger.isTraceEnabled()
        assertNotNull(enabled)
    }

    @Test
    fun testIsDebugEnabled() {
        val enabled = logger.isDebugEnabled()
        assertNotNull(enabled)
    }

    @Test
    fun testIsInfoEnabled() {
        val enabled = logger.isInfoEnabled()
        assertNotNull(enabled)
    }

    @Test
    fun testIsWarnEnabled() {
        val enabled = logger.isWarnEnabled()
        assertNotNull(enabled)
    }

    @Test
    fun testIsErrorEnabled() {
        val enabled = logger.isErrorEnabled()
        assertNotNull(enabled)
    }

    @Test
    fun testMultipleArgs() {
        logger.info("message with {0} and {1}", "arg1", "arg2")
        logger.info("message with {0} {1} and {2}", 1, 2, 3)
    }

}
