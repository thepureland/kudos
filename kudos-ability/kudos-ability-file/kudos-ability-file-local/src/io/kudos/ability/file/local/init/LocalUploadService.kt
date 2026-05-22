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
 * 本地磁盘文件上传服务。把上传的 InputStream 写到 `{basePath}/{bucket}/{fileDir}/{name}` 下，
 * 中间目录按需自动创建。`fileDir` 来自 [AbstractUploadService.dispatchFileDir]（含租户 / 分类 / 日期）。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class LocalUploadService : AbstractUploadService() {

    /** 本地存储配置，提供 basePath 等参数 */
    @Autowired
    private lateinit var properties: LocalProperties

    /**
     * 把上传流写到本地磁盘 `{basePath}/{bucket}/{fileDir}/{name}`。
     *
     * 实现细节：
     * - 文件名未指定时用 UUID + 后缀生成，避免 fileDir 内重名覆盖
     * - 走 [CompressionPipeline] 一路，按 [UploadFileModel.compressionConfig] 决定是否压缩
     * - 返回值用 `/` 替换平台分隔符——Windows 上落盘后路径仍能直接拼到 URL
     * - 不把 basePath 暴露到返回值，让上层不依赖具体磁盘布局
     *
     * @param model 上传请求
     * @param fileDir [AbstractUploadService.dispatchFileDir] 分配出来的相对目录
     * @return 不含 basePath 的相对路径（首字符是 `/`）
     * @throws ServiceException 写盘失败时，错误码 [FileErrorCode.FILE_UPLOAD_FAIL]
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
        //隐藏basePath
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
     * 创建目录（含中间路径，已存在则无操作）。
     *
     * @param dirPath 目标目录绝对路径
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun createFileDir(dirPath: String) {
        val fileDir = File(dirPath)
        if (!fileDir.exists()) {
            log.debug("创建文件目录：{0}", dirPath)
            fileDir.mkdirs()
        }
    }

    /**
     * 本地存储不暴露绝对路径前缀，返回空串。
     * 调用方拼接 URL 时需自行映射到一个静态资源 servlet/路由。
     *
     * @return 始终为空串
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    override fun pathPrefix(): String {
        //本地目录不对外暴露
        return ""
    }

    /** 日志器 */
    private val log = LogFactory.getLog(this::class)

}
