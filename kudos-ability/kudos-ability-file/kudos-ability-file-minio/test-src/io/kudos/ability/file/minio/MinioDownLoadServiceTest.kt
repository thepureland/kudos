package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IDownLoadService
import io.kudos.ability.file.common.IUploadService
import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.code.FileErrorCode
import io.kudos.ability.file.common.entity.DownloadFileModel
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.ability.file.common.entity.UploadFileResult
import io.kudos.base.error.ServiceException
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.test.common.init.EnableKudosTest
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.containers.MinioTestContainer
import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import io.minio.PutObjectArgs
import io.minio.admin.MinioAdminClient
import io.minio.admin.Status
import jakarta.annotation.Resource
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.ByteArrayInputStream
import kotlin.test.Test
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Test cases for MinIO download operations.
 *
 * Notes:
 * 1) Does not use OIDC / STS; interacts with MinIO only via AccessKey/Secret to
 *    avoid 503 / initialization-timing issues.
 * 2) The admin account creates two test users at startup:
 *    - rw_user: read/write on objects in the docs bucket (for upload + default download).
 *    - ro_user: read-only on objects in the docs bucket (used in the "successful
 *      download with a specified AccessKey" scenario).
 * 3) Tests:
 *    - download_with_default_minio_client:            download with the default credentials (configured as rw_user).
 *    - download_with_specify_access_key_without_auth: incorrect credentials, expects FILE_ACCESS_DENY.
 *    - download_with_specify_access_key_with_auth:    use ro_user credentials, expects successful download.
 *
 * @author AI: Codex
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
internal class MinioDownLoadServiceTest {

    @Resource
    private lateinit var downLoadService: IDownLoadService

    @Resource
    private lateinit var uploadService: IUploadService

    private var uploadFileResult: UploadFileResult? = null

