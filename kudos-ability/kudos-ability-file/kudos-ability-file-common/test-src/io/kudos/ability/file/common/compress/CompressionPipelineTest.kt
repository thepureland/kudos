package io.kudos.ability.file.common.compress

import io.kudos.ability.file.common.compress.support.CompressionConfig
import org.junit.jupiter.api.io.TempDir
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Tests for [CompressionPipeline].
 *
 * Covers the decision tree of both entry points:
 *  - compress: disabled -> passthrough result; non-image -> passthrough result
 *    (contentType inferred from extension); enabled image -> real jpeg compression
 *  - compressAndOutputFile: disabled/non-image -> byte-identical copy to target;
 *    enabled image -> compressed file written to target path
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class CompressionPipelineTest {

    private fun jpegBytes(width: Int = 32, height: Int = 24): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.BLUE
        g.fillRect(0, 0, width, height)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        return baos.toByteArray()
    }

    @Test
    fun compress_disabled_returnsPassthroughResult() {
        val config = CompressionConfig() // enabled = false
        val result = CompressionPipeline.compress(ByteArrayInputStream(jpegBytes()), "/tmp/a.jpg", config)
        assertNull(result.outputStream)
        assertEquals("/tmp/a.jpg", result.getOutputFilePath())
        assertEquals("image/jpeg", result.mimeType)
    }

    @Test
    fun compress_nonImage_returnsPassthroughWithInferredContentType() {
        val config = CompressionConfig().apply { enabled = true }
        val result = CompressionPipeline.compress(
            ByteArrayInputStream("plain text".toByteArray()), "/tmp/a.pdf", config
        )
        assertNull(result.outputStream)
        assertEquals("/tmp/a.pdf", result.getOutputFilePath())
        assertEquals("application/pdf", result.mimeType)
    }

    @Test
    fun compress_unknownExtension_fallsBackToOctetStream() {
        val config = CompressionConfig() // disabled
        val result = CompressionPipeline.compress(
            ByteArrayInputStream(ByteArray(0)), "/tmp/file.zzz", config
        )
        assertEquals("application/octet-stream", result.mimeType)
    }

    @Test
    fun compress_enabledJpeg_runsRealCompression() {
        val config = CompressionConfig().apply {
            enabled = true
            quality = 0.5f
        }
        val result = CompressionPipeline.compress(ByteArrayInputStream(jpegBytes()), "/tmp/b.jpg", config)
        val out = assertNotNull(result.outputStream)
        assertTrue(out.size() > 0)
        assertEquals("image/jpeg", result.mimeType)
        assertEquals("/tmp/b.jpg", result.getOutputFilePath())
        val reread = assertNotNull(ImageIO.read(ByteArrayInputStream(out.toByteArray())))
        assertEquals(32, reread.width)
        assertEquals(24, reread.height)
    }

    @Test
    fun compressAndOutputFile_disabled_copiesBytesVerbatim(@TempDir tempDir: File) {
        val source = jpegBytes()
        val target = File(tempDir, "copy.jpg")
        CompressionPipeline.compressAndOutputFile(
            ByteArrayInputStream(source), target.absolutePath, CompressionConfig()
        )
        assertTrue(target.exists())
        assertTrue(source.contentEquals(target.readBytes()))
    }

    @Test
    fun compressAndOutputFile_nonImage_copiesBytesVerbatim(@TempDir tempDir: File) {
        val source = "hello 世界".toByteArray()
        val target = File(tempDir, "note.txt")
        CompressionPipeline.compressAndOutputFile(
            ByteArrayInputStream(source), target.absolutePath, CompressionConfig().apply { enabled = true }
        )
        assertTrue(source.contentEquals(target.readBytes()))
    }

    @Test
    fun compressAndOutputFile_enabledJpeg_writesDecodableCompressedFile(@TempDir tempDir: File) {
        val target = File(tempDir, "c.jpg")
        val config = CompressionConfig().apply {
            enabled = true
            quality = 0.6f
        }
        CompressionPipeline.compressAndOutputFile(
            ByteArrayInputStream(jpegBytes(48, 36)), target.absolutePath, config
        )
        assertTrue(target.exists())
        val reread = assertNotNull(ImageIO.read(target))
        assertEquals(48, reread.width)
        assertEquals(36, reread.height)
    }

}
