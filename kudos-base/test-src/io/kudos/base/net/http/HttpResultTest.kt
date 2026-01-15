package io.kudos.base.net.http

import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * HttpResult测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class HttpResultTest {

    @Test
    fun testConstructor() {
        val result = HttpResult(200, "Success", "data")
        assertEquals(200, result.status)
        assertEquals("Success", result.msg)
        assertEquals("data", result.data)
    }

    @Test
    fun testConstructorWithNullData() {
        val result = HttpResult(404, "Not Found", null)
        assertEquals(404, result.status)
        assertEquals("Not Found", result.msg)
        assertEquals(null, result.data)
    }

    @Test
    fun testOkFactoryMethod() {
        val result = HttpResult.ok("操作成功")
        assertEquals(200, result.status)
        assertEquals("操作成功", result.msg)
        assertEquals(null, result.data)
    }

    @Test
    fun testOkFactoryMethodWithData() {
        val data = mapOf("id" to 1, "name" to "test")
        val result = HttpResult.ok("操作成功", data)
        assertEquals(200, result.status)
        assertEquals("操作成功", result.msg)
        assertEquals(data, result.data)
    }

    @Test
    fun testErrorFactoryMethod() {
        val result = HttpResult.error("操作失败")
        assertEquals(500, result.status)
        assertEquals("操作失败", result.msg)
        assertEquals(null, result.data)
    }

    @Test
    fun testErrorFactoryMethodWithData() {
        val errorInfo = mapOf("code" to "E001", "message" to "系统错误")
        val result = HttpResult.error("操作失败", errorInfo)
        assertEquals(500, result.status)
        assertEquals("操作失败", result.msg)
        assertEquals(errorInfo, result.data)
    }

    @Test
    fun testDifferentStatusCodes() {
        val statusCodes = listOf(200, 201, 400, 401, 403, 404, 500, 502, 503)
        statusCodes.forEach { code ->
            val result = HttpResult(code, "Message", null)
            assertEquals(code, result.status)
        }
    }

    @Test
    fun testEmptyMessage() {
        val result = HttpResult(200, "", null)
        assertEquals(200, result.status)
        assertEquals("", result.msg)
    }

    @Test
    fun testComplexDataTypes() {
        val listData = listOf(1, 2, 3)
        val result1 = HttpResult.ok("Success", listData)
        assertEquals(listData, result1.data)

        val mapData = mapOf("key1" to "value1", "key2" to 123)
        val result2 = HttpResult.ok("Success", mapData)
        assertEquals(mapData, result2.data)

        val objectData = TestData("test", 100)
        val result3 = HttpResult.ok("Success", objectData)
        assertEquals(objectData, result3.data)
    }

    private data class TestData(val name: String, val value: Int)
}
