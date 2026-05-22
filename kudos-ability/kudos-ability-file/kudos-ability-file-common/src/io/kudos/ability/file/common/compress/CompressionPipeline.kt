package io.kudos.ability.file.common.compress

import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import io.kudos.ability.file.common.compress.support.ImageCompressorFactory
import io.kudos.ability.file.common.compress.utils.CompressUtil
import io.kudos.ability.file.common.enums.UploadContentTypeEnum
import io.kudos.base.io.FileKit
import io.kudos.base.io.FilenameKit
import java.io.File
import java.io.IOException
import java.io.InputStream

/**
 * 文件压缩管道：根据文件类型和配置选择合适的压缩器并执行压缩。
 *
 * 决策树：
 * 1. config.enabled=false 或非图片 → 短路返回原始流（构造一个"未压缩"的 result 让上游照常处理）
 * 2. 是图片 → 通过 [ImageCompressorFactory] 按扩展名 + webp 配置选实现，再压缩
 *
 * 提供两种使用形态：
 * - [compress] 返回 [CompressionResult]，由调用方决定如何处置输出流（典型：minio/oss 上传走这条）
 * - [compressAndOutputFile] 直接落地到目标路径（典型：本地文件存储走这条）
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object CompressionPipeline {

    /**
     * 压缩到内存 result：调用方拿到 [CompressionResult] 后可读取 `outputStream` 自行处理。
     * 非图片或禁用压缩时返回带原始 metadata 的"透传 result"（不真压）。
     *
     * @param inputStream 输入流
     * @param outputFilePath 期望输出文件路径（用于推断扩展名和 contentType）
     * @param config 压缩配置（含 enabled / webp / 质量等）
     * @return 压缩结果
     * @throws IOException 读写或压缩过程中失败
     * @author K
     * @since 1.0.0
     */
    @Throws(IOException::class)
    fun compress(inputStream: InputStream, outputFilePath: String, config: CompressionConfig): CompressionResult {
        if (!config.enabled || !CompressUtil.isPic(outputFilePath)) {
            return CompressionResult(
                null,
                outputFilePath,
                UploadContentTypeEnum.enumOf(FilenameKit.getExtension(outputFilePath)).contentType
            )
        }
        val compressor = ImageCompressorFactory.getCompressor(outputFilePath, config.webp)
        return compressor.compress(inputStream, outputFilePath, config)
    }

    /**
     * 压缩并直接落地到 `outputFilePath`。
     * 非图片或禁用压缩时走 [FileKit.copyInputStreamToFile] 原样拷贝，避免无意义解码/编码。
     *
     * @param inputStream 输入流
     * @param outputFilePath 实际写入的文件路径
     * @param config 压缩配置
     * @throws IOException 读写或压缩过程中失败
     * @author K
     * @since 1.0.0
     */
    @Throws(IOException::class)
    fun compressAndOutputFile(inputStream: InputStream, outputFilePath: String, config: CompressionConfig) {
        if (!config.enabled || !CompressUtil.isPic(outputFilePath)) {
            FileKit.copyInputStreamToFile(inputStream, File(outputFilePath))
            return
        }
        val compressor = ImageCompressorFactory.getCompressor(outputFilePath, config.webp)
        compressor.compress(inputStream, outputFilePath, config).writeTo()
    }

}