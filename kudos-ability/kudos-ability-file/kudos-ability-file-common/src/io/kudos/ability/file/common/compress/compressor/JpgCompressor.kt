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

/**
 * JPEG image compression implementation.
 *
 * Uses Thumbnailator for resizing, then ImageIO's JPEG writer for output. Output
 * quality is uniformly controlled by [CompressionConfig.quality]. Marked `open`
 * so [PngCompressor] can reuse its [support]; PNG compression goes through a
 * separate com.xqlee pipeline.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class JpgCompressor : ImageCompressor {

    /**
     * Handles the image whenever the compression feature is enabled overall. Whether
     * this specific compressor is actually used is decided by
     * [io.kudos.ability.file.common.compress.support.ImageCompressorFactory].
     *
     * @param config compression configuration
     * @return the value of [CompressionConfig.enabled]
     */
    override fun support(config: CompressionConfig): Boolean {
        return config.enabled
    }

    /**
     * Main JPEG compression flow: read original -> resize by configured width/height
     * (capped at the original) -> write via the ImageIO JPEG writer. Resizing takes
     * effect only when the configured value is smaller than the original, avoiding
     * "upscaling" of small images.
     *
     * @param inputStream source image input stream
     * @param destination target path (JPEG does not rename)
     * @param config compression configuration
     * @return result containing the compressed byte stream, target path, and `image/jpeg` type
     * @throws IOException on IO failure or encoding error
     * @author K
     * @since 1.0.0
     */
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
