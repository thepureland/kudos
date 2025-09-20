package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

class PngCompressor : JpgCompressor(), ImageCompressor {

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
