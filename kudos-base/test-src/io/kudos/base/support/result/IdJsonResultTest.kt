package io.kudos.base.support.result

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * IdJsonResult测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class IdJsonResultTest {

    @Test
    fun testDefaultIdIsNull() {
        val result = IdJsonResult<String>()
        assertNull(result.id)
    }

    @Test
    fun testSetId() {
        val result = IdJsonResult<String>()
        result.id = "123"
        assertEquals("123", result.id)
    }

    @Test
    fun testIdWithStringType() {
        val result = IdJsonResult<String>()
        result.id = "test-id"
        assertEquals("test-id", result.id)
    }

    @Test
    fun testIdWithLongType() {
        val result = IdJsonResult<Long>()
        result.id = 12345L
        assertEquals(12345L, result.id)
    }

    @Test
    fun testIdWithIntType() {
        val result = IdJsonResult<Int>()
        result.id = 42
        assertEquals(42, result.id)
    }

    @Test
    fun testIdCanBeSetToNull() {
        val result = IdJsonResult<String>()
        result.id = "123"
        assertEquals("123", result.id)
        result.id = null
        assertNull(result.id)
    }

    @Test
    fun testIdChange() {
        val result = IdJsonResult<String>()
        result.id = "old-id"
        assertEquals("old-id", result.id)
        result.id = "new-id"
        assertEquals("new-id", result.id)
    }

    @Test
    fun testIdWithUUID() {
        val result = IdJsonResult<java.util.UUID>()
        val uuid = java.util.UUID.randomUUID()
        result.id = uuid
        assertEquals(uuid, result.id)
    }
}
