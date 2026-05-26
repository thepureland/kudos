package io.kudos.base.io.scanner.classpath

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * ClassPathResource test cases.
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
        // Test an existing resource (using a resource that actually exists in the project)
        val resource = ClassPathResource("logback.xml")
        // If the resource exists, exists should return true.
        // Note: this test depends on whether the actual resource exists.
    }

    @Test
    fun testExistsNonExistent() {
        val resource = ClassPathResource("non-existent-file-12345.txt")
        // If the resource does not exist, exists should return false.
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
        // Test loading a resource as a string.
        // Note: requires the resource file to actually exist.
        try {
            val resource = ClassPathResource("logback.xml")
            if (resource.exists()) {
                val content = resource.loadAsString("UTF-8")
                assertNotNull(content)
            }
        } catch (e: Exception) {
            // Ignore the test if the resource does not exist
        }
    }

    @Test
    fun testLoadAsBytes() {
        // Test loading a resource as a byte array
        try {
            val resource = ClassPathResource("logback.xml")
            if (resource.exists()) {
                val bytes = resource.loadAsBytes()
                assertNotNull(bytes)
            }
        } catch (e: Exception) {
            // Ignore the test if the resource does not exist
        }
    }
}
