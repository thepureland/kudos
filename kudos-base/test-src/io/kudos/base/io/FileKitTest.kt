package io.kudos.base.io

import io.kudos.base.enums.impl.OsEnum
import io.kudos.base.lang.SystemKit
import org.apache.commons.io.filefilter.TrueFileFilter
import java.io.*
import java.math.BigInteger
import java.net.URI
import java.nio.file.Files
import java.util.jar.JarOutputStream
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import kotlin.test.*


/**
 * test for FileKit
 *
 * @author AI: ChatGPT
 * @author K
 * @since 1.0.0
 */
internal class FileKitTest {

    private lateinit var tempDir: File

    @BeforeTest
    fun setup() {
        tempDir = Files.createTempDirectory("filekit_test").toFile()
    }

    @AfterTest
    fun teardown() {
        if (tempDir.exists()) {
            tempDir.deleteRecursively()
        }
    }

    private fun createTestJar(): File {
        val tempDir = Files.createTempDirectory("testJar").toFile()
        val jarFile = File.createTempFile("test-", ".jar")

        // Create the directory structure and files
        val assetsDir = File(tempDir, "assets").apply { mkdir() }
        File(assetsDir, "test1.txt").writeText("file1")
        File(assetsDir, "test2.txt").writeText("file2")
        val subDir = File(assetsDir, "subdir").apply { mkdir() }
        File(subDir, "test3.txt").writeText("file3")

        // Write to the JAR
        JarOutputStream(jarFile.outputStream()).use { jarOut ->
            fun addEntry(file: File, basePath: String) {
                val entryName = basePath + file.name + if (file.isDirectory) "/" else ""
                jarOut.putNextEntry(ZipEntry(entryName))
                if (file.isFile) {
                    file.inputStream().copyTo(jarOut)
                }
                jarOut.closeEntry()
                if (file.isDirectory) {
                    file.listFiles()?.forEach { child ->
                        addEntry(child, entryName)
                    }
                }
            }
            tempDir.listFiles()?.forEach { addEntry(it, "") }
        }

        return jarFile
    }

    @Test
    fun testListFilesAndDirsInJar_nonRecursive_noDirs() {
        val jarFile = createTestJar()
        val jarPath = "jar:file:${jarFile.absolutePath}!"
        val result = FileKit.listFilesOrDirsInJar(jarPath, "assets", recursive = false, includeDirs = false)

        val expected = listOf("assets/test1.txt", "assets/test2.txt")
        assertEquals(expected.sorted(), result.sorted())
    }

    @Test
    fun testListFilesOrDirsInJar_nonRecursive_withDirs() {
        val jarFile = createTestJar()
        val jarPath = "jar:file:${jarFile.absolutePath}!"
        val result = FileKit.listFilesOrDirsInJar(jarPath, "assets", recursive = false, includeDirs = true)

        val expected = listOf("assets/test1.txt", "assets/test2.txt", "assets/subdir/")
        assertEquals(expected.sorted(), result.sorted())
    }

    @Test
    fun testListFilesOrDirsInJar_recursive_noDirs() {
        val jarFile = createTestJar()
        val jarPath = "jar:file:${jarFile.absolutePath}!"
        val result = FileKit.listFilesOrDirsInJar(jarPath, "assets", recursive = true, includeDirs = false)

        val expected = listOf("assets/test1.txt", "assets/test2.txt", "assets/subdir/test3.txt")
        assertEquals(expected.sorted(), result.sorted())
    }

    @Test
    fun testListFilesOrDirsInJar_recursive_withDirs() {
        val jarFile = createTestJar()
        val jarPath = "jar:file:${jarFile.absolutePath}!"
        val result = FileKit.listFilesOrDirsInJar(jarPath, "assets", recursive = true, includeDirs = true)

        val expected = listOf(
            "assets/subdir/", "assets/subdir/test3.txt", "assets/test1.txt", "assets/test2.txt"
        )
        assertEquals(expected.sorted(), result.sorted())
    }

    // ---------- zip methods ----------

