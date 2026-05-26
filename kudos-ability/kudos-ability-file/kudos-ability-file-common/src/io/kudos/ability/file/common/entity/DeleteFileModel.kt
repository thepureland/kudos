package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import java.io.Serializable

/**
 * File deletion request model.
 *
 * Three components: bucket name + file path + auth parameters. Typically parsed from a
 * full path via the [from] static method.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DeleteFileModel : Serializable {
    /**
     * Custom directory | bucket name.
     */
    var bucketName: String? = null

    /**
     * Full file path.
     */
    var filePath: String? = null

    /**
     * Authentication parameters.
     */
    var authServerParam: AuthServerParam? = null

    companion object {
        /**
         * Splits [bucketName] / [filePath] from a full path of the form `/<bucket>/<file/path>`.
         *
         * **Requires `fullPath` to start with `/`** — this is a historical convention
         * (after split, segments[0] is empty and segments[1] is the bucket). Without
         * the leading `/`, the old implementation would treat the first segment as the
         * bucket and the rest as the path (off by one); this is now explicitly rejected
         * via require.
         */
        fun from(fullPath: String): DeleteFileModel {
            require(fullPath.isNotBlank()) { "fullPath must not be blank" }
            require(fullPath.startsWith("/")) { "fullPath must start with '/': $fullPath" }
            val segments = fullPath.split('/').dropLastWhile { it.isEmpty() }
            val bucketName = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("fullPath must contain bucket segment: $fullPath")
            val filePath = fullPath.removePrefix("/$bucketName")
            return DeleteFileModel().apply {
                this.bucketName = bucketName
                this.filePath = filePath
            }
        }
    }
}
