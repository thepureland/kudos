package io.kudos.base.image.barcode

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import io.kudos.base.image.ImageKit
import io.kudos.base.io.FilenameKit
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO


/**
 * QR-code utility class.
 *
 * @author https://www.cnblogs.com/demon7715/p/10984160.html
 * @author K
 * @since 1.0.0
 */
object QrCodeKit {

    /** ZXing QR-code writer; [MultiFormatWriter] is thread-safe and can be reused as a singleton. */
    private val mutiWriter = MultiFormatWriter()

    /**
     * Generate a (square) QR code and save it to the specified file.
     *
     * @param content the content encoded in the QR code, typically a URL
     * @param destImagePath the destination file path
     * @param logoImagePath the logo image path
     * @param sideLength side length of the (square) QR-code image, default 300 pixels
     * @param logoSideLength side length of the (square) logo image, default is 1/5 of the QR-code side length
     * @param margin page margin width in pixels, default 2 pixels
     * @author K
     * @since 1.0.0
     */
    fun genQrCode(
        content: String, destImagePath: String, logoImagePath: String, sideLength: Int = 300,
        logoSideLength: Int = sideLength / 5, margin: Int = 0
    ) {
        val formatName = FilenameKit.getExtension(destImagePath)
        ImageIO.write(
            genQrCode(content, logoImagePath, sideLength, logoSideLength, margin),
            formatName, File(destImagePath)
        )
    }


    /**
     * Generate a (square) QR code.
     *
     * @param content the content encoded in the QR code, typically a URL
     * @param logoImagePath the logo image path
     * @param sideLength side length of the (square) QR-code image, default 300 pixels
     * @param logoSideLength side length of the (square) logo image, default is 1/5 of the QR-code side length
     * @param margin page margin width in pixels, default 2 pixels
     * @return the QR-code BufferedImage
     * @author K
     * @since 1.0.0
     */
    fun genQrCode(
        content: String, logoImagePath: String,
        sideLength: Int = 300, logoSideLength: Int = sideLength / 5, margin: Int = 0
    ): BufferedImage {
        val logoImage = ImageKit.scale(logoImagePath, logoSideLength, logoSideLength, true)
        return genQrCode(content, logoImage, sideLength, margin)
    }

    /**
     * Generate a (square) QR code.
     *
     * @param content the content encoded in the QR code, typically a URL
     * @param logoImage the logo image
     * @param sideLength side length of the (square) QR-code image, default 300 pixels
     * @param margin page margin width in pixels, default 2 pixels
     * @return the QR-code BufferedImage
     * @author https://www.cnblogs.com/demon7715/p/10984160.html
     * @author K
     * @since 1.0.0
     */
    fun genQrCode(content: String, logoImage: BufferedImage, sideLength: Int = 300, margin: Int = 0): BufferedImage {
        // Read the source image
        val logoPixels = Array(logoImage.width) { IntArray(logoImage.height) }
        for (i in 0 until logoImage.width) {
            for (j in 0 until logoImage.height) {
                logoPixels[i][j] = logoImage.getRGB(i, j)
            }
        }

        // Set the error-correction level and encoding of the QR code
        val hint =
            mapOf(EncodeHintType.CHARACTER_SET to "utf-8", EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H)
        // Generate the QR code
        val matrix = mutiWriter.encode(content, BarcodeFormat.QR_CODE, sideLength, sideLength, hint)

        // Convert the 2D matrix to a 1D pixel array
        val halfW = matrix.width / 2
        val halfH = matrix.height / 2
        val logoHalfWidth = logoImage.width / 2
        val value1 = halfW - logoHalfWidth - margin
        val value2 = halfW - logoHalfWidth + margin
        val value3 = halfW + logoHalfWidth + margin
        val value4 = halfW + logoHalfWidth - margin
        val value5 = halfH - logoHalfWidth - margin
        val value6 = halfH - logoHalfWidth + margin
        val value7 = halfH + logoHalfWidth + margin
        val value8 = halfH + logoHalfWidth - margin
        val pixels = IntArray(sideLength * sideLength)
        for (y in 0 until matrix.height) {
            for (x in 0 until matrix.width) {
                // Read pixels from the logo image
                if (x > halfW - logoHalfWidth && x < halfW + logoHalfWidth && y > halfH - logoHalfWidth && y < halfH + logoHalfWidth) {
                    pixels[y * sideLength + x] = logoPixels[x - halfW + logoHalfWidth][y - halfH + logoHalfWidth]
                } else if (x in (value1 + 1) until value2 && y > value5 && y < value7
                    || x in (value4 + 1) until value3 && y > value5 && y < value7
                    || x in (value1 + 1) until value3 && y > value5 && y < value6
                    || x in (value1 + 1) until value3 && y > value8 && y < value7
                ) {
                    pixels[y * sideLength + x] = 0xfffffff
                } else {
                    // The QR code color can be customized here; foreground and background colors may be specified separately
                    pixels[y * sideLength + x] = if (matrix[x, y]) -0x1000000 else 0xfffffff
                }
            }
        }
        val image = BufferedImage(sideLength, sideLength, BufferedImage.TYPE_INT_RGB)
        image.raster.setDataElements(0, 0, sideLength, sideLength, pixels)
        return image
    }

}