package io.kudos.ability.file.common

import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.base.logger.LogFactory
import java.io.InputStream

/**
 * 文件下载服务抽象基类。
 *
 * 把"对外暴露的 [download]/[downloadStream] 接口"与"具体存储实现 ([readFileToByte]/[readFileToStream])"分离，
 * 让 Local / Minio / 云存储等子类只用关注怎么从底层读文件，异常日志与上抛模式由本类统一处理。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
abstract class AbstractDownLoadService : IDownLoadService {

    /**
     * 下载文件为字节数组。异常先记日志再上抛，避免静默失败。
     *
     * @param downloadFileModel 下载请求模型
     * @return 文件字节，可能为 null（具体实现决定）
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
     * 下载文件为流。大文件场景应优先用本方法避免把整个文件载入内存。
     *
     * @param downloadFileModel 下载请求模型
     * @return 文件输入流，可能为 null（具体实现决定）
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
     * 子类实现：把存储中的文件读到内存并以字节数组返回。
     *
     * @param downloadFileModel 下载请求模型
     * @return 文件字节，文件不存在时返回 null
     */
    protected abstract fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray?

    /**
     * 子类实现：把存储中的文件以输入流返回。调用方负责关闭流。
     *
     * @param downloadFileModel 下载请求模型
     * @return 输入流，文件不存在时返回 null
     */
    protected abstract fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream?

    /** 日志器，子类异常统一记此处 */
    private val log = LogFactory.getLog(this::class)

}
