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

object CompressionPipeline {

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