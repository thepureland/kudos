package io.kudos.base.io.scanner.classpath

import io.kudos.base.io.FileKit
import java.io.File
import java.nio.file.Files
import kotlin.test.*

/**
 * FileSystemClassPathLocationScanner测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class FileSystemClassPathLocationScannerTest {

    private lateinit var tempDir: File
    private lateinit var scanner: FileSystemClassPathLocationScanner

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("scanner-test").toFile()
        scanner = FileSystemClassPathLocationScanner()
    }

    @AfterTest
    fun teardown() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    @Test
    fun testFindResourceNames() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")
        
        val locationUrl = tempDir.toURI().toURL()
        val resourceNames = scanner.findResourceNames("", locationUrl)
        
        assertTrue(resourceNames.contains("test.txt"))
    }

    @Test
    fun testFindResourceNamesWithSubdirectory() {
        val subDir = File(tempDir, "sub")
        subDir.mkdirs()
        val testFile = File(subDir, "test.txt")
        testFile.writeText("test")
        
        val locationUrl = tempDir.toURI().toURL()
        val resourceNames = scanner.findResourceNames("", locationUrl)
        
        assertTrue(resourceNames.contains("sub/test.txt"))
    }

    @Test
    fun testFindResourceNamesWithLocation() {
        val locationDir = File(tempDir, "location")
        locationDir.mkdirs()
        val testFile = File(locationDir, "test.txt")
        testFile.writeText("test")
        
        val locationUrl = locationDir.toURI().toURL()
        val resourceNames = scanner.findResourceNames("location", locationUrl)
        
        assertNotNull(resourceNames)
    }

    @Test
    fun testFindResourceNamesWithNonDirectory() {
        val testFile = File(tempDir, "test.txt")
        testFile.writeText("test")
        
        val locationUrl = testFile.toURI().toURL()
        val resourceNames = scanner.findResourceNames("", locationUrl)
        
        // 如果不是目录，应该返回空集合
        assertTrue(resourceNames.isEmpty())
    }

    @Test
    fun testFindResourceNamesFromFileSystem() {
        val testFile1 = File(tempDir, "file1.txt")
        testFile1.writeText("content1")
        val testFile2 = File(tempDir, "file2.txt")
        testFile2.writeText("content2")
        
        val locationUrl = tempDir.toURI().toURL()
        val filePath = FileKit.toFile(locationUrl)?.path
        if (filePath != null) {
            val folder = File(filePath)
            val classPathRootOnDisk = filePath.substring(0, filePath.length) + "/"
            val resourceNames = scanner.findResourceNamesFromFileSystem(classPathRootOnDisk, "", folder)
            
            assertTrue(resourceNames.contains("file1.txt") || resourceNames.contains("file2.txt"))
        }
    }

    @Test
    fun testFindResourceNamesWithMultipleFiles() {
        val files = listOf("file1.txt", "file2.txt", "file3.txt")
        files.forEach { fileName ->
            File(tempDir, fileName).writeText("content")
        }
        
        val locationUrl = tempDir.toURI().toURL()
        val resourceNames = scanner.findResourceNames("", locationUrl)
        
        assertEquals(3, resourceNames.size)
        files.forEach { fileName ->
            assertTrue(resourceNames.contains(fileName))
        }
    }

    @Test
    fun testFindResourceNamesRecursive() {
        val subDir1 = File(tempDir, "dir1")
        subDir1.mkdirs()
        val subDir2 = File(subDir1, "dir2")
        subDir2.mkdirs()
        val testFile = File(subDir2, "test.txt")
        testFile.writeText("test")
        
        val locationUrl = tempDir.toURI().toURL()
        val resourceNames = scanner.findResourceNames("", locationUrl)
        
        assertTrue(resourceNames.contains("dir1/dir2/test.txt"))
    }
}
