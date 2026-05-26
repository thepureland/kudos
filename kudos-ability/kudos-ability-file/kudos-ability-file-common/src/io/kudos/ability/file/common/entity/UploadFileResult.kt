package io.kudos.ability.file.common.entity

import java.io.Serial
import java.io.Serializable

/**
 * Upload file result.
 * Example: if a file is stored on disk at (/var/file/upload/console/-99/2022/11/09/123456789.jpg),
 * where /var/file/upload/ is the planned upload path and the rest is the path the
 * application stored the file under, the returned content is:
 * filepath=console/-99/boss/2022/11/09/123456789.jpg; pathPrefix=/var/file/upload/.
 * Full path = pathPrefix + filePath.
 * For local disk in general, filePath is usually all you need.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class UploadFileResult : Serializable {
    /**
     * Relative file path: where the file is stored relative to the prefix.
     */
    var filePath: String? = null

    /**
     * File path prefix: may be a disk location or a URL.
     */
    var pathPrefix: String? = null

    companion object {
        @Serial
        private val serialVersionUID = -8544059935004940300L
    }
}
