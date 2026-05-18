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
 * 本地磁盘文件下载服务。从 `{basePath}/{bucket}/{filePath}` 读取文件。
 *
 * 路径穿越防御：所有调用都先通过 [resolveSafePath] 做 `Path.normalize() +
 * startsWith(basePath)` 校验——拒绝 `../etc/passwd` 之类穿越企图。`common` 层的
 * `IDeleteService.isValid` 只做粗粒度字符串包含 `..` 检查，不够；此处再加一道。
 *
 * @author K
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
     * 把 (bucketName, filePath) 解析成实际 [File]，并保证解析后路径仍位于 [LocalProperties.basePath]
     * 之下——拒绝 `..` / 绝对路径等穿越企图。命中穿越时抛 [FileErrorCode.FILE_ACCESS_DENY]。
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
