package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult

/**
 * SPI for the file upload service. Each concrete storage backend
 * (local disk / MinIO / OSS) provides its own implementation.
 * Business code injects it via `@Autowired IUploadService`, and the framework picks
 * the concrete implementation based on the submodules the current application pulls in.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IUploadService {
    /**
     * Uploads a file.
     *
     * @param model upload model
     * @return upload result
     */
    fun fileUpload(model: UploadFileModel<*>): UploadFileResult

    /**
     * Returns the file path prefix; for MinIO this is typically the corresponding URL.
     */
    fun pathPrefix(): String
}