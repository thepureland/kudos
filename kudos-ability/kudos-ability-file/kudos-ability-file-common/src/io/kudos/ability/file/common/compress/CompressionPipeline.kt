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
 * File compression pipeline: selects an appropriate compressor based on file type and
 * configuration and runs the compression.
 *
 * Decision tree:
 * 1. config.enabled=false or non-image -> short-circuit, return the original stream
 *    (construct an "uncompressed" result so the upstream handles it as usual)
 * 2. Image -> pick an implementation via [ImageCompressorFactory] based on the
 *    extension and the webp setting, then compress
 *
 * Two usage forms are provided:
 * - [compress] returns [CompressionResult], leaving the caller to decide how to handle
 *   the output stream (typical for minio/oss uploads)
 * - [compressAndOutputFile] writes directly to the target path (typical for local
 *   file storage)
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object CompressionPipeline {

    /**
     * Compresses to an in-memory result: the caller can read `outputStream` from the
     * returned [CompressionResult] and process it themselves. For non-images or when
     * compression is disabled, returns a "passthrough result" carrying the original
     * metadata (no actual compression).
     *
     * @param inputStream input stream
     * @param outputFilePath expected output file path (used to infer extension and contentType)
     * @param config compression configuration (enabled / webp / quality, etc.)
     * @return compression result
     * @throws IOException on read/write or compression failure
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
     * Compresses and writes directly to `outputFilePath`.
     * For non-images or when compression is disabled, uses [FileKit.copyInputStreamToFile]
     * to copy as-is and avoid pointless decode/encode.
     *
     * @param inputStream input stream
     * @param outputFilePath the file path actually written
     * @param config compression configuration
     * @throws IOException on read/write or compression failure
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