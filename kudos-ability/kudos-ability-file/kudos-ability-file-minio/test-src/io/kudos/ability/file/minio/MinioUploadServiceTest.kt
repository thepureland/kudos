package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IUploadService
import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MinioTestContainer
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.admin.MinioAdminClient
import io.minio.admin.Status
import jakarta.annotation.Resource
import org.junit.jupiter.api.TestInstance
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Test cases for MinIO upload operations.
 *
 * Notes:
 * 1) Does not use OIDC / STS; interacts with MinIO only via AccessKey/Secret to
 *    avoid 503 / initialization-timing issues.
 * 2) The admin account creates two test users at startup:
 *    - uploader:         global read/write + create-bucket (used as Spring's default credentials).
 *    - upload_only_user: upload-only + bucket metadata operations + create-bucket
 *      (used in the "successful upload with a specified AccessKey" scenario).
 * 3) To avoid "The specified bucket does not exist", all tests use the fixed
 *    bucket `docs`, and the initialization uses the root account to ensure `docs`
 *    is created (same idea as MinioDeleteServiceTest).
 *
 * Tests:
 * - fileUpload_with_default_minio_client:            default credentials upload successfully (bucket=docs).
 * - fileUpload_with_specify_access_key_without_auth: incorrect credentials are rejected (FILE_ACCESS_DENY).
 * - fileUpload_with_specify_access_key_with_auth:    upload-capable account uploads successfully (bucket=docs).
 *
 * @author AI: Codex
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
internal class MinioUploadServiceTest {

    @Resource
    private lateinit var uploadService: IUploadService

    /**
     * Test conditions
     * 1) minioClient uses the default admin-level user.
     *
     *
     * Test parameters:
     * 1) Fixed bucket name = docs (created during initialization).
     * 2) Random tenant id.
     */
    @Test
    fun fileUpload_with_default_minio_client() {
        val bucketName = "docs"
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: "hello-minio".byteInputStream()

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }

        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        val path = requireNotNull(uploadFileResult.filePath) { "filePath" }
        assertTrue(path.startsWith("/$bucketName"))
        assertTrue(path.contains(tenantId))
        assertTrue(path.endsWith(".txt"))
    }

    /**
     * Test conditions
     * 1) minioClient uses a non-existent test user.
     *
     *
     * Test parameters:
     * 1) Fixed bucket name = docs (created during initialization).
     * 2) Random tenant id.
     */
    @Test
    fun fileUpload_with_specify_access_key_without_auth() {
        val bucketName = "docs"
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: "hello-minio".byteInputStream()

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }

        // Intentionally use incorrect accessKey/secretKey.
        uploadFileModel.authServerParam = AccessKeyServerParam("test", "test")

        val se = assertFailsWith<ServiceException> { uploadService.fileUpload(uploadFileModel) }
        assertEquals(se.errorCode, FileErrorCode.FILE_ACCESS_DENY)
    }

    /**
     * Test conditions
     * 1) Use an AccessKey/Secret with upload permission (upload_only_user).
     *
     *
     * Test parameters:
     * 1) Fixed bucket name = docs (created during initialization).
     * 2) Random tenantId.
     */
    @Test
    fun fileUpload_with_specify_access_key_with_auth() {
        val bucketName = "docs"
        val tenantId = RandomStringKit.uuid()
        val resourceAsStream =
            MinioUploadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: "hello-minio".byteInputStream()

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }

        // Use the "upload-only user"'s AccessKey/Secret.
        uploadFileModel.authServerParam = AccessKeyServerParam(UPLOAD_ONLY_USER, UPLOAD_ONLY_USER_SECRET)

        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        val path = requireNotNull(uploadFileResult.filePath) { "filePath" }
        assertTrue(path.startsWith("/$bucketName"))
        assertTrue(path.endsWith(".txt"))
    }

    companion object {
        // ---- Root administrator (the default root from MinioTestContainer) ----
        private const val ROOT_USER = "admin"
        private const val ROOT_USER_SECRET = "12345678"

        // ---- Default uploader user (used for Spring's default configuration) ----
        private const val DEFAULT_UPLOADER = "uploader"
        private const val DEFAULT_UPLOADER_SECRET = "uploader_secret"

        // ---- Upload-only user (used for "successful upload with a specified AccessKey") ----
        private const val UPLOAD_ONLY_USER = "upload_only_user"
        private const val UPLOAD_ONLY_USER_SECRET = "upload_only_user_secret"

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            val minio = MinioTestContainer.startIfNeeded(registry)
            val endpoint = "http://${minio.ports.first().ip}:${minio.ports.first().publicPort}"

            // Make Spring use the default uploader user we created.
            registry.add("kudos.ability.file.minio.endpoint") { endpoint }
            registry.add("kudos.ability.file.minio.public-endpoint") { endpoint }
            registry.add("kudos.ability.file.minio.accessKey") { DEFAULT_UPLOADER }
            registry.add("kudos.ability.file.minio.secretKey") { DEFAULT_UPLOADER_SECRET }

            // 1) First ensure the fixed bucket docs exists (avoids "The specified bucket does not exist").
            val rootClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()
            val bucket = "docs"
            val exists = rootClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
            )
            if (!exists) {
                rootClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            }

            // 2) Use the root administrator to create policies and users.
            val admin = MinioAdminClient.builder()
                .endpoint(endpoint)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()

            // Default uploader user: full read/write on all buckets + create-bucket (room to extend).
            val uploaderPolicy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetBucketLocation",
                    "s3:ListBucket",
                    "s3:ListBucketVersions",
                    "s3:CreateBucket"
                  ],
                  "Resource": [ "arn:aws:s3:::*" ]
                },
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetObject",
                    "s3:PutObject",
                    "s3:DeleteObject",
                    "s3:GetObjectVersion",
                    "s3:DeleteObjectVersion",
                    "s3:AbortMultipartUpload"
                  ],
                  "Resource": [ "arn:aws:s3:::*/*" ]
                }
              ]
            }
            """.trimIndent()

            // Upload-only user: allows create-bucket + bucket metadata operations + PutObject (this test only needs docs to already exist, but the policy is retained).
            val uploadOnlyPolicy = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetBucketLocation",
                    "s3:ListBucket",
                    "s3:ListBucketVersions",
                    "s3:CreateBucket"
                  ],
                  "Resource": [ "arn:aws:s3:::*" ]
                },
                {
                  "Effect": "Allow",
                  "Action": [
                    "s3:PutObject",
                    "s3:AbortMultipartUpload"
                  ],
                  "Resource": [ "arn:aws:s3:::*/*" ]
                }
              ]
            }
            """.trimIndent()

            // Create policies (ignore exceptions if they already exist).
            runCatching { admin.addCannedPolicy("uploader-rw-all", uploaderPolicy) }
            runCatching { admin.addCannedPolicy("upload-only-all", uploadOnlyPolicy) }

            // Create users (ignore exceptions if they already exist).
            runCatching {
                admin.addUser(DEFAULT_UPLOADER, Status.ENABLED, DEFAULT_UPLOADER_SECRET, null, null)
            }
            runCatching {
                admin.addUser(UPLOAD_ONLY_USER, Status.ENABLED, UPLOAD_ONLY_USER_SECRET, null, null)
            }

            // Bind policies.
            admin.setPolicy(DEFAULT_UPLOADER, false, "uploader-rw-all")
            admin.setPolicy(UPLOAD_ONLY_USER, false, "upload-only-all")
        }
    }
}
