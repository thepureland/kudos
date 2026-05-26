package io.kudos.ability.file.common.compress.support

import io.kudos.ability.file.common.compress.compressor.ImageCompressor
import io.kudos.ability.file.common.compress.compressor.JpgCompressor
import io.kudos.ability.file.common.compress.compressor.PngCompressor
import io.kudos.ability.file.common.compress.compressor.WebPCompressor
import io.kudos.base.io.FilenameKit
import java.util.Locale

/**
 * Image compressor factory.
 *
 * Provides two construction entry points: select explicitly by MIME type, or select
 * by file suffix (with the option to force-override to WebP).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object ImageCompressorFactory {

    /**
     * Selects a compressor by MIME type.
     *
     * @param mimeType one of `image/jpeg` / `image/png` / `image/webp`
     * @return the corresponding [ImageCompressor] instance
     * @throws UnsupportedOperationException when the MIME is not in the supported list
     * @author K
     * @since 1.0.0
     */
    fun getCompressor(mimeType: String): ImageCompressor {
        return when (mimeType) {
            "image/jpeg" -> JpgCompressor()
            "image/png" -> PngCompressor()
            "image/webp" -> WebPCompressor()
            else -> throw UnsupportedOperationException("Unsupported format: $mimeType")
        }
    }

    /**
     * Selects a compressor by file path suffix; if [webp] is true, force-overrides to WebP.
     * `webp = true` lets business code turn "always compress to WebP regardless of source
     * image type" into a single configurable switch.
     *
     * @param outputFilePath target file path used to resolve the suffix
     * @param webp whether to force WebP
     * @return the selected [ImageCompressor]
     * @throws UnsupportedOperationException when the suffix is unsupported
     * @author K
     * @since 1.0.0
     */
    fun getCompressor(outputFilePath: String, webp: Boolean): ImageCompressor {
        if (webp) {
            return WebPCompressor()
        }

        return when (FilenameKit.getExtension(outputFilePath).lowercase(Locale.ROOT)) {
            "jpg", "jpeg" -> JpgCompressor()
            "png" -> PngCompressor()
            "webp" -> WebPCompressor()
            else -> throw UnsupportedOperationException("Unsupported image extension for: $outputFilePath")
        }
    }

}
