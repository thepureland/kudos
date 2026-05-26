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
 * WebP image compression implementation.
 *
 * Uses the luciad/imageio-webp library for WebP encoding. Defaults to lossy mode
 * with quality 0.75 (sample lossless switch code is kept in comments). The output
 * file name appends a `.webp` suffix so the upstream storage does not overwrite
 * the original JPG/PNG file.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class WebPCompressor : ImageCompressor {

    /**
     * Enabled only when both the compression switch and the WebP switch are on.
     * Makes WebP an "explicitly selected" format, avoiding business code accidentally
     * converting every image to WebP when older browsers do not support it.
     *
     * @param config compression configuration
     * @return true means WebP compression is used
     */
    override fun support(config: CompressionConfig): Boolean {
        return config.enabled && config.webp
    }

    /**
     * Main WebP compression flow: ImageIO read -> WebP writer encode -> write to byte stream.
     *
     * @param inputStream source image input stream
     * @param destination target path; this method appends a `.webp` suffix via [rename]
     * @param config compression configuration
     * @return result containing the compressed byte stream, renamed path, and `image/webp` type
     * @throws IOException on IO or encoding failure
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
     * Appends a `.webp` suffix to the target path.
     * Even if the original file name is already `xxx.jpg`, it becomes `xxx.jpg.webp` —
     * keeping the original extension makes file lineage easier for operators to inspect.
     *
     * @param destination original target path
     * @return new path with the suffix appended
     * @author K
     * @since 1.0.0
     */
    private fun rename(destination: String): String = "$destination.webp"

}