    @BeforeAll
    fun before_download() {
        // Fix to the docs bucket so creating a random bucket does not need extra "create-bucket" permission.
        val bucketName = BUCKET
        val tenantId = RandomStringKit.uuid()

        val resourceAsStream =
            MinioDownLoadServiceTest::class.java.classLoader.getResourceAsStream("files/test-file.txt")
                ?: ByteArrayInputStream("hello-minio".toByteArray())

        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.category = "test"
            this.tenantId = tenantId
            this.fileSuffix = ".txt"
            this.inputStreamSource = InputStreamResource(resourceAsStream)
        }
        this.uploadFileResult = uploadService.fileUpload(uploadFileModel)
        require(!uploadFileResult?.filePath.isNullOrBlank()) { "upload failed: filePath is null/blank" }
    }

    /**
     * Downloads using the configured username/password (default: rw_user).
     */
    @Test
    fun download_with_default_minio_client() {
        val uploaded = requireNotNull(uploadFileResult) { "upload not completed" }
        val path = requireNotNull(uploaded.filePath) { "filePath" }
        val downloadFileModel = DownloadFileModel.from(path)
        val rs = requireNotNull(downLoadService.download(downloadFileModel)) { "download returned null" }
        assertTrue(rs.isNotEmpty(), "default download should return bytes")
    }

    /**
     * Use an incorrect AccessKey/Secret; expects rejection (FILE_ACCESS_DENY).
     */
    @Test
    fun download_with_specify_access_key_without_auth() {
        val uploaded = requireNotNull(uploadFileResult) { "upload not completed" }
        val path = requireNotNull(uploaded.filePath) { "filePath" }
        val downloadFileModel = DownloadFileModel.from(path)

        // Intentionally use incorrect accessKey/secretKey.
        downloadFileModel.authServerParam = AccessKeyServerParam("bad", "bad")

        try {
            downLoadService.download(downloadFileModel)
            kotlin.test.fail("expected ServiceException(FileErrorCode.FILE_ACCESS_DENY)")
        } catch (e: ServiceException) {
            assertSame(e.errorCode, FileErrorCode.FILE_ACCESS_DENY)
        }
    }

    /**
     * Downloads using the read-only user ro_user's AccessKey/Secret.
     * Expects success (ro_user's policy only allows GetObject).
     */
    @Test
    fun download_with_specify_access_key_with_auth() {
        val uploaded = requireNotNull(uploadFileResult) { "upload not completed" }
        val path = requireNotNull(uploaded.filePath) { "filePath" }
        val downloadFileModel = DownloadFileModel.from(path)
        downloadFileModel.authServerParam = AccessKeyServerParam(RO_USER, RO_USER_SECRET)

        val rs = requireNotNull(downLoadService.download(downloadFileModel)) { "download returned null" }
        assertTrue(rs.isNotEmpty(), "download with ro_user should return bytes")
    }

    companion object {
        // ---- Root administrator ----
        private const val ROOT_USER = "admin"
        private const val ROOT_USER_SECRET = "12345678"

        // ---- Read/write account (default credentials, used for upload + default download) ----
        private const val RW_USER = "rw_user"
        private const val RW_USER_SECRET = "rw_user_secret"

        // ---- Read-only account (used for "successful download with specified credentials") ----
        private const val RO_USER = "ro_user"
        private const val RO_USER_SECRET = "ro_user_secret"

        // Fixed bucket name for precise authorization in policies.
        private const val BUCKET = "docs"

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            // Start the container and register the endpoint properties.
            val minio = MinioTestContainer.startIfNeeded(registry)
            val address = "http://${minio.ports.first().ip}:${minio.ports.first().publicPort}"

            // 1) Use root credentials to prepare the environment: create the docs bucket + two policies + two users + bind policies.
            prepareMinioForTests(address)

            // 2) Spring environment default credentials: set to rw_user (used by @BeforeAll's upload action and the default download).
            registry.add("kudos.ability.file.minio.endpoint") { address }
            registry.add("kudos.ability.file.minio.public-endpoint") { address }
            registry.add("kudos.ability.file.minio.accessKey") { RW_USER }
            registry.add("kudos.ability.file.minio.secretKey") { RW_USER_SECRET }
        }

        /**
         * Prepares the test environment using the root administrator:
         * - Creates the docs bucket (if it does not exist).
         * - Creates two policies: rw-policy (read/write) and ro-policy (read-only).
         * - Creates two users: rw_user and ro_user.
         * - Binds the respective policies to the users.
         */
        private fun prepareMinioForTests(address: String) {
            // ---- Data-plane client (also used by root to create buckets / put objects) ----
            val rootClient = MinioClient.builder().endpoint(address).credentials(ROOT_USER, ROOT_USER_SECRET).build()

            // Ensure the bucket exists.
            val exists = rootClient.bucketExists(BucketExistsArgs.builder().bucket(BUCKET).build())
            if (!exists) {
                rootClient.makeBucket(MakeBucketArgs.builder().bucket(BUCKET).build())
            }

            // ---- Admin-plane client (creates policies/users/authorizations) ----
            val admin = MinioAdminClient.builder().endpoint(address).credentials(ROOT_USER, ROOT_USER_SECRET).build()

            // Read/write policy: allow docs bucket metadata + read/write on docs/* objects (for upload/download).
            val rwPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "BucketMeta",
                      "Effect": "Allow",
                      "Action": [ "s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketVersions" ],
                      "Resource": [ "arn:aws:s3:::$BUCKET" ]
                    },
                    {
                      "Sid": "ObjectRW",
                      "Effect": "Allow",
                      "Action": [
                        "s3:GetObject","s3:PutObject","s3:DeleteObject",
                        "s3:GetObjectVersion","s3:DeleteObjectVersion","s3:AbortMultipartUpload"
                      ],
                      "Resource": [ "arn:aws:s3:::$BUCKET/*" ]
                    }
                  ]
                }
            """.trimIndent()

            // Read-only policy: allow docs bucket metadata + reads on docs/* objects.
            val roPolicy = """
                {
                  "Version": "2012-10-17",
                  "Statement": [
                    {
                      "Sid": "BucketMeta",
                      "Effect": "Allow",
                      "Action": [ "s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketVersions" ],
                      "Resource": [ "arn:aws:s3:::$BUCKET" ]
                    },
                    {
                      "Sid": "ObjectRead",
                      "Effect": "Allow",
                      "Action": [ "s3:GetObject","s3:GetObjectVersion" ],
                      "Resource": [ "arn:aws:s3:::$BUCKET/*" ]
                    }
                  ]
                }
            """.trimIndent()

            // Create/overwrite policies (addCannedPolicy may throw on conflict; runCatching ignores it).
            runCatching { admin.addCannedPolicy("rw-policy", rwPolicy) }
            runCatching { admin.addCannedPolicy("ro-policy", roPolicy) }

            // Create users (ignore exceptions if they already exist).
            runCatching { admin.addUser(RW_USER, Status.ENABLED, RW_USER_SECRET, null, null) }
            runCatching { admin.addUser(RO_USER, Status.ENABLED, RO_USER_SECRET, null, null) }

            // Bind policies.
            admin.setPolicy(RW_USER, false, "rw-policy")
            admin.setPolicy(RO_USER, false, "ro-policy")

            // We can also pre-place an object for download tests that do not depend on the upload path (not strictly required).
            runCatching {
                rootClient.putObject(
                    PutObjectArgs.builder()
                        .bucket(BUCKET)
                        .`object`("seed/__hello__.txt")
                        .stream(ByteArrayInputStream("seed".toByteArray()), -1, 5 * 1024 * 1024)
                        .contentType("text/plain")
                        .build()
                )
            }
        }
    }
}