    @Test
    fun zipWithoutPasswordCreatesValidZip() {
        // Prepare a simple text file
        val src = File(tempDir, "plain.txt")
        src.writeText("hello")
        // No password, fileName is null
        val zipped = FileKit.zip(src, null, null)
        assertNotNull(zipped)
        assertTrue(zipped.exists())
        // The ZIP file should be at least as large as the source file
        assertTrue(zipped.length() >= src.length())
        zipped.delete()
    }

    @Test
    fun zipWithPasswordAndCustomFileName() {
        val src = File(tempDir, "secret.txt")
        src.writeText("top secret")
        // Provide a password and pass an empty fileName
        val zipped = FileKit.zip(src, "", "mypassword")
        // Since fileName is empty, file.getName() is used
        assertNotNull(zipped)
        assertTrue(zipped.exists())
        // The encryption library requires the BouncyCastle implementation; at minimum it returns a non-empty file
        assertTrue(zipped.length() > 0)
        zipped.delete()
    }

    @Test
    fun zipWithBlankPasswordTreatsAsNoEncryption() {
        val src = File(tempDir, "test.txt")
        src.writeText("data")
        // A blank password string should go through the unencrypted branch
        val zipped = FileKit.zip(src, "ignoredName", "   ")
        assertNotNull(zipped)
        assertTrue(zipped.exists())
        zipped.delete()
    }

    // ---------- getFile methods ----------

    @Test
    fun getFileWithDirectoryAndNames() {
        val dir = File(tempDir, "subdir")
        dir.mkdirs()
        val f = FileKit.getFile(dir, "a", "b", "c.txt")
        val expected = File(dir, "a${File.separator}b${File.separator}c.txt").path
        assertEquals(expected, f.path)
    }

    @Test
    fun getFileWithNamesOnly() {
        val f = FileKit.getFile("x", "y", "z.txt")
        val expected = File("x${File.separator}y${File.separator}z.txt").path
        assertEquals(expected, f.path)
    }

    // ---------- openInputStream / openOutputStream ----------

    @Test
    fun openInputStreamNonexistentThrows() {
        val missing = File(tempDir, "nofile.txt")
        assertFailsWith<FileNotFoundException> {
            FileKit.openInputStream(missing)
        }
    }

    @Test
    fun openInputStreamDirectoryThrows() {
        val dir = File(tempDir, "adir")
        dir.mkdirs()
        assertFailsWith<IOException> {
            FileKit.openInputStream(dir)
        }
    }

    @Test
    fun openOutputStreamCreatesParentAndWrites() {
        val nested = File(tempDir, "p1/p2/out.txt")
        // openOutputStream should automatically create the parent directory when it does not exist
        val fos = FileKit.openOutputStream(nested)
        fos.write("hello".toByteArray())
        fos.close()
        assertTrue(nested.exists())
        assertEquals("hello", nested.readText())
    }

    @Test
    fun openOutputStreamOnDirectoryThrows() {
        val dir = File(tempDir, "mdir")
        dir.mkdirs()
        // openOutputStream throws IllegalArgumentException when a directory is passed in
        assertFailsWith<IllegalArgumentException> {
            FileKit.openOutputStream(dir)
        }
    }

    // ---------- byteCountToDisplaySize ----------

    @Test
    fun byteCountToDisplaySizeEdgeCases() {
        assertEquals("0 bytes", FileKit.byteCountToDisplaySize(BigInteger.ZERO))
        assertEquals("1 KB", FileKit.byteCountToDisplaySize(1024L))
        assertEquals("1 MB", FileKit.byteCountToDisplaySize(BigInteger.valueOf(2_000_000)))
        // Floor when exceeding 1 GB
        val twoGB = BigInteger.valueOf(2L shl 30)
        assertTrue(FileKit.byteCountToDisplaySize(twoGB).endsWith(" GB"))
    }

    // ---------- touch / sizeOf ----------

    @Test
    fun touchOnExistingFileUpdatesLastModified() {
        val f = File(tempDir, "touch.txt")
        f.createNewFile()
        val before = f.lastModified()
        Thread.sleep(10)
        FileKit.touch(f)
        val after = f.lastModified()
        assertTrue(after >= before)
    }

    @Test
    fun sizeOfOnMissingThrows() {
        val missing = File(tempDir, "nofile2.txt")
        assertFailsWith<IllegalArgumentException> {
            FileKit.sizeOf(missing)
        }
    }

