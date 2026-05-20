package io.kudos.ability.file.common.compress.support

import io.kudos.ability.file.common.compress.compressor.ImageCompressor
import io.kudos.ability.file.common.compress.compressor.JpgCompressor
import io.kudos.ability.file.common.compress.compressor.PngCompressor
import io.kudos.ability.file.common.compress.compressor.WebPCompressor
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

/**
 * 图片压缩器工厂。
 *
 * 提供两种构造入口：按 MIME 类型显式选择，或按文件后缀自动探测（可强制覆盖为 WebP）。
 *
 * @author K
 * @since 1.0.0
 */
object ImageCompressorFactory {

    /**
     * 按 MIME 类型选择压缩器。
     *
     * @param mimeType `image/jpeg` / `image/png` / `image/webp` 之一
     * @return 对应的 [ImageCompressor] 实例
     * @throws UnsupportedOperationException 当 MIME 不在支持列表中
     * @author K
     * @since 1.0.0
     */
    fun getCompressor(mimeType: String): ImageCompressor {
        return when (mimeType) {
            "image/jpeg" -> JpgCompressor()
            "image/png" -> PngCompressor()
            "image/webp" -> WebPCompressor()
            else -> throw UnsupportedOperationException("Unsupported format: $mimeType")
        }
    }

    /**
     * 按文件路径探测 MIME 自动选择压缩器；若 [webp] 为 true 则强制覆盖为 WebP。
     * `webp = true` 让业务侧"无论原图类型，统一压成 WebP"成为一行可配置开关。
     *
     * @param outputFilePath 用于探测 MIME 的目标文件路径
     * @param webp 是否强制走 WebP
     * @return 选中的 [ImageCompressor]
     * @throws UnsupportedOperationException MIME 探测失败或不支持时
     * @throws IOException IO 异常时
     * @author K
     * @since 1.0.0
     */
    @Throws(IOException::class)
    fun getCompressor(outputFilePath: String, webp: Boolean): ImageCompressor {
        var mimeType = Files.probeContentType(Path.of(outputFilePath))
        if (mimeType == null) {
            throw UnsupportedOperationException("Unsupported or unknown MIME type for: $outputFilePath")
        }

        if (webp) {
            mimeType = "image/webp"
        }

        return getCompressor(mimeType)
    }

}
