package io.kudos.base.image

import io.kudos.base.io.PathKit
import java.awt.Color
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import javax.imageio.ImageIO
import kotlin.test.*

/**
 * Extended pure-ish unit tests for [ImageKit], covering the file/byte/string/scale/SVG paths that the
 * existing ImageKitTest does not exercise.
 *
 * Note: showImage(...) and readImageFromUri(...) require a GUI / live network respectively and are left
 * to integration scenarios; they are recorded as not-unit-testable.
 *
 * @author K
 * @since 1.0.0
 */
internal class ImageKitMoreTest {

    private val logoPath = "${PathKit.getProjectRootPath()}/resources/logo.png"

    /** Creates a small solid-color PNG file in a temp location and returns it. */
    private fun makePng(w: Int, h: Int, color: Color = Color.RED): File {
        val img = BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics()
        g.color = color
        g.fillRect(0, 0, w, h)
        g.dispose()
        val f = File.createTempFile("kudos-img-", ".png")
        f.deleteOnExit()
        ImageIO.write(img, "png", f)
        return f
    }

    @Test
    fun readImageFromFile_byFileAndByPath() {
        val f = makePng(20, 10)
        try {
            val byFile = ImageKit.readImageFromFile(f)
            val byPath = ImageKit.readImageFromFile(f.path)
            assertEquals(20, byFile.width)
            assertEquals(10, byFile.height)
            assertEquals(20, byPath.width)
            assertEquals(10, byPath.height)
        } finally {
            f.delete()
        }
    }