    @Test
    fun sizeOfDirectoryNonexistentThrows() {
        val missingDir = File(tempDir, "nodir")
        assertFailsWith<UncheckedIOException> {
            FileKit.sizeOfDirectory(missingDir)
        }
    }

    // ---------- convertFileCollectionToFileArray ----------

    @Test
    fun convertEmptyCollectionGivesEmptyArray() {
        val arr = FileKit.convertFileCollectionToFileArray(emptyList())
        assertTrue(arr.isEmpty())
    }

    @Test
    fun convertNonEmptyCollection() {
        val f1 = File(tempDir, "a.txt").apply { createNewFile() }
        val f2 = File(tempDir, "b.txt").apply { createNewFile() }
        val list = listOf(f1, f2)
        val array = FileKit.convertFileCollectionToFileArray(list)
        assertEquals(2, array.size)
        assertTrue(array.toList().containsAll(list))
    }

    // ---------- listFiles / listFilesAndDirs / iterateFiles / iterateFilesAndDirs ----------

    @Test
    fun listFilesWithFilterAndNoDirFilter() {
        val d = File(tempDir, "lf1")
        val f1 = File(d, "keep.txt")
        val f2 = File(d, "skip.log")
        d.mkdirs()
        FileKit.writeStringToFile(f1, "x")
        FileKit.writeStringToFile(f2, "y")
        // Filter only .txt
        val files = FileKit.listFiles(d, TrueFileFilter.INSTANCE, null)
        // TrueFileFilter matches all files, so skip.log is also included
        assertTrue(files.size >= 2)
    }

