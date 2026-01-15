package io.kudos.base.io.scanner.filesystem

import java.io.File
import java.nio.file.Files
import kotlin.test.*

/**
 * FileSystemResource测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class FileSystemResourceTest {

    private lateinit var tempDir: File
    private lateinit var testFile: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("filesystem-resource-test").toFile()
        testFile = File(tempDir, "test.txt")
        testFile.writeText("test content")
    }

    @AfterTest
    fun teardown() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testConstructor() {
        val resource = FileSystemResource(testFile.absolutePath)
        assertNotNull(resource)
    }

    @Test
    fun testLocation() {
        val resource = FileSystemResource(testFile.absolutePath)
        val location = resource.location
        assertNotNull(location)
        assertTrue(location!!.contains("test.txt"))
    }

    @Test
    fun testLocationOnDisk() {
        val resource = FileSystemResource(testFile.absolutePath)
        assertEquals(testFile.absolutePath, resource.locationOnDisk)
    }

    @Test
    fun testFilename() {
        val resource = FileSystemResource(testFile.absolutePath)
        assertEquals("test.txt", resource.filename)
    }

    @Test
    fun testExists() {
        val resource = FileSystemResource(testFile.absolutePath)
        assertTrue(resource.exists())
    }

    @Test
    fun testExistsNonExistent() {
        val nonExistentFile = File(tempDir, "non-existent.txt")
        val resource = FileSystemResource(nonExistentFile.absolutePath)
        assertTrue(!resource.exists())
    }

    @Test
    fun testLoadAsString() {
        val resource = FileSystemResource(testFile.absolutePath)
        val content = resource.loadAsString("UTF-8")
        assertEquals("test content", content)
    }

    @Test
    fun testLoadAsBytes() {
        val resource = FileSystemResource(testFile.absolutePath)
        val bytes = resource.loadAsBytes()
        assertNotNull(bytes)
        assertEquals("test content".toByteArray().size, bytes!!.size)
    }

    @Test
    fun testCompareTo() {
        val file1 = File(tempDir, "a.txt")
        file1.writeText("content")
        val file2 = File(tempDir, "b.txt")
        file2.writeText("content")
        
        val resource1 = FileSystemResource(file1.absolutePath)
        val resource2 = FileSystemResource(file2.absolutePath)
        
        assertTrue(resource1 < resource2)
    }

    @Test
    fun testLoadAsStringWithDifferentEncoding() {
        val utf8File = File(tempDir, "utf8.txt")
        utf8File.writeText("测试内容", Charsets.UTF_8)
        
        val resource = FileSystemResource(utf8File.absolutePath)
        val content = resource.loadAsString("UTF-8")
        assertEquals("测试内容", content)
    }
}
