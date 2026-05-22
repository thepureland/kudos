package io.kudos.ability.file.common.compress.compressor

import com.luciad.imageio.webp.WebPWriteParam
import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import net.coobird.thumbnailator.Thumbnails
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO

/**
 * JPEG 图片压缩实现。
 *
 * 用 Thumbnailator 缩放后通过 ImageIO 的 JPEG writer 输出，统一输出 quality 由 [CompressionConfig.quality] 控制。
 * 标 `open` 是为了让 [PngCompressor] 复用其 [support]，PNG 压缩走另一条 com.xqlee 链路。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class JpgCompressor : ImageCompressor {

    /**
     * 只要压缩功能整体打开就处理。具体决定是否走本压缩器由 [io.kudos.ability.file.common.compress.support.ImageCompressorFactory] 控制。
     *
     * @param config 压缩配置
     * @return [CompressionConfig.enabled] 的值
     */
    override fun support(config: CompressionConfig): Boolean {
        return config.enabled
    }

    /**
     * JPEG 压缩主流程：原图读入 → 按配置宽高（不超过原图）缩放 → 用 ImageIO JPEG writer 写出。
     * 缩放只在配置值小于原图时生效，避免对小图做"放大"操作。
     *
     * @param inputStream 源图片输入流
     * @param destination 目标路径（JPEG 不改名）
     * @param config 压缩配置
     * @return 包含压缩字节流、目标路径、`image/jpeg` 的结果
     * @throws IOException IO 异常或编解码失败时
     * @author K
     * @since 1.0.0
     */
    @Throws(IOException::class)
    override fun compress(
        inputStream: InputStream,
        destination: String,
        config: CompressionConfig
    ): CompressionResult {
        val originalImage = ImageIO.read(inputStream)
        val builder = Thumbnails.of(originalImage)
        var width = originalImage.width
        var height = originalImage.height
        if (width > 0 && config.width < width) {
            width = config.width
        }
        if (height > 0 && config.height < height) {
            height = config.height
        }
        builder.size(width, height)
        builder.outputQuality(config.quality)

        val bufferedImage = builder.asBufferedImage()

        val writer = ImageIO.getImageWritersByMIMEType("image/jpeg").next()
        val param = writer.defaultWriteParam
        param.setCompressionMode(WebPWriteParam.MODE_EXPLICIT)
        val os = ByteArrayOutputStream()
        try {
            ImageIO.createImageOutputStream(os).use { ios ->
                writer.setOutput(ios)
                writer.write(null, IIOImage(bufferedImage, null, null), param)
            }
        } finally {
            writer.dispose()
        }
        return CompressionResult(os, destination, "image/jpeg")
    }

}
