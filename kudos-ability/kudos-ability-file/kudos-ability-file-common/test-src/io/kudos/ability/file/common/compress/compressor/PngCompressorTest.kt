package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [PngCompressor].
 *
 * Covers:
 *  - support() inherited from [JpgCompressor] mirrors config.enabled
 *  - compress() produces a decodable PNG with original pixel dimensions
 *    (PNG pipeline performs no resizing), correct mime type and path
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class PngCompressorTest {

    private fun pngBytes(width: Int, height: Int): ByteArray {
        val image = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)
        val g = image.createGraphics()
        g.color = Color.GREEN
        g.fillRect(0, 0, width, height)
        g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(image, "png", baos)
        return baos.toByteArray()
    }

    @Test
    fun support_mirrorsEnabledFlag() {
        val compressor = PngCompressor()
        assertTrue(compressor.support(CompressionConfig().apply { enabled = true }))
        assertFalse(compressor.support(CompressionConfig()))
    }

    @Test
    fun compress_producesDecodablePngKeepingDimensions() {
        val config = CompressionConfig().apply { enabled = true }
        val result = PngCompressor().compress(ByteArrayInputStream(pngBytes(25, 17)), "/tmp/icon.png", config)

        val out = assertNotNull(result.outputStream)
        assertTrue(out.size() > 0)
        assertEquals("image/png", result.mimeType)
        assertEquals("/tmp/icon.png", result.getOutputFilePath())

        val reread = assertNotNull(ImageIO.read(ByteArrayInputStream(out.toByteArray())))
        assertEquals(25, reread.width)
        assertEquals(17, reread.height)
    }

}
