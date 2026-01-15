package io.kudos.base.io.scanner.filesystem

import io.kudos.base.io.scanner.support.Resource
import java.io.File
import java.nio.file.Files
import kotlin.test.*

/**
 * FileSystemScanner测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class FileSystemScannerTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("filesystem-scanner-test").toFile()
    }

    @AfterTest
    fun teardown() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testScanForResources() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")
        
        val resources = FileSystemScanner.scanForResources(tempDir.absolutePath, "", ".txt")
        
        assertNotNull(resources)
        assertTrue(resources.isNotEmpty())
    }

    @Test
    fun testScanForResourcesWithPrefix() {
        val file1 = File(tempDir, "test1.txt")
        file1.writeText("content1")
        val file2 = File(tempDir, "other.txt")
        file2.writeText("content2")
        
        val resources = FileSystemScanner.scanForResources(tempDir.absolutePath, "test", ".txt")
        
        assertTrue(resources.any { it.filename == "test1.txt" })
        assertTrue(resources.none { it.filename == "other.txt" })
    }

    @Test
    fun testScanForResourcesWithSuffix() {
        val file1 = File(tempDir, "test.txt")
        file1.writeText("content1")
        val file2 = File(tempDir, "test.xml")
        file2.writeText("content2")
        
        val resources = FileSystemScanner.scanForResources(tempDir.absolutePath, "", ".txt")
        
        assertTrue(resources.any { it.filename == "test.txt" })
        assertTrue(resources.none { it.filename == "test.xml" })
    }

    @Test
    fun testScanForResourcesWithPrefixAndSuffix() {
        val file1 = File(tempDir, "test.txt")
        file1.writeText("content1")
        val file2 = File(tempDir, "test.xml")
        file2.writeText("content2")
        val file3 = File(tempDir, "other.txt")
        file3.writeText("content3")
        
        val resources = FileSystemScanner.scanForResources(tempDir.absolutePath, "test", ".txt")
        
        assertTrue(resources.any { it.filename == "test.txt" })
        assertTrue(resources.none { it.filename == "test.xml" })
        assertTrue(resources.none { it.filename == "other.txt" })
    }

    @Test
    fun testScanForResourcesWithSubdirectory() {
        val subDir = File(tempDir, "sub")
        subDir.mkdirs()
        val testFile = File(subDir, "test.txt")
        testFile.writeText("test")
        
        val resources = FileSystemScanner.scanForResources(tempDir.absolutePath, "", ".txt")
        
        assertTrue(resources.isNotEmpty())
    }

    @Test
    fun testScanForResourcesWithInvalidPath() {
        val invalidPath = "/invalid/path/that/does/not/exist"
        assertFailsWith<Exception> {
            FileSystemScanner.scanForResources(invalidPath, "", "")
        }
    }

    @Test
    fun testScanForResourcesWithFileInsteadOfDirectory() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")
        
        assertFailsWith<Exception> {
            FileSystemScanner.scanForResources(testFile.absolutePath, "", "")
        }
    }

    @Test
    fun testScanForResourcesEmptyResult() {
        val resources = FileSystemScanner.scanForResources(tempDir.absolutePath, "nonexistent", ".txt")
        
        assertEquals(0, resources.size)
    }

    @Test
    fun testScanForResourcesReturnsResourceArray() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")
        
        val resources = FileSystemScanner.scanForResources(tempDir.absolutePath, "", ".txt")
        
        resources.forEach { resource ->
            assertNotNull(resource)
        }
    }
}
