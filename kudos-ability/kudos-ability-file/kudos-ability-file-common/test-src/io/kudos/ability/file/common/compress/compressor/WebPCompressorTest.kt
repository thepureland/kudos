package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [WebPCompressor].
 *
 * Covers:
 *  - support(): true only when enabled && webp (full truth table)
 *  - compress(): encodes to WebP bytes (RIFF/WEBP magic), renames target by
 *    appending `.webp`, returns `image/webp` mime type
 *  - non-image input rejected with IllegalArgumentException
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebPCompressorTest {

    private fun jpegBytes(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val g = image.createGraphics()
        g.color = Color.ORANGE
        g.fillRect(0, 0, width, height)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        return baos.toByteArray()
    }

    @Test
    fun support_truthTable() {
        val compressor = WebPCompressor()
        assertTrue(compressor.support(CompressionConfig().apply { enabled = true; webp = true }))
        assertFalse(compressor.support(CompressionConfig().apply { enabled = true; webp = false }))
        assertFalse(compressor.support(CompressionConfig().apply { enabled = false; webp = true }))
        assertFalse(compressor.support(CompressionConfig()))
    }

    @Test
    fun compress_encodesWebpAndAppendsSuffix() {
        val config = CompressionConfig().apply { enabled = true; webp = true }
        val result = WebPCompressor().compress(ByteArrayInputStream(jpegBytes(20, 10)), "/tmp/pic.jpg", config)

        assertEquals("image/webp", result.mimeType)
        assertEquals("/tmp/pic.jpg.webp", result.getOutputFilePath())

        val bytes = assertNotNull(result.outputStream).toByteArray()
        assertTrue(bytes.size > 12)
        // WebP container: "RIFF" .... "WEBP"
        assertEquals("RIFF", String(bytes, 0, 4, Charsets.US_ASCII))
        assertEquals("WEBP", String(bytes, 8, 4, Charsets.US_ASCII))
    }

    @Test
    fun compress_rejectsNonImageStream() {
        val config = CompressionConfig().apply { enabled = true; webp = true }
        assertFailsWith<IllegalArgumentException> {
            WebPCompressor().compress(ByteArrayInputStream("garbage".toByteArray()), "/tmp/x.jpg", config)
        }
    }

}
