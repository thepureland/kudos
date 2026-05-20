package io.kudos.ability.file.common.compress.compressor

import com.luciad.imageio.webp.WebPWriteParam
import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO

/**
 * WebP 图片压缩实现。
 *
 * 用 luciad/imageio-webp 库做 WebP 编码。默认 lossy 模式 quality 0.75（注释里保留了 lossless 切换的样板代码）。
 * 输出文件名追加 `.webp` 后缀，以便上层存储不会和原 JPG/PNG 文件覆盖。
 *
 * @author K
 * @since 1.0.0
 */
class WebPCompressor : ImageCompressor {

    /**
     * 只在压缩开关 + WebP 开关都打开时启用。
     * 让 WebP 成为"显式选择"的格式，避免业务侧无意间把所有图都转 WebP 而旧浏览器不支持。
     *
     * @param config 压缩配置
     * @return true 表示走 WebP 压缩
     */
    override fun support(config: CompressionConfig): Boolean {
        return config.enabled && config.webp
    }

    /**
     * WebP 压缩主流程：ImageIO 读入 → WebP writer 编码 → 写到字节流。
     *
     * @param inputStream 源图片输入流
     * @param destination 目标路径，本方法会通过 [rename] 追加 `.webp` 后缀
     * @param config 压缩配置
     * @return 包含压缩字节流、改名后路径、`image/webp` 的结果
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
        val image = ImageIO.read(inputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()

        val writer = ImageIO.getImageWritersByMIMEType("image/webp").next()
        val param = WebPWriteParam(null)
        param.setCompressionMode(WebPWriteParam.MODE_EXPLICIT)
        // default Lossy and quality 0.75
//        param.setCompressionType(param.getCompressionTypes()[WebPWriteParam.LOSSLESS_COMPRESSION]);
//        param.setCompressionQuality(config.getQuality());
        try {
            ImageIO.createImageOutputStream(byteArrayOutputStream).use { ios ->
                writer.setOutput(ios)
                writer.write(null, IIOImage(image, null, null), param)
            }
        } finally {
            writer.dispose()
        }
        return CompressionResult(byteArrayOutputStream, rename(destination), "image/webp")
    }

    /**
     * 给目标路径追加 `.webp` 后缀。
     * 即便原文件名已是 `xxx.jpg`，也会变成 `xxx.jpg.webp`——保留原扩展名让运维查文件溯源更直观。
     *
     * @param destination 原始目标路径
     * @return 追加后缀的新路径
     * @author K
     * @since 1.0.0
     */
    private fun rename(destination: String): String = "$destination.webp"

}
