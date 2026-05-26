package io.kudos.ability.file.minio

import io.kudos.ability.file.common.IDeleteService
import io.kudos.ability.file.common.IUploadService
import io.kudos.ability.file.common.auth.AccessKeyServerParam
import io.kudos.ability.file.common.entity.DeleteFileModel
import io.kudos.ability.file.common.entity.UploadFileModel
import io.kudos.base.error.ServiceException
import io.kudos.base.io.FileKit
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
import org.junit.jupiter.api.TestInstance
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.InputStreamSource
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.io.ByteArrayInputStream
import java.io.File
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test cases for MinIO delete operations.
 *
 * Design goals:
 * 1. Cover path validity checks (invalid paths return false directly, no exception thrown).
 * 2. Cover the "resource not found" exception branch (throws ServiceException).
 * 3. Cover the successful delete flow using "default credentials" (upload first, then delete).
 * 4. Cover the successful delete flow using a "specified AccessKey/Secret" (isolated from default credentials).
 *
 * Test environment setup (see companion object#property):
 * - Start MinIO via Testcontainers (MinioTestContainer).
 * - Use root (admin/12345678) to:
 *   a) Ensure the fixed bucket docs exists (avoiding "The specified bucket does not exist").
 *   b) Preload an object path/to/__temp__.txt (handy for manual local inspection; does not affect test logic).
 * - Use the MinIO Admin API to create a delete-only account delete_only_user and bind the "delete-policy".
 * - Switch the Spring test environment's default credentials to delete_only_user (to cover the "default credentials successful delete" scenario).
 *
 * Important notes:
 * - To avoid OIDC/STS initialization timing and 503 issues, this test only uses AccessKey/Secret, not the token-exchange flow.
 * - Bucket-level metadata actions such as "GetBucketLocation" must not have prefix conditions, otherwise list-bucket / locate fails (called out separately in the policy).
 *
 * @author AI: Codex
 * @author water
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@EnabledIfDockerInstalled
internal class MinioDeleteServiceTest {

    /** Business-side upload service, used to upload an object first so it can later be deleted. */
    @Resource
    private lateinit var uploadService: IUploadService

    /** Business-side delete service, the subject under test. */
    @Resource
    private lateinit var deleteService: IDeleteService

    /**
     * Scenario 1: invalid paths must be safely rejected.
     *
     * Expectations:
     * - Passing bucketName/filePath containing traversal segments like ".." causes delete() to return false (no interruption, no exception).
     * - Demonstrates the business-side pre-validation against injection / path traversal.
     */
    @Test
    fun delete_path_invalid() {
        val model = DeleteFileModel()

        // Invalid bucketName: just ".."
        model.bucketName = ".."
        assertFalse(deleteService.delete(model))

        // Invalid bucketName: contains path traversal.
        model.bucketName = "a/../b"
        assertFalse(deleteService.delete(model))

        // Invalid filePath: just ".."
        model.filePath = ".."
        assertFalse(deleteService.delete(model))

        // Invalid filePath again: just ".." (repeat check).
        model.filePath = ".."
        assertFalse(deleteService.delete(model))

        // Invalid filePath: path traversal.
        model.filePath = "a/../b"
        assertFalse(deleteService.delete(model))
    }

    /**
     * Scenario 2: resource not found (missing bucket/object) must throw a business exception.
     *
     * Expectations:
     * - When neither bucket nor filePath is provided, or when the bucket/object does not exist, delete() throws ServiceException.
     * - Demonstrates that the "resource not found" error path is uniformly handled at the business layer.
     */
    @Test
    fun delete_path_no_exist() {
        val model = DeleteFileModel()

        // Neither bucket nor path filled in: throws.
        assertFailsWith<ServiceException> { deleteService.delete(model) }

        // Bucket does not exist: throws.
        model.bucketName = "no_exist"
        assertFailsWith<ServiceException> { deleteService.delete(model) }

        // Bucket does not exist plus a path: still throws.
        model.bucketName = "no_exist"
        model.filePath = "_"
        assertFailsWith<ServiceException> { deleteService.delete(model) }
    }

    /**
     * Scenario 3: successful delete using "default credentials".
     *
     * Steps:
     * 1) Create a temp file under the local ~/.kudos directory.
     * 2) Upload it to MinIO via uploadService (bucket: docs).
     * 3) Delete the local temp file (cleanup).
     * 4) Call deleteService.delete() to delete the object from MinIO; expect true.
     *
     * Notes:
     * - The default credentials are set to delete_only_user/delete_only_user_secret via @DynamicPropertySource.
     * - The user's policy allows read/write/delete on objects in the docs bucket (see the policy below).
     */
    @Test
    fun delete_success_default_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = "${System.getProperty("user.home")}/.kudos/$filePath"

        // 1) Create the local temp file (create an empty file if it does not exist).
        val localFile = File(localPath)
        FileKit.touch(localFile)

