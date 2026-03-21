package io.kudos.base.io.scanner.classpath

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.*

/**
 * JarFileClassPathLocationScanner测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class JarFileClassPathLocationScannerTest {

    private lateinit var tempDir: File
    private lateinit var jarFile: File
    private lateinit var scanner: JarFileClassPathLocationScanner

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("jar-scanner-test").toFile()
        jarFile = File(tempDir, "test.jar")
        scanner = JarFileClassPathLocationScanner()
        createTestJar()
    }

    @AfterTest
    fun teardown() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    private fun createTestJar() {
        JarOutputStream(jarFile.outputStream()).use { jos ->
            // 添加文件到JAR
            jos.putNextEntry(JarEntry("test/file1.txt"))
            jos.write("content1".toByteArray())
            jos.closeEntry()
            
            jos.putNextEntry(JarEntry("test/file2.txt"))
            jos.write("content2".toByteArray())
            jos.closeEntry()
            
            jos.putNextEntry(JarEntry("other/file3.txt"))
            jos.write("content3".toByteArray())
            jos.closeEntry()
        }
    }

    /**
     * 构造合法的 `jar:` URL。Windows 下不可用 `jar:file:${absolutePath}` 直接拼路径（反斜杠会破坏 URI）。
     */
    private fun jarUrl(entryPathInJar: String): URL {
        val fileUri = jarFile.toURI()
        return URI("jar:$fileUri!/$entryPathInJar").toURL()
    }

    @Test
    fun testFindResourceNames() {
        val locationUrl = jarUrl("test/")
        val resourceNames = scanner.findResourceNames("test/", locationUrl)
        
        assertNotNull(resourceNames)
        assertTrue(resourceNames.contains("test/file1.txt"))
        assertTrue(resourceNames.contains("test/file2.txt"))
    }

    @Test
    fun testFindResourceNamesWithRootLocation() {
        val locationUrl = jarUrl("")
        val resourceNames = scanner.findResourceNames("", locationUrl)
        
        assertNotNull(resourceNames)
        assertTrue(resourceNames.isNotEmpty())
    }

    @Test
    fun testFindResourceNamesWithSpecificLocation() {
        val locationUrl = jarUrl("test/")
        val resourceNames = scanner.findResourceNames("test/", locationUrl)
        
        // 应该只包含test/目录下的文件
        assertTrue(resourceNames.all { it.startsWith("test/") })
    }

    @Test
    fun testFindResourceNamesExcludesOtherLocations() {
        val locationUrl = jarUrl("test/")
        val resourceNames = scanner.findResourceNames("test/", locationUrl)
        
        // 不应该包含other/目录下的文件
        assertTrue(resourceNames.none { it.startsWith("other/") })
    }
}
