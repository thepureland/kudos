package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.InputStream

interface ImageCompressor {

    fun support(config: CompressionConfig): Boolean

    fun compress(inputStream: InputStream, destination: String, config: CompressionConfig): CompressionResult

}
