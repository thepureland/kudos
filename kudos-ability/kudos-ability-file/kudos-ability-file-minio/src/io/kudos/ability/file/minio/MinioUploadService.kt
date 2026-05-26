package io.kudos.ability.file.minio

import io.kudos.ability.file.common.AbstractUploadService
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.compress.CompressionPipeline
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.minio.client.MinioClientBuilderFactory
import io.kudos.ability.file.minio.init.properties.MinioProperties
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.logger.LogFactory
import io.minio.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import java.io.ByteArrayInputStream


/**
 * MinIO (S3-compatible) file upload service.
 *
 * Client selection strategy: when [UploadFileModel.authServerParam] is empty, reuses the static `minioClient`
 * bean assembled from configuration; when non-empty, constructs a client on the fly via
 * [MinioClientBuilderFactory] based on the authentication type (AK/SK or OAuth token).
 *
 * Before uploading, [createBucket] is invoked to auto-create the bucket - only in static client mode; in
 * dynamic auth mode the bucket is assumed to already exist (dynamic credentials usually lack the
 * `s3:CreateBucket` permission, and even `bucketExists` may return 403).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class MinioUploadService : AbstractUploadService() {

    /** MinIO configuration (endpoint / accessKey / secretKey / publicEndpoint, etc.). */
    @Autowired
    private lateinit var properties: MinioProperties

    /** Used in dynamic authentication scenarios to construct the corresponding type of MinioClient builder. */
    @Autowired
    private lateinit var minioClientBuilderFactory: MinioClientBuilderFactory

    /** Static client: assembled from `kudos.ability.file.minio.{endpoint,accessKey,secretKey}`. */
    @Autowired
    @Qualifier("minioClient")
    private lateinit var minioClientDefault: MinioClient

    /**
     * Decides whether to use the static / dynamic client based on [UploadFileModel.authServerParam].
     *
     * @param model upload request; when `authServerParam` is empty, the default static client is used
     * @throws IllegalArgumentException when no builder is found for the corresponding authentication type
     */
    protected fun getMinioClient(model: UploadFileModel<*>): MinioClient {
        val auth = model.authServerParam ?: return minioClientDefault
        LOG.info("Minio use auth server type:{0}", auth.javaClass.simpleName)
        return requireNotNull(minioClientBuilderFactory.getInstance(auth)) { "MinioClient builder not found" }.build()
    }

    /**
     * Creates the bucket if it does not exist (static client mode only).
     *
     * In dynamic auth scenarios, returns directly - dynamically issued tokens usually lack the
     * `s3:CreateBucket` permission, and even `bucketExists` may return 403. **This requires the business
     * side to pre-create all used buckets in the MinIO console in advance**, otherwise subsequent
     * `putObject` calls will get a `NoSuchBucket` error.
     *
     * Note: the old implementation had a `setPolicy(...)` block here that configured an anonymous read
     * policy, but its `Version` field was written as `"2025-07-02"` (the standard should be the AWS IAM
     * `"2012-10-17"`); MinIO / S3 would reject it. Also, enabling anonymous read is not acceptable for all
     * deployments. This dead code has been removed - bucket policy should be configured explicitly via the
     * MinIO console / `mc` command, avoiding the application layer silently opening public read access.
     */
    protected fun createBucket(minioClient: MinioClient, model: UploadFileModel<*>) {
        if (model.authServerParam != null) {
            return
        }
        val bucketArg = BucketExistsArgs.builder().bucket(model.bucketName).build()
        if (!minioClient.bucketExists(bucketArg)) {
            val makeBucketArgs = MakeBucketArgs.builder()
                .bucket(model.bucketName)
                .build()
            minioClient.makeBucket(makeBucketArgs)
        }
    }

    /**
     * Uploads a file to MinIO/S3.
     *
     * Flow: select client -> create bucket if needed -> run compression pipeline -> `putObject`.
     * Passing -1 for the stream size means unknown; the SDK internally uploads in chunks based on
     * [MinioProperties.partSize] - this is the standard MinIO client usage for handling "streams of
     * unknown size". The returned path includes the bucketName so the business side can store it directly,
     * and the frontend can concatenate publicEndpoint to form a complete URL.
     *
     * @param model upload request
     * @param fileDir relative directory allocated by [AbstractUploadService.dispatchFileDir]
     * @return path in the form of `/{bucket}/{objectKey}`
     * @throws ServiceException on failure, split into two error codes by error type:
     *   - [FileErrorCode.FILE_ACCESS_DENY]: [io.minio.errors.ErrorResponseException] (auth/permission/bucket-not-exists, etc.)
     *   - [FileErrorCode.FILE_ACCESS_ERROR]: other local or network exceptions
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    override fun saveFile(model: UploadFileModel<*>, fileDir: String): String {
        try {
            val minioClient: MinioClient = getMinioClient(model)
            createBucket(minioClient, model)
            val fName = model.fileName?.takeUnless { it.isBlank() }
                ?: "${RandomStringKit.uuid()}.${model.fileSuffix}"
            val fullFilePath = "$fileDir/$fName"

            val inputStream = requireNotNull(model.inputStreamSource) { "inputStreamSource is null" }.inputStream
            val result = CompressionPipeline.compress(inputStream, fullFilePath, model.compressionConfig)

            val uploadStream = result.outputStream?.let { ByteArrayInputStream(it.toByteArray()) } ?: inputStream
            val putArgs = PutObjectArgs.builder()
                .bucket(model.bucketName)
                .`object`(result.getOutputFilePath())
                .stream(uploadStream, -1, properties.partSize)
                .contentType(result.mimeType).build()

            val rs = minioClient.putObject(putArgs)
            // access path includes bucketName, convenient for the business to store directly and the frontend to concatenate the absolute http address
            return "/${rs.bucket()}/${rs.`object`()}"
        } catch (e: io.minio.errors.ErrorResponseException) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_DENY, e)
        } catch (e: Exception) {
            throw ServiceException(FileErrorCode.FILE_ACCESS_ERROR, e)
        }
    }

    /**
     * Returns the MinIO public domain; the frontend concatenates it before the relative path from
     * [saveFile] to form a complete URL.
     *
     * @return [MinioProperties.publicEndpoint]
     * @throws IllegalArgumentException when publicEndpoint is not specified in the configuration
     * @author K
     * @since 1.0.0
     */
    override fun pathPrefix(): String =
        requireNotNull(properties.publicEndpoint) { "publicEndpoint is null" }

    /** Logger. */
    private val LOG = LogFactory.getLog(this::class)

}
