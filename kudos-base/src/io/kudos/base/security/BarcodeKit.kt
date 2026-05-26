package io.kudos.base.security

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


/**
 * QR code / barcode rendering utility.
 *
 * Depends only on `zxing-core` (already in kudos-base's `api` dependencies); does not pull in `zxing-javase`
 * (which provides [com.google.zxing.client.j2se.MatrixToImageWriter]) — encodes via `java.awt` +
 * `ImageIO` instead, to avoid bloating dependencies.
 *
 * Typical usage:
 *
 * ```kotlin
 * val png = BarcodeKit.qrcodePng("otpauth://totp/kudos:alice?secret=ABCDEF&issuer=kudos")
 * // Write png to the HTTP response (Content-Type: image/png)
 * ```
 *
 * @author K
 * @since 1.0.0
 */
object BarcodeKit {

    /** Default QR code side length (pixels). */
    const val DEFAULT_SIZE = 200

    /** Default error-correction level. L=Low 7%, M=Medium 15%, Q=Quartile 25%, H=High 30%. M is sufficient for short text. */
    private val DEFAULT_EC_LEVEL = ErrorCorrectionLevel.M

    /** Default quiet-zone (white border) modules around the code. */
    private const val DEFAULT_MARGIN = 1

    /**
     * Generates a QR code as a PNG byte array.
     *
     * @param text the text carried by the QR code; a common use case is an `otpauth://...` URL
     * @param size QR code side length in pixels, > 0
     * @param margin number of quiet-zone modules, starting at 0; too small may be rejected by some apps, too large wastes space
     * @param errorCorrectionLevel error-correction level
     * @return PNG binary data
     */
    fun qrcodePng(
        text: String,
        size: Int = DEFAULT_SIZE,
        margin: Int = DEFAULT_MARGIN,
        errorCorrectionLevel: ErrorCorrectionLevel = DEFAULT_EC_LEVEL,
    ): ByteArray {
        require(text.isNotEmpty()) { "QR code text must not be empty" }
        require(size > 0) { "QR code side length must be > 0, got ${size}" }
        require(margin >= 0) { "margin must be >= 0, got ${margin}" }

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to margin,
            EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
        )
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)

        // Two-color bitmap: black pixels indicate filled modules, white indicates empty. ARGB int encoding: 0xFFRRGGBB
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val image = BufferedImage(matrix.width, matrix.height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                image.setRGB(x, y, if (matrix.get(x, y)) black else white)
            }
        }

        val out = ByteArrayOutputStream()
        check(ImageIO.write(image, "PNG", out)) { "ImageIO could not find a PNG writer; please check your JRE configuration" }
        return out.toByteArray()
    }
}
