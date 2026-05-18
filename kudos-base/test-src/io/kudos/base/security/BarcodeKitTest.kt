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
 * 主要校验：PNG 文件头正确、可被 ImageIO 重新解码、尺寸符合预期。
 * 不做 zxing decode 回环（zxing-core 不带 j2se reader；引入只是为了测试不划算）。
 *
 * @author K
 * @since 1.0.0
 */
class BarcodeKitTest {

    /** 标准 PNG 文件签名：8 字节固定开头。 */
    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    @Test
    fun qrcodePng_returnsValidPngBytes() {
        val bytes = BarcodeKit.qrcodePng("otpauth://totp/test:alice?secret=ABC")
        assertTrue(bytes.size > PNG_SIGNATURE.size, "PNG 应该至少包含签名 + 数据")
        val signature = bytes.copyOfRange(0, PNG_SIGNATURE.size)
        assertTrue(signature.contentEquals(PNG_SIGNATURE), "前 8 字节应是 PNG 文件签名")
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
