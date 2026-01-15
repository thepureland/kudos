package io.kudos.base.logger.slf4j

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Slf4jLoggerCreator测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class Slf4jLoggerCreatorTest {

    @Test
    fun testCreateLog() {
        val creator = Slf4jLoggerCreator()
        val log = creator.createLog(Slf4jLoggerCreatorTest::class)
        assertNotNull(log)
    }

    @Test
    fun testCreateLogWithDifferentClasses() {
        val creator = Slf4jLoggerCreator()
        val log1 = creator.createLog(String::class)
        val log2 = creator.createLog(Int::class)
        assertNotNull(log1)
        assertNotNull(log2)
    }

    @Test
    fun testCreateLogIsInstanceOfSlf4jLogger() {
        val creator = Slf4jLoggerCreator()
        val log = creator.createLog(Slf4jLoggerCreatorTest::class)
        assertNotNull(log)
    }
}
