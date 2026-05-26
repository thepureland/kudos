package io.kudos.ability.file.local.init

import io.kudos.ability.file.common.AbstractDownLoadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.base.io.FileKit
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream

/**
 * Local disk file download service. Reads files from `{basePath}/{bucket}/{filePath}`.
 *
 * Path traversal defense: all calls first go through [resolveSafePath], which performs `Path.normalize() +
 * startsWith(basePath)` validation - rejecting traversal attempts such as `../etc/passwd`. The
 * `IDeleteService.isValid` in the `common` layer only does a coarse-grained string `..` contains check,
 * which is not enough; an additional layer is added here.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class LocalDownLoadService : AbstractDownLoadService() {

    @Autowired
    private lateinit var properties: LocalProperties

    override fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray {
        val file = resolveSafePath(downloadFileModel)
        if (!file.exists()) {
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
        return FileKit.readFileToByteArray(file)
    }

    override fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream {
        val file = resolveSafePath(downloadFileModel)
        return try {
            FileInputStream(file)
        } catch (_: FileNotFoundException) {
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
    }

    /**
     * Resolves (bucketName, filePath) into an actual [File], ensuring that the resolved path remains under
     * [LocalProperties.basePath] - rejecting traversal attempts such as `..` / absolute paths. Throws
     * [FileErrorCode.FILE_ACCESS_DENY] when traversal is detected.
     */
    private fun resolveSafePath(model: DownloadFileModel<*>): File {
        val base = requireNotNull(properties.basePath) { "kudos.ability.file.local.base-path is not set" }
        val baseDir = File(base).toPath().toAbsolutePath().normalize()
        val rawPath = listOf(base, model.bucketName, model.filePath)
            .joinToString(File.separator)
        val resolved = File(rawPath).toPath().toAbsolutePath().normalize()
        if (!resolved.startsWith(baseDir)) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY)
        }
        return resolved.toFile()
    }

}
