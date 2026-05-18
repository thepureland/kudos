package io.kudos.base.security

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO


/**
 * 二维码 / 条码渲染工具。
 *
 * 仅依赖 `zxing-core`（已在 kudos-base 的 `api` 依赖里），没有引入 `zxing-javase`
 * （后者带 [com.google.zxing.client.j2se.MatrixToImageWriter]）——用 `java.awt` +
 * `ImageIO` 自行编码，避免增加依赖体积。
 *
 * 典型用法：
 *
 * ```kotlin
 * val png = BarcodeKit.qrcodePng("otpauth://totp/kudos:alice?secret=ABCDEF&issuer=kudos")
 * // 把 png 写入 HTTP response（Content-Type: image/png）即可
 * ```
 *
 * @author K
 * @since 1.0.0
 */
object BarcodeKit {

    /** 默认二维码边长（像素） */
    const val DEFAULT_SIZE = 200

    /** 默认纠错等级。L=低 7%、M=中 15%、Q=四分位 25%、H=高 30%。短文本用 M 就够 */
    private val DEFAULT_EC_LEVEL = ErrorCorrectionLevel.M

    /** 默认 quiet zone（四周白边）模块数 */
    private const val DEFAULT_MARGIN = 1

    /**
     * 生成 PNG 字节数组形式的二维码。
     *
     * @param text 二维码承载的文本；常见用例是 `otpauth://...` URL
     * @param size 二维码边长（像素），>0
     * @param margin quiet zone 模块数，0 起；过小会被部分 App 拒识，过大浪费空间
     * @param errorCorrectionLevel 容错等级
     * @return PNG 二进制
     */
    fun qrcodePng(
        text: String,
        size: Int = DEFAULT_SIZE,
        margin: Int = DEFAULT_MARGIN,
        errorCorrectionLevel: ErrorCorrectionLevel = DEFAULT_EC_LEVEL,
    ): ByteArray {
        require(text.isNotEmpty()) { "二维码文本不能为空" }
        require(size > 0) { "二维码边长必须 > 0，得到 ${size}" }
        require(margin >= 0) { "margin 必须 >= 0，得到 ${margin}" }

        val hints = mapOf(
            EncodeHintType.CHARACTER_SET to "UTF-8",
            EncodeHintType.MARGIN to margin,
            EncodeHintType.ERROR_CORRECTION to errorCorrectionLevel,
        )
        val matrix = MultiFormatWriter().encode(text, BarcodeFormat.QR_CODE, size, size, hints)

        // 双色位图：黑色像素表示有模块，白色表示空。ARGB int 编码：0xFFRRGGBB
        val black = 0xFF000000.toInt()
        val white = 0xFFFFFFFF.toInt()
        val image = BufferedImage(matrix.width, matrix.height, BufferedImage.TYPE_INT_RGB)
        for (x in 0 until matrix.width) {
            for (y in 0 until matrix.height) {
                image.setRGB(x, y, if (matrix.get(x, y)) black else white)
            }
        }

        val out = ByteArrayOutputStream()
        check(ImageIO.write(image, "PNG", out)) { "ImageIO 未找到 PNG writer，请检查 JRE 配置" }
        return out.toByteArray()
    }
}
