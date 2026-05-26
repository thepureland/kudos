package io.kudos.ability.file.common.compress.compressor

import io.kudos.ability.file.common.compress.support.CompressionConfig
import io.kudos.ability.file.common.compress.support.CompressionResult
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream

/**
 * PNG image compression implementation.
 *
 * Reuses [JpgCompressor]'s [support] check directly, but [compress] goes through
 * the com.xqlee PNG compression library (Thumbnailator's quality parameter is
 * unfriendly for PNG; the dedicated library yields a more stable compression ratio).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class PngCompressor : JpgCompressor(), ImageCompressor {

    /**
     * Main PNG compression flow: hand off to the com.xqlee PngCompressor utility and
     * write directly to a byte stream. Unlike [JpgCompressor], this does not perform
     * resizing — PNG is typically used for icons/screenshots, where business code
     * cares more about pixel-precise sizes.
     *
     * @param inputStream source image input stream
     * @param destination target path
     * @param config compression configuration (currently unused; retained to match the interface)
     * @return result containing the compressed byte stream, target path, and `image/png` type
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
        val baos = ByteArrayOutputStream()
        com.xqlee.image.png.PngCompressor.compress(inputStream, baos)
        return CompressionResult(baos, destination, "image/png")
    }

}
