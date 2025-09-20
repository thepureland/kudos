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

open class JpgCompressor : ImageCompressor {

    override fun support(config: CompressionConfig): Boolean {
        return config.enabled
    }

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