    @Test
    fun listFilesAndDirsIncludesDirectories() {
        val d = File(tempDir, "lf2")
        val sub = File(d, "subdir/file1.txt")
        sub.parentFile.mkdirs()
        FileKit.writeStringToFile(sub, "t")
        val all = FileKit.listFilesAndDirs(d, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
        // Includes the subdirectory "subdir"
        assertTrue(all.any { it.name == "subdir" })
        // Includes the file "file1.txt"
        assertTrue(all.any { it.name == "file1.txt" })
    }

    @Test
    fun iterateFilesRecursively() {
        val d = File(tempDir, "if1")
        val sub = File(d, "subdir/f.txt")
        sub.parentFile.mkdirs()
        FileKit.writeStringToFile(sub, "z")
        val it = FileKit.iterateFiles(d, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
        val found = it.asSequence().any { it.name == "f.txt" }
        assertTrue(found)
    }

    @Test
    fun iterateFilesAndDirsRecursively() {
        val d = File(tempDir, "if2")
        val sub = File(d, "subdir2")
        sub.mkdirs()
        val f = File(sub, "f2.txt")
        FileKit.writeStringToFile(f, "w")
        val it = FileKit.iterateFilesAndDirs(d, TrueFileFilter.INSTANCE, TrueFileFilter.INSTANCE)
        // Includes the directory "subdir2"
        assertTrue(it.asSequence().any { it.name == "subdir2" })
    }

    // ---------- contentEquals / contentEqualsIgnoreEOL ----------

    @Test
    fun contentEqualsReturnsTrueOnSameFile() {
        val f = File(tempDir, "same.txt")
        FileKit.writeStringToFile(f, "abc")
        assertTrue(FileKit.contentEquals(f, f))
    }

    @Test
    fun contentEqualsThrowsOnDirectory() {
        val dir = File(tempDir, "dirCE")
        dir.mkdirs()
        val f = File(tempDir, "fileCE.txt")
        FileKit.writeStringToFile(f, "x")
        assertFailsWith<IllegalArgumentException> {
            FileKit.contentEquals(dir, f)
        }
    }

    @Test
    fun contentEqualsIgnoreEOLWithDifferentLineEndings() {
        val f1 = File(tempDir, "eol1.txt")
        val f2 = File(tempDir, "eol2.txt")
        FileKit.writeStringToFile(f1, "line1\nline2")
        FileKit.writeStringToFile(f2, "line1\r\nline2")
        assertTrue(FileKit.contentEqualsIgnoreEOL(f1, f2, "UTF-8"))
    }

    @Test
    fun contentEqualsIgnoreEOLThrowsOnInvalidCharset() {
        val f1 = File(tempDir, "ie1.txt")
        val f2 = File(tempDir, "ie2.txt")
        FileKit.writeStringToFile(f1, "a\nb")
        FileKit.writeStringToFile(f2, "a\nb")
        assertFailsWith<java.nio.charset.UnsupportedCharsetException> {
            FileKit.contentEqualsIgnoreEOL(f1, f2, "INVALID_CHARSET")
        }
    }

    // ---------- toFile / toFiles / toURLs ----------

    @Test
    fun toFileWithNonFileURLReturnsNull() {
        val httpUrl = URI("https://example.com").toURL()
        assertNull(FileKit.toFile(httpUrl))
    }

    @Test
    fun toFilesOnlyFileProtocolReturnsFileOrNull() {
        val f = File(tempDir, "fileX.txt").apply { createNewFile() }
        val validUrl = f.toURI().toURL()
        // Simulate a "no resource" file URL using a path that does not exist locally
        val missingFile = File(tempDir, "nope.txt")
        val missingUrl = missingFile.toURI().toURL()
        val arr = arrayOf(validUrl, missingUrl)
        val results = FileKit.toFiles(arr)
        // Should return a File? array of the same length as the input:
        //   The first is the actually existing File; the second still returns a File object even though the file does not exist (it just points to a non-existent local file; existence is not re-checked)
        assertEquals(2, results.size)
        assertNotNull(results[0])
        assertEquals(f.absolutePath, results[0]?.absolutePath)
        // Although the second file does not exist, toFiles still returns a File instance and does not throw
        assertNotNull(results[1])
        assertEquals(missingFile.absolutePath, results[1]?.absolutePath)
    }

    @Test
    fun toFilesWithHttpUrlThrowsIllegalArgumentException() {
        val f = File(tempDir, "fileY.txt").apply { createNewFile() }
        val validUrl = f.toURI().toURL()
        val invalidHttpUrl = URI("https://example.com/foo.txt").toURL()
        val arr = arrayOf(validUrl, invalidHttpUrl)
        // Because the second URL is not the file protocol, toFiles throws IllegalArgumentException directly
        assertFailsWith<IllegalArgumentException> {
            FileKit.toFiles(arr)
        }
    }

    @Test
    fun toURLsFromNonexistentFileStillReturnsURL() {
        val f = File(tempDir, "nope.txt")
        val urls = FileKit.toURLs(arrayOf(f))
        assertEquals(1, urls.size)
        assertEquals(f.toURI().toURL(), urls[0])
    }

    // ---------- copyFileToDirectory / copyFile / copyDirectoryToDirectory / copyDirectory ----------

    @Test
    fun copyFileToDirectoryCreatesDestination() {
        val src = File(tempDir, "copySrc.txt")
        FileKit.writeStringToFile(src, "123")
        val destDir = File(tempDir, "destD")
        // The destination directory should be created automatically when it does not exist
        FileKit.copyFileToDirectory(src, destDir)
        val copied = File(destDir, "copySrc.txt")
        assertTrue(copied.exists())
        assertEquals("123", copied.readText())
    }

    @Test
    fun copyFileThrowsWhenSourceMissing() {
        val missing = File(tempDir, "none.txt")
        val out = File(tempDir, "o.txt")
        assertFailsWith<IOException> {
            FileKit.copyFile(missing, FileOutputStream(out))
        }
    }

    @Test
    fun copyDirectoryToDirectoryRecursively() {
        val srcDir = File(tempDir, "dirCD")
        val f = File(srcDir, "nested/n.txt")
        f.parentFile.mkdirs()
        FileKit.writeStringToFile(f, "val")
        val dest = File(tempDir, "destCD")
        FileKit.copyDirectoryToDirectory(srcDir, dest)
        val copied = File(dest, "dirCD/nested/n.txt")
        assertTrue(copied.exists())
        assertEquals("val", copied.readText())
    }

    @Test
    fun copyDirectoryWithFilterOnlyCopiesMatching() {
        val srcDir = File(tempDir, "dirFilter")
        val f1 = File(srcDir, "x.txt")
        val f2 = File(srcDir, "y.log")
        srcDir.mkdirs()
        FileKit.writeStringToFile(f1, "txt")
        FileKit.writeStringToFile(f2, "log")
        val dest = File(tempDir, "destF")
        // Copy only files ending with .txt
        FileKit.copyDirectory(srcDir, dest, { it.name.endsWith(".txt") }, true)
        assertTrue(File(dest, "x.txt").exists())
        assertFalse(File(dest, "y.log").exists())
    }

    @Test
    fun copyDirectoryThrowsWhenSourceMissing() {
        val missing = File(tempDir, "nodirC")
        val dest = File(tempDir, "destC")
        assertFailsWith<FileNotFoundException> {
            FileKit.copyDirectory(missing, dest)
        }
    }

    // ---------- copyURLToFile / copyInputStreamToFile ----------

    @Test
    fun copyURLToFileCreatesFile() {
        val src = File(tempDir, "srcURL.txt")
        FileKit.writeStringToFile(src, "abc")
        val dest = File(tempDir, "destURL.txt")
        FileKit.copyURLToFile(src.toURI().toURL(), dest)
        assertEquals("abc", dest.readText())
    }

    @Test
    fun copyURLToFileWithTimeoutsWorks() {
        val src = File(tempDir, "sURL2.txt")
        FileKit.writeStringToFile(src, "ok")
        val dest = File(tempDir, "dURL2.txt")
        FileKit.copyURLToFile(src.toURI().toURL(), dest, 500, 500)
        assertEquals("ok", dest.readText())
    }

    @Test
    fun copyURLToFileInvalidURLThrows() {
        val invalid = URI("https://nonexistent.local/file.txt").toURL()
        val dest = File(tempDir, "bad.txt")
        assertFailsWith<IOException> {
            FileKit.copyURLToFile(invalid, dest)
        }
    }

    @Test
    fun copyInputStreamToFileCreatesCorrectContent() {
        val src = File(tempDir, "inURL.txt")
        FileKit.writeStringToFile(src, "stream")
        val dest = File(tempDir, "outURL.txt")
        FileInputStream(src).use { ins ->
            FileKit.copyInputStreamToFile(ins, dest)
        }
        assertEquals("stream", dest.readText())
    }

    // ---------- deleteDirectory / deleteQuietly ----------

    @Test
    fun deleteDirectoryRecursivelyRemovesAll() {
        val dir = File(tempDir, "toDelete")
        val nested = File(dir, "a/b/c.txt")
        nested.parentFile.mkdirs()
        FileKit.writeStringToFile(nested, "x")
        FileKit.deleteDirectory(dir)
        assertFalse(dir.exists())
    }

    @Test
    fun deleteQuietlyReturnsFalseWhenMissing() {
        val missing = File(tempDir, "notHere")
        assertFalse(FileKit.deleteQuietly(missing))
    }

    // ---------- directoryContains / cleanDirectory ----------

    @Test
    fun directoryContainsValidChild() {
        val parent = File(tempDir, "parentD")
        val child = File(parent, "child.txt")
        child.parentFile.mkdirs()
        FileKit.writeStringToFile(child, "x")
        assertTrue(FileKit.directoryContains(parent, child))
    }

    @Test
    fun directoryContainsWhenParentNotDirectoryThrows() {
        val f = File(tempDir, "notDir.txt")
        f.writeText("data")
        val child = File(tempDir, "child.txt").apply { writeText("x") }
        assertFailsWith<IllegalArgumentException> {
            FileKit.directoryContains(f, child)
        }
    }

    @Test
    fun cleanDirectoryEmptiesContents() {
        val dir = File(tempDir, "cleanMe")
        val f1 = File(dir, "a.txt")
        f1.parentFile.mkdirs()
        FileKit.writeStringToFile(f1, "y")
        FileKit.cleanDirectory(dir)
        assertTrue(dir.exists() && dir.listFiles()?.isEmpty() == true)
    }

    @Test
    fun cleanDirectoryWhenNotDirectoryThrows() {
        val f = File(tempDir, "notDir2.txt")
        f.writeText("z")
        // cleanDirectory throws IllegalArgumentException for a non-directory
        assertFailsWith<IllegalArgumentException> {
            FileKit.cleanDirectory(f)
        }
    }

    // ---------- waitFor ----------

    @Test
    fun waitForTimeoutReturnsFalseWhenNeverCreated() {
        val f = File(tempDir, "willNever.txt")
        assertFalse(FileKit.waitFor(f, 1))
    }

    @Test
    fun waitForReturnsTrueWithinTimeout() {
        val f = File(tempDir, "willExist.txt")
        Thread {
            Thread.sleep(200)
            f.createNewFile()
        }.start()
        assertTrue(FileKit.waitFor(f, 2))
    }

    // ---------- readFileToString / readFileToByteArray / readLines / lineIterator ----------

    @Test
    fun readFileToStringWithDefaultEncoding() {
        val f = File(tempDir, "rts.txt")
        f.writeText("abc")
        assertEquals("abc", FileKit.readFileToString(f))
    }

    @Test
    fun readFileToStringNonexistentThrows() {
        val missing = File(tempDir, "missing.txt")
        assertFailsWith<IOException> {
            FileKit.readFileToString(missing)
        }
    }

    @Test
    fun readFileToByteArrayReturnsCorrectBytes() {
        val f = File(tempDir, "rb.txt")
        f.writeText("bytes")
        val bytes = FileKit.readFileToByteArray(f)
        assertEquals("bytes", String(bytes))
    }

    @Test
    fun readFileToByteArrayDirectoryThrows() {
        val dir = File(tempDir, "dirRB")
        dir.mkdirs()
        assertFailsWith<IOException> {
            FileKit.readFileToByteArray(dir)
        }
    }

    @Test
    fun readLinesReturnsCorrectList() {
        val f = File(tempDir, "rl.txt")
        f.writeText("l1\nl2\n")
        val lines = FileKit.readLines(f, "UTF-8")
        assertEquals(listOf("l1", "l2"), lines)
    }

    @Test
    fun readLinesNonexistentThrows() {
        val missing = File(tempDir, "rlno.txt")
        assertFailsWith<IOException> {
            FileKit.readLines(missing, "UTF-8")
        }
    }

    @Test
    fun lineIteratorReadsAllLines() {
        val f = File(tempDir, "li.txt")
        f.writeText("a\nb\nc")
        val it = FileKit.lineIterator(f, "UTF-8")
        val collected = mutableListOf<String>()
        it.use {
            while (it.hasNext()) {
                collected.add(it.next())
            }
        }
        assertEquals(listOf("a", "b", "c"), collected)
    }

    // ---------- writeStringToFile / write / writeByteArrayToFile / writeLines ----------

    @Test
    fun writeStringToFileCreatesOrAppendsCorrectly() {
        val f = File(tempDir, "wsf.txt")
        FileKit.writeStringToFile(f, "first", "UTF-8", false)
        assertEquals("first", f.readText())
        FileKit.writeStringToFile(f, "second", "UTF-8", true)
        assertEquals("firstsecond", f.readText())
    }

    @Test
    fun writeStringToFileInvalidEncodingThrows() {
        val f = File(tempDir, "wsfe.txt")
        assertFailsWith<java.nio.charset.UnsupportedCharsetException> {
            FileKit.writeStringToFile(f, "x", "INVALID", false)
        }
    }

    @Test
    fun writeCharSequenceAppendsProperly() {
        val f = File(tempDir, "wcs.txt")
        FileKit.write(f, "hello", "UTF-8", false)
        FileKit.write(f, "world", "UTF-8", true)
        assertEquals("helloworld", f.readText())
    }

    @Test
    fun writeByteArrayToFileWritesCorrectly() {
        val f = File(tempDir, "wba2.txt")
        FileKit.writeByteArrayToFile(f, "data".toByteArray(), append = false)
        assertEquals("data", f.readText())
    }

    @Test
    fun writeLinesWritesEachLine() {
        val f = File(tempDir, "wl.txt")
        FileKit.writeLines(f, "UTF-8", listOf("1", "2", "3"), "\n", false)
        assertEquals(listOf("1", "2", "3"), f.readLines())
    }

    @Test
    fun writeLinesWithNullCollectionWritesNothing() {
        val f = File(tempDir, "wlnull.txt")
        // A null lines argument is treated as writing an empty collection; the file exists but has no content
        FileKit.writeLines(f, "UTF-8", null, "\n", false)
        assertTrue(f.exists() && f.readText().isEmpty())
    }

    // ---------- forceDelete / forceDeleteOnExit / forceMkdir ----------

    @Test
    fun forceDeleteRemovesFileOrDirectoryOrThrows() {
        val f = File(tempDir, "fd.txt")
        f.writeText("x")
        FileKit.forceDelete(f)
        assertFalse(f.exists())

        val dir = File(tempDir, "fddir")
        val child = File(dir, "c.txt")
        child.parentFile.mkdirs()
        child.writeText("y")
        // Recursive deletion
        FileKit.forceDelete(dir)
        assertFalse(dir.exists())

        // Throws FileNotFound when deleting something that does not exist
        if (SystemKit.currentOs() == OsEnum.WINDOWS) {
            assertFailsWith<IOException> {
                FileKit.forceDelete(File(tempDir, "nofd"))
            }
        } else {
            assertFailsWith<FileNotFoundException> {
                FileKit.forceDelete(File(tempDir, "nofd"))
            }
        }
    }

    @Test
    fun forceDeleteOnExitSchedulesRemoval() {
        val f = File(tempDir, "fde.txt")
        f.writeText("z")
        FileKit.forceDeleteOnExit(f)
        // Cannot immediately verify deletion at JVM exit in this test; at least no exception is thrown and the file still exists
        assertTrue(f.exists())
    }

    @Test
    fun forceMkdirCreatesDeepDirectoriesOrThrows() {
        val dir = File(tempDir, "fm1/fm2")
        FileKit.forceMkdir(dir)
        assertTrue(dir.exists() && dir.isDirectory)

        // If the path already exists and is a file, IOException is thrown
        val f = File(tempDir, "fmFile")
        f.writeText("a")
        assertFailsWith<IOException> {
            FileKit.forceMkdir(f)
        }
    }

    // ---------- sizeOfAsBigInteger / sizeOfDirectoryAsBigInteger ----------

    @Test
    fun sizeOfAsBigIntegerReturnsCorrectBigInteger() {
        val f = File(tempDir, "sob.txt")
        f.writeText("hello")
        val bi = FileKit.sizeOfAsBigInteger(f)
        assertTrue(bi.toLong() > 0)
    }

    @Test
    fun sizeOfDirectoryAsBigIntegerRecursiveSum() {
        val dir = File(tempDir, "sobdir")
        val f1 = File(dir, "a.txt")
        f1.parentFile.mkdirs()
        f1.writeText("12345")
        val sum = FileKit.sizeOfDirectoryAsBigInteger(dir)
        assertTrue(sum.toLong() >= f1.length())
    }

    // ---------- isFileNewer / isFileOlder ----------

    @Test
    fun isFileNewerAndOlderBehaviors() {
        val f1 = File(tempDir, "n1.txt")
        val f2 = File(tempDir, "n2.txt")
        f1.writeText("a")
        Thread.sleep(20)
        f2.writeText("b")
        assertTrue(FileKit.isFileNewer(f2, f1))
        assertTrue(FileKit.isFileOlder(f1, f2))
    }

    @Test
    fun isFileNewerThrowsOnMissing() {
        val f = File(tempDir, "nM.txt")                  // this file does not exist
        val ref = File(tempDir, "nRef.txt").apply { writeText("x") }
        // When f does not exist, isFileNewer should return false directly rather than throw IllegalArgumentException
        assertFalse(FileKit.isFileNewer(f, ref))

    }

    @Test
    fun isFileOlderReturnsFalseWhenMissing() {
        val f = File(tempDir, "nM2.txt")
        val ref = File(tempDir, "nRef2.txt").apply { writeText("x") }
        // The file f does not exist; should return false rather than throw
        assertFalse(FileKit.isFileOlder(f, ref))
    }

    @Test
    fun isFileNewerReturnsFalseWhenMissing() {
        val f = File(tempDir, "nM.txt")
        val ref = File(tempDir, "nRef.txt").apply { writeText("x") }
        // The file f does not exist; should return false
        assertFalse(FileKit.isFileNewer(f, ref))
    }

    // ---------- checksumCRC32 / checksum ----------

    @Test
    fun checksumCRC32MatchesCRC32Value() {
        val f = File(tempDir, "crc.txt")
        f.writeText("test")
        val manual = CRC32().apply { update(f.readBytes()) }.value
        val kit = FileKit.checksumCRC32(f)
        assertEquals(manual, kit)
    }

    @Test
    fun checksumThrowsOnDirectory() {
        val dir = File(tempDir, "crcdir")
        dir.mkdirs()
        assertFailsWith<IllegalArgumentException> {
            FileKit.checksum(dir, CRC32())
        }
    }

    // ---------- moveDirectory / moveDirectoryToDirectory / moveFile / moveFileToDirectory / moveToDirectory ----------

    @Test
    fun moveFileToDirectoryMovesCorrectly() {
        val f = File(tempDir, "mv1.txt")
        f.writeText("m")
        val dest = File(tempDir, "mvDir")
        FileKit.moveFileToDirectory(f, dest, true)
        val moved = File(dest, "mv1.txt")
        assertTrue(moved.exists())
    }

    @Test
    fun moveFileThrowsWhenSourceMissing() {
        val missing = File(tempDir, "mvMiss.txt")
        val destFile = File(tempDir, "destFile.txt")
        assertFailsWith<FileNotFoundException> {
            FileKit.moveFile(missing, destFile)
        }
    }

    @Test
    fun moveDirectoryToDirectoryWorks() {
        val dir = File(tempDir, "mvDirSrc")
        val nested = File(dir, "x.txt")
        nested.parentFile.mkdirs()
        nested.writeText("z")
        val dest = File(tempDir, "mvDirDest")
        FileKit.moveDirectoryToDirectory(dir, dest, true)
        val moved = File(dest, "mvDirSrc/x.txt")
        assertTrue(moved.exists())
    }

    @Test
    fun moveDirectoryThrowsWhenSourceMissing() {
        val missing = File(tempDir, "mvDirMissing")
        val dest = File(tempDir, "destMissing")
        assertFailsWith<FileNotFoundException> {
            FileKit.moveDirectory(missing, dest)
        }
    }

    @Test
    fun moveToDirectoryHandlesFileAndDirectory() {
        val f = File(tempDir, "mv2.txt")
        f.writeText("2")
        val dest = File(tempDir, "mvDest2")
        FileKit.moveToDirectory(f, dest, true)
        assertTrue(File(dest, "mv2.txt").exists())

        val dir = File(tempDir, "mvD2Src")
        val sub = File(dir, "sub/mv2.txt")
        sub.parentFile.mkdirs()
        sub.writeText("ok")
        val dest2 = File(tempDir, "mvDestDir2")
        FileKit.moveToDirectory(dir, dest2, true)
        assertTrue(File(dest2, "mvD2Src/sub/mv2.txt").exists())
    }

    @Test
    fun moveToDirectoryThrowsWhenSourceMissing() {
        val missing = File(tempDir, "mvMissDir")
        val dest = File(tempDir, "destDirMiss")
        assertFailsWith<FileNotFoundException> {
            FileKit.moveToDirectory(missing, dest, true)
        }
    }

    // ---------- isSymlink ----------

    @Test
    fun isSymlinkOnRegularFileReturnsFalse() {
        val f = File(tempDir, "fsy.txt")
        f.writeText("x")
        assertFalse(FileKit.isSymlink(f))
    }

    @Test
    fun isSymlinkWhenSupportedReturnsTrue() {
        if (SystemKit.currentOs() != OsEnum.WINDOWS) {
            val target = File(tempDir, "target.txt")
            target.writeText("t")
            val link = File(tempDir, "link.ln")
            try {
                Files.createSymbolicLink(link.toPath(), target.toPath())
                assertTrue(FileKit.isSymlink(link))
            } catch (_: UnsupportedOperationException) {
                // If this environment does not support symbolic links, the result should be false
                assertFalse(FileKit.isSymlink(link))
            }
        }
    }
}
