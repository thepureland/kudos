package io.kudos.base.security

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue


/**
 * test for BarcodeKit
 *
 * Primary assertions: the PNG header is correct, the bytes can be decoded again by ImageIO, and the size matches
 * what was requested.
 * Does not perform a zxing decode round-trip (zxing-core does not include a j2se reader; pulling it in just for
 * tests is not worth it).
 *
 * @author K
 * @since 1.0.0
 */
class BarcodeKitTest {

    /** Standard PNG file signature: a fixed 8-byte prefix. */
    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    @Test
    fun qrcodePng_returnsValidPngBytes() {
        val bytes = BarcodeKit.qrcodePng("otpauth://totp/test:alice?secret=ABC")
        assertTrue(bytes.size > PNG_SIGNATURE.size, "PNG should contain at least the signature plus data")
        val signature = bytes.copyOfRange(0, PNG_SIGNATURE.size)
        assertTrue(signature.contentEquals(PNG_SIGNATURE), "the first 8 bytes should be the PNG file signature")
    }

    @Test
    fun qrcodePng_imageHasRequestedSize() {
        val size = 256
        val bytes = BarcodeKit.qrcodePng("hello", size = size)
        val img = ImageIO.read(ByteArrayInputStream(bytes))
        assertEquals(size, img.width)
        assertEquals(size, img.height)
    }

    @Test
    fun qrcodePng_emptyText_throws() {
        assertFailsWith<IllegalArgumentException> { BarcodeKit.qrcodePng("") }
    }

    @Test
    fun qrcodePng_zeroSize_throws() {
        assertFailsWith<IllegalArgumentException> { BarcodeKit.qrcodePng("x", size = 0) }
    }

    @Test
    fun qrcodePng_negativeMargin_throws() {
        assertFailsWith<IllegalArgumentException> { BarcodeKit.qrcodePng("x", margin = -1) }
    }
}