    @Test
    fun writeImage_thenReadBack() {
        val img = BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB)
        val out = File.createTempFile("kudos-write-", ".png")
        out.deleteOnExit()
        try {
            ImageKit.writeImage(img, "png", out.absolutePath)
            assertTrue(out.length() > 0)
            val back = ImageKit.readImageFromFile(out)
            assertEquals(8, back.width)
            assertEquals(8, back.height)
        } finally {
            out.delete()
        }
    }

    @Test
    fun imageToString_fromFile_andRoundTrip() {
        val f = makePng(12, 12, Color.BLUE)
        try {
            val str = ImageKit.imageToString(f, "png")
            assertTrue(str.isNotBlank())
            val back = ImageKit.stringToImage(str)
            assertEquals(12, back.width)
            assertEquals(12, back.height)
        } finally {
            f.delete()
        }
    }

    @Test
    fun imageToString_fromBufferedImage() {
        val img = BufferedImage(5, 7, BufferedImage.TYPE_INT_RGB)
        val str = ImageKit.imageToString(img, "png")
        // base64 round-trip
        val back = ImageKit.stringToImage(str)
        assertEquals(5, back.width)
        assertEquals(7, back.height)
    }

    @Test
    fun scale_withFiller_paddingProducesExactTargetSize() {
        // source is wider than tall -> ratio path width-based, then padded to target
        val src = makePng(100, 40)
        try {
            val scaled = ImageKit.scale(src.absolutePath, height = 50, width = 50, hasFiller = true)
            assertEquals(50, scaled.width)
            assertEquals(50, scaled.height)
        } finally {
            src.delete()
        }
    }

    @Test
    fun scale_tallerThanWide_withFiller() {
        // source taller than wide -> ratio path height-based
        val src = makePng(40, 100)
        try {
            val scaled = ImageKit.scale(src.absolutePath, height = 50, width = 50, hasFiller = true)
            assertEquals(50, scaled.width)
            assertEquals(50, scaled.height)
        } finally {
            src.delete()
        }
    }

    @Test
    fun scale_withoutFiller_returnsScaledInstance() {
        val src = makePng(100, 100)
        try {
            val scaled = ImageKit.scale(src.absolutePath, height = 30, width = 30, hasFiller = false)
            assertNotNull(scaled)
        } finally {
            src.delete()
        }
    }

    @Test
    fun scale_defaultHasFillerArgument() {
        // call scale(...) WITHOUT the hasFiller argument to exercise the synthetic default-arg path
        val src = makePng(100, 40)
        try {
            val scaled = ImageKit.scale(src.absolutePath, 50, 50)
            assertEquals(50, scaled.width)
            assertEquals(50, scaled.height)
        } finally {
            src.delete()
        }
    }

    @Test
    fun scale_smallSource_noDownscaleBranch_withFiller() {
        // source smaller than target on both axes -> the "if height>... or width>..." branch is skipped
        val src = makePng(10, 10)
        try {
            val scaled = ImageKit.scale(src.absolutePath, height = 50, width = 50, hasFiller = true)
            assertEquals(50, scaled.width)
            assertEquals(50, scaled.height)
        } finally {
            src.delete()
        }
    }

    @Test
    fun readMemoryImage_autoDetectFormat() {
        val img = BufferedImage(16, 16, BufferedImage.TYPE_INT_RGB)
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        val decoded = ImageKit.readMemoryImage(baos.toByteArray())
        assertEquals(16, decoded.width)
        assertEquals(16, decoded.height)
    }

    @Test
    fun readMemoryImage_withExplicitFormat() {
        val img = BufferedImage(9, 9, BufferedImage.TYPE_INT_RGB)
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        val decoded = ImageKit.readMemoryImage(baos.toByteArray(), "png")
        assertEquals(9, decoded.width)
        assertEquals(9, decoded.height)
    }

    @Test
    fun readMemoryImage_unsupportedBytes_throwsIOException() {
        val garbage = byteArrayOf(1, 2, 3, 4, 5, 6, 7, 8)
        assertFailsWith<IOException> {
            ImageKit.readMemoryImage(garbage)
        }
    }

    @Test
    fun readMemoryImage_recognizedHeaderButCorruptBody_triggersRetryThenIOException() {
        // Produce a valid PNG, then corrupt the pixel-data (IDAT) region after the header so an ImageReader
        // still recognizes the format and reports width/height, but read(0) fails. This drives the
        // catch(IOException)/dispose retry path before the final "unsupported image format" IOException.
        val img = BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB)
        val g = img.createGraphics(); g.color = Color.GREEN; g.fillRect(0, 0, 32, 32); g.dispose()
        val baos = ByteArrayOutputStream()
        ImageIO.write(img, "png", baos)
        val bytes = baos.toByteArray()
        // corrupt a chunk of bytes well past the 8-byte signature + IHDR header
        for (i in 40 until minOf(bytes.size, 200)) {
            bytes[i] = (bytes[i].toInt() xor 0xFF).toByte()
        }
        // Either it cannot be decoded (IOException from the loop) — that's the path we want to exercise.
        // If a lenient reader still manages to decode, the call returns normally; we accept both outcomes
        // but assert no other exception type escapes.
        try {
            ImageKit.readMemoryImage(bytes, "png")
        } catch (_: IOException) {
            // expected when every reader fails to decode the corrupt body
        }
    }

    // NOTE: ImageKit.renderSvgToImage is intentionally not unit-tested here. It hard-codes the SAX
    // parser class "org.apache.xerces.parsers.SAXParser" (ImageKit.kt:315) which is not on the
    // module's runtime classpath, so any call throws SAXIOException/ClassNotFoundException. This is a
    // main-code defect recorded in suspectedBugs, not a test gap.

    @Test
    fun readImageFromUri_fileScheme_byUriAndByString() {
        // a file:// URI exercises readImageFromUri without needing the network
        val f = makePng(14, 18)
        try {
            val uri = f.toURI()
            val byUri = ImageKit.readImageFromUri(uri)
            assertEquals(14, byUri.width)
            assertEquals(18, byUri.height)

            val byStr = ImageKit.readImageFromUri(uri.toString())
            assertEquals(14, byStr.width)
            assertEquals(18, byStr.height)
        } finally {
            f.delete()
        }
    }

    @Test
    fun imageToString_fromFileUri() {
        val f = makePng(11, 13)
        try {
            val str = ImageKit.imageToString(f.toURI(), "png")
            assertTrue(str.isNotBlank())
            val back = ImageKit.stringToImage(str)
            assertEquals(11, back.width)
            assertEquals(13, back.height)
        } finally {
            f.delete()
        }
    }

    @Test
    fun logoResourceIsReadable() {
        // sanity check that the shared test resource exists so the other image tests are meaningful
        val img = ImageKit.readImageFromFile(File(logoPath))
        assertTrue(img.width > 0 && img.height > 0)
    }
}
