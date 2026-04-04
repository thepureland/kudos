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


open class LocalUploadService : AbstractUploadService() {
    
    @Autowired
    private lateinit var properties: LocalProperties
    
    protected fun createBucket(model: UploadFileModel<*>) {
        val bucketPath = properties.basePath + File.separator + model.bucketName
        val bucketDir = File(bucketPath)
        if (!bucketDir.exists()) {
            log.debug("创建文件目录：{0}$bucketPath")
            bucketDir.mkdirs()
        }
    }

    override fun saveFile(model: UploadFileModel<*>, fileDir: String): String {
        createBucket(model)
        val relativeDir = model.bucketName?.takeIf { it.isNotBlank() }?.let { bucket ->
            bucket + File.separator + fileDir
        } ?: fileDir
        val rDir = properties.basePath + File.separator + relativeDir
        createFileDir(rDir)
        var fName = model.fileName
        if (fName.isNullOrBlank()) {
            fName = RandomStringKit.uuid() + "." + model.fileSuffix
        }
        val fullFilePath = rDir + File.separator + fName
        try {
            requireNotNull(model.inputStreamSource) { "inputStreamSource is null" }.getInputStream().use { inputStream ->
                CompressionPipeline.compressAndOutputFile(inputStream, fullFilePath, model.compressionConfig)
            }
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_UPLOAD_FAIL, e)
        }
        //隐藏basePath
        val filePath = (relativeDir + File.separator + fName).replace('\\', '/')
        return "/$filePath"
    }

    /**
     * 创建目录
     *
     * @param dirPath
     */
    private fun createFileDir(dirPath: String) {
        val fileDir = File(dirPath)
        if (!fileDir.exists()) {
            log.debug("创建文件目录：{0}$dirPath")
            fileDir.mkdirs()
        }
    }

    override fun pathPrefix(): String {
        //本地目录不对外暴露
        return ""
    }

    private val log = LogFactory.getLog(this::class)

}
