package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * PNG 图片压缩实现。
 *
 * 直接复用 [JpgCompressor] 的 [support] 判定，但 [compress] 走 com.xqlee 的 png 压缩库
 * （Thumbnailator 对 PNG 的 quality 参数不友好，专用库压缩比更稳定）。
 *
 * @author K
 * @since 1.0.0
 */
class PngCompressor : JpgCompressor(), ImageCompressor {

    /**
     * PNG 压缩主流程：交给 com.xqlee 的 PngCompressor 工具，直接写入字节流。
     * 与 [JpgCompressor] 不同的是这里不做缩放——PNG 一般用于图标/截图，业务上更介意尺寸精度。
     *
     * @param inputStream 源图片输入流
     * @param destination 目标路径
     * @param config 压缩配置（当前未使用，保留以匹配接口）
     * @return 包含压缩字节流、目标路径、`image/png` 的结果
     * @throws IOException IO 或编解码失败时
     * @author K
     * @since 1.0.0
     */
    @Throws(IOException::class)
    override fun compress(
        inputStream: InputStream,
        destination: String,
        config: CompressionConfig
    ): CompressionResult {
        val baos = ByteArrayOutputStream()
        com.xqlee.image.png.PngCompressor.compress(inputStream, baos)
        return CompressionResult(baos, destination, "image/png")
    }

}
