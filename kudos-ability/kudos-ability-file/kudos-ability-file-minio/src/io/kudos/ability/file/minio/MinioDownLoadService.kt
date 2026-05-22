package io.kudos.ability.file.minio

import io.kudos.ability.file.common.AbstractDownLoadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.minio.GetObjectArgs
import io.minio.MinioClient
import java.io.InputStream
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier


/**
 * MinIO（S3 兼容）文件下载服务。
 *
 * 客户端选取策略与 [MinioUploadService] 一致：[DownloadFileModel.authServerParam] 为空走静态客户端，
 * 非空走 [MinioClientBuilderFactory] 现场构造。
 *
 * **不做本地路径穿越检查**——对象存储里 "filePath" 是 S3 key（任意字符串），不是
 * 文件系统路径，没有真正的"跳出 base-path"语义。`common` 层 `isValid` 的 `..` 检查
 * 在这里更多是兼容形式而非安全屏障。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MinioDownLoadService : AbstractDownLoadService() {

    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    /** 静态客户端：来自 `kudos.ability.file.minio.{endpoint,accessKey,secretKey}` 装配。 */
    @Autowired
    @Qualifier("minioClient")
    private lateinit var minioClientDefault: MinioClient

    /**
     * 按 [DownloadFileModel.authServerParam] 决定使用静态 / 动态客户端。
     */
    protected fun getMinioClient(model: DownloadFileModel<*>): MinioClient {
        val auth = model.authServerParam ?: return minioClientDefault
        LOG.info("Minio use auth server type:{0}", auth.javaClass.simpleName)
        return requireNotNull(minioClientBuilderFactory.getInstance(auth)) { "MinioClient builder not found" }.build()
    }

    override fun readFileToByte(downloadFileModel: DownloadFileModel<*>): ByteArray? {
        val getArgs = GetObjectArgs.builder()
            .bucket(downloadFileModel.bucketName)
            .`object`(downloadFileModel.filePath)
            .build()
        return try {
            getMinioClient(downloadFileModel).getObject(getArgs).use { it.readAllBytes() }
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    override fun readFileToStream(downloadFileModel: DownloadFileModel<*>): InputStream {
        val getArgs = GetObjectArgs.builder()
            .bucket(downloadFileModel.bucketName)
            .`object`(downloadFileModel.filePath)
            .build()
        return try {
            getMinioClient(downloadFileModel).getObject(getArgs)
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    private val LOG = LogFactory.getLog(this::class)

}
