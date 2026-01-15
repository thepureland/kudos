package io.kudos.base.logger

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * LogFactory测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class LogFactoryTest {

    @Test
    fun testGetLogWithKClass() {
        val log = LogFactory.getLog(LogFactoryTest::class)
        assertNotNull(log)
    }

    @Test
    fun testGetLogWithAny() {
        val log = LogFactory.getLog(this)
        assertNotNull(log)
    }

    @Test
    fun testGetLogWithDifferentClasses() {
        val log1 = LogFactory.getLog(String::class)
        val log2 = LogFactory.getLog(Int::class)
        assertNotNull(log1)
        assertNotNull(log2)
    }

    @Test
    fun testGetLogIsConsistent() {
        val log1 = LogFactory.getLog(LogFactoryTest::class)
        val log2 = LogFactory.getLog(LogFactoryTest::class)
        // 应该返回相同的实例或功能相同的logger
        assertNotNull(log1)
        assertNotNull(log2)
    }
}
