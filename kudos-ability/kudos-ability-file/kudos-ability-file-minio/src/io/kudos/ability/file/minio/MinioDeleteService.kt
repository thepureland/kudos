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
 * MinIO (S3-compatible) file deletion service.
 *
 * Deletion flow: first call `statObject` to probe whether the object exists (precisely distinguishing
 * NoSuchKey vs NoSuchBucket), then `removeObject` if it exists. That extra RTT for `statObject` is so that
 * `delete()` throws `FILE_NO_EXISTS` when the file does not exist rather than returning false - aligned
 * with the semantics of the file-local version.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MinioDeleteService : IDeleteService {

    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    /** Static client: assembled from `kudos.ability.file.minio.{endpoint,accessKey,secretKey}`. */
    @Autowired
    @Qualifier("minioClient")
    private lateinit var minioClientDefault: MinioClient

    /**
     * Picks the static or dynamic client based on [DeleteFileModel.authServerParam].
     */
    protected fun getMinioClient(model: DeleteFileModel): MinioClient {
        val auth = model.authServerParam ?: return minioClientDefault
        LOG.info("Minio use auth server type:{0}", auth.javaClass.simpleName)
        return requireNotNull(minioClientBuilderFactory.getInstance(auth)) { "MinioClient builder not found" }.build()
    }

    /**
     * @param model request path
     * @return whether deletion succeeded; returns false outright for invalid paths
     * @throws ServiceException when the file does not exist, authentication fails, deletion fails, etc.
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
