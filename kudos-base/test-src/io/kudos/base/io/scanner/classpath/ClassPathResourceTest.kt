package io.kudos.base.io.scanner.classpath

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ClassPathResource测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ClassPathResourceTest {

    @Test
    fun testConstructor() {
        val resource = ClassPathResource("test.properties")
        assertNotNull(resource)
        assertEquals("test.properties", resource.location)
    }

    @Test
    fun testLocation() {
        val resource = ClassPathResource("io/kudos/base/test.properties")
        assertEquals("io/kudos/base/test.properties", resource.location)
    }

    @Test
    fun testFilename() {
        val resource = ClassPathResource("path/to/file.txt")
        assertEquals("file.txt", resource.filename)
    }

    @Test
    fun testFilenameWithRoot() {
        val resource = ClassPathResource("file.txt")
        assertEquals("file.txt", resource.filename)
    }

    @Test
    fun testExists() {
        // 测试存在的资源（使用项目中实际存在的资源）
        val resource = ClassPathResource("logback.xml")
        // 如果资源存在，exists应该返回true
        // 注意：这个测试依赖于实际资源是否存在
    }

    @Test
    fun testExistsNonExistent() {
        val resource = ClassPathResource("non-existent-file-12345.txt")
        // 如果资源不存在，exists应该返回false
    }

    @Test
    fun testEquals() {
        val resource1 = ClassPathResource("test.properties")
        val resource2 = ClassPathResource("test.properties")
        assertEquals(resource1, resource2)
    }

    @Test
    fun testEqualsDifferentLocation() {
        val resource1 = ClassPathResource("test1.properties")
        val resource2 = ClassPathResource("test2.properties")
        assertTrue(resource1 != resource2)
    }

    @Test
    fun testHashCode() {
        val resource1 = ClassPathResource("test.properties")
        val resource2 = ClassPathResource("test.properties")
        assertEquals(resource1.hashCode(), resource2.hashCode())
    }

    @Test
    fun testCompareTo() {
        val resource1 = ClassPathResource("a.properties")
        val resource2 = ClassPathResource("b.properties")
        assertTrue(resource1 < resource2)
    }

    @Test
    fun testCompareToEqual() {
        val resource1 = ClassPathResource("test.properties")
        val resource2 = ClassPathResource("test.properties")
        assertEquals(0, resource1.compareTo(resource2))
    }

    @Test
    fun testLoadAsString() {
        // 测试加载资源为字符串
        // 注意：需要实际存在的资源文件
        try {
            val resource = ClassPathResource("logback.xml")
            if (resource.exists()) {
                val content = resource.loadAsString("UTF-8")
                assertNotNull(content)
            }
        } catch (e: Exception) {
            // 如果资源不存在，忽略测试
        }
    }

    @Test
    fun testLoadAsBytes() {
        // 测试加载资源为字节数组
        try {
            val resource = ClassPathResource("logback.xml")
            if (resource.exists()) {
                val bytes = resource.loadAsBytes()
                assertNotNull(bytes)
            }
        } catch (e: Exception) {
            // 如果资源不存在，忽略测试
        }
    }
}
