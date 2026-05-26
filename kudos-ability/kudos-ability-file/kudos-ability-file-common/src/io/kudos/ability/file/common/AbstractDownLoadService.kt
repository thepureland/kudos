package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.base.logger.LogFactory
import java.io.InputStream

/**
 * Abstract base class for file download services.
 *
 * Separates the "externally exposed [download]/[downloadStream] interfaces" from the "concrete storage
 * implementations ([readFileToByte]/[readFileToStream])", so that subclasses such as Local / Minio / cloud
 * storage only need to focus on how to read files from the underlying storage. Exception logging and
 * rethrow patterns are handled uniformly by this class.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class AbstractDownLoadService : IDownLoadService {

    /**
     * Downloads a file as a byte array. Logs the exception before rethrowing to avoid silent failures.
     *
     * @param downloadFileModel download request model
     * @return file bytes, may be null (decided by the concrete implementation)
     * @author K
     * @since 1.0.0
     */
    @Throws(Exception::class)
    override fun download(downloadFileModel: DownloadFileModel<*>): ByteArray? {
        try {
            return readFileToByte(downloadFileModel)
        } catch (e: Exception) {
            log.error(e)
            throw e
        }
    }

    /**
     * Downloads a file as a stream. For large files, this method should be preferred to avoid loading the
     * entire file into memory.
     *
     * @param downloadFileModel download request model
     * @return file input stream, may be null (decided by the concrete implementation)
     * @author K
     * @since 1.0.0
     */
    @Throws(Exception::class)
    override fun downloadStream(downloadFileModel: DownloadFileModel<*>): InputStream? {
        try {
            return readFileToStream(downloadFileModel)
        } catch (e: Exception) {
            log.error(e)
            throw e
        }
    }

    /**
     * Subclass implementation: reads a file from storage into memory and returns it as a byte array.
     *
     * @param downloadFileModel download request model
     * @return file bytes, returns null when the file does not exist
     */
    protected abstract fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray?

    /**
     * Subclass implementation: returns a file from storage as an input stream. The caller is responsible
     * for closing the stream.
     *
     * @param downloadFileModel download request model
     * @return input stream, returns null when the file does not exist
     */
    protected abstract fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream?

    /** Logger; subclass exceptions are uniformly logged here. */
    private val log = LogFactory.getLog(this::class)

}
