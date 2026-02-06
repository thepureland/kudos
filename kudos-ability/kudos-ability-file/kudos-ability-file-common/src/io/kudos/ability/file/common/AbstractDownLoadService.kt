package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.base.logger.LogFactory
import java.io.InputStream

abstract class AbstractDownLoadService : IDownLoadService {

    @Throws(Exception::class)
    override fun download(downloadFileModel: DownloadFileModel<*>): ByteArray? {
        try {
            return readFileToByte(downloadFileModel)
        } catch (e: Exception) {
            log.error(e)
            throw e
        }
    }

    @Throws(Exception::class)
    override fun downloadStream(downloadFileModel: DownloadFileModel<*>): InputStream? {
        try {
            return readFileToStream(downloadFileModel)
        } catch (e: Exception) {
            log.error(e)
            throw e
        }
    }

    protected abstract fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray?

    protected abstract fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream?

    private val log = LogFactory.getLog(this)

}
