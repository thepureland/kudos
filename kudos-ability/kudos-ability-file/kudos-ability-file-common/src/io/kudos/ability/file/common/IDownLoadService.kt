package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DownloadFileModel
import java.io.InputStream

/**
 * 文件下载服务 SPI。具体存储后端（本地磁盘 / MinIO / OSS）各自实现。
 *
 * @author K
 * @since 1.0.0
 */
interface IDownLoadService {
    /**
     * 根据参数 获取文件字节流
     * 适合小文件快速操作，大文件会 OOM
     *
     * @param downloadFileModel
     * @throws Exception
     */
    @Throws(Exception::class)
    fun download(downloadFileModel: DownloadFileModel<*>): ByteArray?

    /**
     * 根据参数 获取文件输出流
     * 适合大文件，需手动管理缓冲
     *
     * @param downloadFileModel
     * @throws Exception
     */
    @Throws(Exception::class)
    fun downloadStream(downloadFileModel: DownloadFileModel<*>): InputStream?
}