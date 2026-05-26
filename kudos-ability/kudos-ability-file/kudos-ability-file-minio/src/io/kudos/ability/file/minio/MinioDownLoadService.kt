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
 * MinIO (S3-compatible) file download service.
 *
 * Client-selection strategy mirrors [MinioUploadService]: a null
 * [DownloadFileModel.authServerParam] uses the static client; non-null builds one on
 * the fly via [MinioClientBuilderFactory].
 *
 * **Does not perform local path-traversal checks** — in object storage, `filePath`
 * is an S3 key (arbitrary string), not a filesystem path; there is no genuine
 * "escape the base-path" semantics. The `..` check in the `common` layer's
 * `isValid` is more a form of compatibility here than a security guard.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MinioDownLoadService : AbstractDownLoadService() {

    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    /** Static client: assembled from `kudos.ability.file.minio.{endpoint,accessKey,secretKey}`. */
    @Autowired
    @Qualifier("minioClient")
    private lateinit var minioClientDefault: MinioClient

    /**
     * Picks the static or dynamic client based on [DownloadFileModel.authServerParam].
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