        // 2) Build the upload model and upload to MinIO.
        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.fileSuffix = "txt"
            this.inputStreamSource = FileSystemResource(localFile)
        }
        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertFalse(uploadFileResult.filePath.isNullOrBlank())

        // 3) Delete the local temp file (cleanup only; does not affect the remote object).
        FileKit.forceDelete(localFile)

        // 4) Delete the object from MinIO.
        val path = requireNotNull(uploadFileResult.filePath) { "filePath" }
        val model = DeleteFileModel.from(path)
        assertTrue(deleteService.delete(model))
    }

    /**
     * Scenario 4: successful delete with an explicitly specified AccessKey/Secret.
     *
     * Steps are similar to the previous case, with the following difference:
     * - Explicitly specify the account delete_only_user/delete_only_user_secret on the DeleteFileModel.
     * - This covers the behavior path of "the caller performs the delete with different credentials".
     *
     * Note:
     * - An earlier comment stated that "the user must be created and authorized manually", but this test now performs that automatically in @DynamicPropertySource.
     */
    @Test
    fun delete_success_specify_auth_access_key() {
        val bucketName = "docs"
        val filePath = "__temp__.txt"
        val localPath = "${System.getProperty("user.home")}/.kudos/$filePath"

        // 1) Create the local temp file.
        val localFile = File(localPath)
        FileKit.touch(localFile)

        // 2) Upload to MinIO.
        val uploadFileModel = UploadFileModel<InputStreamSource>().apply {
            this.bucketName = bucketName
            this.fileSuffix = "txt"
            this.inputStreamSource = FileSystemResource(localFile)
        }
        val uploadFileResult = uploadService.fileUpload(uploadFileModel)
        assertFalse(uploadFileResult.filePath.isNullOrBlank())

        // 3) Delete the local file.
        FileKit.forceDelete(localFile)

        // 4) Use the "delete-only user" credentials and delete the MinIO object.
        val path = requireNotNull(uploadFileResult.filePath) { "filePath" }
        val model = DeleteFileModel.from(path)
        model.authServerParam = AccessKeyServerParam(DELETE_ONLY_USER, DELETE_ONLY_USER_SECRET)
        assertTrue(deleteService.delete(model))
    }

    /**
     * Test environment bootstrap (runs as soon as the Spring Context is created):
     *
     * - Start or reuse the MinIO Testcontainer.
     * - Set Spring's default access credentials to delete_only_user (overriding the default behavior).
     * - Use root (admin/12345678) to ensure:
     *   a) The docs bucket exists.
     *   b) An object path/to/__temp__.txt is preloaded (handy for inspection).
     * - Via the MinIO Admin API:
     *   a) Create delete-policy (allowing read/write/delete on docs objects plus bucket metadata operations).
     *   b) Create delete_only_user and bind the policy.
     *
     * Note:
     * - "s3:GetBucketLocation / ListBucket / ListBucketVersions" act on the bucket-level resource (arn:aws:s3:::docs);
     *   do not attach object-level prefix conditions to them, or list-bucket / locate will break.
     */
    companion object {
        private const val ROOT_USER = "admin"
        private const val ROOT_USER_SECRET = "12345678"
        private const val DELETE_ONLY_USER = "delete_only_user"
        private const val DELETE_ONLY_USER_SECRET = "delete_only_user_secret"

        @JvmStatic
        @DynamicPropertySource
        fun property(registry: DynamicPropertyRegistry) {
            // 1) Start or reuse the MinIO container and switch Spring's default credentials to the "delete-only user".
            val minio = MinioTestContainer.startIfNeeded(registry)
            registry.add("kudos.ability.file.minio.accessKey") { DELETE_ONLY_USER }
            registry.add("kudos.ability.file.minio.secretKey") { DELETE_ONLY_USER_SECRET }

            // 2) Compute the externally accessible address.
            val address = "http://${minio.ports.first().ip}:${minio.ports.first().publicPort}"
            val bucket = "docs"     // Fixed bucket (avoids the "bucket does not exist" issue).
            val prefix = "path/to/" // Example prefix (for grouping/inspection); note the trailing slash.

            // 3) Use the root account to ensure the bucket exists, and insert an example object (handy for manual inspection).
            val rootClient = MinioClient.builder()
                .endpoint(address)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()

            if (!rootClient.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                rootClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
            }

            rootClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucket)
                    .`object`("${prefix}__temp__.txt")
                    .stream(ByteArrayInputStream("__temp__".toByteArray()), -1, 5 * 1024 * 1024)
                    .contentType("text/plain")
                    .build()
            )

            // 4) Use the admin API to create the policy, user, and bind the policy.
            val admin = MinioAdminClient.builder()
                .endpoint(address)
                .credentials(ROOT_USER, ROOT_USER_SECRET)
                .build()

            // Note: bucket-level actions (GetBucketLocation/ListBucket/ListBucketVersions) bind to the bucket ARN (no /*).
            // Object-level actions (Get/Put/DeleteObject, etc.) bind to the object ARN (with /*).
            val policyJson = """
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Sid": "BucketMetaAndList",
                  "Effect": "Allow",
                  "Action": [ "s3:GetBucketLocation", "s3:ListBucket", "s3:ListBucketVersions" ],
                  "Resource": [ "arn:aws:s3:::docs" ]
                },
                {
                  "Sid": "ObjectRWAll",
                  "Effect": "Allow",
                  "Action": [
                    "s3:GetObject", "s3:PutObject", "s3:DeleteObject",
                    "s3:GetObjectVersion", "s3:DeleteObjectVersion",
                    "s3:AbortMultipartUpload"
                  ],
                  "Resource": [ "arn:aws:s3:::docs/*" ]
                }
              ]
            }
            """.trimIndent()

            // Create the policy (throws on conflict if it already exists; the original logic is preserved here. For idempotency, wrap in runCatching).
            admin.addCannedPolicy("delete-policy", policyJson)

            // Create the user (ignore the conflict exception if it already exists).
            runCatching {
                admin.addUser(
                    DELETE_ONLY_USER,
                    Status.ENABLED,
                    DELETE_ONLY_USER_SECRET,
                    null,
                    null
                )
            }

            // Bind the policy to the user.
            admin.setPolicy(DELETE_ONLY_USER, false, "delete-policy")
        }
    }
}
