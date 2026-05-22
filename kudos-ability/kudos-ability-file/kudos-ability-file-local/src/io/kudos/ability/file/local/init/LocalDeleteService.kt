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
 * 本地磁盘文件删除服务。
 *
 * 双重防穿越：先走 [IDeleteService.isValid] 的字符串包含 `..` 粗筛，再做
 * `Path.normalize() + startsWith(basePath)` 强校验——后者命中穿越时直接返回 false，
 * 不会真正落到 `file.delete()`。拒绝删目录（只支持文件级别）。
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
            log.warn("拒绝删除：路径穿越企图 path={0}", rawPath)
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
