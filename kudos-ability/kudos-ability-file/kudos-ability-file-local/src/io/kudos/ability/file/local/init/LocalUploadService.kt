package io.kudos.ability.file.local.init

import io.kudos.ability.file.common.AbstractUploadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.compress.CompressionPipeline
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.nio.file.Path


/**
 * Local disk file upload service. Writes the uploaded InputStream to `{basePath}/{bucket}/{fileDir}/{name}`,
 * creating intermediate directories on demand. `fileDir` comes from [AbstractUploadService.dispatchFileDir]
 * (containing tenant / category / date).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class LocalUploadService : AbstractUploadService() {

    /** Local storage configuration, providing parameters such as basePath. */
    @Autowired
    private lateinit var properties: LocalProperties

    /**
     * Writes the upload stream to the local disk at `{basePath}/{bucket}/{fileDir}/{name}`.
     *
     * Implementation details:
     * - When file name is unspecified, generates one using UUID + suffix to avoid name collisions within fileDir
     * - Goes through [CompressionPipeline], with compression decided by [UploadFileModel.compressionConfig]
     * - Return value replaces platform separators with `/` - on Windows the stored path can still be directly
     *   concatenated into a URL
     * - Does not expose basePath in the return value, so upper layers do not depend on the concrete disk layout
     *
     * @param model upload request
     * @param fileDir relative directory allocated by [AbstractUploadService.dispatchFileDir]
     * @return relative path without basePath (first character is `/`)
     * @throws ServiceException error code [FileErrorCode.FILE_UPLOAD_FAIL] when writing to disk fails
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    override fun saveFile(model: UploadFileModel<*>, fileDir: String): String {
        val relativeDir = model.bucketName?.takeIf { it.isNotBlank() }
            ?.let { "$it${File.separator}$fileDir" }
            ?: fileDir
        val baseDir = basePath().toAbsolutePath().normalize()
        val targetDir = baseDir.resolve(relativeDir).normalize()
        if (!targetDir.startsWith(baseDir)) {
            throw ServiceException(
                FileErrorCode.FILE_UPLOAD_FAIL,
                IllegalArgumentException("invalid upload directory: $relativeDir")
            )
        }
        createFileDir(targetDir.toString())
        val fName = resolveFileName(model)
        val targetFile = targetDir.resolve(fName).normalize()
        if (!targetFile.startsWith(baseDir)) {
            throw ServiceException(FileErrorCode.FILE_UPLOAD_FAIL, IllegalArgumentException("invalid fileName: $fName"))
        }
        try {
            requireNotNull(model.inputStreamSource) { "inputStreamSource is null" }.getInputStream().use { inputStream ->
                CompressionPipeline.compressAndOutputFile(inputStream, targetFile.toString(), model.compressionConfig)
            }
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_UPLOAD_FAIL, e)
        }
        // hide basePath
        val filePath = "$relativeDir${File.separator}$fName".replace('\\', '/')
        return "/$filePath"
    }

    private fun basePath(): Path =
        File(requireNotNull(properties.basePath) { "kudos.ability.file.local.base-path is not set" }).toPath()

    private fun resolveFileName(model: UploadFileModel<*>): String {
        val fileName = model.fileName?.takeUnless { it.isBlank() }
            ?: "${RandomStringKit.uuid()}.${model.fileSuffix}"
        if (fileName.contains("..") || fileName.contains('/') || fileName.contains('\\')) {
            throw ServiceException(FileErrorCode.FILE_UPLOAD_FAIL, IllegalArgumentException("invalid fileName: $fileName"))
        }
        return fileName
    }

    /**
     * Creates a directory (including intermediate paths; no-op if already exists).
     *
     * @param dirPath absolute path of the target directory
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun createFileDir(dirPath: String) {
        val fileDir = File(dirPath)
        if (!fileDir.exists()) {
            log.debug("create file directory: {0}", dirPath)
            fileDir.mkdirs()
        }
    }

    /**
     * Local storage does not expose an absolute path prefix; returns an empty string.
     * When concatenating URLs, callers need to map it to a static resource servlet/route themselves.
     *
     * @return always an empty string
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    override fun pathPrefix(): String {
        // local directory is not exposed externally
        return ""
    }

    /** Logger. */
    private val log = LogFactory.getLog(this::class)

}
