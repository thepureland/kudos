package io.kudos.ability.file.common.entity

import io.kudos.ability.file.common.auth.AuthServerParam
import io.kudos.ability.file.common.compress.support.CompressionConfig
import org.springframework.core.io.InputStreamSource
import java.io.Serial
import java.io.Serializable

/**
 * Model object for file uploads.
 *
 * Final on-disk path shape: `{system-planned path}/{bucket}/{tenantId}/{catePath?}/{yyyy}/{mm}/{dd}/{uuid}.{fileSuffix}`.
 * The actual directory is assembled by [io.kudos.ability.file.common.AbstractUploadService.dispatchFileDir];
 * this class only carries the input parameters.
 *
 * @param S input stream type; the generic is preserved so callers (e.g. Spring MultipartFile)
 *          can pass concrete types without casting
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class UploadFileModel<S : InputStreamSource?> : Serializable {
    /**
     * Custom directory | bucket name.
     */
    var bucketName: String? = null

    /**
     * Tenant id.
     */
    var tenantId: String? = null

    /**
     * Category directory.
     */
    var category: String? = null

    /**
     * File suffix.
     */
    var fileSuffix: String? = null

    /**
     * Input stream.
     */
    var inputStreamSource: S? = null

    /**
     * Authentication parameters.
     */
    var authServerParam: AuthServerParam? = null

    /**
     * File name, example: test.jpg
     */
    var fileName: String? = null

    /** Image compression configuration; default value means no compression. */
    var compressionConfig = CompressionConfig()

    companion object {
        /** Serializable version id. */
        @Serial
        private val serialVersionUID = -8498350660950356072L
    }

}
