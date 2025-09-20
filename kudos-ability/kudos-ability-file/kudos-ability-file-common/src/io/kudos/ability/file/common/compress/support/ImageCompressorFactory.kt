package io.kudos.ability.file.common.compress.support

import io.kudos.ability.file.common.compress.compressor.ImageCompressor
import io.kudos.ability.file.common.compress.compressor.JpgCompressor
import io.kudos.ability.file.common.compress.compressor.PngCompressor
import io.kudos.ability.file.common.compress.compressor.WebPCompressor
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path

object ImageCompressorFactory {

    fun getCompressor(mimeType: String): ImageCompressor {
        return when (mimeType) {
            "image/jpeg" -> JpgCompressor()
            "image/png" -> PngCompressor()
            "image/webp" -> WebPCompressor()
            else -> throw UnsupportedOperationException("Unsupported format: $mimeType")
        }
    }

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
