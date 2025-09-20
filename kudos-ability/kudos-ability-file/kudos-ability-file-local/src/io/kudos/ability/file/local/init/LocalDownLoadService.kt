package io.kudos.ability.file.local.init

import io.kudos.ability.file.common.AbstractDownLoadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.local.init.properties.LocalProperties
import io.kudos.base.error.ServiceException
import io.kudos.base.io.FileKit
import io.kudos.base.logger.LogFactory
import org.springframework.beans.factory.annotation.Autowired
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException


class LocalDownLoadService : AbstractDownLoadService() {
    
    @Autowired
    private lateinit var properties: LocalProperties

    protected override fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray {
        val bucketName = downloadFileModel.bucketName
        val filePath = downloadFileModel.filePath
        val realFilePath = properties.basePath + File.separator + bucketName + File.separator + filePath
        val file = File(realFilePath)
        if (!file.exists()) {
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
        return FileKit.readFileToByteArray(file)
    }

    override fun readFileToStream(downloadFileModel: DownloadFileModel<*>): java.io.InputStream {
        try {
            val bucketName = downloadFileModel.bucketName
            val filePath: String? = downloadFileModel.filePath
            val realFilePath = properties.basePath + File.separator + bucketName + File.separator + filePath
            return FileInputStream(realFilePath)
        } catch (_: FileNotFoundException) {
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
    }

    private val log = LogFactory.getLog(this)

}
