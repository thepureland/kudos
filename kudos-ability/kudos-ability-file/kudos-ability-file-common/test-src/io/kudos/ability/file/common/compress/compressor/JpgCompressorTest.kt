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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [JpgCompressor].
 *
 * Regression locks:
 *  - default CompressionConfig (width=0 / height=0) must NOT be passed to
 *    Thumbnailator as `size(0, 0)` (the old implementation crashed with
 *    IllegalArgumentException for every JPEG once compression was enabled)
 *  - configured width/height smaller than the original must shrink the output
 *  - non-image input must fail with a clear IllegalArgumentException instead of an NPE
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class JpgCompressorTest {

    private fun jpegBytes(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()
        graphics.color = Color.RED
        graphics.fillRect(0, 0, width, height)
        graphics.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "jpg", baos)
        return baos.toByteArray()
    }

    @Test
    fun compress_withDefaultConfig_keepsOriginalDimensions() {
        val config = CompressionConfig().apply { enabled = true }
        val result = JpgCompressor()
            .compress(ByteArrayInputStream(jpegBytes(40, 30)), "/tmp/a.jpg", config)

        val out = assertNotNull(result.outputStream)
        assertTrue(out.size() > 0)
        assertEquals("image/jpeg", result.mimeType)
        assertEquals("/tmp/a.jpg", result.getOutputFilePath())

        val reread = assertNotNull(ImageIO.read(ByteArrayInputStream(out.toByteArray())))
        assertEquals(40, reread.width)
        assertEquals(30, reread.height)
    }

    @Test
    fun compress_withSmallerTarget_shrinksImage() {
        val config = CompressionConfig().apply {
            enabled = true
            width = 20
            height = 15
            quality = 0.8f
        }
        val result = JpgCompressor()
            .compress(ByteArrayInputStream(jpegBytes(40, 30)), "/tmp/b.jpg", config)

        val out = assertNotNull(result.outputStream)
        val reread = assertNotNull(ImageIO.read(ByteArrayInputStream(out.toByteArray())))
        assertEquals(20, reread.width)
        assertEquals(15, reread.height)
    }

    @Test
    fun compress_withTargetLargerThanOriginal_doesNotUpscale() {
        val config = CompressionConfig().apply {
            enabled = true
            width = 800
            height = 600
        }
        val result = JpgCompressor()
            .compress(ByteArrayInputStream(jpegBytes(40, 30)), "/tmp/c.jpg", config)

        val out = assertNotNull(result.outputStream)
        val reread = assertNotNull(ImageIO.read(ByteArrayInputStream(out.toByteArray())))
        assertEquals(40, reread.width)
        assertEquals(30, reread.height)
    }

    @Test
    fun compress_rejectsNonImageStream() {
        val config = CompressionConfig().apply { enabled = true }
        assertFailsWith<IllegalArgumentException> {
            JpgCompressor().compress(ByteArrayInputStream("not an image".toByteArray()), "/tmp/d.jpg", config)
        }
    }

}
