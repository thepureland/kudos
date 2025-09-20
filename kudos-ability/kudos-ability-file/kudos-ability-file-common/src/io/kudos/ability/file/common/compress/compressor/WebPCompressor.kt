package io.kudos.ability.file.common.compress.compressor

import com.luciad.imageio.webp.WebPWriteParam
import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import javax.imageio.IIOImage
import javax.imageio.ImageIO

class WebPCompressor : ImageCompressor {

    public override fun support(config: CompressionConfig): Boolean {
        return config.enabled && config.webp
    }

    @Throws(IOException::class)
    public override fun compress(
        inputStream: InputStream,
        destination: String,
        config: CompressionConfig
    ): CompressionResult {
        val image = ImageIO.read(inputStream)
        val byteArrayOutputStream = ByteArrayOutputStream()

        val writer = ImageIO.getImageWritersByMIMEType("image/webp").next()
        val param: WebPWriteParam = WebPWriteParam(null)
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

    private fun rename(destination: String): String {
        var destination = destination
        destination = "$destination.webp"
        return destination
    }

    private fun rename(originalFile: File): File {
        return File(originalFile.getParent(), originalFile.getName() + ".webp")
    }

}
