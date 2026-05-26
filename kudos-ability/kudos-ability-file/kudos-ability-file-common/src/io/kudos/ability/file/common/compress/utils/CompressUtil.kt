package io.kudos.ability.file.common.compress.utils

import io.kudos.base.io.FilenameKit
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.util.Locale

/**
 * Small utility set related to image compression.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object CompressUtil {

    /** Allowlist of suffixes the current framework treats as "compressible images". When adding new types, also update [io.kudos.ability.file.common.compress.support.ImageCompressorFactory]. */
    private val EXTENSIONS = setOf("jpg", "jpeg", "png")

    /** Whether the file name corresponds to a "compressible image" (determined by suffix, does not read file header). */
    fun isPic(fileName: String): Boolean = validExtension(FilenameKit.getExtension(fileName))

    /** Whether the suffix is in the [EXTENSIONS] allowlist; case conversion uses [Locale.ROOT] to avoid Turkish-locale deviations. */
    fun validExtension(extension: String): Boolean {
        if (extension.isBlank()) return false
        return EXTENSIONS.contains(extension.lowercase(Locale.ROOT))
    }

    /** Thin forwarder over JDK [Files.probeContentType]; returns null when the type cannot be identified. */
    @Throws(IOException::class)
    fun mimeType(fileName: String): String? = Files.probeContentType(Path.of(fileName))

}
