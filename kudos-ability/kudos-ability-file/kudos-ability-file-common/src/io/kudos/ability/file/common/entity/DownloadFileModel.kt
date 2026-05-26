package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import org.springframework.core.io.InputStreamSource
import java.io.Serial
import java.io.Serializable

/**
 * File download request model.
 *
 * Structurally identical to [DeleteFileModel]; the extra generic parameter `S` exists
 * so that upload/download share the same [InputStreamSource] constraint.
 *
 * @param S input stream type, defaults to [InputStreamSource]
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class DownloadFileModel<S : InputStreamSource> : Serializable {
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
        /** Serializable version id. */
        @Serial
        private val serialVersionUID = -8498350660950356072L

        /**
         * Splits [bucketName] / [filePath] from a full path of the form `/<bucket>/<file/path>`.
         *
         * **Requires `fullPath` to start with `/`** — same constraint as [DeleteFileModel.from].
         */
        fun from(fullPath: String): DownloadFileModel<*> {
            require(fullPath.isNotBlank()) { "fullPath must not be blank" }
            require(fullPath.startsWith("/")) { "fullPath must start with '/': $fullPath" }
            val segments = fullPath.split('/').dropLastWhile { it.isEmpty() }
            val bucketName = segments.getOrNull(1)?.takeIf { it.isNotBlank() }
                ?: throw IllegalArgumentException("fullPath must contain bucket segment: $fullPath")
            val filePath = fullPath.removePrefix("/$bucketName")
            return DownloadFileModel<InputStreamSource>().apply {
                this.bucketName = bucketName
                this.filePath = filePath
            }
        }
    }

}
