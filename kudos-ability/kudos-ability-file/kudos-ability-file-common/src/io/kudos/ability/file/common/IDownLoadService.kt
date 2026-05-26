package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DownloadFileModel
import java.io.InputStream

/**
 * File download service SPI. Implemented by each storage backend (local disk / MinIO / OSS).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IDownLoadService {
    /**
     * Obtains the file byte stream based on the parameters.
     * Suitable for quick operations on small files; large files will cause OOM.
     *
     * @param downloadFileModel
     * @throws Exception
     */
    @Throws(Exception::class)
    fun download(downloadFileModel: DownloadFileModel<*>): ByteArray?

    /**
     * Obtains the file output stream based on the parameters.
     * Suitable for large files; buffering must be managed manually.
     *
     * @param downloadFileModel
     * @throws Exception
     */
    @Throws(Exception::class)
    fun downloadStream(downloadFileModel: DownloadFileModel<*>): InputStream?
}