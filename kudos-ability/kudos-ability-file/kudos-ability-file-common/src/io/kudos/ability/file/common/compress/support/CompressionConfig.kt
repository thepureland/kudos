package io.kudos.ability.file.common.compress.support

import java.io.Serializable

/**
 * Compression configuration passed in from the client side.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CompressionConfig : Serializable {

    /**
     * Whether compression is enabled.
     */
    var enabled: Boolean = false

    /**
     * Whether to convert to WebP format.
     */
    var webp: Boolean = false

    /**
     * Effective only in non-WebP mode.
     */
    var width: Int = 0

    /**
     * Effective only in non-WebP mode.
     */
    var height: Int = 0

    /**
     * Effective only in non-WebP mode.
     */
    var quality: Float = 1f
        set(quality) {
            require(quality in 0.0f..1.0f) { "Quality must be between 0.0 and 1.0" }
            field = quality
        }
}
