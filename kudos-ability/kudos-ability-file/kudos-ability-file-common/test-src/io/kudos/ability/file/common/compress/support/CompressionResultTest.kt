package io.kudos.ability.file.common.compress.support

import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [CompressionResult].
 *
 * Covers:
 *  - both constructors and getOutputFilePath (set vs unset -> IllegalArgumentException)
 *  - writeTo: flushes bytes to disk; no-op when outputStream is null;
 *    swallows (logs) IO errors for an unwritable path without throwing
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class CompressionResultTest {

    @Test
    fun primaryConstructor_hasNoOutputFilePath() {
        val result = CompressionResult(null, "image/png")
        assertEquals("image/png", result.mimeType)
        assertNull(result.outputStream)
        val e = assertFailsWith<IllegalArgumentException> { result.getOutputFilePath() }
        assertTrue(e.message!!.contains("outputFilePath is null"))
    }

    @Test
    fun secondaryConstructor_exposesOutputFilePath() {
        val os = ByteArrayOutputStream()
        val result = CompressionResult(os, "/tmp/a.jpg", "image/jpeg")
        assertEquals("/tmp/a.jpg", result.getOutputFilePath())
        assertEquals("image/jpeg", result.mimeType)
        assertEquals(os, result.outputStream)
    }

    @Test
    fun writeTo_flushesBytesToFile(@TempDir tempDir: File) {
        val os = ByteArrayOutputStream().apply { write(byteArrayOf(10, 20, 30)) }
        val target = File(tempDir, "out.bin")
        CompressionResult(os, target.absolutePath, "application/octet-stream").writeTo()
        assertTrue(target.exists())
        assertEquals(listOf<Byte>(10, 20, 30), target.readBytes().toList())
    }

    @Test
    fun writeTo_isNoOpWhenOutputStreamIsNull(@TempDir tempDir: File) {
        val target = File(tempDir, "never.bin")
        CompressionResult(null, target.absolutePath, "image/png").writeTo()
        assertFalse(target.exists())
    }

    @Test
    fun writeTo_swallowsIoErrorsWithoutThrowing(@TempDir tempDir: File) {
        val os = ByteArrayOutputStream().apply { write(1) }
        // a directory cannot be opened as FileOutputStream -> exception path is logged, not thrown
        CompressionResult(os, tempDir.absolutePath, "image/png").writeTo()
        // also exercise the "outputFilePath = null" failure inside writeTo
        CompressionResult(os, "image/png").writeTo()
    }

}
