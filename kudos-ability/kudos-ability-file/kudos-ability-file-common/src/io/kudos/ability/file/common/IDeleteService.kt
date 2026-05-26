package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DeleteFileModel
import java.io.File

/**
 * SPI for the file delete service. Each concrete storage backend
 * (local disk / MinIO / OSS) provides its own implementation.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDeleteService {
    /**
     * Deletes a file.
     * @param model request path
     * @return whether the deletion succeeded
     * @throws ServiceException when the file does not exist
     */
    fun delete(model: DeleteFileModel): Boolean

    /**
     * Whether the path is valid — currently only performs coarse-grained `..`
     * path-traversal protection.
     *
     * Historical bug: the old implementation used [File.pathSeparator] (the PATH
     * environment variable separator: Unix `:` / Windows `;`) when concatenating,
     * which is a different concept from "separators between file path segments"
     * ([File.separator]: Unix `/` / Windows `\`). That made the concatenation result
     * wrong (e.g. `bucket:filePath` instead of `bucket/filePath`), causing the `..`
     * check to inspect a string that did not match the actual path about to be used.
     * Fixed.
     *
     * Note: this check is **far from sufficient to replace proper path allowlisting /
     * normalization**. Production deployments should perform stronger validation such
     * as `Path.normalize() + startsWith(rootDir)` inside each concrete backend's `delete`.
     *
     * @param model request path
     * @return whether the path is valid
     */
    fun isValid(model: DeleteFileModel): Boolean {
        val relativePath = (model.bucketName ?: "") + File.separator + (model.filePath ?: "")
        return relativePath.isNotBlank() && !relativePath.contains("..")
    }

}
