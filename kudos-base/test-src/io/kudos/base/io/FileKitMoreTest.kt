package io.kudos.base.io

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.Files
import java.util.zip.CRC32
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Additional [FileKit] test cases covering overloads and default-argument variants not reached by [FileKitTest]:
 * extension-filtered listing, copyFile-to-OutputStream, default-encoding read/write paths, append/byte/line writers,
 * the timeMillis comparison overloads, custom Checksum, and the move operations.
 *
 * @author K
 * @since 1.0.0
 */
internal class FileKitMoreTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("filekit_more").toFile()
    }

    @AfterTest
    fun teardown() {
        if (tempDir.exists()) tempDir.deleteRecursively()
    }

    @Test
    fun listFiles_and_iterateFiles_byExtension() {
        File(tempDir, "a.txt").writeText("a")
        File(tempDir, "b.log").writeText("b")
        val sub = File(tempDir, "sub").apply { mkdir() }
        File(sub, "c.txt").writeText("c")

        val txtRecursive = FileKit.listFiles(tempDir, arrayOf("txt"), true)
        assertEquals(2, txtRecursive.size)

        val txtNonRecursive = FileKit.listFiles(tempDir, arrayOf("txt"), false)
        assertEquals(1, txtNonRecursive.size)

        // null extensions -> all files
        val all = FileKit.listFiles(tempDir, null, true)
        assertTrue(all.size >= 3)

        val it = FileKit.iterateFiles(tempDir, arrayOf("txt"), true)
        var count = 0
        while (it.hasNext()) { it.next(); count++ }
        assertEquals(2, count)
    }

    @Test
    fun copyFile_toOutputStream() {
        val src = File(tempDir, "src.txt").apply { writeText("payload") }
        val baos = ByteArrayOutputStream()
        val n = FileKit.copyFile(src, baos)
        assertEquals(7L, n)
        assertEquals("payload", baos.toString(Charsets.UTF_8.name()))
    }

    @Test
    fun copyFile_and_copyFileToDirectory_defaultPreserveDate() {
        val src = File(tempDir, "s.txt").apply { writeText("x") }
        val dst = File(tempDir, "d.txt")
        FileKit.copyFile(src, dst) // default preserveFileDate=true
        assertEquals("x", dst.readText())

        val destDir = File(tempDir, "destdir")
        FileKit.copyFileToDirectory(src, destDir) // default preserveFileDate
        assertTrue(File(destDir, "s.txt").exists())
    }

    @Test
    fun copyDirectory_defaultAndFilter() {
        val srcDir = File(tempDir, "from").apply { mkdir() }
        File(srcDir, "keep.txt").writeText("k")
        File(srcDir, "skip.log").writeText("s")

        val dst1 = File(tempDir, "to1")
        FileKit.copyDirectory(srcDir, dst1) // default preserveFileDate
        assertTrue(File(dst1, "keep.txt").exists())

        val dst2 = File(tempDir, "to2")
        FileKit.copyDirectory(srcDir, dst2, { f -> f.isDirectory || f.name.endsWith(".txt") }) // filter overload
        assertTrue(File(dst2, "keep.txt").exists())
        assertFalse(File(dst2, "skip.log").exists())
    }

    @Test
    fun readLines_and_lineIterator_defaultEncoding() {
        val f = File(tempDir, "lines.txt").apply { writeText("l1\nl2") }
        assertEquals(listOf("l1", "l2"), FileKit.readLines(f))
        val it = FileKit.lineIterator(f)
        val collected = buildList { while (it.hasNext()) add(it.next()) }
        it.close()
        assertEquals(listOf("l1", "l2"), collected)
    }

    @Test
    fun write_variants_defaultEncoding_and_append() {
        val f = File(tempDir, "w.txt")
        FileKit.writeStringToFile(f, "one", append = false) // append overload, default encoding
        FileKit.writeStringToFile(f, "two", append = true)
        assertEquals("onetwo", f.readText())

        val f2 = File(tempDir, "cs.txt")
        FileKit.write(f2, "seq") // CharSequence, default encoding & append=false
        assertEquals("seq", f2.readText())

        val f3 = File(tempDir, "bytes.txt")
        FileKit.writeByteArrayToFile(f3, "bytes".toByteArray()) // default append=false
        assertEquals("bytes", f3.readText())

        val f4 = File(tempDir, "ls.txt")
        FileKit.writeLines(f4, lines = listOf("p", "q")) // default encoding/lineEnding/append
        assertTrue(f4.readText().contains("p"))
    }

    @Test
    fun isFileNewer_and_isFileOlder_byTimeMillis() {
        val f = File(tempDir, "t.txt").apply { writeText("t") }
        val mod = f.lastModified()
        assertTrue(FileKit.isFileNewer(f, mod - 10_000))
        assertFalse(FileKit.isFileNewer(f, mod + 10_000))
        assertTrue(FileKit.isFileOlder(f, mod + 10_000))
        assertFalse(FileKit.isFileOlder(f, mod - 10_000))
    }

    @Test
    fun checksum_withCustomChecksumObject() {
        val f = File(tempDir, "c.bin").apply { writeBytes(byteArrayOf(1, 2, 3, 4)) }
        val expected = FileKit.checksumCRC32(f)
        val result = FileKit.checksum(f, CRC32())
        assertEquals(expected, result.value)
    }

    @Test
    fun moveFile_and_moveDirectory() {
        val src = File(tempDir, "mv.txt").apply { writeText("m") }
        val dst = File(tempDir, "moved.txt")
        FileKit.moveFile(src, dst)
        assertFalse(src.exists())
        assertTrue(dst.exists())

        val srcDir = File(tempDir, "md").apply { mkdir() }
        File(srcDir, "inner.txt").writeText("i")
        val dstDir = File(tempDir, "md-moved")
        FileKit.moveDirectory(srcDir, dstDir)
        assertFalse(srcDir.exists())
        assertTrue(File(dstDir, "inner.txt").exists())

        // moveFileToDirectory with createDestDir
        val src2 = File(tempDir, "mv2.txt").apply { writeText("2") }
        val targetDir = File(tempDir, "mvdir")
        FileKit.moveFileToDirectory(src2, targetDir, true)
        assertTrue(File(targetDir, "mv2.txt").exists())

        // moveDirectoryToDirectory
        val srcDir2 = File(tempDir, "md2").apply { mkdir() }
        File(srcDir2, "x.txt").writeText("x")
        val parentDir = File(tempDir, "parent")
        FileKit.moveDirectoryToDirectory(srcDir2, parentDir, true)
        assertTrue(File(parentDir, "md2/x.txt").exists())

        // moveToDirectory (generic)
        val src3 = File(tempDir, "mv3.txt").apply { writeText("3") }
        val genDir = File(tempDir, "gendir")
        FileKit.moveToDirectory(src3, genDir, true)
        assertTrue(File(genDir, "mv3.txt").exists())
    }

    @Test
    fun sizeOf_and_sizeOfDirectory_defaultEncoding() {
        val f = File(tempDir, "sz.txt").apply { writeText("12345") }
        assertEquals(5L, FileKit.sizeOf(f))
        assertEquals(java.math.BigInteger.valueOf(5L), FileKit.sizeOfAsBigInteger(f))
        assertTrue(FileKit.sizeOfDirectory(tempDir) >= 5L)
        assertTrue(FileKit.sizeOfDirectoryAsBigInteger(tempDir) >= java.math.BigInteger.valueOf(5L))
    }

    @Test
    fun openOutputStream_defaultAppendFalse() {
        val f = File(tempDir, "oos.txt")
        FileKit.openOutputStream(f).use { it.write("oos".toByteArray()) }
        assertEquals("oos", f.readText())
    }

    @Test
    fun zip_nonExistentFile_returnsNull() {
        // addFile on a non-existent source throws -> runCatching.getOrElse logs, deletes temp, returns null.
        val missing = File(tempDir, "does-not-exist.txt")
        val result = FileKit.zip(missing, "x.txt", null)
        assertEquals(null, result)
    }

    @Test
    fun listFilesOrDirsInJar_defaultArgs() {
        // Build a tiny jar so the default (recursive=false, includeDirs=true) path is exercised.
        val jarFile = File.createTempFile("more-", ".jar")
        java.util.jar.JarOutputStream(jarFile.outputStream()).use { jar ->
            jar.putNextEntry(java.util.zip.ZipEntry("assets/")); jar.closeEntry()
            jar.putNextEntry(java.util.zip.ZipEntry("assets/a.txt")); jar.write("a".toByteArray()); jar.closeEntry()
            jar.putNextEntry(java.util.zip.ZipEntry("assets/sub/b.txt")); jar.write("b".toByteArray()); jar.closeEntry()
        }
        try {
            val jarPath = "jar:file:${jarFile.absolutePath}!"
            // call with all-default trailing args
            val result = FileKit.listFilesOrDirsInJar(jarPath, "assets")
            assertTrue(result.contains("assets/a.txt"))
            assertTrue(result.contains("assets/sub/")) // first-level subdir included by default
        } finally {
            jarFile.delete()
        }
    }

    @Test
    fun isSymlink_onRegularFile_isFalse() {
        val f = File(tempDir, "reg.txt").apply { writeText("r") }
        // On Windows this always returns false; on Unix a regular file is not a symlink.
        assertFalse(FileKit.isSymlink(f))
    }
}
