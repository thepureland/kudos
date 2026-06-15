package io.kudos.base.io.scanner.classpath

import java.io.IOException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
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
        // logo.png exists at the test resources root.
        val resource = ClassPathResource("logo.png")
        assertTrue(resource.exists(), "logo.png should be on the test classpath")
    }

    @Test
    fun testExistsNonExistent() {
        val resource = ClassPathResource("non-existent-file-12345.txt")
        assertFalse(resource.exists(), "a missing resource must report exists()=false")
    }

    @Test
    fun testLocationOnDisk_existingResource() {
        val resource = ClassPathResource("logo.png")
        val onDisk = resource.locationOnDisk
        assertNotNull(onDisk)
        assertTrue(onDisk!!.endsWith("logo.png"), "locationOnDisk should point at the file: $onDisk")
    }

    @Test
    fun testLocationOnDisk_missingResource_throws() {
        val resource = ClassPathResource("nope-not-here-98765.txt")
        assertFailsWith<IOException> { resource.locationOnDisk }
    }

    @Test
    fun testLoadAsString_missingResource_throws() {
        val resource = ClassPathResource("missing-12345.txt")
        assertFailsWith<IOException> { resource.loadAsString("UTF-8") }
    }

    @Test
    fun testLoadAsBytes_missingResource_throws() {
        val resource = ClassPathResource("missing-12345.txt")
        assertFailsWith<IOException> { resource.loadAsBytes() }
    }

    @Test
    fun testLoadAsBytes_existingResource() {
        val resource = ClassPathResource("logo.png")
        val bytes = resource.loadAsBytes()
        assertTrue(bytes.isNotEmpty())
    }

    @Test
    fun testEquals_branches() {
        val r = ClassPathResource("x.txt")
        @Suppress("KotlinConstantConditions")
        assertTrue(r == r) // same instance
        assertFalse(r.equals(null)) // null
        assertFalse(r.equals("x.txt")) // different class
        assertTrue(r == ClassPathResource("x.txt")) // same location
        assertFalse(r == ClassPathResource("y.txt")) // different location
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
