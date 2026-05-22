package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IDeleteService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DeleteFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.base.error.ServiceException
import io.kudos.base.logger.LogFactory
import io.minio.MinioClient
import io.minio.RemoveObjectArgs
import io.minio.StatObjectArgs
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier


/**
 * MinIO（S3 兼容）文件删除服务。
 *
 * 删除流程：先调用 `statObject` 探测对象是否存在（精确区分 NoSuchKey vs NoSuchBucket），
 * 存在再 `removeObject`。`statObject` 那一次额外 RTT 是为了让 `delete()` 在文件不存在时
 * 抛 `FILE_NO_EXISTS` 而不是返回 false——和 file-local 版语义对齐。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MinioDeleteService : IDeleteService {

    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    /** 静态客户端：来自 `kudos.ability.file.minio.{endpoint,accessKey,secretKey}` 装配。 */
    @Autowired
    @Qualifier("minioClient")
    private lateinit var minioClientDefault: MinioClient

    /**
     * 按 [DeleteFileModel.authServerParam] 决定使用静态 / 动态客户端。
     */
    protected fun getMinioClient(model: DeleteFileModel): MinioClient {
        val auth = model.authServerParam ?: return minioClientDefault
        LOG.info("Minio use auth server type:{0}", auth.javaClass.simpleName)
        return requireNotNull(minioClientBuilderFactory.getInstance(auth)) { "MinioClient builder not found" }.build()
    }

    /**
     * @param model 请求路径
     * @return 删除是否成功；不合法路径直接 false
     * @throws ServiceException 文件不存在 / 鉴权失败 / 删除失败等
     */
    override fun delete(model: DeleteFileModel): Boolean {
        if (!isValid(model)) {
            return false
        }

        try {
            val client = getMinioClient(model)
            val objectArgs = StatObjectArgs.builder()
                .bucket(model.bucketName)
                .`object`(model.filePath)
                .build()
            client.statObject(objectArgs)
            client.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(model.bucketName)
                    .`object`(model.filePath)
                    .build()
            )
            return true
        } catch (e: io.minio.errors.ErrorResponseException) {
            val code = e.errorResponse().code()
            when (code) {
                "NoSuchKey", "NoSuchBucket" -> throw ServiceException(FileErrorCode.FILE_NO_EXISTS, e)
                "InvalidAccessKeyId" -> throw ServiceException(FileErrorCode.FILE_INVALID_ACCESS_KEY)
                "AccessDenied" -> throw ServiceException(FileErrorCode.FILE_ACCESS_DENY)
                else -> throw ServiceException(FileErrorCode.FILE_DELETE_FAIL, e)
            }
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_DELETE_FAIL, e)
        }
    }

    private val LOG = LogFactory.getLog(this::class)

}
