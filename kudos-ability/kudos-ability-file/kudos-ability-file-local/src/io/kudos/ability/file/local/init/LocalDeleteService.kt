package io.kudos.ability.file.local.init

import io.kudos.ability.file.common.IDeleteService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DeleteFileModel
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File

/**
 * Local disk file deletion service.
 *
 * Double traversal protection: first goes through [IDeleteService.isValid] for the coarse string `..` contains
 * check, then performs strict `Path.normalize() + startsWith(basePath)` validation - the latter directly returns
 * false when traversal is detected, and never actually invokes `file.delete()`. Refuses to delete directories
 * (only file-level deletion is supported).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LocalDeleteService : IDeleteService {

    @Autowired
    private lateinit var properties: LocalProperties

    override fun delete(model: DeleteFileModel): Boolean {
        if (!this.isValid(model)) {
            return false
        }

        val base = requireNotNull(properties.basePath) { "kudos.ability.file.local.base-path is not set" }
        val baseDir = File(base).toPath().toAbsolutePath().normalize()
        val rawPath = listOf(base, model.bucketName, model.filePath).joinToString(File.separator)
        val resolved = File(rawPath).toPath().toAbsolutePath().normalize()
        if (!resolved.startsWith(baseDir)) {
            log.warn("delete refused: path traversal attempt path={0}", rawPath)
            return false
        }
        val file = resolved.toFile()
        if (file.isDirectory) {
            log.warn("can't delete a folder: {0}", resolved)
            return false
        }
        if (!file.exists()) {
            log.warn("file path is not exists: {0}", resolved)
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
        log.debug("file delete: {0}", resolved)
        return file.delete()
    }

    private val log = LogFactory.getLog(this::class)

}
