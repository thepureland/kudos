package io.kudos.base.io.scanner.classpath

import java.io.File
import java.net.URI
import java.net.URL
import java.nio.file.Files
import java.util.jar.JarEntry
import java.util.jar.JarOutputStream
import kotlin.test.*

/**
 * JarFileClassPathLocationScanner test cases.
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
            // Add files to the JAR
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
     * Construct a valid `jar:` URL. On Windows, `jar:file:${absolutePath}` cannot be concatenated directly (backslashes break the URI).
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
        
        // Should only include files under the test/ directory
        assertTrue(resourceNames.all { it.startsWith("test/") })
    }

    @Test
    fun testFindResourceNamesExcludesOtherLocations() {
        val locationUrl = jarUrl("test/")
        val resourceNames = scanner.findResourceNames("test/", locationUrl)
        
        // Should not include any files under the other/ directory
        assertTrue(resourceNames.none { it.startsWith("other/") })
    }
}
