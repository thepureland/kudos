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


class LocalDownLoadService : AbstractDownLoadService() {
    
    @Autowired
    private lateinit var properties: LocalProperties

    override fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray {
        val realFilePath = listOf(
            properties.basePath,
            downloadFileModel.bucketName,
            downloadFileModel.filePath,
        ).joinToString(File.separator)
        val file = File(realFilePath)
        if (!file.exists()) {
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
        return FileKit.readFileToByteArray(file)
    }

    override fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream {
        try {
            val realFilePath = listOf(
                properties.basePath,
                downloadFileModel.bucketName,
                downloadFileModel.filePath,
            ).joinToString(File.separator)
            return FileInputStream(realFilePath)
        } catch (_: FileNotFoundException) {
            throw ServiceException(FileErrorCode.FILE_NO_EXISTS)
        }
    }

}
