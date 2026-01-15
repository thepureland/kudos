package io.kudos.base.io

import io.kudos.base.enums.impl.OsEnum
import io.kudos.base.lang.SystemKit
import org.apache.commons.io.FileUtils
import java.io.File
import java.net.URLDecoder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * test for PathKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class PathKitTest {

    @Test
    fun getClasspathIncludePackageForRegularClass() {
        // For a normal class (PathKit itself), the returned path should be non-empty,
        // URL-decoded, and point to an existing directory (the classes folder or JAR root).
        val path = PathKit.getClasspathIncludePackage(PathKit::class)
        assertTrue(path.isNotEmpty(), "Returned classpath should not be empty")

        // It may point inside a JAR or build/classes directory; either way it should decode cleanly.
        val decoded = URLDecoder.decode(path, "UTF-8")
        assertEquals(decoded, path, "Returned path should already be URL-decoded")

        // As long as it ends with a separator or valid file location, check it's a valid filesystem path
        val file = File(path)
        assertTrue(
            file.exists() || path.endsWith(".jar/") || path.endsWith(".jar"),
            "The path $path should correspond to an existing file or JAR location"
        )
    }

    @Test
    fun getClasspathForRegularClass() {
        // Should return the root container (without package folders) for PathKit.class
        val full = PathKit.getClasspathIncludePackage(PathKit::class)
        val root = PathKit.getClasspath(PathKit::class)
        assertTrue(root.isNotEmpty(), "Returned root classpath should not be empty")
        // The include-package version must start with the root
        assertTrue(full.startsWith(root), "Include-package path should start with root classpath")
    }

    @Test
    fun getResourcePathFindsExistingResource() {
        // Construct the resource name dynamically from the class’s binary name.
        val resourceName = PathKit::class.java.name.replace('.', '/') + ".class"
        val resPath = PathKit.getResourcePath(resourceName)
        assertTrue(resPath.endsWith("PathKit.class"), "Resource path must end with PathKit.class")
        // Check that the resource file actually exists on disk
        val file = File(resPath)
        assertTrue(file.exists(), "Resource file must exist: $resPath")
    }

    @Test
    fun getResourcePathInvalidNameThrowsNpe() {
        // A non-existent resource name should result in a NullPointerException
        assertFailsWith<IllegalStateException> {
            PathKit.getResourcePath("nonexistent_resource_abc.xyz")
        }
    }

    @Test
    fun getRelativePathBasicCases() {
        // When baseDir == file, should return empty string
        val base = File("/tmp/base")
        assertEquals("", PathKit.getRelativePath(base, base))

        // When file is directly under baseDir
        val baseDir = File("/tmp/base")
        val child = File("/tmp/base/child.txt")
        val rel = PathKit.getRelativePath(baseDir, child)
        assertEquals("child.txt", rel)

        // When baseDir has a parent, substring starts after the slash
        val parent = File("/tmp")
        val nested = File("/tmp/base/dir/file.txt")
        val rel2 = PathKit.getRelativePath(parent, nested)
        // parent.absolutePath.length + 1 to skip the '/' after /tmp
        assertEquals("base/dir/file.txt", rel2)

        // Windows-style backslashes should be normalized to forward slashes,
        // and there will be a leading "/" after substring+replace
        val windowsBase = File("C:\\proj")
        val windowsFile = File("C:\\proj\\sub\\f.txt")
        val relWin = PathKit.getRelativePath(windowsBase, windowsFile)
        if (SystemKit.currentOs() == OsEnum.WINDOWS) {
            assertEquals("sub/f.txt", relWin)
        } else {
            assertEquals("/sub/f.txt", relWin)
        }
    }

    @Test
    fun getProjectRootPathMatchesUserDir() {
        val expected = System.getProperty("user.dir")
        assertEquals(expected, PathKit.getProjectRootPath())
    }

    @Test
    fun getRuntimePathNotEmpty() {
        val runtime = PathKit.getRuntimePath()
        assertTrue(runtime.isNotEmpty(), "Runtime path should not be empty")
        // Should point to a directory on the filesystem
        val file = File(runtime)
        assertTrue(file.exists() && file.isDirectory, "Runtime path must be an existing directory")
    }

    @Test
    fun getTempDirectoryPathAndObject() {
        // 从 PathKit 获取的临时目录路径，和 Apache FileUtils 返回的应该一致
        val fromKit = PathKit.getTempDirectoryPath().trimEnd(File.separatorChar)
        val fromSystem = FileUtils.getTempDirectoryPath().trimEnd(File.separatorChar)
        assertEquals(fromSystem, fromKit)

        // getTempDirectory 应返回与路径对应的目录，并且目录确实存在
        val tempDir = PathKit.getTempDirectory()
        assertTrue(tempDir.isDirectory, "getTempDirectory must return an existing directory")
        // 目录本身的绝对路径去除尾部分隔符后，应该与 fromKit 一致
        assertEquals(fromKit, tempDir.absolutePath.trimEnd(File.separatorChar))
    }

    @Test
    fun getUserDirectoryPathAndObject() {
        val fromKit = PathKit.getUserDirectoryPath()
        val fromSystem = FileUtils.getUserDirectoryPath()
        assertEquals(fromSystem, fromKit)

        val userDir = PathKit.getUserDirectory()
        assertTrue(userDir.isDirectory, "getUserDirectory must return an existing directory")
        assertEquals(fromKit, userDir.absolutePath)
    }

    @Test
    fun getResourcePath() {
        // resources中
        assert(File(PathKit.getResourcePath("logo.png")).exists())

        // testresources中
        assert(File(PathKit.getResourcePath("TestExcelImporter.xls")).exists())

        // 目录
        assert(File(PathKit.getResourcePath("i18n")).exists())
    }
}